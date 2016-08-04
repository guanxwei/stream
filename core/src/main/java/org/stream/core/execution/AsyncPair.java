package org.stream.core.execution;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class AsyncPair {

    private String host;

    private String aysncNode;

}
