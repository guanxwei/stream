package org.stream.core.execution.test;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.FutureTask;

import org.stream.core.component.ActivityRepository;
import org.stream.core.component.ActivityResult;
import org.stream.core.exception.WorkFlowExecutionExeception;
import org.stream.core.execution.DefaultEngine;
import org.stream.core.execution.Engine;
import org.stream.core.execution.ExecutionRecord;
import org.stream.core.execution.GraphContext;
import org.stream.core.execution.WorkFlowContext;
import org.stream.core.helper.LocalGraphLoader;
import org.stream.core.helper.ResourceHelper;
import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceTank;
import org.stream.core.resource.ResourceType;
import org.stream.core.test.base.CascadeTestActivity;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class GraphExecutionTest {

    private Engine engine;
    private GraphContext graphContext;
    private LocalGraphLoader graphLoader;
    private ActivityRepository activityRepository;
    private List<String> paths;

    @BeforeMethod
    public void BeforeMethod() {
        this.activityRepository = new ActivityRepository();
        this.graphContext = new GraphContext();
        this.graphLoader = new LocalGraphLoader();
        graphContext.setActivityRepository(activityRepository);
        graphLoader.setGraphContext(graphContext);
        paths = new LinkedList<>();
        graphLoader.setGraphFilePaths(paths);
        this.engine = new DefaultEngine();
    }

    @Test(expectedExceptions = {WorkFlowExecutionExeception.class}, expectedExceptionsMessageRegExp = "Graph is not present! Please double check the graph name you provide.")
    public void testWrongGraphName() throws Exception {
        String path = "ComprehensiveCase.graph";
        paths.add(path);
        graphLoader.init();
        engine.execute(graphContext, "test", false, ResourceType.OBJECT);
        Exception e = WorkFlowContext.extractException();
        throw e;
    }

    @Test
    public void testSimpleCaseWithNoAutoRecord() throws Exception {
        String path = "ComprehensiveCase.graph";
        paths.add(path);
        graphLoader.init();
        Resource primaryResource = Resource.builder()
                .resourceReference("testprimary")
                .value(null)
                .resourceType(ResourceType.OBJECT)
                .build();
        engine.execute(graphContext, "comprehensive", primaryResource, false, ResourceType.OBJECT);
        List<ExecutionRecord> records = WorkFlowContext.getRecords();
        Assert.assertNotNull(records);
        Assert.assertEquals(records.size(), 2);
        Resource primary = WorkFlowContext.getPrimary();
        Assert.assertEquals(primary.getResourceReference(), "testprimary");
        ExecutionRecord record1 = records.get(0);
        ExecutionRecord record2 = records.get(1);
        Assert.assertEquals(record1.getDescription(), "keep a record");
        Assert.assertEquals(record2.getDescription(), "keep a success record");
    }

    @Test
    public void testSimpleCaseWithAutoRecord() throws Exception {
        String path = "ComprehensiveCase.graph";
        paths.add(path);
        graphLoader.init();
        Resource primaryResource = Resource.builder()
                .resourceReference("testprimary")
                .value(null)
                .resourceType(ResourceType.OBJECT)
                .build();
        engine.execute(graphContext, "comprehensive", primaryResource, true, ResourceType.OBJECT);
        List<ExecutionRecord> records = WorkFlowContext.getRecords();
        Assert.assertEquals(records.size(), 5);
    }

    @Test
    public void testCascadeCase() throws Exception {
        String entryPath = "ComprehensiveCase.graph";
        String cascadePath = "CascadeCase.graph";
        paths.add(entryPath);
        paths.add(cascadePath);
        graphLoader.init();
        CascadeTestActivity cascadeActivity = (CascadeTestActivity) activityRepository.getActivity("org.stream.core.test.base.CascadeTestActivity");
        cascadeActivity.setGraphContext(graphContext);
        Resource primaryResource = Resource.builder()
                .resourceReference("testprimary")
                .value(null)
                .resourceType(ResourceType.PRIMITIVE)
                .build();
        engine.execute(graphContext, "cascade", primaryResource, false, ResourceType.OBJECT);
        List<ExecutionRecord> records = WorkFlowContext.getRecords();
        Assert.assertEquals(records.size(), 2);
        Assert.assertEquals(records.get(0).getDescription(), "keep a cascade record");
        Assert.assertEquals(records.get(1).getDescription(), "keep a success record");
        primaryResource = WorkFlowContext.getPrimary();
        Assert.assertEquals(((Long)primaryResource.getValue()).longValue(), 100000);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAsyncCase() throws Exception {
        String asyncPath = "ComprehensiveWithAsyncNodeCase.graph";
        paths.add(asyncPath);
        graphLoader.init();
        engine.execute(graphContext, "ComprehensiveWithAsyncNodeCase", null, false, ResourceType.OBJECT);
        Thread.sleep(1100);
        List<ExecutionRecord> records = WorkFlowContext.getRecords();
        Assert.assertEquals(records.size(), 2);
        Resource asyncTaskWrapper = WorkFlowContext.resolveResource("node4" + ResourceHelper.ASYNC_TASK_SUFFIX);
        Assert.assertNotNull(asyncTaskWrapper);
        FutureTask<ActivityResult> task = (FutureTask<ActivityResult>) asyncTaskWrapper.getValue();
        Assert.assertNotNull(task);
        Assert.assertEquals(task.get(), ActivityResult.SUCCESS);
        Resource resource = WorkFlowContext.resolveResource("asyncreference");
        Assert.assertNotNull(resource);
        Assert.assertEquals(resource.getValue(), "asyncvalue");
    }

    @Test(expectedExceptions = {WorkFlowExecutionExeception.class}, expectedExceptionsMessageRegExp = "The resourceType does not match the the specified one in the definition file")
    public void testResourceTypeMissMatch() throws Exception {
        String asyncPath = "ComprehensiveWithAsyncNodeCase.graph";
        paths.add(asyncPath);
        graphLoader.init();
        engine.execute(graphContext, "ComprehensiveWithAsyncNodeCase", null, false, ResourceType.SESSION);
    }

    @Test(expectedExceptions = {WorkFlowExecutionExeception.class}, expectedExceptionsMessageRegExp = "The work-flow instance has been closed!")
    public void testExceptionThrownAfterWorkflowClosed() throws Exception {
        String asyncPath = "ComprehensiveWithAsyncNodeCase.graph";
        paths.add(asyncPath);
        graphLoader.init();
        engine.execute(graphContext, "ComprehensiveWithAsyncNodeCase", null, false, ResourceType.OBJECT);
        WorkFlowContext.close(true);
        WorkFlowContext.getPrimary();
    }

    @Test(expectedExceptions = CancellationException.class)
    public void testCancelAsyncTask() throws Exception {
        String asyncPath = "ComprehensiveWithAsyncNodeCanceledCase.graph";
        paths.add(asyncPath);
        graphLoader.init();
        engine.execute(graphContext, "ComprehensiveWithAsyncNodeCanceledCase", null, false, ResourceType.OBJECT);
        Resource asyncTaskWrapper = WorkFlowContext.resolveResource("node4" + ResourceHelper.ASYNC_TASK_SUFFIX);
        // Make sure the async task has chance to start.
        Thread.sleep(1000);
        WorkFlowContext.close(true);

        Assert.assertNotNull(asyncTaskWrapper);
        @SuppressWarnings("unchecked")
        FutureTask<ActivityResult> task = (FutureTask<ActivityResult>) asyncTaskWrapper.getValue();
        task.get();
    }

    @Test
    public void testAutoClose() throws Exception {
        String path = "ComprehensiveCase.graph";
        paths.add(path);
        graphLoader.init();
        Resource primaryResource = Resource.builder()
                .resourceReference("testprimary")
                .value(null)
                .resourceType(ResourceType.OBJECT)
                .build();
        engine.executeOnce(graphContext, "comprehensive", primaryResource, true, ResourceType.OBJECT);
        Assert.assertFalse(WorkFlowContext.isThereWorkingWorkFlow());

    }

    @Test
    public void testMultiTimes() throws Exception {
        String entryPath = "ComprehensiveCase.graph";
        String cascadePath = "CascadeCase.graph";
        paths.add(entryPath);
        paths.add(cascadePath);
        graphLoader.init();
        CascadeTestActivity cascadeActivity = (CascadeTestActivity) activityRepository.getActivity("org.stream.core.test.base.CascadeTestActivity");
        cascadeActivity.setGraphContext(graphContext);
        Resource primaryResource = Resource.builder()
                .resourceReference("testprimary")
                .value(null)
                .resourceType(ResourceType.PRIMITIVE)
                .build();
        ResourceTank resourceTank1 = engine.execute(graphContext, "cascade", primaryResource, false, ResourceType.OBJECT);
        Assert.assertNotNull(WorkFlowContext.getPrimary());
        ResourceTank resourceTank2 = engine.execute(graphContext, "cascade", null, false, ResourceType.OBJECT);
        Assert.assertNotEquals(resourceTank1, resourceTank2);
        Assert.assertNull(WorkFlowContext.getPrimary());
    }

    @Test
    public void testThrowException() throws Exception {
        String path = "ThrowException.graph";
        paths.add(path);
        paths.add("ThrowExceptionSuccesor.graph");
        graphLoader.init();
        try {
            engine.execute(graphContext, "ThrowException", null, true, ResourceType.OBJECT);
        } catch (Exception e) {
            System.out.println(e.getClass().getSimpleName());
        }
        engine.execute(graphContext, "ThrowExceptionSuccesor", null, true, ResourceType.OBJECT);
    }

    @AfterMethod
    public void AfterMethod() {
        if (WorkFlowContext.isThereWorkingWorkFlow())
            WorkFlowContext.reboot();
        Assert.assertFalse(WorkFlowContext.isThereWorkingWorkFlow());
    }
}
