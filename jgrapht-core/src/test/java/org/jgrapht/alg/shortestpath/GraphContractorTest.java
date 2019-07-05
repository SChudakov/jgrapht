package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jheaps.tree.PairingHeap;
import org.junit.Test;

import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class GraphContractorTest {

    @Test
    public void testEmptyGraph() {
        Graph<Integer, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        GraphContractor<Integer, DefaultWeightedEdge> contractor = new GraphContractor<>(graph,
                new PairingHeap<>(), Random::new, 0, Runtime.getRuntime().availableProcessors());
        Pair<Graph<GraphContractor.ContractionVertex<Integer>,
                GraphContractor.ContractionEdge<DefaultWeightedEdge>>,
                Map<Integer, GraphContractor.ContractionVertex<Integer>>> p = contractor.computeContractionHierarchy();

        assertNotNull(p);


        Graph<GraphContractor.ContractionVertex<Integer>,
                GraphContractor.ContractionEdge<DefaultWeightedEdge>> contractionGraph = p.getFirst();
        Map<Integer, GraphContractor.ContractionVertex<Integer>> contractionMapping = p.getSecond();


        assertNotNull(contractionGraph);
        assertNotNull(contractionMapping);

        assertTrue(contractionGraph.vertexSet().isEmpty());
        assertTrue(contractionGraph.edgeSet().isEmpty());
        assertTrue(contractionMapping.keySet().isEmpty());
    }

    @Test
    public void testDirectedGraph1() {
        Graph<Integer, DefaultWeightedEdge> graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);

        Graphs.addEdgeWithVertices(graph, 1, 2, 1);
        Graphs.addEdgeWithVertices(graph, 2, 3, 1);

        GraphContractor<Integer, DefaultWeightedEdge> contractor = new GraphContractor<>(graph,
                new PairingHeap<>(), Random::new, 0, 1);
        Pair<Graph<GraphContractor.ContractionVertex<Integer>,
                GraphContractor.ContractionEdge<DefaultWeightedEdge>>,
                Map<Integer, GraphContractor.ContractionVertex<Integer>>> p = contractor.computeContractionHierarchy();

        assertNotNull(p);

        Graph<GraphContractor.ContractionVertex<Integer>,
                GraphContractor.ContractionEdge<DefaultWeightedEdge>> contractionGraph = p.getFirst();
        Map<Integer, GraphContractor.ContractionVertex<Integer>> contractionMapping = p.getSecond();

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
        Graph<Integer, DefaultWeightedEdge> graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);

        Graphs.addEdgeWithVertices(graph, 1, 2, 1);
        Graphs.addEdgeWithVertices(graph, 2, 3, 1);
        Graphs.addEdgeWithVertices(graph, 2, 1, 1);
        Graphs.addEdgeWithVertices(graph, 3, 2, 1);

        GraphContractor<Integer, DefaultWeightedEdge> contractor = new GraphContractor<>(graph,
                new PairingHeap<>(), Random::new, 0, 1);
        Pair<Graph<GraphContractor.ContractionVertex<Integer>,
                GraphContractor.ContractionEdge<DefaultWeightedEdge>>,
                Map<Integer, GraphContractor.ContractionVertex<Integer>>> p = contractor.computeContractionHierarchy();

        assertNotNull(p);

        Graph<GraphContractor.ContractionVertex<Integer>,
                GraphContractor.ContractionEdge<DefaultWeightedEdge>> contractionGraph = p.getFirst();
        Map<Integer, GraphContractor.ContractionVertex<Integer>> contractionMapping = p.getSecond();

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
        Graph<Integer, DefaultWeightedEdge> graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);

        Graphs.addEdgeWithVertices(graph, 1, 5, 1);
        Graphs.addEdgeWithVertices(graph, 2, 5, 1);

        Graphs.addEdgeWithVertices(graph, 5, 6, 1);
        Graphs.addEdgeWithVertices(graph, 5, 7, 1);

        GraphContractor<Integer, DefaultWeightedEdge> contractor = new GraphContractor<>(graph,
                new PairingHeap<>(), Random::new, 0, 1);
        Pair<Graph<GraphContractor.ContractionVertex<Integer>,
                GraphContractor.ContractionEdge<DefaultWeightedEdge>>,
                Map<Integer, GraphContractor.ContractionVertex<Integer>>> p = contractor.computeContractionHierarchy();

        assertNotNull(p);

        Graph<GraphContractor.ContractionVertex<Integer>,
                GraphContractor.ContractionEdge<DefaultWeightedEdge>> contractionGraph = p.getFirst();
        Map<Integer, GraphContractor.ContractionVertex<Integer>> contractionMapping = p.getSecond();

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
        Graph<Integer, DefaultWeightedEdge> graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);

        Graphs.addEdgeWithVertices(graph, 1, 5, 1);
        Graphs.addEdgeWithVertices(graph, 5, 1, 1);
        Graphs.addEdgeWithVertices(graph, 2, 5, 1);
        Graphs.addEdgeWithVertices(graph, 5, 2, 1);

        Graphs.addEdgeWithVertices(graph, 5, 6, 1);
        Graphs.addEdgeWithVertices(graph, 6, 5, 1);
        Graphs.addEdgeWithVertices(graph, 5, 7, 1);
        Graphs.addEdgeWithVertices(graph, 7, 5, 1);

        GraphContractor<Integer, DefaultWeightedEdge> contractor = new GraphContractor<>(graph,
                new PairingHeap<>(), Random::new, 0, 1);
        Pair<Graph<GraphContractor.ContractionVertex<Integer>,
                GraphContractor.ContractionEdge<DefaultWeightedEdge>>,
                Map<Integer, GraphContractor.ContractionVertex<Integer>>> p = contractor.computeContractionHierarchy();

        assertNotNull(p);

        Graph<GraphContractor.ContractionVertex<Integer>,
                GraphContractor.ContractionEdge<DefaultWeightedEdge>> contractionGraph = p.getFirst();
        Map<Integer, GraphContractor.ContractionVertex<Integer>> contractionMapping = p.getSecond();

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
        Graph<Integer, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);

        Graphs.addEdgeWithVertices(graph, 1, 2, 1);
        Graphs.addEdgeWithVertices(graph, 2, 3, 1);

        GraphContractor<Integer, DefaultWeightedEdge> contractor = new GraphContractor<>(graph,
                new PairingHeap<>(), Random::new, 0, 1);
        Pair<Graph<GraphContractor.ContractionVertex<Integer>,
                GraphContractor.ContractionEdge<DefaultWeightedEdge>>,
                Map<Integer, GraphContractor.ContractionVertex<Integer>>> p = contractor.computeContractionHierarchy();

        assertNotNull(p);

        Graph<GraphContractor.ContractionVertex<Integer>,
                GraphContractor.ContractionEdge<DefaultWeightedEdge>> contractionGraph = p.getFirst();
        Map<Integer, GraphContractor.ContractionVertex<Integer>> contractionMapping = p.getSecond();

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
        Graph<Integer, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);

        Graphs.addEdgeWithVertices(graph, 1, 5, 1);
        Graphs.addEdgeWithVertices(graph, 2, 5, 1);

        Graphs.addEdgeWithVertices(graph, 5, 6, 1);
        Graphs.addEdgeWithVertices(graph, 5, 7, 1);

        GraphContractor<Integer, DefaultWeightedEdge> contractor = new GraphContractor<>(graph,
                new PairingHeap<>(), Random::new, 0, 1);
        Pair<Graph<GraphContractor.ContractionVertex<Integer>,
                GraphContractor.ContractionEdge<DefaultWeightedEdge>>,
                Map<Integer, GraphContractor.ContractionVertex<Integer>>> p = contractor.computeContractionHierarchy();

        assertNotNull(p);

        Graph<GraphContractor.ContractionVertex<Integer>,
                GraphContractor.ContractionEdge<DefaultWeightedEdge>> contractionGraph = p.getFirst();
        Map<Integer, GraphContractor.ContractionVertex<Integer>> contractionMapping = p.getSecond();

        assertEquals(5, graph.vertexSet().size());

        if (contractionMapping.get(2).contractionIndex == 0) {
            assertEquals(10, contractionGraph.edgeSet().size());

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
    public void test1() {

    }

    @Test
    public void test2() {

    }

    @Test
    public void test3() {

    }
}