package org.stream.core.execution;

import lombok.Builder;
import lombok.Data;

/**
 * Async pair to link up the host node and the asynchronous node.
 * @author hzweiguanxiong
 *
 */
@Builder
@Data
public class AsyncPair {

    // Target node's name.
    private String host;

    // Asynchronous node's name.
    private String aysncNode;

}
