package org.stream.assemble;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.stream.core.component.ActivityRepository;
import org.stream.core.component.AsyncActivity;
import org.stream.core.component.Graph;
import org.stream.core.exception.GraphLoadException;
import org.stream.core.exception.StreamException;
import org.stream.core.execution.DefaultEngine;
import org.stream.core.execution.Engine;
import org.stream.core.execution.GraphContext;
import org.stream.core.test.base.AsyncTestActivity;
import org.stream.core.test.base.FailTestActivity;
import org.stream.core.test.base.PrintRecordActivity;
import org.stream.core.test.base.SuccessTestActivity;
import org.stream.core.test.base.SuspendActivity;
import org.stream.core.test.base.TestActivity;
import org.stream.extension.assemble.ProcedureCompiler;
import org.stream.extension.assemble.ProcedureCondition;

@Configuration
public class SpringConfiguration {

    @Resource
    private ApplicationContext applicationContext;

    @Bean
    public TestActivity testActivity() {
        return new TestActivity();
    }

    @Bean
    public SuspendActivity suspendActivity() {
        return new SuspendActivity();
    }

    @Bean
    public FailTestActivity failTestActivity() {
        return new FailTestActivity();
    }

    @Bean
    public SuccessTestActivity successTestActivity() {
        return new SuccessTestActivity();
    }

    @Bean
    public ActivityRepository activityRepository() {
        return new ActivityRepository();
    }

    @Bean
    public AsyncActivity asyncActivity() {
        return new AsyncTestActivity();
    }

    @Bean
    public PrintRecordActivity printRecordActivity() {
        return new PrintRecordActivity();
    }

    @Bean
    public GraphContext graphContext() {
        GraphContext graphContext = new GraphContext();
        graphContext.setActivityRepository(activityRepository());
        return graphContext;
    }

    @Bean
    public Graph graph() throws GraphLoadException, StreamException {
        List<String> dependencies = new LinkedList<String>();
        dependencies.add("aysnc");
        Graph graph = ProcedureCompiler.builder()
            .withName("ProcedureSpringTest")
            .withContext(graphContext())
            .withSpringContext(applicationContext)
            .addAction("successAction")
                .act(successTestActivity())
                    .when(ProcedureCondition.SUCCEED).then("suspendedAction")
                    .when(ProcedureCondition.FAILED).then("errorProcess")
                .done()
            .addAction("suspendedAction")
                .act(suspendActivity())
                    .when(ProcedureCondition.SUCCEED).then("failedAction")
                .done()
            .addAction("aysnc")
                .act(asyncActivity())
                .done()
            .addAction("failedAction")
                .act(failTestActivity())
                .dependsOn(dependencies)
                .done()
            .addAction("errorProcess")
                .act(printRecordActivity())
                .done()
            .startFrom("startNode")
                .act(testActivity())
                    .when(ProcedureCondition.SUCCEED).then("successAction")
                    .when(ProcedureCondition.FAILED).then("errorProcess")
                .done()
            .compile();
        return graph;
    }

    @Bean
    public Engine engine() {
        return new DefaultEngine();
    }
}
