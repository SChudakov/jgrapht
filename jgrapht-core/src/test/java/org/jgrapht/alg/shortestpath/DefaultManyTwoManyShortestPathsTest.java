package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.ManyToManyShortestPathsAlgorithm;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DefaultManyTwoManyShortestPathsTest extends BaseManyTwoManyShortestPathsTest {

    @Test
    public void testEmptyGraph() {
        Graph<Integer, DefaultWeightedEdge> graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        new DefaultManyTwoManyShortestPaths<>(graph).getManyTwoManyPaths(Collections.emptyList(), Collections.emptyList());
    }

    @Test(expected = NullPointerException.class)
    public void testSourcesIsNull() {
        Graph<Integer, DefaultWeightedEdge> graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        new DefaultManyTwoManyShortestPaths<>(graph).getManyTwoManyPaths(null, new ArrayList<>());
    }

    @Test(expected = NullPointerException.class)
    public void testTargetsIsNull() {
        Graph<Integer, DefaultWeightedEdge> graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        new DefaultManyTwoManyShortestPaths<>(graph).getManyTwoManyPaths(new ArrayList<>(), null);
    }

    @Test
    public void testNoPath() {
        Graph<Integer, DefaultWeightedEdge> graph = new DirectedWeightedMultigraph<>(DefaultWeightedEdge.class);
        graph.addVertex(1);
        graph.addVertex(2);

        ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<Integer, DefaultWeightedEdge> shortestPaths
                = new DefaultManyTwoManyShortestPaths<>(graph).getManyTwoManyPaths(
                Collections.singletonList(1), Collections.singletonList(2));

        assertEquals(Double.POSITIVE_INFINITY, shortestPaths.getWeight(1, 2), 1e-9);
        assertNull(shortestPaths.getPath(1, 2));
    }

    @Test
    public void testDifferentSourcesAndTargets1() {
        testDifferentSourcesAndTargets1(new DefaultManyTwoManyShortestPaths<>(getSimpleGraph()));
    }

    @Test
    public void testDifferentSourcesAndTargets2() {
        testDifferentSourcesAndTargets2(new DefaultManyTwoManyShortestPaths<>(getMultigraph()));
    }

    @Test
    public void testSourcesEqualTargets1() {
        testSourcesEqualTargets1(new DefaultManyTwoManyShortestPaths<>(getSimpleGraph()));
    }

    @Test
    public void testSourcesEqualTargets2() {
        testSourcesEqualTargets2(new DefaultManyTwoManyShortestPaths<>(getMultigraph()));
    }

    @Test
    public void testOnRandomGraphs() {
        int numOfVertices = 100;
        int vertexDegree = 5;
        int numOfIterations = 100;
        int numOfRandomVertices = 10;

        Random random = new Random(SEED);

        for (int i = 0; i < numOfIterations; i++) {
            Graph<Integer, DefaultWeightedEdge> graph = generateRandomGraph(
                    numOfVertices, vertexDegree * numOfVertices, random);

            ManyToManyShortestPathsAlgorithm<Integer, DefaultWeightedEdge> algorithm
                    = new DefaultManyTwoManyShortestPaths<>(graph);

            List<Integer> sources = getRandomVertices(graph, numOfRandomVertices, random);
            List<Integer> targets = getRandomVertices(graph, numOfRandomVertices, random);
            test(algorithm, graph, sources, targets);
        }
    }
}