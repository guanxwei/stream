/*
 * Copyright (C) 2021 guanxiongwei
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.stream.core.component;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.stream.core.execution.NextSteps;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

/**
 * Encapsulation of graph nodes.
 * Each node holds a concrete entity {@link Activity} who is responsible to do the designed work.
 * <p>
 * The work-flow engines will invoke the nodes in a graph one by one following
 * the order defined in *.graph files except asynchronous tasks.
 * <p>
 * Stream work flow framework provides a mechanism to help customers
 * leverage from concurrent processing. Customer can make a node as asynchronous just by adding
 * it to the target node as its asyncDependencies. AsyncDependencies nodes will be executed
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
 * <br>The only difference between the normal nodes is that these nodes' activities should extend {@link AsyncActivity}.
 * For detail, please refer to the sample graph files located in the test resource folder.
 * <p>
 * Nodes will be treated as singleton instances in a single JVM context and can be shared in multiple graphs running in multiple threads
 * while having no side effect.
 *
 */
@Builder
@Data
@Slf4j
@ToString(exclude = {"graph"})
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

    // Asynchronous dependencies that should be run as daemon works. Workflow engine should not cancel them when they finish the current execution flow.
    private List<Node> daemons;

    // Retry intervals.
    private List<Integer> intervals;

    // The flag indicated it the node is degradable,
    // once the engine found that this node is not available and the flag is set as true,
    // the engine may skip execute this node until it's available again.
    private boolean degradable;

    /**
     * Detail description of the purpose of the node.
     */
    private String description;

    /**
     * Condition configuration detail.
     */
    private List<Condition> conditions;

    /**
     * Sub workflow configuration.
     */
    private List<SubFlow> subflows;

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
            return intervals.getLast();
        }

        return intervals.get(times);
    }

    /**
     * Retrieve a next step node from the graph by returned condition code.
     * @param conditionCode Condition code.
     * @return Next step node.
     */
    public Node getNode(final int conditionCode) {
        if (CollectionUtils.isEmpty(conditions)) {
            log.error("Condition is not configured, please update your graph before using condition strategy");
            return graph.getDefaultErrorNode();
        }
        for (Condition condition : conditions) {
            if (condition.getCode() == conditionCode) {
                return graph.getNode(condition.getNextStep());
            }
        }
        log.error("Condition configuration info is not sufficient, can not find the next node according to the"
                + " condition code [{}] and node name [{}]", conditionCode, conditionCode);
        return null;
    }
}
