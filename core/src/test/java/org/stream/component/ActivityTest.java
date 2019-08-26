package org.stream.component;

import static org.testng.Assert.assertEquals;

import org.stream.core.component.Node;
import org.stream.core.test.base.TestActivity;
import org.testng.annotations.Test;

public class ActivityTest {

    @org.testng.annotations.BeforeMethod
    public void BeforeMethod() {
        Node current = Node.builder()
                .activity(new TestActivity())
                .asyncDependencies(null)
                .graph(null)
                .intervals(null)
                .next(null)
                .nodeName("testNode")
                .build();
        Node.CURRENT.set(current);

    }

    @Test
    public void test() {
        TestActivity testActivity = new TestActivity();
        assertEquals(testActivity.getActivityName(), TestActivity.class.getName());
        assertEquals(testActivity.getExecutionContext(), Node.CURRENT.get());
    }

}
