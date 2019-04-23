package org.stream.core.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Resource;

import org.springframework.context.ApplicationContext;
import org.stream.core.component.Activity;
import org.stream.core.component.ActivityResult;
import org.stream.core.component.ActivityResult.Visitor;
import org.stream.core.component.Graph;
import org.stream.core.component.Node;
import org.stream.core.component.TowerActivity;
import org.stream.core.exception.GraphLoadException;
import org.stream.core.execution.AsyncPair;
import org.stream.core.execution.GraphContext;
import org.stream.core.execution.NextSteps;
import org.stream.core.execution.NextSteps.NextStepType;
import org.stream.core.execution.StepPair;
import org.stream.core.helper.NodeConfiguration.AsyncNodeConfiguration;
import org.stream.extension.io.Tower;

import com.google.gson.Gson;

import lombok.Data;

/**
 * Abstract of stream graph loader. Define the basic procedure to load a graph the input resource,
 * a resource may refer to a local file or even a remote HTTP page. Implementations only have to implement
 * the method {@link #loadInputStream(String)}, which load the graph as input stream from the input resource.
 * Other standard work will all be done by the pre-defined methods.
 *
 * @author weiguanxiong
 *
 */
@Data
public abstract class AbstractGraphLoader implements GraphLoader {

    private static final Gson GSON = new Gson();

    private GraphContext graphContext;

    @Resource
    private ApplicationContext applicationContext;

    private boolean circuitChecking = false;

    protected List<String> graphFilePaths;

    /**
     * {@inheritDoc}
     */
    @Override
    public Graph loadGraphFromSource(final String sourcePath) throws GraphLoadException {
        InputStream inputStream = loadInputStream(sourcePath);
        Graph graph = loadGraph(inputStream);

        graph.setOriginalDefinition(sourcePath);
        return graph;
    }

    /**
     * Set the spring application context so that the spring framework can manage the life cycle of the graphs and their
     * underling activities.
     * 
     * @param applicationContext Spring application context.
     */
    public void setSpringAppplicationContext(final ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    protected abstract InputStream loadInputStream(final String sourcePath) throws GraphLoadException;

    private Graph loadGraph(final InputStream inputStream) throws GraphLoadException {
        Graph graph = new Graph();
        StringBuilder cause = new StringBuilder(100);
        List<StepPair> stepPairs = new LinkedList<StepPair>();
        List<AsyncPair> asyncPairs = new LinkedList<AsyncPair>();
        Map<String, Node> knowNodes = new HashMap<String, Node>();
        try {
            parse(graph, inputStream, stepPairs, asyncPairs, knowNodes, cause);
            inputStream.close();
        } catch (IOException e) {
            throw new GraphLoadException("Failed to load graph configuration information from the definition file", e);
        } catch (ClassNotFoundException e) {
            throw new GraphLoadException(String.format("Class is not found, name is [%s]", cause), e);
        } catch (InstantiationException e) {
            throw new GraphLoadException(String.format("Failed to initiate class [%s]", cause), e);
        } catch (IllegalAccessException e) {
            throw new GraphLoadException(String.format("No access to class [%s]", cause), e);
        }
        if (circuitChecking) {
            checkCircuit(graph);
        }
        graphContext.addGraph(graph);
        return graph;
    }

    private void parse(final Graph graph, final InputStream input, final List<StepPair> stepPairs, final List<AsyncPair> asyncPairs,
            final Map<String, Node> knowNodes, final StringBuilder cause) throws GraphLoadException, ClassNotFoundException,
                    InstantiationException, IllegalAccessException, IOException {
        GraphConfiguration graphConfiguration = parseGraphConfiguration(buildStringfyInput(input), graph);
        graph.setPrimaryResourceType(graphConfiguration.getPrimaryResourceType());
        NodeConfiguration[] nodes = graphConfiguration.getNodes();
        List<Node> staticNodes = new ArrayList<Node>();
        for (NodeConfiguration nodeConfiguration : nodes) {
            parseNodeConfiguration(nodeConfiguration, graph, stepPairs, asyncPairs, knowNodes, cause, staticNodes);
        }

        setDefaultHandlers(graph, staticNodes, knowNodes, graphConfiguration);

        /**
         * Set up node relationship net.
         */
        for (StepPair pair : stepPairs) {
            trackNodes(knowNodes, pair);
        }

        resolveAsyncDependencies(asyncPairs, knowNodes);
    }

    private GraphConfiguration parseGraphConfiguration(final String compressedInputString, final Graph graph) throws GraphLoadException {
        GraphConfiguration graphConfiguration = GSON.fromJson(compressedInputString, GraphConfiguration.class);
        checkGraphConfiguration(graphConfiguration);
        graph.setGraphName(graphConfiguration.getGraphName());
        if (graphConfiguration.getResourceType() == null) {
            throw new GraphLoadException("ResourceType is not specified!");
        }
        return graphConfiguration;
    }

    private void setDefaultHandlers(final Graph graph, final List<Node> staticNodes, final Map<String, Node> knowNodes,
            final GraphConfiguration graphConfiguration) {
        graph.setNodes(staticNodes);
        graph.setStartNode(knowNodes.get(graphConfiguration.getStartNode()));
        graph.setDefaultErrorNode(knowNodes.get(graphConfiguration.getDefaultErrorNode()));
    }

    private void parseNodeConfiguration(final NodeConfiguration nodeConfiguration, final Graph graph, final List<StepPair> stepPairs,
            final List<AsyncPair> asyncPairs, final Map<String, Node> knowNodes, final StringBuilder cause, final List<Node> staticNodes)
                    throws GraphLoadException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        checkNodeConfiguration(nodeConfiguration);
        String currentNodeName = nodeConfiguration.getNodeName();
        String providerClass = nodeConfiguration.getProviderClass();
        Activity activity = null;
        if (!graphContext.isActivityRegistered(providerClass)) {
            cause.append(providerClass);
            Class<?> clazz = Class.forName(providerClass);
            if (Tower.class.isAssignableFrom(clazz)) {
                // Actor case works in spring context only.
                activity = processActorCase(clazz);
            } else {
                activity = processLocalCase(clazz);
            }
            graphContext.registerActivity(activity);
        } else {
            activity = graphContext.getActivity(providerClass);
        }

        stepPairs.addAll(setUpNextSteps(nodeConfiguration, currentNodeName));
        asyncPairs.addAll(setUpAsyncPairs(nodeConfiguration, currentNodeName));

        Node node = Node.builder()
                .activity(activity)
                .nodeName(currentNodeName)
                .next(new NextSteps())
                .graph(graph)
                .build();

        staticNodes.add(node);
        node.setIntervals(nodeConfiguration.getIntervals());
        knowNodes.put(node.getNodeName(), node);
    }

    private Activity processLocalCase(final Class<?> clazz) throws GraphLoadException {
        if (applicationContext != null && !applicationContext.getBeansOfType(Activity.class).isEmpty()) {
            Map<String, Activity> candidates = applicationContext.getBeansOfType(Activity.class);
            Optional<Activity> activityOptional = candidates.values().stream()
                    .filter(candidate -> clazz.isAssignableFrom(candidate.getClass()))
                    .findFirst();
            if (activityOptional.isPresent()) {
                return activityOptional.get();
            }
        } else {
            try {
                return (Activity) clazz.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new GraphLoadException(e);
            }
        }
        throw new GraphLoadException(String.format("Fail to initiate or locate the activity %s",
                clazz.getName()));
    }

    private <T> TowerActivity<T> processActorCase(final Class<?> clazz) {
        /**
         * Load the actor from the spring context, typically the actor is a proxy object that can be used
         * to communicate with a remote server, for example a RPC client.
         */
        @SuppressWarnings("unchecked")
        Tower<T> actor = (Tower<T>) applicationContext.getBean(clazz);
        TowerActivity<T> activity = new TowerActivity<T>(actor);
        return activity;
    }

    private void trackNodes(final Map<String, Node> knowNodes, final StepPair pair) {
        final Node predecessor = knowNodes.get(pair.getPredecessor());
        final Node successor = knowNodes.get(pair.getSuccessor());
        final NextStepType type = pair.getNextStepType();
        for (final ActivityResult result : ActivityResult.values()) {
            result.accept(new Visitor<Void>() {
                public Void success() {
                    if (type.equals(NextStepType.SUCCESS)) {
                        predecessor.getNext().setSuccess(successor);
                    }
                    return null;
                }

                public Void fail() {
                    if (type.equals(NextStepType.FAIL)) {
                        predecessor.getNext().setFail(successor);
                    }
                    return null;
                }

                public Void suspend() {
                    if (type.equals(NextStepType.SUSPEND)) {
                        predecessor.getNext().setSuspend(successor);
                    }
                    return null;
                }

                @Override
                public Void check() {
                    if (type.equals(NextStepType.CHECK)) {
                        predecessor.getNext().setCheck(successor);
                    }
                    return null;
                }
            });
        }
    }

    private void resolveAsyncDependencies(final List<AsyncPair> asyncPairs, final Map<String, Node> knowNodes) {
        asyncPairs.forEach(asyncPair -> {
            String host = asyncPair.getHost();
            Node hostNode = knowNodes.get(host);
            List<Node> asyncNodes = hostNode.getAsyncDependencies() == null ? new LinkedList<>() : hostNode.getAsyncDependencies();
            asyncNodes.add(knowNodes.get(asyncPair.getAysncNode()));
            hostNode.setAsyncDependencies(asyncNodes);
        });
    }

    private String buildStringfyInput(final InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
        String singleLine = null;
        StringBuffer buffer = new StringBuffer();
        while ((singleLine = reader.readLine()) != null) {
            buffer.append(singleLine.trim());
        }
        return buffer.toString();
    }

    private void checkGraphConfiguration(final GraphConfiguration graphConfiguration) throws GraphLoadException {
        String graphName = graphConfiguration.getGraphName();
        String startNode = graphConfiguration.getStartNode();
        NodeConfiguration[] nodes = graphConfiguration.getNodes();
        if (graphName == null) {
            throw new GraphLoadException("There is something wrong in the graph definition file, graph name should not be empty or missing!");
        }
        if (startNode == null) {
            throw new GraphLoadException("Start node is not specified!");
        }
        if (nodes == null || nodes.length == 0) {
            throw new GraphLoadException("No node definition information found in the graph definition file!");
        }
    }

    private void checkNodeConfiguration(final NodeConfiguration nodeConfiguration) throws GraphLoadException {
        String nodeName = nodeConfiguration.getNodeName();
        String providerClass = nodeConfiguration.getProviderClass();
        if (nodeName == null || nodeName.length() == 0) {
            throw new GraphLoadException("Node name is not specified!");
        }
        if (providerClass == null || providerClass.length() == 0 || !providerClass.contains(".")) {
            throw new GraphLoadException("Provider class name is not correct or not specified!");
        }
    }

    private List<StepPair> setUpNextSteps(final NodeConfiguration nodeConfiguration, final String predecessorNode) {
        String successStep = nodeConfiguration.getSuccessNode();
        String failStep = nodeConfiguration.getFailNode();
        String suspendStep = nodeConfiguration.getSuspendNode();
        String checkStep = nodeConfiguration.getCheckNode();
        List<StepPair> list = new LinkedList<StepPair>();
        String[] nexts = {successStep, failStep, suspendStep, checkStep};
        int count = 0;
        for (String nextStep : nexts) {
            if (nextStep != null) {
                StepPair pair = StepPair.builder()
                        .predecessor(predecessorNode)
                        .successor(nextStep)
                        .nextStepType(NextStepType.values()[count++])
                        .build();
                list.add(pair);
            } else {
                count++;
            }
        }
        return list;
    }

    private List<AsyncPair> setUpAsyncPairs(final NodeConfiguration nodeConfiguration, final String host) {
        List<AsyncPair> list = new LinkedList<>();

        /**
         * Extract the asynchronous dependency nodes of the current node.
         */
        AsyncNodeConfiguration[] asyncDependencies = nodeConfiguration.getAsyncDependencies();
        if (asyncDependencies ==  null || asyncDependencies.length == 0) {
            return list;
        }

        for (AsyncNodeConfiguration asyncConfiguration : asyncDependencies) {
            AsyncPair asyncPair = AsyncPair.builder()
                    .host(host)
                    .aysncNode(asyncConfiguration.getAsyncNode())
                    .build();
            list.add(asyncPair);
        }

        return list;
    }

    private void checkCircuit(final Graph graph) throws GraphLoadException {
        Set<String> nodes = new HashSet<>();
        Node targetNode = graph.getStartNode();
        nodes.add(targetNode.getNodeName());
        doCheck(targetNode, nodes, graph);
    }

    private void doCheck(final Node targetNode, final Set<String> nodes, final Graph graph) throws GraphLoadException {
        if (targetNode == null) {
            return;
        }

        NextSteps nextSteps = targetNode.getNext();
        doCheckChild(targetNode, nextSteps.onSuccess(), nodes, graph);
        doCheckChild(targetNode, nextSteps.onCheck(), nodes, graph);
        doCheckChild(targetNode, nextSteps.onFail(), nodes, graph);
    }

    private void doCheckChild(final Node targetNode, final Node child, final Set<String> nodes, final Graph graph) throws GraphLoadException {
        if (child == null) {
            return;
        }

        if (nodes.contains(child.getNodeName())) {
            throw new GraphLoadException(String.format("Circuit condition found at node [%s] in graph [%s]",
                    targetNode.getNodeName(), graph.getGraphName()));
        }

        nodes.add(child.getNodeName());
        doCheck(child, nodes, graph);
        nodes.remove(child.getNodeName());
    }
}
