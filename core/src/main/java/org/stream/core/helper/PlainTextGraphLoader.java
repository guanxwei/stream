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

package org.stream.core.helper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.stream.core.exception.GraphLoadException;

import lombok.extern.slf4j.Slf4j;

/**
 * Plain text based graph loader. The graph definition content will be plain text input directly from
 * the user interface.
 * @author weiguanxiong.
 *
 */
@Slf4j
public class PlainTextGraphLoader extends AbstractGraphLoader {

    /**
     * {@inheritDoc}
     */
    @Override
    protected InputStream loadInputStream(final String sourcePath) throws GraphLoadException {
        log.info("Graph context [{}]", sourcePath);
        return new ByteArrayInputStream(sourcePath.getBytes());
    }

}
