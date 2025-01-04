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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;

import org.stream.core.exception.WorkFlowExecutionException;
import org.stream.core.execution.WorkFlowContext;
import org.stream.extension.io.StreamTransferData;
import org.stream.extension.io.Tower;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * Special activity containing a {@link Tower} instance to communicate with external service.
 * <p>
 * Users should not use this class themselves, the framework will help construct an instance of this
 * type when the activityClass is an implementation of {@link Tower} in graph definition.
 * @author guanxiong wei
 *
 */
@Setter
@Getter
@Slf4j
public class TowerActivity extends Activity {

    private Tower tower;

    public TowerActivity(final Tower tower) {
        this.tower = tower;
    }

    public TowerActivity() {
        this(null);
    }

    /**
     * {@inheritDoc}
     */
    public ActivityResult act() {
        if (tower == null) {
            throw new WorkFlowExecutionException("Actor must be specified");
        }

        var resource = WorkFlowContext.resolveResource(WorkFlowContext.WORK_FLOW_TRANSFER_DATA_REFERENCE);
        var contextData = resource.resolveValue(StreamTransferData.class);
        try {
            var request = StreamTransferData.succeed(contextData.getObjects());
            var primary = WorkFlowContext.getPrimary();
            request.add("primary", (Serializable) primary.getValue());
            StreamTransferData streamTransferData = tower.call(request);
            StreamTransferData.merge(contextData, streamTransferData);
            contextData.getObjects().remove("errorMessage");
            contextData.getObjects().remove("errorStack");
            return ActivityResult.valueOf(streamTransferData.getActivityResult());
        } catch (Exception e) {
            log.error("Fail to call actor [{}]", tower.getClass().getName(), e);
            WorkFlowContext.markException(e);
            var streamTransferData = StreamTransferData.failed();
            StreamTransferData.merge(contextData, streamTransferData);
            contextData.add("errorMessage", e.getMessage());
            try (StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw, true);) {
                e.printStackTrace(pw);
                var stack = sw.getBuffer().toString();
                contextData.add("errorStack", stack);
            } catch (IOException e1) {
                log.error("Fail to save error stack in the activity context");
            }
            return ActivityResult.SUSPEND;
        }
    }
}
