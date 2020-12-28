package org.stream.core.component;

import lombok.Data;

/**
 * Abstract of work-flow condition configuration.
 * @author guanxiong wei
 *
 */
@Data
public class Condition {

    /**
     * Condition value.
     */
    private int condition;

    /**
     * Next step.
     */
    private String nextStep;
}