package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jheaps.tree.PairingHeap;
import org.junit.Test;

import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class ContractionHierarchyAlgorithmTest {
    private static final long SEED = 19L;

    @Test
    public void testEmptyGraph() {
        Graph<Integer, DefaultWeightedEdge> graph = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        ContractionHierarchyAlgorithm<Integer, DefaultWeightedEdge> contractor = new ContractionHierarchyAlgorithm<>(graph, new PairingHeap<>());
        Pair<Graph<ContractionHierarchyAlgorithm.ContractionVertex<Integer>,
                ContractionHierarchyAlgorithm.ContractionEdge<DefaultWeightedEdge>>,
                Map<Integer, ContractionHierarchyAlgorithm.ContractionVertex<Integer>>> p = contractor.computeContractionHierarchy();

        assertNotNull(p);


        Graph<ContractionHierarchyAlgorithm.ContractionVertex<Integer>,
                ContractionHierarchyAlgorithm.ContractionEdge<DefaultWeightedEdge>> contractionGraph = p.getFirst();
        Map<Integer, ContractionHierarchyAlgorithm.ContractionVertex<Integer>> contractionMapping = p.getSecond();


        assertNotNull(contractionGraph);
        assertNotNull(contractionMapping);

        assertTrue(contractionGraph.vertexSet().isEmpty());
        assertTrue(contractionGraph.edgeSet().isEmpty());
        assertTrue(contractionMapping.keySet().isEmpty());
    }

    @Test
    public void testDirectedGraph1() {
        Graph<Integer, DefaultWeightedEdge> graph = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);

        Graphs.addEdgeWithVertices(graph, 1, 2, 1);
        Graphs.addEdgeWithVertices(graph, 2, 3, 1);

        ContractionHierarchyAlgorithm<Integer, DefaultWeightedEdge> contractor = new ContractionHierarchyAlgorithm<>(graph, new PairingHeap<>());
        Pair<Graph<ContractionHierarchyAlgorithm.ContractionVertex<Integer>,
                ContractionHierarchyAlgorithm.ContractionEdge<DefaultWeightedEdge>>,
                Map<Integer, ContractionHierarchyAlgorithm.ContractionVertex<Integer>>> p = contractor.computeContractionHierarchy();

        assertNotNull(p);

        Graph<ContractionHierarchyAlgorithm.ContractionVertex<Integer>,
                ContractionHierarchyAlgorithm.ContractionEdge<DefaultWeightedEdge>> contractionGraph = p.getFirst();
        Map<Integer, ContractionHierarchyAlgorithm.ContractionVertex<Integer>> contractionMapping = p.getSecond();

        assertEquals(3, contractionGraph.vertexSet().size());

        if (contractionMapping.get(2).contractionIndex == 0) {
            assertEquals(3, contractionGraph.edgeSet().size());
            assertTrue(contractionGraph.containsEdge(contractionMapping.get(1), contractionMapping.get(3)));
            assertEquals(2.0, contractionGraph.getEdgeWeight(
                    contractionGraph.getEdge(
                            contractionMapping.get(1), contractionMapping.get(3))), 1e-9);
        } else {
            assertEquals(2, contractionGraph.edgeSet().size());
        }
    }

    @Test
    public void testDirectedGraph2() {
        Graph<Integer, DefaultWeightedEdge> graph = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);

        Graphs.addEdgeWithVertices(graph, 1, 2, 1);
        Graphs.addEdgeWithVertices(graph, 2, 1, 1);
        Graphs.addEdgeWithVertices(graph, 2, 3, 1);
        Graphs.addEdgeWithVertices(graph, 3, 2, 1);

        ContractionHierarchyAlgorithm<Integer, DefaultWeightedEdge> contractor = new ContractionHierarchyAlgorithm<>(graph, new PairingHeap<>());
        Pair<Graph<ContractionHierarchyAlgorithm.ContractionVertex<Integer>,
                ContractionHierarchyAlgorithm.ContractionEdge<DefaultWeightedEdge>>,
                Map<Integer, ContractionHierarchyAlgorithm.ContractionVertex<Integer>>> p = contractor.computeContractionHierarchy();

        assertNotNull(p);

        Graph<ContractionHierarchyAlgorithm.ContractionVertex<Integer>,
                ContractionHierarchyAlgorithm.ContractionEdge<DefaultWeightedEdge>> contractionGraph = p.getFirst();
        Map<Integer, ContractionHierarchyAlgorithm.ContractionVertex<Integer>> contractionMapping = p.getSecond();

        assertEquals(3, graph.vertexSet().size());

        if (contractionMapping.get(2).contractionIndex == 0) {
            assertEquals(6, contractionGraph.edgeSet().size());
            assertTrue(contractionGraph.containsEdge(contractionMapping.get(1), contractionMapping.get(3)));
            assertTrue(contractionGraph.containsEdge(contractionMapping.get(3), contractionMapping.get(1)));
            assertEquals(2.0, contractionGraph.getEdgeWeight(
                    contractionGraph.getEdge(
                            contractionMapping.get(1), contractionMapping.get(3))), 1e-9);
            assertEquals(2.0, contractionGraph.getEdgeWeight(
                    contractionGraph.getEdge(
                            contractionMapping.get(3), contractionMapping.get(1))), 1e-9);
        } else {
            assertEquals(4, contractionGraph.edgeSet().size());
        }
    }

    @Test
    public void testDirectedGraph3() {
        Graph<Integer, DefaultWeightedEdge> graph = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);

        Graphs.addEdgeWithVertices(graph, 1, 5, 1);
        Graphs.addEdgeWithVertices(graph, 2, 5, 1);

        Graphs.addEdgeWithVertices(graph, 5, 6, 1);
        Graphs.addEdgeWithVertices(graph, 5, 7, 1);

        ContractionHierarchyAlgorithm<Integer, DefaultWeightedEdge> contractor = new ContractionHierarchyAlgorithm<>(graph, new PairingHeap<>());
        Pair<Graph<ContractionHierarchyAlgorithm.ContractionVertex<Integer>,
                ContractionHierarchyAlgorithm.ContractionEdge<DefaultWeightedEdge>>,
                Map<Integer, ContractionHierarchyAlgorithm.ContractionVertex<Integer>>> p = contractor.computeContractionHierarchy();

        assertNotNull(p);

        Graph<ContractionHierarchyAlgorithm.ContractionVertex<Integer>,
                ContractionHierarchyAlgorithm.ContractionEdge<DefaultWeightedEdge>> contractionGraph = p.getFirst();
        Map<Integer, ContractionHierarchyAlgorithm.ContractionVertex<Integer>> contractionMapping = p.getSecond();

        assertEquals(5, graph.vertexSet().size());

        if (contractionMapping.get(5).contractionIndex == 0) {
            assertEquals(8, contractionGraph.edgeSet().size());
            assertTrue(contractionGraph.containsEdge(contractionMapping.get(1), contractionMapping.get(6)));
            assertTrue(contractionGraph.containsEdge(contractionMapping.get(1), contractionMapping.get(7)));
            assertTrue(contractionGraph.containsEdge(contractionMapping.get(2), contractionMapping.get(6)));
            assertTrue(contractionGraph.containsEdge(contractionMapping.get(2), contractionMapping.get(7)));
            assertEquals(2.0, contractionGraph.getEdgeWeight(
                    contractionGraph.getEdge(
                            contractionMapping.get(1), contractionMapping.get(6))), 1e-9);
            assertEquals(2.0, contractionGraph.getEdgeWeight(
                    contractionGraph.getEdge(
                            contractionMapping.get(1), contractionMapping.get(7))), 1e-9);
            assertEquals(2.0, contractionGraph.getEdgeWeight(
                    contractionGraph.getEdge(
                            contractionMapping.get(2), contractionMapping.get(6))), 1e-9);
            assertEquals(2.0, contractionGraph.getEdgeWeight(
                    contractionGraph.getEdge(
                            contractionMapping.get(2), contractionMapping.get(7))), 1e-9);
        } else {
            assertEquals(4, contractionGraph.edgeSet().size());
        }
    }

    @Test
    public void testDirectedGraph4() {
        Graph<Integer, DefaultWeightedEdge> graph = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);

        Graphs.addEdgeWithVertices(graph, 1, 5, 1);
        Graphs.addEdgeWithVertices(graph, 5, 1, 1);
        Graphs.addEdgeWithVertices(graph, 2, 5, 1);
        Graphs.addEdgeWithVertices(graph, 5, 2, 1);

        Graphs.addEdgeWithVertices(graph, 5, 6, 1);
        Graphs.addEdgeWithVertices(graph, 6, 5, 1);
        Graphs.addEdgeWithVertices(graph, 5, 7, 1);
        Graphs.addEdgeWithVertices(graph, 7, 5, 1);

        ContractionHierarchyAlgorithm<Integer, DefaultWeightedEdge> contractor = new ContractionHierarchyAlgorithm<>(graph, new PairingHeap<>());
        Pair<Graph<ContractionHierarchyAlgorithm.ContractionVertex<Integer>,
                ContractionHierarchyAlgorithm.ContractionEdge<DefaultWeightedEdge>>,
                Map<Integer, ContractionHierarchyAlgorithm.ContractionVertex<Integer>>> p = contractor.computeContractionHierarchy();

        assertNotNull(p);

        Graph<ContractionHierarchyAlgorithm.ContractionVertex<Integer>,
                ContractionHierarchyAlgorithm.ContractionEdge<DefaultWeightedEdge>> contractionGraph = p.getFirst();
        Map<Integer, ContractionHierarchyAlgorithm.ContractionVertex<Integer>> contractionMapping = p.getSecond();

        assertEquals(5, graph.vertexSet().size());

        if (contractionMapping.get(5).contractionIndex == 0) {
            assertEquals(20, contractionGraph.edgeSet().size());
            assertTrue(contractionGraph.containsEdge(contractionMapping.get(1), contractionMapping.get(2)));
            assertTrue(contractionGraph.containsEdge(contractionMapping.get(1), contractionMapping.get(6)));
            assertTrue(contractionGraph.containsEdge(contractionMapping.get(1), contractionMapping.get(7)));

            assertTrue(contractionGraph.containsEdge(contractionMapping.get(2), contractionMapping.get(1)));
            assertTrue(contractionGraph.containsEdge(contractionMapping.get(2), contractionMapping.get(6)));
            assertTrue(contractionGraph.containsEdge(contractionMapping.get(2), contractionMapping.get(7)));

            assertTrue(contractionGraph.containsEdge(contractionMapping.get(6), contractionMapping.get(1)));
            assertTrue(contractionGraph.containsEdge(contractionMapping.get(6), contractionMapping.get(2)));
            assertTrue(contractionGraph.containsEdge(contractionMapping.get(6), contractionMapping.get(7)));

            assertTrue(contractionGraph.containsEdge(contractionMapping.get(7), contractionMapping.get(1)));
            assertTrue(contractionGraph.containsEdge(contractionMapping.get(7), contractionMapping.get(2)));
            assertTrue(contractionGraph.containsEdge(contractionMapping.get(7), contractionMapping.get(6)));

            assertEquals(2.0, contractionGraph.getEdgeWeight(
                    contractionGraph.getEdge(
                            contractionMapping.get(1), contractionMapping.get(2))), 1e-9);
            assertEquals(2.0, contractionGraph.getEdgeWeight(
                    contractionGraph.getEdge(
                            contractionMapping.get(1), contractionMapping.get(6))), 1e-9);
            assertEquals(2.0, contractionGraph.getEdgeWeight(
                    contractionGraph.getEdge(
                            contractionMapping.get(1), contractionMapping.get(7))), 1e-9);


            assertEquals(2.0, contractionGraph.getEdgeWeight(
                    contractionGraph.getEdge(
                            contractionMapping.get(2), contractionMapping.get(1))), 1e-9);
            assertEquals(2.0, contractionGraph.getEdgeWeight(
                    contractionGraph.getEdge(
                            contractionMapping.get(2), contractionMapping.get(6))), 1e-9);
            assertEquals(2.0, contractionGraph.getEdgeWeight(
                    contractionGraph.getEdge(
                            contractionMapping.get(2), contractionMapping.get(7))), 1e-9);


            assertEquals(2.0, contractionGraph.getEdgeWeight(
                    contractionGraph.getEdge(
                            contractionMapping.get(6), contractionMapping.get(1))), 1e-9);
            assertEquals(2.0, contractionGraph.getEdgeWeight(
                    contractionGraph.getEdge(
                            contractionMapping.get(6), contractionMapping.get(2))), 1e-9);
            assertEquals(2.0, contractionGraph.getEdgeWeight(
                    contractionGraph.getEdge(
                            contractionMapping.get(6), contractionMapping.get(7))), 1e-9);

            assertEquals(2.0, contractionGraph.getEdgeWeight(
                    contractionGraph.getEdge(
                            contractionMapping.get(7), contractionMapping.get(1))), 1e-9);
            assertEquals(2.0, contractionGraph.getEdgeWeight(
                    contractionGraph.getEdge(
                            contractionMapping.get(7), contractionMapping.get(2))), 1e-9);
            assertEquals(2.0, contractionGraph.getEdgeWeight(
                    contractionGraph.getEdge(
                            contractionMapping.get(7), contractionMapping.get(6))), 1e-9);
        } else {
            assertEquals(8, contractionGraph.edgeSet().size());
        }
    }

    @Test
    public void testUndirectedGraph1() {
        Graph<Integer, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        Graphs.addEdgeWithVertices(graph, 1, 2, 1);
        Graphs.addEdgeWithVertices(graph, 2, 3, 1);

        ContractionHierarchyAlgorithm<Integer, DefaultWeightedEdge> contractor = new ContractionHierarchyAlgorithm<>(graph, new PairingHeap<>());
        Pair<Graph<ContractionHierarchyAlgorithm.ContractionVertex<Integer>,
                ContractionHierarchyAlgorithm.ContractionEdge<DefaultWeightedEdge>>,
                Map<Integer, ContractionHierarchyAlgorithm.ContractionVertex<Integer>>> p = contractor.computeContractionHierarchy();

        assertNotNull(p);

        Graph<ContractionHierarchyAlgorithm.ContractionVertex<Integer>,
                ContractionHierarchyAlgorithm.ContractionEdge<DefaultWeightedEdge>> contractionGraph = p.getFirst();
        Map<Integer, ContractionHierarchyAlgorithm.ContractionVertex<Integer>> contractionMapping = p.getSecond();

        assertEquals(3, graph.vertexSet().size());

        if (contractionMapping.get(2).contractionIndex == 0) {
            assertEquals(3, contractionGraph.edgeSet().size());
            assertTrue(contractionGraph.containsEdge(contractionMapping.get(1), contractionMapping.get(3)));
            assertEquals(2.0, contractionGraph.getEdgeWeight(
                    contractionGraph.getEdge(
                            contractionMapping.get(1), contractionMapping.get(3))), 1e-9);
        } else {
            assertEquals(2, contractionGraph.edgeSet().size());
        }
    }

    @Test
    public void testUndirectedGraph2() {
        Graph<Integer, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);

        Graphs.addEdgeWithVertices(graph, 1, 5, 1);
        Graphs.addEdgeWithVertices(graph, 2, 5, 1);

        Graphs.addEdgeWithVertices(graph, 5, 6, 1);
        Graphs.addEdgeWithVertices(graph, 5, 7, 1);

        ContractionHierarchyAlgorithm<Integer, DefaultWeightedEdge> contractor = new ContractionHierarchyAlgorithm<>(graph, new PairingHeap<>());
        Pair<Graph<ContractionHierarchyAlgorithm.ContractionVertex<Integer>,
                ContractionHierarchyAlgorithm.ContractionEdge<DefaultWeightedEdge>>,
                Map<Integer, ContractionHierarchyAlgorithm.ContractionVertex<Integer>>> p = contractor.computeContractionHierarchy();

        assertNotNull(p);

        Graph<ContractionHierarchyAlgorithm.ContractionVertex<Integer>,
                ContractionHierarchyAlgorithm.ContractionEdge<DefaultWeightedEdge>> contractionGraph = p.getFirst();
        Map<Integer, ContractionHierarchyAlgorithm.ContractionVertex<Integer>> contractionMapping = p.getSecond();

        assertEquals(5, graph.vertexSet().size());

        if (contractionMapping.get(5).contractionIndex == 0) {
//            assertEquals(10, contractionGraph.edgeSet().size());

            assertTrue(contractionGraph.containsEdge(contractionMapping.get(1), contractionMapping.get(2)));
            assertTrue(contractionGraph.containsEdge(contractionMapping.get(1), contractionMapping.get(6)));
            assertTrue(contractionGraph.containsEdge(contractionMapping.get(1), contractionMapping.get(7)));

            assertTrue(contractionGraph.containsEdge(contractionMapping.get(2), contractionMapping.get(6)));
            assertTrue(contractionGraph.containsEdge(contractionMapping.get(1), contractionMapping.get(7)));

            assertTrue(contractionGraph.containsEdge(contractionMapping.get(6), contractionMapping.get(7)));

            assertEquals(2.0, contractionGraph.getEdgeWeight(
                    contractionGraph.getEdge(
                            contractionMapping.get(1), contractionMapping.get(2))), 1e-9);
            assertEquals(2.0, contractionGraph.getEdgeWeight(
                    contractionGraph.getEdge(
                            contractionMapping.get(1), contractionMapping.get(6))), 1e-9);
            assertEquals(2.0, contractionGraph.getEdgeWeight(
                    contractionGraph.getEdge(
                            contractionMapping.get(1), contractionMapping.get(7))), 1e-9);

            assertEquals(2.0, contractionGraph.getEdgeWeight(
                    contractionGraph.getEdge(
                            contractionMapping.get(2), contractionMapping.get(6))), 1e-9);
            assertEquals(2.0, contractionGraph.getEdgeWeight(
                    contractionGraph.getEdge(
                            contractionMapping.get(1), contractionMapping.get(7))), 1e-9);

            assertEquals(2.0, contractionGraph.getEdgeWeight(
                    contractionGraph.getEdge(
                            contractionMapping.get(6), contractionMapping.get(7))), 1e-9);
        } else {
            assertEquals(4, contractionGraph.edgeSet().size());
        }
    }

    @Test
    public void testUndirectedGraph3() {
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

        ContractionHierarchyAlgorithm<Integer, DefaultWeightedEdge> contractor = new ContractionHierarchyAlgorithm<>(graph, new PairingHeap<>());
        Pair<Graph<ContractionHierarchyAlgorithm.ContractionVertex<Integer>,
                ContractionHierarchyAlgorithm.ContractionEdge<DefaultWeightedEdge>>,
                Map<Integer, ContractionHierarchyAlgorithm.ContractionVertex<Integer>>> p = contractor.computeContractionHierarchy();

        assertNotNull(p);

        Graph<ContractionHierarchyAlgorithm.ContractionVertex<Integer>,
                ContractionHierarchyAlgorithm.ContractionEdge<DefaultWeightedEdge>> contractionGraph = p.getFirst();
        Map<Integer, ContractionHierarchyAlgorithm.ContractionVertex<Integer>> contractionMapping = p.getSecond();

        assertEquals(9, graph.vertexSet().size());
        assertEquals(12, contractionGraph.edgeSet().size());
    }

//    @Test
    public void testUndirectedGraph4() {
        int size = 20;
        Graph<Integer, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        fillLineGraph(graph, size);

        Pair<Graph<ContractionHierarchyAlgorithm.ContractionVertex<Integer>,
                ContractionHierarchyAlgorithm.ContractionEdge<DefaultWeightedEdge>>,
                Map<Integer, ContractionHierarchyAlgorithm.ContractionVertex<Integer>>> p
                = new ContractionHierarchyAlgorithm<>(
                graph,
                new PairingHeap<>(),
                () -> new Random(SEED),
                1
        ).computeContractionHierarchy();
        Graph<ContractionHierarchyAlgorithm.ContractionVertex<Integer>,
                ContractionHierarchyAlgorithm.ContractionEdge<DefaultWeightedEdge>> contractionGraph
                = p.getFirst();
        Map<Integer, ContractionHierarchyAlgorithm.ContractionVertex<Integer>> mapping = p.getSecond();

        assertTrue(contractionGraph.containsEdge(mapping.get(1), mapping.get(3)));
        assertTrue(contractionGraph.containsEdge(mapping.get(1), mapping.get(11)));
//        assertTrue(contractionGraph.containsEdge(mapping.get(1),mapping.get(15)));
//        assertTrue(contractionGraph.containsEdge(mapping.get(1), mapping.get(18)));
        assertTrue(contractionGraph.containsEdge(mapping.get(1), mapping.get(19)));

        assertTrue(contractionGraph.containsEdge(mapping.get(3), mapping.get(5)));
        assertTrue(contractionGraph.containsEdge(mapping.get(3), mapping.get(6)));
//        assertTrue(contractionGraph.containsEdge(mapping.get(3),mapping.get(11)));

        assertTrue(contractionGraph.containsEdge(mapping.get(6), mapping.get(8)));
        assertTrue(contractionGraph.containsEdge(mapping.get(6), mapping.get(11)));

        assertTrue(contractionGraph.containsEdge(mapping.get(8), mapping.get(11)));

        assertTrue(contractionGraph.containsEdge(mapping.get(9), mapping.get(11)));

        assertTrue(contractionGraph.containsEdge(mapping.get(11), mapping.get(13)));
//        assertTrue(contractionGraph.containsEdge(mapping.get(11), mapping.get(15)));

        assertTrue(contractionGraph.containsEdge(mapping.get(13), mapping.get(15)));

        assertTrue(contractionGraph.containsEdge(mapping.get(15), mapping.get(17)));
//        assertTrue(contractionGraph.containsEdge(mapping.get(15),mapping.get(18)));


        double sum1 = 0.0;
        for (int i = 1; i < 10; ++i) {
            sum1 += graph.getEdgeWeight(graph.getEdge(i, i + 1));
        }
        double sum2 = 0.0;
        for (int i = 11; i <= 20; ++i) {
            sum2 += graph.getEdgeWeight(graph.getEdge(i % 20, (i + 1) % 20));
        }
        System.out.println(sum1 + " " + sum2);
    }

    private void fillLineGraph(Graph<Integer, DefaultWeightedEdge> graph, int size) {
        Random random = new Random(SEED);
        for (int i = 0; i < size; ++i) {
            graph.addVertex(i);
        }
        for (int i = 0; i < size; ++i) {
            graph.addEdge(i, (i + 1) % size);
            graph.setEdgeWeight(graph.getEdge(i, (i + 1) % size), random.nextDouble());
        }
        for (DefaultWeightedEdge edge : graph.edgeSet()) {
            System.out.println(edge + " " + graph.getEdgeWeight(edge));
        }
    }
}