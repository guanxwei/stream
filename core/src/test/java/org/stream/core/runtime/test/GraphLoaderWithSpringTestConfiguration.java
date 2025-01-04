package org.stream.core.runtime.test;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stream.core.component.ActivityRepository;
import org.stream.core.exception.GraphLoadException;
import org.stream.core.execution.DefaultEngine;
import org.stream.core.execution.Engine;
import org.stream.core.execution.GraphContext;
import org.stream.core.runtime.LocalGraphLoader;
import org.stream.core.test.base.SpringActivity;
import org.stream.core.test.base.TestTower;

@Configuration
public class GraphLoaderWithSpringTestConfiguration {

    @Resource
    private ApplicationContext applicationContext;

    @Bean
    public SpringModule springModule() {
        return new SpringModule();
    }

    @Bean
    public SpringActivity springActivity() {
        return new SpringActivity();
    }

    @Bean
    public TestTower testTower() {
        return new TestTower();
    }

    @Bean
    public GraphContext graphContext() {
        GraphContext graphContext = new GraphContext();
        graphContext.setActivityRepository(new ActivityRepository());
        return graphContext;
    }

    @Bean
    public LocalGraphLoader graphLoader() throws GraphLoadException {
        LocalGraphLoader graphLoader = new LocalGraphLoader();
        graphLoader.setGraphContext(graphContext());
        List<String> graphFilePaths = new LinkedList<>();
        graphFilePaths.add("SpringCase");
        graphLoader.setGraphFilePaths(graphFilePaths);
        graphLoader.setApplicationContext(applicationContext);

        graphLoader.init();
        return graphLoader;
    }

    @Bean
    public Engine engine() {
        DefaultEngine defaultEngine = new DefaultEngine();
        return defaultEngine;
    }
}
