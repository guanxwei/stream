package org.stream.component;

import static org.testng.Assert.assertEquals;

import org.stream.core.test.base.TestActivity;
import org.testng.annotations.Test;

public class ActivityTest {
    @Test
    public void test() {
        TestActivity testActivity = new TestActivity();
        assertEquals(testActivity.getActivityName(), TestActivity.class.getName());
    }

}
