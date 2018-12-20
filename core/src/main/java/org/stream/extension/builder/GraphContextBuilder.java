package org.stream.extension.builder;

import java.util.List;

import org.springframework.context.ApplicationContext;
import org.stream.core.component.ActivityRepository;
import org.stream.core.execution.GraphContext;
import org.stream.core.helper.GraphLoader;

public final class GraphContextBuilder {

    private GraphContextBuilder() { }

    private List<String> graphs;

    private ActivityRepository activityRepository;

    private ApplicationContext applicationContext;

    private GraphLoader graphLoader;

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

    public GraphContextBuilder graphLoader(final GraphLoader graphLoader) {
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
