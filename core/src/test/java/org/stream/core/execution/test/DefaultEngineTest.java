package org.stream.core.execution.test;

import static org.testng.Assert.assertEquals;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.stream.core.component.ActivityRepository;
import org.stream.core.execution.DefaultEngine;
import org.stream.core.execution.ExecutionRecord;
import org.stream.core.execution.GraphContext;
import org.stream.core.execution.WorkFlowContext;
import org.stream.core.helper.LocalGraphLoader;
import org.stream.core.resource.Resource;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(singleThreaded = true)
public class DefaultEngineTest {

    private DefaultEngine defaultEngine;

    private GraphContext graphContext;

    private List<String> paths;

    private LocalGraphLoader graphLoader;

    @BeforeClass
    public void BeforeClass() throws Exception {
        this.defaultEngine = new DefaultEngine();
        this.paths = new LinkedList<String>();
        paths.add("ComprehensiveCase.graph");
        paths.add("ComprehensiveCase2.graph");
        paths.add("ComprehensiveCase4.graph");
        paths.add("CheckCase");
        paths.add("ThrowException.graph");
        paths.add("ComprehensiveWithAsyncNodeCanceledCase.graph");
        paths.add("ComprehensiveWithAsyncNodeCase.graph");
        paths.add("DefaultErrorProcessCase");

        paths.add("ComprehensiveWithAsyncNodeCase.graph");

        this.graphContext = new GraphContext();
        this.graphLoader = new LocalGraphLoader();
        graphLoader.setGraphContext(graphContext);
        this.graphContext.setActivityRepository(new ActivityRepository());
        graphLoader.setGraphFilePaths(paths);
        this.graphLoader.init();
    }

    @BeforeMethod
    public void BeforeMethod() {
        if (WorkFlowContext.isThereWorkingWorkFlow()) {
            WorkFlowContext.reboot();
        }
    }

    @AfterMethod
    public void AfterMethod() {
        WorkFlowContext.reboot();
    }

    @Test
    public void testExecute() {
        defaultEngine.execute(graphContext, "comprehensive", false);
        Assert.assertNull(WorkFlowContext.getPrimary());
        Assert.assertTrue(CollectionUtils.isNotEmpty(WorkFlowContext.getRecords()));
    }

    @Test
    public void testExecuteWithDefaultErrorProcess() {
        defaultEngine.execute(graphContext, "defaultErrorNode", true);
        Assert.assertNull(WorkFlowContext.getPrimary());
        Assert.assertTrue(CollectionUtils.isNotEmpty(WorkFlowContext.getRecords()));
        // 创建workflow + 每次执行节点前（三个，默认error handle 也会被执行） + success test activity + TestActivity.
        assertEquals(WorkFlowContext.getRecords().size(), 7);
    }

    @Test(dependsOnMethods = "testExecute")
    public void testExecuteWithAutoRecord() {
        defaultEngine.execute(graphContext, "comprehensive2", true);
        Assert.assertNull(WorkFlowContext.getPrimary());
        Assert.assertTrue(CollectionUtils.isNotEmpty(WorkFlowContext.getRecords()));

        Resource resource = WorkFlowContext.resolveResource("PrintRecordActivity");
        @SuppressWarnings("unchecked")
        List<ExecutionRecord> executionRecords = (List<ExecutionRecord>) resource.getValue();
        Assert.assertEquals(executionRecords.size(), 6);
    }

    @Test(dependsOnMethods = "testExecuteWithAutoRecord")
    public void testExecuteWithPrimary() {
        Resource primary = Resource.builder()
                .resourceReference("testExecuteWithPrimary")
                .build();
        defaultEngine.execute(graphContext, "comprehensive", primary, false);
        Assert.assertNotNull(WorkFlowContext.getPrimary());
        Assert.assertTrue(CollectionUtils.isNotEmpty(WorkFlowContext.getRecords()));
        primary = WorkFlowContext.getPrimary();

        Assert.assertEquals(100000L, primary.getValue());
    }

    @Test(dependsOnMethods = "testExecuteWithPrimary")
    public void testExecuteWithAutoRecordWithPrimay() {
        Resource primary = Resource.builder()
                .resourceReference("testExecuteWithPrimary")
                .build();
        defaultEngine.execute(graphContext, "comprehensive2", primary, true);
        Assert.assertNotNull(WorkFlowContext.getPrimary());
        Assert.assertTrue(CollectionUtils.isNotEmpty(WorkFlowContext.getRecords()));

        Resource resource = WorkFlowContext.resolveResource("PrintRecordActivity");
        @SuppressWarnings("unchecked")
        List<ExecutionRecord> executionRecords = (List<ExecutionRecord>) resource.getValue();
        Assert.assertEquals(executionRecords.size(), 6);
        primary = WorkFlowContext.getPrimary();

        Assert.assertEquals(100000l, primary.getValue());
    }

    @Test(dependsOnMethods = "testExecuteWithAutoRecordWithPrimay")
    public void testExecuteWithException() {
        Resource primary = Resource.builder()
                .resourceReference("testExecuteWithException")
                .build();
        defaultEngine.execute(graphContext, "ThrowException", primary, true);
        Assert.assertNotNull(WorkFlowContext.getPrimary());
        Assert.assertTrue(CollectionUtils.isNotEmpty(WorkFlowContext.getRecords()));

        primary = WorkFlowContext.getPrimary();

        Assert.assertEquals(100000l, primary.getValue());
    }

    @Test(dependsOnMethods = "testExecuteWithException")
    public void testSuspend() {
        defaultEngine.execute(graphContext, "comprehensive4", true);
        Assert.assertNull(WorkFlowContext.getPrimary());
        Assert.assertTrue(CollectionUtils.isNotEmpty(WorkFlowContext.getRecords()));

        Resource resource = WorkFlowContext.resolveResource("PrintRecordActivity");
        @SuppressWarnings("unchecked")
        List<ExecutionRecord> executionRecords = (List<ExecutionRecord>) resource.getValue();
        Assert.assertEquals(executionRecords.size(), 8);
    }

    @Test(dependsOnMethods = "testSuspend")
    public void testExecuteWithAsyncNode() throws Exception {
        Resource primary = Resource.builder()
                .resourceReference("testExecuteWithAsyncNode")
                .build();
        defaultEngine.execute(graphContext, "ComprehensiveWithAsyncNodeCase", primary, false);
        Assert.assertNotNull(WorkFlowContext.getPrimary());
        Assert.assertTrue(CollectionUtils.isNotEmpty(WorkFlowContext.getRecords()));

        primary = WorkFlowContext.getPrimary();

        Assert.assertEquals(100000l, primary.getValue());

        int counter = 3;
        Resource resource = WorkFlowContext.resolveResource("asyncreference");
        while (counter-- > 0 && resource == null) {
            resource = WorkFlowContext.resolveResource("asyncreference");
            Thread.sleep(100);
        }

        Assert.assertEquals("asyncvalue", resource.getValue());
    }

    @Test(dependsOnMethods = "testExecuteWithAsyncNode")
    public void testCheck() throws Exception {
        Resource primary = Resource.builder()
                .resourceReference("testExecuteWithAsyncNode")
                .build();
        defaultEngine.execute(graphContext, "checkCase", primary, false);
        Thread.sleep(1000);
        Assert.assertNotNull(WorkFlowContext.getPrimary());
        Assert.assertTrue(CollectionUtils.isNotEmpty(WorkFlowContext.getRecords()));

        primary = WorkFlowContext.getPrimary();
        long value = (long) primary.getValue();
        assertEquals(value, 100000l);
    }
}
