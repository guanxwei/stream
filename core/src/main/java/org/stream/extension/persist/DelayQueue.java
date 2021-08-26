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
 * Delay queue.
 * @author weiguanxiong.
 *
 */
public interface DelayQueue {

    /**
     * Get enqueued items.
     * @param queueName Delay queue name.
     * @param end End time.
     * @return Delayed items.
     */
    Collection<String> getItems(final String queueName, final double end);

    /**
     * Delete the enqueued item.
     * @param queueName Delay queue name.
     * @param item Target item to be deleted.
     */
    void deleteItem(final String queueName, final String item);

    /**
     * Put an item into the delay queue.
     * @param queueName Delay queue name.
     * @param item Target item to be deleted.
     * @param delayTime delay time.
     */
    void enqueue(final String queueName, final String item, final double delayTime);
}
