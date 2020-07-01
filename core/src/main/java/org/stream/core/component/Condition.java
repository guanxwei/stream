package org.stream.core.component;

import lombok.Data;

/**
 * Abstract of work-flow condition configuration.
 * @author weiguanxiong
 *
 */
@Data
public class Condition {

    /**
     * The host node.
     */
    private String nodeName;

    /**
     * Condition value.
     */
    private int condition;

    /**
     * Next step.
     */
    private String nextStep;
}
