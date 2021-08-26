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

package org.stream.extension.reader;

import org.stream.core.resource.AbstractResourceReader;
import org.stream.core.resource.ResourceAuthority;
import org.stream.core.resource.ResourceURL;
import org.stream.extension.meta.Task;
import org.stream.extension.persist.TaskStorage;

import lombok.Setter;

/**
 * ROA bases framework used reader to read {@link Task} from somewhere.
 * @author guanxiong wei
 *
 */
public class TaskReader extends AbstractResourceReader<Task> {

    @Setter
    private TaskStorage taskStorage;

    private ResourceAuthority taskResourceAuthority = new ResourceAuthority("TaskReader", Task.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public ResourceAuthority resolve() {
        return this.taskResourceAuthority;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Task doRead(final ResourceURL resourceURL) {
        String key = resourceURL.getPath();
        return taskStorage.query(key);
    }
}
