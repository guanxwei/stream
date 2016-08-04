package org.stream.core.execution;

import org.stream.core.execution.NextSteps.NextStepType;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StepPair {

    private String predecessor;

    private String successor;

    private NextStepType nextStepType;
}
