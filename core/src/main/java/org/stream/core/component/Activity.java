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

/**
 * Encapsulation of customer specific activity, which will be performed in a specific {@link Node}.
 *
 * Users should extend this interface to customize their own logics and configure them in graph definition files.
 * The work-flow engine will automatically pick up these activities and execute them per graph definition files.
 * @author guanxiong wei
 */
public abstract class Activity {

    /**
     * Perform an activity as part of a work-flow.
     * @return The activity result.
     */
    public abstract ActivityResult act();

    /**
     * Get the name of the activity.
     * @return The activity's name
     */
    public String getActivityName() {
        return getClass().getName();
    }

}
