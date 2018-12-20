package org.stream.core.component;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.stream.core.execution.NextSteps;

import lombok.Builder;
import lombok.Data;


/**
 * Encapsulation of graph nodes. Each node holds a concrete entity {@link Activity} who is responsible to do the designed work.
 *
 * The work-flow engine will invoke the nodes in a graph one by one following the order defined in *.graph files, except asynchronized tasks,
 * stream work flow framework provides a asynchronous mechanism to help customers to leverage from asynchronous concurrent processing.
 *
 * Nodes will be treated as singleton instances in single JVM context and can be shared in multi-graphs running in multi-threads
 * while having no side-effect.
 *
 * Customers can easily benefit from asynchronous processing by just adding asynchronous dependencies to the target node in graph definition files,
 * the only requirement is that these nodes' activities should extend {@link AsyncActivity}. For detail, please refer to the sample graph files located
 * in the test resource folder.
 *
 * The work-flow engine will help construct asynchronous workers and submit the asynchronous tasks to thread pool
 * before it invoke the host Nodes. When the asynchronous tasks are completed, customers can use the result they provide to complete other tasks.
 *
 * Customers should always keep in mind that, they should be responsible for managing the asynchronous activities created by themselves.
 * Before they try to reboot the work-flow or leave or close the work-flow, they'd make sure
 * all necessary work has been done, for example, {@link AsyncActivity#cleanUp()} should be invoked before leave the work flow.
 */
@Builder
@Data
public class Node {

    // Node name
    private String nodeName;

    // Underlying activity that will the performed when invoked the work flow engine.
    private Activity activity;

    // The next node that will be executed when everything is okay.
    private NextSteps next;

    // Host graph, managing the brother nodes.
    private Graph graph;

    // Asynchronous dependencies.
    private List<Node> asyncDependencies;

    // Retry intervals.
    private List<Integer> intervals;

    /**
     * Thread local storage to hold the reference to the current invoking node.
     */
    public static final ThreadLocal<Node> CURRENT = new ThreadLocal<>();

    /**
     * Perform the configured activity's job.
     * @return Activity execution result.
     */
    public ActivityResult perform() {
        return activity.act();
    }

    /**
     * Get the time to be elapsed before rerun the next invocation.
     * @param times Retry times.
     * @return Time to be elapsed.
     */
    public int getNextRetryInterval(final int times) {
        if (CollectionUtils.isEmpty(intervals)) {
            return 0;
        }

        if (intervals.size() <= times) {
            return intervals.get(intervals.size() - 1);
        }

        return intervals.get(times);
    }
}
