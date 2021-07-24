package org.stream.extension.assemble;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationContext;
import org.springframework.util.CollectionUtils;
import org.stream.core.component.Activity;
import org.stream.core.component.ActivityResult;
import org.stream.core.component.Condition;
import org.stream.core.component.Graph;
import org.stream.core.exception.GraphLoadException;
import org.stream.core.execution.GraphContext;
import org.stream.core.execution.WorkFlowContext;
import org.stream.core.helper.AbstractGraphLoader;
import org.stream.core.helper.GraphConfiguration;
import org.stream.core.helper.Jackson;
import org.stream.core.helper.NodeConfiguration;
import org.stream.core.helper.NodeConfiguration.AsyncNodeConfiguration;
import org.stream.core.helper.PlainTextGraphLoader;

import lombok.extern.slf4j.Slf4j;

/**
 * Encapsulation of procedure compiler.
 * A procedure is defined as a series of work to be done in one turn around. Stream work-flow framework
 * was initiated to define a procedure in graph definition file which is constructed of json string.
 * To make it more convenient for developers to cooperate with the stream framework using the
 * Java program language, stream provides the abstract of the way defining the work in Java object instead of writing a graph file
 * in the local file system. Eventually the procedure will be re-compiled into a {@link Graph}, users can use the
 * graph like the way they use graph files.
 * 
 * @author weiguanxiong.
 *
 */
@Slf4j
public class ProcedureCompiler {

    private Activity defaultErrorActivity = new DefaultErrorHanlder();
    private GraphContext graphContext;
    private String graphName;
    private boolean startNodeSpecified = false;
    private String currentAction;
    private List<ProcedureStub> stubs = new LinkedList<>();
    private Map<String, Activity> activities = new HashMap<>();
    private Map<Activity, String> mapping = new HashMap<>();
    private ApplicationContext applicationContext;
    private String startNode;

    public static ProcedureCompiler builder() {
        return new ProcedureCompiler();
    }

    public ProcedureCompiler withSpringContext(final ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        return this;
    }

    public ProcedureCompiler withContext(final GraphContext graphContext) {
        this.graphContext = graphContext;
        return this;
    }

    public ProcedureCompiler withName(final String name) {
        this.graphName = name;
        return this;
    }

    public ProcedureCompiler defaultErrorNode(final Activity activity) {
        this.defaultErrorActivity = activity;
        return this;
    }

    public ProcedureCompiler defaultErrorNode(final String action) {
        return defaultErrorNode(activities.get(action));
    }

    public ProcedureStub startFrom(final String action) {
        this.startNodeSpecified = true;
        this.startNode = action;
        return addAction(action);
    }

    public ProcedureStub addAction(final String action) {
        ProcedureStub procedureStub = new ProcedureStub(this);
        this.currentAction = action;
        return procedureStub;
    }

    /**
     * Build a graph based on the configuration.
     * @throws GraphLoadException Graph load exception.
     * @return Compiled graph.
     */
    public Graph compile() throws GraphLoadException {
        if (!startNodeSpecified) {
            throw new GraphLoadException("Start action must be specified");
        }

        GraphConfiguration graphConfiguration = translate();
        AbstractGraphLoader graphLoader = new PlainTextGraphLoader();
        graphLoader.setApplicationContext(applicationContext);
        graphLoader.setCircuitChecking(true);
        graphLoader.setGraphContext(graphContext);
        return graphLoader.loadGraphFromSource(Jackson.json(graphConfiguration));
    }

    protected void addStub(final ProcedureStub procedureStub) {
        if (procedureStub.getAction() == null) {
            // If the activity is not specified, do nothing.
            return;
        }
        this.activities.put(currentAction, procedureStub.getAction());
        this.mapping.put(procedureStub.getAction(), currentAction);
        graphContext.registerActivity(procedureStub.getAction());
        this.stubs.add(procedureStub);
    }

    private GraphConfiguration translate() {
        GraphConfiguration graphConfiguration = new GraphConfiguration();
        graphConfiguration.setDefaultErrorNode(defaultErrorActivity.getActivityName());
        if (mapping.containsKey(defaultErrorActivity)) {
            graphConfiguration.setDefaultErrorNode(mapping.get(defaultErrorActivity));
        }
        graphConfiguration.setGraphName(graphName);
        graphConfiguration.setStartNode(startNode);
        addNodes(graphConfiguration);
        return graphConfiguration;
    }

    private void addNodes(final GraphConfiguration graphConfiguration) {
        List<NodeConfiguration> nodeConfigurations = new LinkedList<>();
        stubs.forEach(stub -> {
            NodeConfiguration nodeConfiguration = new NodeConfiguration();
            if (stub.getAction() != null) {
                nodeConfiguration.setActivityClass(stub.getAction().getClass().getName());
            }
            if (stub.getTower() != null) {
                nodeConfiguration.setActorClass(stub.getTower().getClass().getName());
            }
            nodeConfiguration.setDescription(stub.getDescription());
            nodeConfiguration.setIntervals(stub.getIntervals());
            nodeConfiguration.setNodeName(retrieveNodeName(stub));
            nodeConfiguration.setSuccessNode(stub.getNextSteps()[ProcedureStub.SUCCEED]);
            nodeConfiguration.setFailNode(stub.getNextSteps()[ProcedureStub.FAILED]);
            nodeConfiguration.setSuspendNode(stub.getNextSteps()[ProcedureStub.SUSPENED]);
            nodeConfiguration.setCheckNode(stub.getNextSteps()[ProcedureStub.CHECKED]);
            nodeConfiguration.setConditions(builderConditions(stub.getConditions()));
            addAsyncDependency(nodeConfiguration, stub);
            nodeConfigurations.add(nodeConfiguration);
        });
        NodeConfiguration[] configurations = new NodeConfiguration[nodeConfigurations.size()];
        nodeConfigurations.toArray(configurations);
        graphConfiguration.setNodes(configurations);
    }

    private void addAsyncDependency(final NodeConfiguration nodeConfiguration, final ProcedureStub stub) {
        if (CollectionUtils.isEmpty(stub.getDependencies())) {
            return;
        }
        AsyncNodeConfiguration[] asyncDependencies = new AsyncNodeConfiguration[stub.getDependencies().size()];
        AtomicInteger counter = new AtomicInteger(0);
        stub.getDependencies().forEach(dependency -> {
            AsyncNodeConfiguration asyncNodeConfiguration = new AsyncNodeConfiguration();
            asyncNodeConfiguration.setAsyncNode(dependency);
            asyncDependencies[counter.getAndIncrement()] = asyncNodeConfiguration;
        });
        nodeConfiguration.setAsyncDependencies(asyncDependencies);
    }

    private String retrieveNodeName(final ProcedureStub procedureStub) {
        if (procedureStub == null) {
            return null;
        }
        return retrieveNodeName(procedureStub.getAction());
    }

    private String retrieveNodeName(final Activity activity) {
        if (activity == null) {
            return null;
        }
        return mapping.get(activity);
    }

    private List<Condition> builderConditions(final Map<Integer, String> conditions) {
        if (conditions == null) {
            return Collections.emptyList();
        }
        return conditions.keySet().parallelStream()
                    .map(key -> new Condition(key, conditions.get(key)))
                    .collect(Collectors.toList());
    }

    public static class DefaultErrorHanlder extends Activity {

        /**
         * {@inheritDoc}
         */
        @Override
        public ActivityResult act() {
            if (WorkFlowContext.extractException() != null) {
                log.error("Error detected", WorkFlowContext.extractException());
                WorkFlowContext.markException(null);
            }
            return ActivityResult.SUCCESS;
        }
    }
}
