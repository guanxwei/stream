/*
 * Copyright (C) 2021 guanxiongwei
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.stream.core.component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Activity repository.
 * @author guanxiong wei
 */
public class ActivityRepository {

    private final Map<String, Activity> activities = new ConcurrentHashMap<>();

    /**
     * Register an activity into the repository.
     * @param activity Activity needs to be registered in the repository.
     */
    public void register(final Activity activity) {
        if (!activities.containsKey(activity.getActivityName())) {
            activities.put(activity.getActivityName(), activity);
        }
    }

    /**
     * Check if the repository contains the activity.
     * @param activity Activity to check if it exists in the repository.
     * @return Checking Result.
     */
    public boolean isActivityRegistered(final Activity activity) {
        return activities.containsKey(activity.getActivityName());
    }

    /**
     * Check if a {@link Activity} with name activityName has been registered.
     * @param activityName The activity's name.
     * @return Checking Result.
     */
    public boolean isActivityRegistered(final String activityName) {
        return activities.containsKey(activityName);
    }

    /**
     * Get a {@link Activity} from the repository having name activityName.
     * @param activityName The activity's name.
     * @return Activity instance registered in this repository.
     */
    public Activity getActivity(final String activityName) {
        return activities.get(activityName);
    }

    /**
     * Query how many activities have been registered.
     * @return activity numbers.
     */
    public int getActivityNum() {
        return activities.size();
    }
}
