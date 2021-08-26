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

package org.stream.core.resource;

/**
 * Abstract implement of {@link ResourceReader}
 */
public abstract class AbstractResourceReader<T> implements ResourceReader {

    /**
     * {@inheritDoc}
     */
    public Resource read(final ResourceURL resourceURL) {
        return Resource.builder()
                .resourceURL(resourceURL)
                .value(doRead(resourceURL))
                .build();
    }

    /**
     * Read a object from the resource specific storage.
     * @param resourceURL A reference to the object.
     * @return An object read from the storage.
     */
    protected abstract T doRead(final ResourceURL resourceURL);
}
