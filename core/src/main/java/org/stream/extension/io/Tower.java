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

package org.stream.extension.io;

import org.stream.core.component.TowerActivity;
import org.stream.core.execution.AutoScheduledEngine;
import org.stream.extension.persist.TaskStepStorage;
import org.stream.extension.persist.TaskStorage;

/**
 * Abstract of "towers" which are used to communicate with the remote services by {@link AutoScheduledEngine}.
 * Every tower will be wrapped in a {@link TowerActivity} instance by the work-flow engine when loading the graph.
 * <p>
 * Users should only use tower when below scenarios fulfill:
 * (1) Plenty of distributed services will be arranged cooperating doing one thing in the single procedure
 *     Normally one RPC framework or something like that will be used, clients use the predefine
 *     interface to communicate with the remove service,
 * (2) {@link AutoScheduledEngine} is used to execute the procedure
 *      In such cases {@link AutoScheduledEngine} will help execute the work by the predefined graph,
 *      In each step, remote service's response status will be used to determine what to do at the next step.
 * (3) All the related resources {@link AutoScheduledEngine} need are ready and reliable, like Redis cluster;
 * and {@link TaskStorage}, {@link TaskStepStorage} implementations are provided, etc.
 * @author guanxiong wei
 *
 */
public interface Tower {

    /**
     * Call the remote service to complete the subtask. The Stream framework will use the response to decide what to do in the next step, and help
     * merge the return value in to the work-flow branch.
     * @param request Request to be sent to the remote service.
     * @return Stream framework defined transfer data.
     */
    StreamTransferData call(final StreamTransferData request);
}
