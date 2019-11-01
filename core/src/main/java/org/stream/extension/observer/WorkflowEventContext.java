package org.stream.extension.observer;

import org.stream.core.component.Node;

import lombok.Data;

@Data
public class WorkflowEventContext {

    private String procedure;

    private Node currentNode;

    private Object source;

    public static WorkflowEventContext of (final Node node, final Object source) {
        WorkflowEventContext workflowEventContext = new WorkflowEventContext();
        workflowEventContext.setCurrentNode(node);
        workflowEventContext.setProcedure(node.getGraph().getGraphName());
        workflowEventContext.setSource(source);

        return workflowEventContext;
    }
}
