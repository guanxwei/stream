package org.stream.core.component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Activity repository which is used to store all the activities. 
 */
public class ActivityRepository {

    private Map<String, Activity> activities = new ConcurrentHashMap<String, Activity>();

    /**
     * Register a activity into the repository.
     * @param activity Activity need to be registerred in the repository.
     */
    public void register(final Activity activity) {
        if (!activities.containsKey(activity.getActivityName())) {
            activities.put(activity.getActivityName(), activity);
        }
    }

    /**
     * Check if the repository contains the activity.
     * @param activity Activity to check if it exists in the repository.
     * @return
     */
    public boolean isActivityRegistered(final Activity activity) {
        return activities.containsKey(activity.getActivityName());
    }

    /**
     * Check if a {@link Activity} with name activityName has been registered.
     * @param activityName
     * @return
     */
    public boolean isActivityRegistered(final String activityName) {
        return activities.containsKey(activityName);
    }

    /**
     * Get a {@link Activity} from the repository having name activityName.
     * @param activityName
     * @return
     */
    public Activity getActivity(final String activityName) {
        return activities.get(activityName);
    }

    /**
     * Query how many activities has been registerd.
     * @return activity numbers.
     */
    public int getActivityNum() {
        return activities.size();
    }
}
