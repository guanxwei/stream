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

package org.stream.core.runtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.annotation.Resource;

import org.apache.commons.lang3.ArrayUtils;
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
import org.stream.core.runtime.NodeConfiguration.AsyncNodeConfiguration;
import org.stream.core.runtime.NodeConfiguration.DaemonNodeConfiguration;
import org.stream.core.sentinel.SentinelConfiguration;
import org.stream.core.sentinel.SentinelRuleType;
import org.stream.extension.io.Tower;

import com.google.gson.Gson;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * Abstract of stream graph loader. Define the basic procedure to load a graph from the input resource.
 * Abstract graph loader will first translate the input source into an input-stream object, then read
 * the graph detail from the stream. Implementations should only override the method {@link #loadGraphFromSource(String)}.
 *
 * @author weiguanxiong
 *
 */
@Data
@Slf4j
public abstract class AbstractGraphLoader implements GraphLoader {

    private static final Gson GSON = new Gson();

    private GraphContext graphContext;

    @Resource
    private ApplicationContext applicationContext;

    private boolean circuitChecking = false;

    protected List<String> graphFilePaths = new LinkedList<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public Graph loadGraphFromSource(final String sourcePath) throws GraphLoadException {
        log.info("Trying to load graph from [{}] now", sourcePath);
        try (InputStream inputStream = loadInputStream(sourcePath)) {
            Graph graph = loadGraph(inputStream, sourcePath);
            graph.setOriginalDefinition(sourcePath);
            return graph;
        } catch (Exception e) {
            throw wrap(e);
        }
    }

    private GraphLoadException wrap(final Exception e) {
        GraphLoadException throwable;
        if (e instanceof GraphLoadException) {
            throwable = (GraphLoadException) e;
        } else {
            throwable = new GraphLoadException(e);
        }
        return throwable;
    }

    /**
     * Load input stream from the source path.
     * A source path could be file path in local storage, or an url to a remote file.
     * @param sourcePath Path to the graph definition file.
     * @return Input stream of the graph file.
     * @throws GraphLoadException Graph load exception.
     */
    protected abstract InputStream loadInputStream(final String sourcePath) throws GraphLoadException;

    private Graph loadGraph(final InputStream inputStream, final String sourcePath) throws GraphLoadException {
        Graph graph = new Graph();
        // error cause
        StringBuilder cause = new StringBuilder(100);
        // Node and it's successor node pairs.
        List<StepPair> stepPairs = new LinkedList<>();
        // Node and it's async dependency pairs.
        List<AsyncPair> asyncPairs = new LinkedList<>();
        // Node and it's daemon task pairs.
        List<AsyncPair> daemonPairs = new LinkedList<>();
        // Tracked nodes in the graph.
        Map<String, Node> knowNodes = new HashMap<>();
        try {
            parse(graph, inputStream, stepPairs, asyncPairs, daemonPairs, knowNodes, cause);
        } catch (IOException e) {
            throw new GraphLoadException("Failed to load graph configuration information from the definition file " + sourcePath, e);
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

    private void parse(final Graph graph, final InputStream input, final List<StepPair> stepPairs, final List<AsyncPair> asyncPairs, final List<AsyncPair> daemonPairs,
            final Map<String, Node> knowNodes, final StringBuilder cause) throws GraphLoadException, ClassNotFoundException,
                    InstantiationException, IllegalAccessException, IOException {
        String json = buildStringInput(input);
        log.info("Graph definition [{}] parsed from the input source", json);
        GraphConfiguration graphConfiguration = parseGraphConfiguration(json, graph);
        graph.setPrimaryResourceType(graphConfiguration.getPrimaryResourceType());
        NodeConfiguration[] nodes = graphConfiguration.getNodes();
        List<Node> staticNodes = new ArrayList<>();
        for (NodeConfiguration nodeConfiguration : nodes) {
            parseNodeConfiguration(nodeConfiguration, graph, stepPairs, asyncPairs, daemonPairs, knowNodes, cause, staticNodes);
        }

        setDefaultHandlers(graph, staticNodes, knowNodes, graphConfiguration);

        for (StepPair pair : stepPairs) {
            trackNodes(knowNodes, pair);
        }

        resolveAsyncDependencies(asyncPairs, knowNodes);
        resolveDaemonDependencies(daemonPairs, knowNodes);
    }

    private GraphConfiguration parseGraphConfiguration(final String compressedInputString, final Graph graph) throws GraphLoadException {
        GraphConfiguration graphConfiguration = GSON.fromJson(compressedInputString, GraphConfiguration.class);
        checkGraphConfiguration(graphConfiguration);
        graph.setGraphName(graphConfiguration.getGraphName());
        return graphConfiguration;
    }

    private void setDefaultHandlers(final Graph graph, final List<Node> staticNodes, final Map<String, Node> knowNodes,
            final GraphConfiguration graphConfiguration) {
        graph.setNodes(staticNodes);
        graph.setStartNode(knowNodes.get(graphConfiguration.getStartNode()));
        graph.setDefaultErrorNode(knowNodes.get(graphConfiguration.getDefaultErrorNode()));
    }

    private void parseNodeConfiguration(final NodeConfiguration nodeConfiguration, final Graph graph, final List<StepPair> stepPairs,
            final List<AsyncPair> asyncPairs, final List<AsyncPair> daemonPairs, final Map<String, Node> knowNodes, final StringBuilder cause, final List<Node> staticNodes)
                    throws GraphLoadException, ClassNotFoundException {
        checkNodeConfiguration(nodeConfiguration);
        var currentNodeName = nodeConfiguration.getNodeName();
        Activity activity = initiateActivity(nodeConfiguration.getProviderClass(), cause);

        stepPairs.addAll(setUpNextSteps(nodeConfiguration, currentNodeName));
        asyncPairs.addAll(setUpAsyncPairs(nodeConfiguration, currentNodeName));
        daemonPairs.addAll(setUpDaemonPairs(nodeConfiguration, currentNodeName));

        Node node = Node.builder()
                .activity(activity)
                .conditions(nodeConfiguration.getConditions())
                .degradable(nodeConfiguration.isDegradable())
                .graph(graph)
                .nodeName(currentNodeName)
                .next(new NextSteps())
                .subflows(nodeConfiguration.getSubflows())
                .build();

        staticNodes.add(node);
        node.setIntervals(nodeConfiguration.getIntervals());
        knowNodes.put(node.getNodeName(), node);

        addSentinelHook(graph, nodeConfiguration);
    }

    private Activity initiateActivity(final String providerClass, final StringBuilder cause) throws ClassNotFoundException, GraphLoadException {
        if (!graphContext.isActivityRegistered(providerClass)) {
            cause.append(providerClass);
            Class<?> clazz = Class.forName(providerClass);
            Activity activity = null;
            if (Tower.class.isAssignableFrom(clazz)) {
                // Actor case works in spring context only.
                activity = processActorCase(clazz);
            } else {
                activity = processLocalCase(clazz);
            }
            graphContext.registerActivity(activity);
            return activity;
        } else {
            return graphContext.getActivity(providerClass);
        }
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

    private TowerActivity processActorCase(final Class<?> clazz) {
        Tower actor = (Tower) applicationContext.getBean(clazz);
        return new TowerActivity(actor);
    }

    private void trackNodes(final Map<String, Node> knowNodes, final StepPair pair) throws GraphLoadException {
        final Node predecessor = knowNodes.get(pair.getPredecessor());
        final Node successor = knowNodes.get(pair.getSuccessor());
        if (successor == null) {
            throw new GraphLoadException(String.format("Node %s not found for "
                    + "predecessor node %s with step type %s", pair.getSuccessor(), pair.getPredecessor(),
                    pair.getNextStepType().name()));
        }
        final NextStepType type = pair.getNextStepType();
        for (final ActivityResult result : ActivityResult.values()) {
            result.accept(new Visitor<Void>() {
                public Void onSuccess() {
                    if (type.equals(NextStepType.SUCCESS)) {
                        predecessor.getNext().setSuccess(successor);
                    }
                    return null;
                }

                public Void onFail() {
                    if (type.equals(NextStepType.FAIL)) {
                        predecessor.getNext().setFail(successor);
                    }
                    return null;
                }

                public Void onSuspend() {
                    if (type.equals(NextStepType.SUSPEND)) {
                        predecessor.getNext().setSuspend(successor);
                    }
                    return null;
                }

                @Override
                public Void onCheck() {
                    if (type.equals(NextStepType.CHECK)) {
                        predecessor.getNext().setCheck(successor);
                    }
                    return null;
                }

                @Override
                public Void onCondition() {
                    // Do nothing when we get on condition result, let the workflow engines determine in run-time which
                    // node to be continued
                    return null;
                }

                @Override
                public Void onInvoke() {
                    // Do nothing when we get on invoke result, let the workflow engines determine in run-time which
                    // child procedure to be executed.
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
            asyncNodes.add(knowNodes.get(asyncPair.getAsyncNode()));
            hostNode.setAsyncDependencies(asyncNodes);
        });
    }

    private void resolveDaemonDependencies(final List<AsyncPair> daemonPairs, final Map<String, Node> knowNodes) {
        daemonPairs.forEach(asyncPair -> {
            String host = asyncPair.getHost();
            Node hostNode = knowNodes.get(host);
            List<Node> asyncNodes = hostNode.getDaemons() == null ? new LinkedList<>() : hostNode.getDaemons();
            asyncNodes.add(knowNodes.get(asyncPair.getAsyncNode()));
            hostNode.setDaemons(asyncNodes);
        });
    }

    private String buildStringInput(final InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8));
        String singleLine;
        StringBuilder buffer = new StringBuilder();
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
            throw new GraphLoadException("Start node is not specified for graph " + graphName);
        }
        if (nodes == null || nodes.length == 0) {
            throw new GraphLoadException("No node definition information found in the graph definition file!");
        }
    }

    private void checkNodeConfiguration(final NodeConfiguration nodeConfiguration) throws GraphLoadException {
        String nodeName = nodeConfiguration.getNodeName();
        String providerClass = nodeConfiguration.getProviderClass();
        if (nodeName == null || nodeName.isEmpty()) {
            throw new GraphLoadException("Node name is not specified!");
        }
        if (providerClass == null || !providerClass.contains(".")) {
            throw new GraphLoadException("Provider class name is not correct or not specified!");
        }
    }

    private List<StepPair> setUpNextSteps(final NodeConfiguration nodeConfiguration, final String predecessorNode) {
        String successStep = nodeConfiguration.getSuccessNode();
        String failStep = nodeConfiguration.getFailNode();
        String suspendStep = nodeConfiguration.getSuspendNode();
        String checkStep = nodeConfiguration.getCheckNode();
        List<StepPair> list = new LinkedList<>();
        String[] next = {successStep, failStep, suspendStep, checkStep};
        int count = 0;
        for (String nextStep : next) {
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

        AsyncNodeConfiguration[] asyncDependencies = nodeConfiguration.getAsyncDependencies();
        if (ArrayUtils.isEmpty(asyncDependencies)) {
            return list;
        }

        for (AsyncNodeConfiguration asyncConfiguration : asyncDependencies) {
            AsyncPair asyncPair = AsyncPair.builder()
                    .host(host)
                    .asyncNode(asyncConfiguration.getAsyncNode())
                    .build();
            list.add(asyncPair);
        }

        return list;
    }

    private List<AsyncPair> setUpDaemonPairs(final NodeConfiguration nodeConfiguration, final String host) {
        List<AsyncPair> list = new LinkedList<>();

        DaemonNodeConfiguration[] daemons = nodeConfiguration.getDaemons();
        if (ArrayUtils.isEmpty(daemons)) {
            return list;
        }

        for (DaemonNodeConfiguration daemonNodeConfiguration : daemons) {
            AsyncPair asyncPair = AsyncPair.builder()
                    .host(host)
                    .asyncNode(daemonNodeConfiguration.getDaemonNode())
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

    private void addSentinelHook(final Graph graph, final NodeConfiguration nodeConfiguration) {
        if (nodeConfiguration.getSentinelConfiguration() != null) {
            for (SentinelConfiguration sentinelConfiguration : nodeConfiguration.getSentinelConfiguration()) {
                SentinelRuleType sentinelRuleType = SentinelRuleType.fromType(sentinelConfiguration.getType());
                sentinelConfiguration.setResourceName(graph.getGraphName() + "::" + nodeConfiguration.getNodeName());
                sentinelRuleType.addRule(sentinelConfiguration);
            }
        }
    }
}
