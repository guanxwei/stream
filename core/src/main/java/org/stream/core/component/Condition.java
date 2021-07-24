package org.stream.core.component;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Abstract of work-flow condition configuration.
 * @author guanxiong wei
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
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