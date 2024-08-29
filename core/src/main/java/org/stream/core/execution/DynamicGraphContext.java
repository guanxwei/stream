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

package org.stream.core.execution;

import org.stream.core.component.Graph;

import lombok.extern.slf4j.Slf4j;

/**
 * Enhanced graph context of {@link GraphContext} enable modifying the graph configuration in run-time.
 * @author guanxiong wei
 *
 */
@Slf4j
public class DynamicGraphContext extends GraphContext {

    /**
     * Add a graph to the graph context.
     * @param graph Graph instance to be added.
     */
    @Override
    public void addGraph(final Graph graph) {
        if (graphs.containsKey(graph.getGraphName())) {
            var originalGraph = graphs.get(graph.getGraphName());
            log.info("Graph [{}] is changed, original definition \n [{}] \n while updated definition is \n [{}]",
                    graph.getGraphName(), originalGraph.getOriginalDefinition(), graph.getOriginalDefinition());
        }
        graphs.put(graph.getGraphName(), graph);
    }
}
