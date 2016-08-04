package org.stream.core.execution;

import org.stream.core.component.Node;

import lombok.Setter;

public class NextSteps {

    @Setter
    private Node success;

    @Setter
    private Node fail;

    @Setter
    private Node suspend;

    public Node onSuccess() {
        return success;
    }

    public Node onFail() {
        return fail;
    }

    public Node onSuspend() {
        return suspend;
    }

    public enum NextStepType {
        SUCCESS, FAIL, SUSPEND;
    }
}
