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

package org.apache.storm.eventhubs.spout;

import java.util.HashMap;
import java.util.Map;

import com.microsoft.azure.eventhubs.EventData;

public class EventDataMock implements EventData {
	private static final long serialVersionUID = -1362022940535977850L;
	private SystemProperties sysprops;
	private byte[] data;

	
	public EventDataMock(byte[] data, HashMap<String, Object> map) {		
		this.sysprops = new SystemProperties(map);
		this.data = data;

		System.out.println("OFF: " + sysprops.getOffset());
		System.out.println("SEQ: " + sysprops.getSequenceNumber());
	}

	@Override
	public Object getObject() {
		return null;
	}

	@Override
	public byte[] getBytes() {
		return data;
	}

	@Override
	public Map<String, Object> getProperties() {
		return null;
	}

	@Override
	public SystemProperties getSystemProperties() {
		return sysprops;
	}
}
