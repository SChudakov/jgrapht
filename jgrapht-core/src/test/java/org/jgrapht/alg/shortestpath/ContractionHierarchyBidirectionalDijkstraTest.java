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
    private static final long SEED = 17L;

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
    public void testStoppingCondition() {
        Graph<Integer, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);


//        (0 : 1) 0.707454821689446
//        (0 : 5) 0.4416485026111072
//
//        (1 : 9) 0.8737514355314853
//        (1 : 2) 0.7173088313649671
//
//        (2 : 3) 0.08295611145017068
//        (2 : 8) 0.0443859375038691
//        (2 : 5) 0.773460334713085
//        (2 : 7) 0.8997086715585764
//
//        (3 : 5) 0.23973661293649284
//
//        (4 : 6) 0.6973704783607497
//        (4 : 5) 0.8162364511057306
//        (4 : 0) 0.8495837971198396
//        (4 : 3) 0.9347709230744503
//
//        (5 : 8) 0.4273447581898605
//        (5 : 6) 0.8644847558635339
//
//        (6 : 0) 0.7323115139597316
//        (6 : 7) 0.8834055329751666
//
//        (7 : 3) 0.6594836922590074
//        (7 : 5) 0.008329735080938927
//
//        (8 : 7) 0.003754742582413595
//        (8 : 4) 0.7745557286626394
//        (8 : 3) 0.3245563274356865
//        (8 : 9) 0.9169861023091815
//
//        (9 : 0) 0.858996580616418
//        (9 : 6) 0.651138665517512
//
        Graphs.addEdgeWithVertices(graph, 0, 1, 707);
        Graphs.addEdgeWithVertices(graph, 0, 5, 441);
        Graphs.addEdgeWithVertices(graph, 1, 9, 873);
        Graphs.addEdgeWithVertices(graph, 1, 2, 717);
        Graphs.addEdgeWithVertices(graph, 2, 3, 82);
        Graphs.addEdgeWithVertices(graph, 2, 8, 44);
        Graphs.addEdgeWithVertices(graph, 2, 5, 773);
        Graphs.addEdgeWithVertices(graph, 2, 7, 899);
        Graphs.addEdgeWithVertices(graph, 3, 5, 239);
        Graphs.addEdgeWithVertices(graph, 4, 6, 697);
        Graphs.addEdgeWithVertices(graph, 4, 5, 816);
        Graphs.addEdgeWithVertices(graph, 4, 0, 849);
        Graphs.addEdgeWithVertices(graph, 4, 3, 934);
        Graphs.addEdgeWithVertices(graph, 5, 8, 427);
        Graphs.addEdgeWithVertices(graph, 5, 6, 864);
        Graphs.addEdgeWithVertices(graph, 6, 0, 732);
        Graphs.addEdgeWithVertices(graph, 6, 7, 883);
        Graphs.addEdgeWithVertices(graph, 7, 3, 659);
        Graphs.addEdgeWithVertices(graph, 7, 5, 8);
        Graphs.addEdgeWithVertices(graph, 8, 7, 3);
        Graphs.addEdgeWithVertices(graph, 8, 4, 774);
        Graphs.addEdgeWithVertices(graph, 8, 3, 324);
        Graphs.addEdgeWithVertices(graph, 8, 9, 916);
        Graphs.addEdgeWithVertices(graph, 9, 0, 858);
        Graphs.addEdgeWithVertices(graph, 9, 6, 651);

        ContractionHierarchyBidirectionalDijkstra<Integer, DefaultWeightedEdge> dijkstra
                = new ContractionHierarchyBidirectionalDijkstra<>(graph);
        assertEquals(Arrays.asList(0, 5, 7, 8, 2), dijkstra.getPath(0, 2).getVertexList());
    }

    @Test
    public void testOnRandomGraphs() {
        int numOfVertices = 10;
        int vertexDegree = 3;
        int numOfIterations = 10000;
        int source = 0;
        for (int i = 0; i < numOfIterations; i++) {
            System.out.println("iteration: " + i);
            Graph<Integer, DefaultWeightedEdge> graph =
                    generateRandomGraph(numOfVertices, vertexDegree * numOfVertices);
            try {
                test(graph, source);
            } catch (Error e) {
                System.out.println(graph);
                System.out.println(source);
                for (Integer vertex : graph.vertexSet()) {
                    for (DefaultWeightedEdge edge : graph.outgoingEdgesOf(vertex)) {
                        if (graph.getEdgeSource(edge).equals(vertex)) {
                            System.out.println(edge + " " + graph.getEdgeWeight(edge));
                        }
                    }
                    System.out.println();
                }
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

        assertEqualPaths(dijkstraShortestPaths, contractionDijkstra, graph.vertexSet(), graph);
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
            graph.setEdgeWeight(edge, random.nextDouble());
        }
    }

    private void assertEqualPaths(
            ShortestPathAlgorithm.SingleSourcePaths<Integer, DefaultWeightedEdge> expected,
            ShortestPathAlgorithm.SingleSourcePaths<Integer, DefaultWeightedEdge> actual,
            Set<Integer> vertexSet,
            Graph<Integer, DefaultWeightedEdge> graph) {
        for (Integer sink : vertexSet) {
            GraphPath<Integer, DefaultWeightedEdge> path1 = expected.getPath(sink);
            GraphPath<Integer, DefaultWeightedEdge> path2 = actual.getPath(sink);
            System.out.println("expected: " + path1);
            for (DefaultWeightedEdge edge : path1.getEdgeList()) {
                System.out.println(edge + " " + graph.getEdgeWeight(edge));
            }
            System.out.println("actual: " + path2);
            for (DefaultWeightedEdge edge : path2.getEdgeList()) {
                System.out.println(edge + " " + graph.getEdgeWeight(edge));
            }
            if (path1 == null) {
                assertNull(path2);
            } else {
                assertEquals(
                        expected.getPath(sink).getWeight(), actual.getPath(sink).getWeight(), 1e-9);
            }
        }
    }
}