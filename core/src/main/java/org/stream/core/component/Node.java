package org.stream.core.component;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.stream.core.execution.NextSteps;

import lombok.Builder;
import lombok.Data;

/**
 * Encapsulation of graph nodes.
 * Each node holds a concrete entity {@link Activity} who is responsible to do the designed work.
 *
 * The work-flow engines will invoke the nodes in a graph one by one following
 * the order defined in *.graph files except asynchronous tasks.
 *
 * Stream work flow framework provides a mechanism to help customers
 * leverage from concurrent processing. Customer can make a node as asynchronous just by adding
 * it the to the target node as it's asyncDependencies. AsyncDependencies nodes will be executed
 * parallel when executing the target node.
 * <p> For example <p>
 * <code>
 *     <br>{
          <br>"graphName":"ComprehensiveWithAsyncNodeCase",
          <br>"resourceType":"OBJECT",
          <br>"startNode":"node1",
          <br>"defaultErrorNode":"node3",
          <br>"nodes":[
            <br>{
              <br>"nodeName":"node1",
              <br>"activityClass":"org.stream.core.test.base.TestActivity",
              <br>"successNode":"node2",
              <br>"failNode":"node3",
              <br>"asyncDependencies": [
                <br>{
                  <br>"asyncNode":"node4"
                <br>}
              <br>]
            <br>},
            <br>{
              <br>"nodeName":"node2",
              <br>"activityClass":"org.stream.core.test.base.SuccessTestActivity",
              <br>"failNode":"node3"
            <br>},
            <br>{
              <br>"nodeName":"node3",
              <br>"activityClass":"org.stream.core.test.base.FailTestActivity"
            <br>},
            <br>{
              <br>"nodeName":"node4",
              <br>"activityClass":"org.stream.core.test.base.AsyncTestActivity"
            <br>}
          <br>]
        <br>}
 * </code>
 * <br>The only difference between the normals nodes is that these nodes' activities should extend {@link AsyncActivity}.
 * For detail, please refer to the sample graph files located in the test resource folder.
 *
 * Nodes will be treated as singleton instances in single JVM context and can be shared in multiple graphs running in multiple threads
 * while having no side-effect.
 *
 */
@Builder
@Data
public class Node {

    // Node name
    private String nodeName;

    // Underlying activity that will do the real job when invoked by the work flow engine.
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
