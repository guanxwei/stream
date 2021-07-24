package org.stream.assemble;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import com.google.common.collect.ImmutableMap;

import org.stream.core.component.Activity;
import org.stream.core.component.ActivityRepository;
import org.stream.core.component.Graph;
import org.stream.core.exception.GraphLoadException;
import org.stream.core.exception.StreamException;
import org.stream.core.execution.DefaultEngine;
import org.stream.core.execution.Engine;
import org.stream.core.execution.GraphContext;
import org.stream.core.execution.WorkFlowContext;
import org.stream.core.resource.Resource;
import org.stream.core.test.base.ConditionActivity;
import org.stream.core.test.base.PrintRecordActivity;
import org.stream.core.test.base.SuspendActivity;
import org.stream.core.test.base.TestActivity;
import org.stream.extension.assemble.ProcedureCompiler;
import org.stream.extension.assemble.ProcedureCondition;
import org.testng.annotations.Test;

public class ProcedureTest {

    @Test
    public void testWithNoSpring() throws GraphLoadException, StreamException {
        GraphContext graphContext = new GraphContext();
        ActivityRepository activityRepository = new ActivityRepository();
        graphContext.setActivityRepository(activityRepository);
        Activity activity1 = new TestActivity();
        Activity activity2 = new SuspendActivity();
        Activity activity3 = new PrintRecordActivity();
        Graph graph = ProcedureCompiler.builder()
                .withName("ProcedureTest")
                .withContext(graphContext)
                .startFrom("startNode")
                    .act(activity1)
                        .when(ProcedureCondition.SUCCEED).then("successAction")
                        .when(ProcedureCondition.FAILED).then("errorProcess")
                    .done()
                .addAction("successAction")
                    .act(activity2)
                        .when(ProcedureCondition.SUCCEED).then(null)
                        .when(ProcedureCondition.FAILED).then("errorProcess")
                    .done()
                .addAction("errorProcess")
                    .act(activity3)
                    .done()
                .defaultErrorNode(activity3)
                .compile();
        assertNotNull(graph);
        assertEquals(activityRepository.isActivityRegistered(activity3), true);
        assertEquals(activityRepository.isActivityRegistered(activity2), true);
        assertEquals(activityRepository.isActivityRegistered(activity1), true);
        assertEquals(activityRepository.isActivityRegistered(activity3.getClass().getName()), true);
        assertEquals(activityRepository.isActivityRegistered(activity2.getClass().getName()), true);
        assertEquals(activityRepository.isActivityRegistered(activity1.getClass().getName()), true);

        Engine engine = new DefaultEngine();
        engine.executeOnce(graphContext, "ProcedureTest", true);
    }

    @Test
    public void testCondition() throws GraphLoadException, StreamException {
        GraphContext graphContext = new GraphContext();
        ActivityRepository activityRepository = new ActivityRepository();
        graphContext.setActivityRepository(activityRepository);
        Activity activity1 = new ConditionActivity();
        Activity activity3 = new PrintRecordActivity();

        Graph graph = ProcedureCompiler.builder()
                .withName("ConditionProcedureTest")
                .withContext(graphContext)
                .startFrom("startNode")
                    .act(activity1)
                        .when(ProcedureCondition.CONDITION).conditions(ImmutableMap.of(1, "node2"))
                    .done()
                .addAction("node2")
                    .act(activity3)
                    .done()
                .defaultErrorNode(activity3)
                .compile();
        assertNotNull(graph);
        Engine engine = new DefaultEngine();
        engine.execute(graphContext, "ConditionProcedureTest", true);
        Resource resource = WorkFlowContext.resolveResource("PrintRecordActivity");
        assertNotNull(resource);
    }
}
