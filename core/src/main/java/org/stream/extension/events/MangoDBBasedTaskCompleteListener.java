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

package org.stream.extension.events;

import org.stream.extension.clients.MongoClient;
import org.stream.extension.meta.Task;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * MangoDB event listener.
 * Default worker to back-up completed tasks.
 *
 */
@Slf4j
public class MangoDBBasedTaskCompleteListener implements Listener {

    @Setter
    private MongoClient mongoClient;

    @Setter
    private String collectionName;

    /**
     * {@inheritDoc}
     */
    @Override
    public void handle(final Event event) {
        TaskCompleteEvent realEvent = (TaskCompleteEvent) event;
        Task task = (Task) realEvent.getTrigger();
        log.info("Save completed task [{}]", task.getTaskId());

        mongoClient.save(task.getTaskId(), task, collectionName);
    }

}
