package org.stream.core.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.stream.core.component.Activity;
import org.stream.core.component.ActivityResult;
import org.stream.core.component.ActivityResult.Visitor;
import org.stream.core.component.Graph;
import org.stream.core.component.Node;
import org.stream.core.exception.GraphLoadException;
import org.stream.core.execution.AsyncPair;
import org.stream.core.execution.GraphContext;
import org.stream.core.execution.NextSteps;
import org.stream.core.execution.NextSteps.NextStepType;
import org.stream.core.execution.StepPair;
import org.stream.core.helper.NodeConfiguration.AsyncNodeConfiguration;
import org.stream.core.resource.ResourceType;

import lombok.Data;

import com.google.gson.Gson;

/**
 * Graph helper who is responsible to load a graph from a input stream. Basically, graph definition information is stored in a file with suffix ".graph".
 * The file content should be stored as Json object stringfy style.
 */
@Data
public final class GraphLoader {

    private static final Gson gson = new Gson();

    private List<String> graphFilePaths;

    private GraphContext graphContext;

    /**
     * Use absolute path to load the graph definition files, the graph folder should be put at the root directory of the projects.
     * If the customer create a Maven project, then the graphs should be put at the folder "graph" in the resources.
     */
    private static final String SYSTEM_PATH_SEPARATOR = "/";

    private static final String DEFAULT_GRAPH_FILE_PATH_PREFIX = SYSTEM_PATH_SEPARATOR + "graph" + SYSTEM_PATH_SEPARATOR;

    /**
     * Initiate graph loading process, load all the graphs specified in the {{@link #graphFilePaths} located in the 
     * default graph directory. 
     * @throws GraphLoadException
     */
    public void init() throws GraphLoadException{
        if (graphFilePaths == null|| graphFilePaths.size() == 0) {
            throw new GraphLoadException("Graph definition file paths not specified!");
        }
        for (String path : graphFilePaths) {
            InputStream input = getClass().getResourceAsStream(DEFAULT_GRAPH_FILE_PATH_PREFIX + path);
            if (input == null) {
                throw new GraphLoadException(String.format("Graph definition file is not found, file name is [%s]", DEFAULT_GRAPH_FILE_PATH_PREFIX + path));
            }
            Graph graph = loadGraphFromFile(input);
            graphContext.addGraph(graph);
        }
    }

    private Graph loadGraphFromFile(InputStream input) throws GraphLoadException {
        Graph graph = new Graph();
        String cause = null;
        List<StepPair> stepPairs = new LinkedList<StepPair>();
        List<AsyncPair> asyncPairs = new LinkedList<AsyncPair>();
        Map<String, Node> knowNodes = new HashMap<String, Node>();
        try {
            String compressedInputString = buildStringfyInput(input);
            GraphConfiguration graphConfiguration = gson.fromJson(compressedInputString, GraphConfiguration.class);
            checkGraphConfiguration(graphConfiguration);
            graph.setGraphName(graphConfiguration.getGraphName());
            if (graphConfiguration.getResourceType() == null) {
            	throw new GraphLoadException("ResourceType is not specified!");
            }
            graph.setResourceType(ResourceType.valueOf(graphConfiguration.getResourceType()));
            NodeConfiguration[] nodes = graphConfiguration.getNodes();
            List<Node> staticNodes = new ArrayList<Node>();
            for (NodeConfiguration nodeConfiguration : nodes) {
                checkNodeConfiguration(nodeConfiguration);
                String currentNodeName = nodeConfiguration.getNodeName();
                String activityClass = nodeConfiguration.getActivityClass();
                Activity activity;
                if (!graphContext.isActivityRegistered(activityClass)) {
                    cause = activityClass;
                    Class<?> clazz = Class.forName(activityClass);
                    activity = (Activity) clazz.newInstance();
                    graphContext.registerActivity(activity);
                } else {
                    activity = graphContext.getActivity(activityClass);
                }

                stepPairs.addAll(setUpNextSteps(nodeConfiguration, currentNodeName));
                asyncPairs.addAll(setUpAsyncPairs(nodeConfiguration, currentNodeName));

                Node node = Node.builder()
                        .activity(activity)
                        .nodeName(currentNodeName)
                        .next(new NextSteps())
                        .graph(graph)
                        .build();
                activity.setNode(node);
                staticNodes.add(node);
                knowNodes.put(node.getNodeName(), node);
            }
            graph.setNodes(staticNodes);
            graph.setStartNode(knowNodes.get(graphConfiguration.getStartNode()));
            graph.setDefaultErrorNode(knowNodes.get(graphConfiguration.getDefaultErrorNode()));

            /**
             * Set up node corerationship net.
             */
            for (StepPair pair : stepPairs) {
                final Node predecessor = knowNodes.get(pair.getPredecessor());
                final Node successor = knowNodes.get(pair.getSuccessor());
                final NextStepType type = pair.getNextStepType();
                for (final ActivityResult result : ActivityResult.values()) {
                    result.accept(new Visitor<Void>() {
                        public Void success() {
                            if (type.equals(NextStepType.SUCCESS))
                                predecessor.getNext().setSuccess(successor);
                            return null;
                        }
                        public Void fail() {
                            if (type.equals(NextStepType.FAIL))
                                predecessor.getNext().setFail(successor);
                            return null;
                        }
                        public Void suspend() {
                            if (type.equals(NextStepType.SUSPEND))
                                predecessor.getNext().setSuspend(successor);
                            return null;
                        }
                    });
                }
            }

            asyncPairs.forEach(asyncPair -> {
                String host = asyncPair.getHost();
                Node hostNode = knowNodes.get(host);
                List<Node> asyncNodes = hostNode.getAsyncDependencies() == null ? new LinkedList<>() : hostNode.getAsyncDependencies();
                asyncNodes.add(knowNodes.get(asyncPair.getAysncNode()));
                hostNode.setAsyncDependencies(asyncNodes);
            });

        } catch (IOException e) {
            throw new GraphLoadException("Failed to load graph configuration information from the definition file", e);
        } catch (ClassNotFoundException e) {
            throw new GraphLoadException(String.format("Class is not found, name is [%s]", cause), e);
        } catch (InstantiationException e) {
            throw new GraphLoadException(String.format("Failed to initiate class [%s]", cause), e);
        } catch (IllegalAccessException e) {
            throw new GraphLoadException(String.format("No access to class [%s]", cause), e);
        }
        return graph;
    }

    private String buildStringfyInput(InputStream input) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(input));
        String singleLine = null;
        StringBuffer buffer = new StringBuffer();
        while ( (singleLine = reader.readLine()) != null) {
            buffer.append(singleLine.trim());
        }
        return buffer.toString();
    }

    private void checkGraphConfiguration(GraphConfiguration graphConfiguration) throws GraphLoadException {
        String graphName = graphConfiguration.getGraphName();
        String startNode = graphConfiguration.getStartNode();
        NodeConfiguration[] nodes = graphConfiguration.getNodes();
        if (graphName == null) {
            throw new GraphLoadException("There is something wrong in graph definition file, graph name should not be empty or missing!");
        }
        if (startNode == null) {
            throw new GraphLoadException("Start node is not specified!");
        }
        if (nodes == null || nodes.length == 0) {
            throw new GraphLoadException("No node definition information found in the graph definition file!");
        }
    }

    private void checkNodeConfiguration(NodeConfiguration nodeConfiguration) throws GraphLoadException {
        String nodeName = nodeConfiguration.getNodeName();
        String activityClass = nodeConfiguration.getActivityClass();
        if (nodeName == null || nodeName.length() == 0) {
            throw new GraphLoadException("Node name is not specified!");
        }
        if (activityClass == null || activityClass.length() == 0 || !activityClass.contains(".")) {
            throw new GraphLoadException("Activity class name is not correct or not specified!");
        }
    }

    private List<StepPair> setUpNextSteps(final NodeConfiguration nodeConfiguration, final String predecessorNode) {
        String successStep = nodeConfiguration.getSuccessNode();
        String failStep = nodeConfiguration.getFailNode();
        String suspendStep = nodeConfiguration.getSuspendNode();
        List<StepPair> list = new LinkedList<StepPair>();
        String[] nexts = {successStep, failStep, suspendStep};
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
                count ++;
            }
        }
        return list;
    }

    private List<AsyncPair> setUpAsyncPairs(final NodeConfiguration nodeConfiguration, final String host) {
        List<AsyncPair> list = new LinkedList<>();

        /**
         * Extract the async dependency nodes of the current node.
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
}
