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

package org.stream.core.runtime;

import java.io.InputStream;

import org.springframework.util.CollectionUtils;
import org.stream.core.exception.GraphLoadException;

/**
 * Graph helper who is responsible to load graphs from specific graph definition files in local disk.
 * Basically a graph definition file should be named with suffix ".graph" and the file content
 * should be stored as standard json string of object {@link GraphConfiguration}.
 */
public class LocalGraphLoader extends AbstractGraphLoader {

    /**
     * Use absolute path to load the graph definition files, the graph folder should be put at the root directory of the projects.
     * If the customers use Maven to manage project, then the graphs should be put at the folder "graph" in the resources folder.
     */
    private static final String SYSTEM_PATH_SEPARATOR = "/";

    private static final String DEFAULT_GRAPH_FILE_PATH_PREFIX = SYSTEM_PATH_SEPARATOR + "graph" + SYSTEM_PATH_SEPARATOR;

    /**
     * Initiate graph loading process, load all the graphs specified in the {@link #graphFilePaths}, which is located in the
     * default graph directory.
     * @throws GraphLoadException GraphLoadException.
     */
    public void init() throws GraphLoadException {
        if (CollectionUtils.isEmpty(graphFilePaths)) {
            throw new GraphLoadException("Graph definition file paths not specified!");
        }
        for (String path : graphFilePaths) {
            loadGraphFromSource(path);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected InputStream loadInputStream(String sourcePath) throws GraphLoadException {
        if (!sourcePath.endsWith(".graph")) {
            sourcePath += ".graph";
        }
        InputStream input = getClass().getResourceAsStream(DEFAULT_GRAPH_FILE_PATH_PREFIX + sourcePath);
        if (input == null) {
            throw new GraphLoadException(String.format("Graph definition file is not found, file name is [%s]",
                    DEFAULT_GRAPH_FILE_PATH_PREFIX + sourcePath));
        }

        return input;
    }
}
