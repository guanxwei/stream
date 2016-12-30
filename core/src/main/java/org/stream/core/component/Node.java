package org.stream.core.component;

import java.util.List;

import org.stream.core.execution.NextSteps;

import lombok.Builder;
import lombok.Data;


/**
 * Encapsulation of graph nodes. Each node holds a concrete entity {@link Activity} executing specific logic that graph should invoke at the right time.
 * The work-flow engine will invoke the nodes in a graph one by one following the order defined in *.graph files. Except synchronized tasks, stream also 
 * provides a asynchronized mechanism that customers can benefit from async tasks. 
 * To gain the async tasks's profit, customers can add async tasks as a Node's asyncDependecncies property, The work-flow engine will construct aysnc workers
 * and submit the async tasks to thread pool before it invoke the host Nodes. When the async tasks are completed, customers can use the result they provide to complete other tasks.
 * 
 * Customers should always keep in mind that, they should be responsible for managing the async tasks created themselves.
 * Before they try to reboot the work-flow or leave or close the work-flow, they'd make sure
 * all necessary work has been done for example de-link the async tasks with the work-flow instance so that GC can collect the memory collectly.
 */
@Builder
@Data
public class Node {

    private String nodeName;

    private Activity activity;

    private NextSteps next;

    private Graph graph;

    private List<Node> asyncDependencies;

    /**
     * Invoked by the work-flow engine causing the activity being performed.
     * @return
     */
    public ActivityResult perform() {
        return activity.act();
    }

}
