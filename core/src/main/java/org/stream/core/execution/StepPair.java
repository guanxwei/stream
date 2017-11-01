package org.stream.core.execution;

import org.stream.core.execution.NextSteps.NextStepType;

import lombok.Builder;
import lombok.Data;

/**
 * Encapsulation of work-flow execution step information.
 *
 * Each pair will indicate the host node's name and the successor's name,
 * and the next step's type.
 * @author hzweiguanxiong
 *
 */
@Data
@Builder
public class StepPair {

    private String predecessor;

    private String successor;

    private NextStepType nextStepType;
}
