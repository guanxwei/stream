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

package org.stream.core.execution;

import org.stream.core.execution.WorkFlow.WorkFlowStatus;

/**
 * A utility class only for test purpose.
 * Encapsulate some useful methods of {@link WorkFlowContext} so that test cases
 * can easily set up work-flow context.
 * @author guanxiong wei
 *
 */
public final class Debuger {

    private Debuger() { }

    /**
     * Set up a new work-flow instance for the current thread. The new created work-flow instance's status will be {@link WorkFlowStatus#WAITING}.
     * Clients should manually start the work-flow by invoking the method {@link WorkFlow#start()}, normally the {@linkplain Engine} implementation will help
     * to invoke this method when create a new work-flow instance.
     *
     * Users should not invoke this method in any cases.
     * @return The work-flow reference.
     */
    public static WorkFlow setUpWorkFlow() {
        return WorkFlowContext.setUpWorkFlow();
    }

    /**
     * Provide the current working work-flow reference.
     * @return The work-flow instance adhered to the current thread.
     */
    public static WorkFlow provide() {
        return WorkFlowContext.provide();
    }

}
