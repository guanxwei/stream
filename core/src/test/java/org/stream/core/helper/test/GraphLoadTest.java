package org.stream.core.helper.test;

import java.util.LinkedList;
import java.util.List;

import org.stream.core.component.ActivityRepository;
import org.stream.core.component.Graph;
import org.stream.core.component.Node;
import org.stream.core.exception.GraphLoadException;
import org.stream.core.execution.GraphContext;
import org.stream.core.helper.GraphLoader;
import org.stream.core.resource.ResourceType;
import org.stream.core.test.base.TestActivity;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class GraphLoadTest {

    private GraphLoader graphLoader;
    private GraphContext graphContext;
    private List<String> paths;

    @BeforeMethod
    public void BeforeMethod() {
        this.paths = new LinkedList<String>();
        this.graphContext = new GraphContext();
        this.graphLoader = new GraphLoader();
        graphLoader.setGraphContext(graphContext);
        graphLoader.setGraphFilePaths(paths);
    }

    /**
     * Test that given a path not exists, expected exception will be thrown.
     */
    @Test(expectedExceptions = {GraphLoadException.class})
    public void testGraphFileNotExist() throws Exception {
        String filePath = "notExist.graph";
        paths.add(filePath);
        graphLoader.init();
    }

    /**
     * Test that given a path exist but configuration is not correct, expected exception will be thrown.
     */
    @Test(expectedExceptions = {GraphLoadException.class}, expectedExceptionsMessageRegExp = "There is something wrong in the graph definition file, graph name should not be empty or missing!")
    public void testGraphKeyConfigurationInfoMissing() throws Exception {
        String path = "NoName.graph";
        paths.add(path);
        graphLoader.init();
    }

    /**
     * Test that given a path exist and configuration is correct, the graph loader can load the graph properly.
     * Here we pre-create a graph difinition file, provide pretty simple graph definition information. No complicated node relationship is set up.
     */
    @Test
    public void testSimpleCase() throws Exception {
        String path = "SimpleCase.graph";
        paths.add(path);
        ActivityRepository activityRepository = new ActivityRepository();
        graphContext.setActivityRepository(activityRepository);
        graphLoader.init();

        Assert.assertEquals(activityRepository.getActivityNum(), 1);
        Assert.assertEquals(graphLoader.getGraphContext().getGraphRegistered(), 1);
        Graph graph = graphLoader.getGraphContext().getGraph("testname");
        Assert.assertNotNull(graph);
        Assert.assertEquals(graph.getResourceType(), ResourceType.OBJECT);

        Node startNode = graph.getStartNode();
        Assert.assertNotNull(startNode);
        Assert.assertEquals(startNode.getNodeName(), "node1");
        Assert.assertEquals(startNode.getActivity(), graphContext.getActivity("org.stream.core.test.base.TestActivity"));

        Node defaultErrorNode = graph.getDefaultErrorNode();
        Assert.assertNotNull(defaultErrorNode);
        Assert.assertEquals(defaultErrorNode.getNodeName(), "node2");
        Assert.assertEquals(defaultErrorNode.getActivity(), graphContext.getActivity("org.stream.core.test.base.TestActivity"));
 
        List<Node> nodes = graph.getNodes();
        Assert.assertNotNull(nodes);
        Assert.assertEquals(nodes.size(), 2);
        Node node1 = nodes.get(0);
        Node node2 = nodes.get(1);
        Assert.assertEquals(node1, startNode);
        Assert.assertEquals(node2, defaultErrorNode);
    }

    /**
     * Test that given a path exist and configuration is comprehensive, the graph loader can load the graph properly.
     * The graph definition file provides comprehensive node relationship.
     * @throws Exception 
     */
    @Test
    public void testComprehensiveCase() throws Exception {
        String path = "ComprehensiveCase.graph";
        paths.add(path);
        ActivityRepository activityRepository = new ActivityRepository();
        graphContext.setActivityRepository(activityRepository);
        graphLoader.init();

        Assert.assertEquals(activityRepository.getActivityNum(), 3);
        Graph graph = graphContext.getGraph("comprehensive");
        Assert.assertNotNull(graph);

        Node startNode = graph.getStartNode();
        Node defaultErrorNode = graph.getDefaultErrorNode();
        List<Node> nodes = graph.getNodes();
        Assert.assertEquals(nodes.size(), 3);
        Assert.assertEquals(startNode.getNodeName(), "node1");
        Assert.assertEquals(startNode.getActivity().getActivityName(), TestActivity.class.getName());
        Node node1 = nodes.get(0);
        Node node2 = nodes.get(1);
        Node node3 = nodes.get(2);
        Assert.assertEquals(startNode, node1);
        Assert.assertEquals(node1.getNext().onSuccess(), node2);
        Assert.assertEquals(node1.getNext().onFail(), node3);
        Assert.assertNull(node1.getNext().onSuspend());
        Assert.assertEquals(defaultErrorNode, node3);
    }

    /**
     * Test that given async node information, the graph loader can load the graph properly.
     * @throws Exception 
     */
    @Test
    public void testComprehensiveCaseWithAsyncNodes() throws Exception {
        String path = "ComprehensiveWithAsyncNodeCase.graph";
        paths.add(path);
        ActivityRepository activityRepository = new ActivityRepository();
        graphContext.setActivityRepository(activityRepository);
        graphLoader.init();

        Assert.assertEquals(activityRepository.getActivityNum(), 4);
        Graph graph = graphContext.getGraph("comprehensive2");
        Assert.assertNotNull(graph);

        List<Node> nodes = graph.getNodes();
        Assert.assertEquals(nodes.size(), 4);
        Node node1 = nodes.get(0);

        List<Node> asyncNodes = node1.getAsyncDependencies();
        Assert.assertNotNull(asyncNodes);
        Assert.assertEquals(asyncNodes.size(), 1);
        Node asyncNode = asyncNodes.get(0);
        Assert.assertNotNull(asyncNode);
        Assert.assertEquals(asyncNode.getNodeName(), "node4");
    }

}
