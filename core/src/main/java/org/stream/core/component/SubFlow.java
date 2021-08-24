package org.stream.core.component;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Abstract of work-flow sub procedure configuration.
 * @author guanxiong wei
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubFlow {

    // Target procedure to be executed as sub workflow.
    private String target;

    // Target procedure's corresponding graph name.
    private String graph;
}
