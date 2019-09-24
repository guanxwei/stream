package org.stream.core.helper;

import org.stream.core.component.Graph;
import org.stream.core.exception.GraphLoadException;
import org.stream.core.execution.Engine;

/**
 * A graph loader who is responsible to load the graph definition from the input source,
 * parse the definition and translate them into java objects so that {@link Engine} implementation
 * can use them to execute tasks.
 * @author weiguanxiong
 *
 */
public interface GraphLoader {

    /**
     * Load graph from the input source. A input source may be a local file or a remote http page or somewhere else.
     * @param sourcePath Source path of the graph definition file is located. It can be located at the local disk 
     *      or even can be located on remote server retrieved by HTTP apis.
     * @return A graph loaded from the source path.
     * @throws GraphLoadException Exception thrown when loading the graph.
     */
    Graph loadGraphFromSource(final String sourcePath) throws GraphLoadException;
}
