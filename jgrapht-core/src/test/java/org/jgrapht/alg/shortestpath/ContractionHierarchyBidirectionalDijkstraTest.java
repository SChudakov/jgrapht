package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.util.SupplierUtil;
import org.jheaps.tree.PairingHeap;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ContractionHierarchyBidirectionalDijkstraTest {
    private static final long SEED = 13L;

    @Test
    public void testEmptyGraph() {
        Graph<Integer, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        ContractionHierarchyBidirectionalDijkstra<Integer, DefaultWeightedEdge> dijkstra
                = new ContractionHierarchyBidirectionalDijkstra<>(graph);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSourceNotPresent() {
        Graph<Integer, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        graph.addVertex(2);
        ContractionHierarchyBidirectionalDijkstra<Integer, DefaultWeightedEdge> dijkstra
                = new ContractionHierarchyBidirectionalDijkstra<>(graph);
        dijkstra.getPath(1, 2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTargetNotPresent() {
        Graph<Integer, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        graph.addVertex(1);
        ContractionHierarchyBidirectionalDijkstra<Integer, DefaultWeightedEdge> dijkstra
                = new ContractionHierarchyBidirectionalDijkstra<>(graph);
        dijkstra.getPath(1, 2);
    }

    @Test
    public void testNoPath() {
        Graph<Integer, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        graph.addVertex(1);
        graph.addVertex(2);
        ContractionHierarchyBidirectionalDijkstra<Integer, DefaultWeightedEdge> dijkstra
                = new ContractionHierarchyBidirectionalDijkstra<>(graph);
        GraphPath<Integer, DefaultWeightedEdge> path = dijkstra.getPath(1, 2);
        assertNull(path);
    }

    @Test
    public void testSimpleGraph() {
        Graph<Integer, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        Graphs.addEdgeWithVertices(graph, 1, 2, 3);
        Graphs.addEdgeWithVertices(graph, 1, 4, 1);

        Graphs.addEdgeWithVertices(graph, 2, 3, 3);
        Graphs.addEdgeWithVertices(graph, 2, 5, 1);

        Graphs.addEdgeWithVertices(graph, 3, 6, 1);

        Graphs.addEdgeWithVertices(graph, 4, 5, 1);
        Graphs.addEdgeWithVertices(graph, 4, 7, 1);

        Graphs.addEdgeWithVertices(graph, 5, 6, 1);
        Graphs.addEdgeWithVertices(graph, 5, 8, 1);

        Graphs.addEdgeWithVertices(graph, 6, 9, 1);

        Graphs.addEdgeWithVertices(graph, 7, 8, 3);
        Graphs.addEdgeWithVertices(graph, 8, 9, 3);

        ContractionHierarchyBidirectionalDijkstra<Integer, DefaultWeightedEdge> dijkstra =
                new ContractionHierarchyBidirectionalDijkstra<>(graph);


        assertEquals(Collections.singletonList(1), dijkstra.getPath(1, 1).getVertexList());
        assertEquals(Arrays.asList(1, 2), dijkstra.getPath(1, 2).getVertexList());
        assertEquals(Arrays.asList(1, 4, 5, 6, 3), dijkstra.getPath(1, 3).getVertexList());
        assertEquals(Arrays.asList(1, 4, 5, 6, 9), dijkstra.getPath(1, 9).getVertexList());
        assertEquals(Arrays.asList(7, 4, 1), dijkstra.getPath(7, 1).getVertexList());
        assertEquals(Arrays.asList(8, 5, 2), dijkstra.getPath(8, 2).getVertexList());
    }

    @Test
    public void testOnRandomGraphs() {
        int numOfVertices = 100;
        int vertexDegree = 10;
        int numOfIterations = 1000;
        int source = 0;
        for (int i = 0; i < numOfIterations; i++) {
            System.out.println("iteration: " + i);
            Graph<Integer, DefaultWeightedEdge> graph =
                    generateRandomGraph(numOfVertices, vertexDegree * numOfVertices);
            try {
                test(graph, source);
            } catch (IllegalArgumentException e) {
                System.out.println(graph);
                System.out.println(source);
                throw e;
            }
        }
    }

    private void test(Graph<Integer, DefaultWeightedEdge> graph, Integer source) {
        ShortestPathAlgorithm.SingleSourcePaths<Integer,
                DefaultWeightedEdge> dijkstraShortestPaths =
                new DijkstraShortestPath<>(graph).getPaths(source);
        ShortestPathAlgorithm.SingleSourcePaths<Integer, DefaultWeightedEdge> contractionDijkstra =
                new ContractionHierarchyBidirectionalDijkstra<>(graph,
                        new ContractionHierarchyAlgorithm<>(
                                graph,
                                new PairingHeap<>(),
                                () -> new Random(SEED), 1).computeContractionHierarchy()
                ).getPaths(source);

        assertEqualPaths(dijkstraShortestPaths, contractionDijkstra, graph.vertexSet());
    }

    private Graph<Integer, DefaultWeightedEdge> generateRandomGraph(
            int numOfVertices, int numOfEdges) {
        SimpleWeightedGraph<Integer, DefaultWeightedEdge> graph =
                new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        graph.setVertexSupplier(SupplierUtil.createIntegerSupplier());

        Random random = new Random(SEED);

        GraphGenerator<Integer, DefaultWeightedEdge, Integer> generator =
                new GnmRandomGraphGenerator<>(numOfVertices, numOfEdges - numOfVertices + 1, SEED);
        generator.generateGraph(graph);
        makeConnected(graph);
        addEdgeWeights(graph, random);

        return graph;
    }

    private void makeConnected(Graph<Integer, DefaultWeightedEdge> graph) {
        Object[] vertices = graph.vertexSet().toArray();
        for (int i = 0; i < vertices.length - 1; i++) {
            graph.addEdge((Integer) vertices[i], (Integer) vertices[i + 1]);
        }
    }

    private void addEdgeWeights(Graph<Integer, DefaultWeightedEdge> graph, Random random) {
        for (DefaultWeightedEdge edge : graph.edgeSet()) {
            graph.setEdgeWeight(edge, random.nextInt(10));
        }
    }

    private void assertEqualPaths(
            ShortestPathAlgorithm.SingleSourcePaths<Integer, DefaultWeightedEdge> expected,
            ShortestPathAlgorithm.SingleSourcePaths<Integer, DefaultWeightedEdge> actual,
            Set<Integer> vertexSet) {
        for (Integer sink : vertexSet) {
            GraphPath<Integer, DefaultWeightedEdge> path1 = expected.getPath(sink);
            GraphPath<Integer, DefaultWeightedEdge> path2 = actual.getPath(sink);
            if (path1 == null) {
                assertNull(path2);
            } else {
                assertEquals(
                        expected.getPath(sink).getWeight(), actual.getPath(sink).getWeight(), 1e-9);
            }
        }
    }
}