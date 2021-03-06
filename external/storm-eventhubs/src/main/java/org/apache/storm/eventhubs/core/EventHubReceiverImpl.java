/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/
package org.apache.storm.eventhubs.core;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.microsoft.azure.eventhubs.EventHubException;
import org.apache.storm.metric.api.CountMetric;
import org.apache.storm.metric.api.MeanReducer;
import org.apache.storm.metric.api.ReducedMetric;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;
import com.microsoft.azure.eventhubs.EventData;
import com.microsoft.azure.eventhubs.EventHubClient;
import com.microsoft.azure.eventhubs.PartitionReceiver;

/**
 * {@link PartitionReceiver} based implementation to receives messages from a
 * given Eventhub partition
 *
 */
public class EventHubReceiverImpl implements IEventHubReceiver {
	private static final Logger logger = LoggerFactory.getLogger(EventHubReceiverImpl.class);

	private final Iterable<EventData> emptyEventBatch = new LinkedList<>();

	private final EventHubConfig eventHubConfig;
	private final String partitionId;

	private PartitionReceiver receiver;
	private EventHubClient ehClient;
	private ExecutorService executorService;

	private ReducedMetric receiveApiLatencyMean;
	private CountMetric receiveApiCallCount;
	private CountMetric receiveMessageCount;

	/**
	 * Creates a new instance based on provided configuration. The connection, and
	 * consumer group settings are read from the passed in EventHubConfig instance.
	 * 
	 * @param config
	 *            Connection, consumer group settings
	 * @param partitionId
	 *            target partition id to connect to and read from
	 */
	public EventHubReceiverImpl(EventHubConfig config, String partitionId) {
		this.partitionId = partitionId;
		this.eventHubConfig = config;

		receiveApiLatencyMean = new ReducedMetric(new MeanReducer());
		receiveApiCallCount = new CountMetric();
		receiveMessageCount = new CountMetric();
	}

	@Override
	public void open(IEventFilter filter) throws IOException, EventHubException {
		long start = System.currentTimeMillis();
		logger.debug(String.format("Creating EventHub Client: partitionId: %s, filter value:%s, prefetchCount: %s",
				partitionId, filter.toString(), String.valueOf(eventHubConfig.getPrefetchCount())));
		executorService = Executors.newSingleThreadExecutor();
		ehClient = EventHubClient.createSync(eventHubConfig.getConnectionString(), executorService);
		receiver = PartitionReceiverFactory.createReceiver(ehClient, filter, eventHubConfig, partitionId);
		receiver.setPrefetchCount(eventHubConfig.getPrefetchCount());
		logger.debug("created eventhub receiver, time taken(ms): " + (System.currentTimeMillis() - start));
	}

	@Override
	public void close() {

		try {
			if (receiver != null) {
				receiver.closeSync();
			}
		} catch (EventHubException e) {
			logger.warn("Exception occurred while closing PartitionReceiver: " + e.toString());
		}

		try {
			if (ehClient != null) {
				ehClient.closeSync();
			}
		} catch (EventHubException e) {
			logger.warn("Exception occurred while closing EventHubClient: " + e.toString());
		}

		executorService.shutdown();

		try {
			executorService.awaitTermination(2, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			logger.warn("Exception occurred while terminating ExecutorService: " + e.toString());
		}

		logger.info("closed eventhub receiver: partitionId=" + partitionId);
		ehClient = null;
		receiver = null;
		executorService = null;
	}

	@Override
	public boolean isOpen() {
		return (receiver != null);
	}

	@Override
	public Iterable<EventData> receive() {
		return receive(eventHubConfig.getReceiveEventsMaxCount());
	}

	@Override
	public Iterable<EventData> receive(int batchSize) {
		final long start = System.currentTimeMillis();
		final Iterable<EventData> receivedEvents;

		try {
			receivedEvents = receiver.receiveSync(batchSize);

			if (receivedEvents != null) {
				final long end = System.currentTimeMillis();
				final long millis = (end - start);
				receiveApiLatencyMean.update(millis);
				receiveApiCallCount.incr();

				logger.debug("Batchsize: " + batchSize + ", Received event count: " + Iterables.size(receivedEvents));
			}
		} catch (EventHubException e) {
			logger.error("Exception occured during receive: " + e.toString());
			return null;
		}

		return receivedEvents == null ? this.emptyEventBatch : receivedEvents;
	}

	@Override
	public Map<String, Object> getMetricsData() {
		final Map<String, Object> ret = new HashMap<String, Object>();
		ret.put(partitionId + "/receiveApiLatencyMean", receiveApiLatencyMean.getValueAndReset());
		ret.put(partitionId + "/receiveApiCallCount", receiveApiCallCount.getValueAndReset());
		ret.put(partitionId + "/receiveMessageCount", receiveMessageCount.getValueAndReset());
		return ret;
	}
}
