package org.stream.assemble;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.List;

import javax.annotation.Resource;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.stream.core.execution.Engine;
import org.stream.core.execution.ExecutionRecord;
import org.stream.core.execution.GraphContext;
import org.stream.core.execution.WorkFlowContext;
import org.stream.core.resource.ResourceTank;
import org.testng.annotations.Test;

@ContextConfiguration(classes = SpringConfiguration.class)
public class ProcedureSpringTest extends AbstractTestNGSpringContextTests {

    @Resource
    private Engine engine;

    @Resource
    private GraphContext graphContext;

    @Test
    public void test() {
        ResourceTank resourceTank = engine.execute(graphContext, "ProcedureSpringTest", true);
        assertNotNull(resourceTank);
        List<ExecutionRecord> executionRecords = WorkFlowContext.getRecords();
        assertEquals(executionRecords.size(), 9);
    }
}
