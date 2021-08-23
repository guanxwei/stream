package org.stream.component;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import org.apache.commons.lang3.RandomStringUtils;
import org.mockito.Mockito;
import org.stream.core.component.TowerActivity;
import org.stream.core.execution.Debuger;
import org.stream.core.execution.WorkFlowContext;
import org.stream.core.resource.Resource;
import org.stream.extension.io.StreamTransferData;
import org.stream.extension.io.Tower;
import org.testng.annotations.Test;

public class TowerActivityTest {

    private TowerActivity towerActivity;

    private Tower mock;

    @org.testng.annotations.BeforeMethod
    public void BeforeMethod() {
        this.mock = Mockito.mock(Tower.class);
        this.towerActivity = new TowerActivity(mock);
        Debuger.setUpWorkFlow();
        Resource data = Resource.builder()
                .resourceReference(WorkFlowContext.WORK_FLOW_TRANSTER_DATA_REFERENCE)
                .value(StreamTransferData.succeed()
                        .add("primaryClass", String.class.getName())
                        .add("errorStack", "fdasfadf")
                        .add("errorMessage", "fdasfdasdfdaf"))
                .build();
        Resource primaryResource = Resource.builder()
                .resourceReference(RandomStringUtils.randomAlphabetic(32))
                .value("primary")
                .build();
        WorkFlowContext.attachPrimaryResource(primaryResource);
        WorkFlowContext.attachResource(data);
    }

    @Test
    public void testNormalCase() {
        Mockito.when(mock.call(Mockito.any())).thenReturn(StreamTransferData.failed());
        towerActivity.act();
        StreamTransferData data = WorkFlowContext.resolve(WorkFlowContext.WORK_FLOW_TRANSTER_DATA_REFERENCE, StreamTransferData.class);
        assertEquals(data.getActivityResult(), StreamTransferData.failed().getActivityResult());

        assertNull(data.get("errorStack"));
        assertNull(data.get("errorMessage"));
    }

    @Test
    public void testExceptionCase() {
        Mockito.when(mock.call(Mockito.any())).thenThrow(new RuntimeException("fadf"));
        towerActivity.act();
        StreamTransferData data = WorkFlowContext.resolve(WorkFlowContext.WORK_FLOW_TRANSTER_DATA_REFERENCE, StreamTransferData.class);
        assertEquals(data.getActivityResult(), StreamTransferData.failed().getActivityResult());

        assertNotNull(data.get("errorStack"));
        assertNotNull(data.get("errorMessage"));

        assertEquals(data.get("errorMessage"), "fadf");
    }
}
