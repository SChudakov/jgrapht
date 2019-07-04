package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jheaps.tree.PairingHeap;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


public class GraphContractorTest {

    @Test
    public void testEmptyGraph() {
        Graph<Integer, DefaultWeightedEdge> graph = new DefaultUndirectedGraph<>(DefaultWeightedEdge.class);
        GraphContractor<Integer, DefaultWeightedEdge> contractor = new GraphContractor<>(graph,
                new PairingHeap<>(), 0, Runtime.getRuntime().availableProcessors());
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
    public void testDirectedGraph() {

    }

    @Test
    public void testUndirectedGraph() {

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