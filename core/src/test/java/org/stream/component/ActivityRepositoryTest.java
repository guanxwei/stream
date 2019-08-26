package org.stream.component;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import org.stream.core.component.ActivityRepository;
import org.stream.core.test.base.TestActivity;
import org.testng.annotations.Test;

public class ActivityRepositoryTest {

    @Test
    public void test() {
        ActivityRepository activityRepository = new ActivityRepository();
        assertFalse(activityRepository.isActivityRegistered(new TestActivity()));

        assertFalse(activityRepository.isActivityRegistered(new TestActivity().getActivityName()));

        activityRepository.register(new TestActivity());

        assertTrue(activityRepository.isActivityRegistered(new TestActivity()));

        assertTrue(activityRepository.isActivityRegistered(new TestActivity().getActivityName()));

        assertEquals(activityRepository.getActivityNum(), 1);
        assertNotNull(activityRepository.getActivity(new TestActivity().getActivityName()));
    }
}
