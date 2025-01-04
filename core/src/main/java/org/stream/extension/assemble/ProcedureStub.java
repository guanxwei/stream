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

package org.stream.extension.assemble;

import java.util.List;
import java.util.Map;

import org.stream.core.component.Activity;
import org.stream.core.exception.StreamException;
import org.stream.core.runtime.NodeConfiguration;
import org.stream.extension.io.Tower;

import lombok.Getter;

/**
 * A procedure stub that is used to temporally store the step configuration for a {@link ProcedureCompiler}, each step in
 * a procedure contains the instruction that the engine should do when the step is reached.
 *
 * Technically, each stub will be eventually translated into the {@link NodeConfiguration} so that this information can be
 * used by the stream work-flow engines and graph loaders.
 * @author weiguanxiong.
 *
 */
public class ProcedureStub {

    public static final int SUCCEED = 0;
    public static final int FAILED = 1;
    public static final int SUSPENDED = 2;
    public static final int CHECKED = 3;
    public static final int CONDITION = 4;
    public static final int SUBFLOW = 5;

    private final ProcedureCompiler procedureCompiler;
    private int index = -100;
    @Getter
    private String[] nextSteps = new String[16];
    private boolean activityNeeded = false;
    @Getter
    private Activity action;
    @Getter
    private Tower tower;
    @Getter
    private List<String> dependencies;
    @Getter
    private List<String> daemons;
    @Getter
    private Map<Integer, String> conditions;
    @Getter
    private String description;
    @Getter
    private List<Integer> intervals;
    @Getter
    private Map<String, String> subflows;

    public ProcedureStub(final ProcedureCompiler procedureCompiler) {
        this.procedureCompiler = procedureCompiler;
    }

    public ProcedureCompiler done() {
        this.procedureCompiler.addStub(this);
        return this.procedureCompiler;
    }

    public ProcedureStub when(final ProcedureCondition procedureCondition) throws StreamException {
        procedureCondition.onCondition(this);
        return this;
    }

    protected ProcedureStub whenSucceeded() throws StreamException {
        check();
        index = SUCCEED;
        return this;
    }

    protected ProcedureStub whenFailed() throws StreamException {
        check();
        index = FAILED;
        return this;
    }

    protected ProcedureStub whenSuspended() throws StreamException {
        check();
        index = SUSPENDED;
        return this;
    }

    protected ProcedureStub whenChecked() throws StreamException {
        check();
        index = CHECKED;
        return this;
    }

    protected ProcedureStub whenCondition() throws StreamException {
        check();
        index = CONDITION;
        return this;
    }

    protected ProcedureStub whenSubflow() throws StreamException {
        check();
        index = SUBFLOW;
        return this;
    }

    private void check() throws StreamException {
        if (activityNeeded) {
            throw new StreamException("A activity must be specified before configuring new condition steps");
        }
        activityNeeded = true;
    }

    public ProcedureStub description(final String description) {
        this.description = description;
        return this;
    }

    public ProcedureStub intervals(final List<Integer> intervals) {
        this.intervals = intervals;
        return this;
    }

    public ProcedureStub act(final Activity activity) {
        this.action = activity;
        return this;
    }

    public ProcedureStub call(final Tower tower) {
        this.tower = tower;
        return this;
    }

    public ProcedureStub dependsOn(final List<String> activities) throws StreamException {
        this.dependencies = activities;
        return this;
    }

    public ProcedureStub hasDaemon(final List<String> activities) throws StreamException {
        this.daemons = activities;
        return this;
    }

    public ProcedureStub conditions(final Map<Integer, String> conditions) {
        this.conditions = conditions;
        return this;
    }

    public ProcedureStub subflows(final Map<String, String> subflows) {
        this.subflows = subflows;
        return this;
    }

    public ProcedureStub then(final String action) {
        nextSteps[index] = action;
        activityNeeded = false;
        return this;
    }
}
