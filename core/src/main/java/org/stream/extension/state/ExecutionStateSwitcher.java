package org.stream.extension.state;

import org.stream.core.component.ActivityResult;
import org.stream.core.component.Graph;
import org.stream.core.component.Node;

/**
 * A execution state used to check the state of the running work-flow instances,
 * terminate abnormal workflows.
 * @author 魏冠雄
 *
 */
public interface ExecutionStateSwitcher {

    /**
     * Determine if the work flow is out of control.
     * @param previous Previous node.
     * @param next Next node.
     * @param activityResult The activity result the previous node returned.
     * @return {@code true} the work flow is running in 
     */
    boolean isOpen(final Node previous, final Node next, final ActivityResult activityResult);

    /**
     * Terminate the work-flow.
     * Normally will return null so that work flow engine can stop executing the next node.
     * @param graph Target graph.
     * @param previous Previous node.
     * @return End node that will terminate the workflow engine.
     */
    Node open(final Graph graph, final Node previous);

    /**
     * Clear the execution context so that it can be reused for other work flow instances.
     */
    void clear();
}
