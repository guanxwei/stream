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

package org.stream.extension.builder;

import java.util.List;

import org.springframework.context.ApplicationContext;
import org.stream.core.component.ActivityRepository;
import org.stream.core.execution.GraphContext;
import org.stream.core.runtime.LocalGraphLoader;

public final class GraphContextBuilder {

    private GraphContextBuilder() { }

    private List<String> graphs;

    private ActivityRepository activityRepository;

    private ApplicationContext applicationContext;

    private LocalGraphLoader graphLoader;

    public static GraphContextBuilder builder() {
        return new GraphContextBuilder();
    }

    public GraphContextBuilder graphs(final List<String> graphs) {
        this.graphs = graphs;
        return this;
    }

    public GraphContextBuilder activityRepository(final ActivityRepository activityRepository) {
        this.activityRepository = activityRepository;
        return this;
    }

    public GraphContextBuilder applicationContext(final ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        return this;
    }

    public GraphContextBuilder graphLoader(final LocalGraphLoader graphLoader) {
        this.graphLoader = graphLoader;
        return this;
    }

    public GraphContext build() throws Exception {
        GraphContext graphContext = new GraphContext();
        graphContext.setActivityRepository(activityRepository);
        this.graphLoader.setApplicationContext(applicationContext);
        this.graphLoader.setGraphContext(graphContext);
        this.graphLoader.setGraphFilePaths(graphs);
        this.graphLoader.init();
        return graphContext;
    }
}
