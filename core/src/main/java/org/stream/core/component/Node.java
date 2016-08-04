package org.stream.core.component;

import java.util.List;

import org.stream.core.execution.NextSteps;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
/**
 * Encapsulation of graphs nodes. Each node holds a concrete logic that graph should invoke at the right time. The workflow engine will invoke the nodes in a graph one by one following
 * the order defined in *.graph file. Also, stream provides a mechanism that customers can define series async tasks, customers will benifit from the power java async framework provides. 
 * Customers can add async tasks as a Node's asyncDependecncies property, formally they will be treated as Nodes but hosted in a outernal Node. The workflow engine will construct aysnc workers
 * and execute the async tasks before it invoke the host Nodes. When the async tasks are completed, customers can use the result they provide to complete other tasks.
 * 
 * Customers should always keep in mind that, they should be responsible for the async tasks created themselves. Before they try to reboot the workflow or close the workflow, they'd make sure
 * all necessary work has been done.
 */
public class Node {

    private String nodeName;

    private Activity activity;

    private NextSteps next;

    private Graph graph;

    private List<Node> asyncDependencies;

    public ActivityResult perform() {
        return activity.act();
    }

}
