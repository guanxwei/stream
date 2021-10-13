/*
 * Copyright (C) 2021 guanxiongwei
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.stream.extension.persist;

import java.util.Collection;

/**
 * 
 * @author guanxiongwei
 *
 */
public interface DelayedQueueStorage {

    /**
     * Push the item to the tail of the queue.
     * @param queueName Target queue.
     * @param item Item to be added
     */
    public void push(final String queueName, final String item);

    /**
     * Remove the item from the queue.
     * @param queueName Queue name.
     * @param item Target item.
     * @return <code>true</code> item removed, otherwise <code>false</code>
     */
    public boolean remove(final String queueName, final String item);

    /**
     * Get top items.
     * @param queueName Delay queue name.
     * @param end End index.
     * @return Delayed items.
     */
    public Collection<String> pop(final String queueName, final int end);
}
