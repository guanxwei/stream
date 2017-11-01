package org.stream.core.execution;

import lombok.Builder;
import lombok.Data;

/**
 * Async pair to link up the host node and the aysnc node.
 * @author hzweiguanxiong
 *
 */
@Builder
@Data
public class AsyncPair {

    private String host;

    private String aysncNode;

}
