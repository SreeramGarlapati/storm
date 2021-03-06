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

import java.util.List;

/**
 * Contracts for assigning and handling workers to read/write data from EventHub
 * partitions.
 */
public interface IPartitionCoordinator {

    /**
     * Retrieve list of {@link IPartitionManager} instances for the target EventHub.
     *
     * @return List of {@link IPartitionManager} instances
     */
    List<IPartitionManager> getMyPartitionManagers();

    /**
     * Retrieves {@link IPartitionManager} instance for the EventHub partition
     * identified by specified id.
     *
     * @param partitionId partition id
     * @return {@link IPartitionManager} implementation
     */
    IPartitionManager getPartitionManager(String partitionId);
}
