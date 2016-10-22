package stream.core.execution.test;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.FutureTask;

import org.stream.core.component.ActivityRepository;
import org.stream.core.component.ActivityResult;
import org.stream.core.exception.WorkFlowExecutionExeception;
import org.stream.core.execution.DefaultEngine;
import org.stream.core.execution.Engine;
import org.stream.core.execution.ExecutionRecord;
import org.stream.core.execution.GraphContext;
import org.stream.core.execution.WorkFlowContext;
import org.stream.core.helper.GraphLoader;
import org.stream.core.helper.ResourceHelper;
import org.stream.core.resource.Resource;
import org.stream.core.resource.ResourceType;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import stream.core.test.base.CascadeTestActivity;

public class GraphExecutionTest {

    private Engine engine;
    private GraphContext graphContext;
    private GraphLoader graphLoader;
    private ActivityRepository activityRepository;
    private List<String> paths;

    @BeforeMethod
    public void BeforeMethod() {
        this.activityRepository = new ActivityRepository();
        this.graphContext = new GraphContext();
        this.graphLoader = new GraphLoader();
        graphContext.setActivityRepository(activityRepository);
        graphLoader.setGraphContext(graphContext);
        paths = new LinkedList<>();
        graphLoader.setGraphFilePaths(paths);
        this.engine = new DefaultEngine();
        WorkFlowContext.reboot();
    }

    @Test(expectedExceptions = {WorkFlowExecutionExeception.class}, expectedExceptionsMessageRegExp = "Graph is not present! Please double check the graph name you provide.")
    public void testWrongGraphName() throws Exception {
        String path = "ComprehensiveCase.graph";
        paths.add(path);
        graphLoader.init();
        engine.execute(graphContext, "test", false, null);
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
                .resourceType(ResourceType.PRIMITIVE)
                .build();
        engine.execute(graphContext, "comprehensive", primaryResource, false, null);
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
                .resourceType(ResourceType.PRIMITIVE)
                .build();
        engine.execute(graphContext, "comprehensive", primaryResource, true, null);
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
        CascadeTestActivity cascadeActivity = (CascadeTestActivity) activityRepository.getActivity("stream.core.test.base.CascadeTestActivity");
        cascadeActivity.setGraphContext(graphContext);
        Resource primaryResource = Resource.builder()
                .resourceReference("testprimary")
                .value(null)
                .resourceType(ResourceType.PRIMITIVE)
                .build();
        engine.execute(graphContext, "cascade", primaryResource, false, ResourceType.OBJECT);
        List<ExecutionRecord> records = WorkFlowContext.getRecords();
        Assert.assertEquals(records.size(), 4);
        Assert.assertEquals(records.get(0).getDescription(), "keep a cascade record");
        Assert.assertEquals(records.get(1).getDescription(), "keep a record");
        Assert.assertEquals(records.get(2).getDescription(), "keep a success record");
        Assert.assertEquals(records.get(3).getDescription(), "keep a success record");
        primaryResource = WorkFlowContext.getPrimary();
        Assert.assertEquals(((Long)primaryResource.getValue()).longValue(), 100000);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAsyncCase() throws Exception {
        String asyncPath = "ComprehensiveWithAsyncNodeCase.graph";
        paths.add(asyncPath);
        graphLoader.init();
        engine.execute(graphContext, "comprehensive2", null, false, ResourceType.OBJECT);
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

}
