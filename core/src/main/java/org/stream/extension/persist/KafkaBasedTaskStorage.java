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

import java.util.Collections;
import java.util.List;

import org.stream.extension.events.Event;
import org.stream.extension.events.EventCenter;
import org.stream.extension.events.TaskCompleteEvent;
import org.stream.extension.meta.Task;

import lombok.Setter;

/**
 * Kafka based implementation of {@linkplain TaskStorage}.
 * Will push the entity to the kafka message queue so that anybody that is interested in can process the data.
 * @author guanxiong wei
 *
 */
public class KafkaBasedTaskStorage implements TaskStorage {

    @Setter
    private EventCenter eventCenter;

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean persist(final Task task) {
        eventCenter.fireEvent(Event.of(TaskCompleteEvent.class, task, null));
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean update(final Task task) {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Task query(final String taskID) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Task> queryStuckTasks() {
        return Collections.emptyList();
    }

}
