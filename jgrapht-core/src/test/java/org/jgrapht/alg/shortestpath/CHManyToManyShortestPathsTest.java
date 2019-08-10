package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.ManyToManyShortestPathsAlgorithm;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CHManyToManyShortestPathsTest extends BaseManyTwoManyShortestPathsTest {

    @Test
    public void testEmptyGraph() {
        Graph<Integer, DefaultWeightedEdge> graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        new CHManyToManyShortestPaths<>(graph).getManyTwoManyPaths(Collections.emptyList(), Collections.emptyList());
    }

    @Test(expected = NullPointerException.class)
    public void testSourcesIsNull() {
        Graph<Integer, DefaultWeightedEdge> graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        new CHManyToManyShortestPaths<>(graph).getManyTwoManyPaths(null, new ArrayList<>());
    }

    @Test(expected = NullPointerException.class)
    public void testTargetsIsNull() {
        Graph<Integer, DefaultWeightedEdge> graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        new CHManyToManyShortestPaths<>(graph).getManyTwoManyPaths(new ArrayList<>(), null);
    }

    @Test
    public void testNoPath() {
        Graph<Integer, DefaultWeightedEdge> graph = new DirectedWeightedMultigraph<>(DefaultWeightedEdge.class);
        graph.addVertex(1);
        graph.addVertex(2);

        ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<Integer, DefaultWeightedEdge> shortestPaths
                = new CHManyToManyShortestPaths<>(graph).getManyTwoManyPaths(
                Collections.singletonList(1), Collections.singletonList(2));

        assertEquals(Double.POSITIVE_INFINITY, shortestPaths.getWeight(1, 2), 1e-9);
        assertNull(shortestPaths.getPath(1, 2));
    }

    @Test
    public void testDifferentSourcesAndTargets1() {
        testDifferentSourcesAndTargets1(new CHManyToManyShortestPaths<>(getSimpleGraph()));
    }

    @Test
    public void testDifferentSourcesAndTargets2() {
        testDifferentSourcesAndTargets2(new CHManyToManyShortestPaths<>(getMultigraph()));
    }

    @Test
    public void testSourcesEqualTargets1() {
        testSourcesEqualTargets1(new CHManyToManyShortestPaths<>(getSimpleGraph()));
    }

    @Test
    public void testSourcesEqualTargets2() {
        testSourcesEqualTargets2(new CHManyToManyShortestPaths<>(getMultigraph()));
    }

    @Test
    public void testMoreSourcesThanTargets1() {

        Graph<Integer, DefaultWeightedEdge> graph = getSimpleGraph();

        ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<Integer, DefaultWeightedEdge> shortestPaths
                = new CHManyToManyShortestPaths<>(graph).getManyTwoManyPaths(
                Arrays.asList(1, 3, 7, 9),
                Collections.singletonList(5)
        );

        assertEquals(2.0, shortestPaths.getWeight(1, 5), 1e-9);
        assertEquals(Arrays.asList(1, 4, 5), shortestPaths.getPath(1, 5));

        assertEquals(2.0, shortestPaths.getWeight(3, 5), 1e-9);
        assertEquals(Arrays.asList(3, 6, 5), shortestPaths.getPath(3, 5));

        assertEquals(2.0, shortestPaths.getWeight(7, 5), 1e-9);
        assertEquals(Arrays.asList(7, 4, 5), shortestPaths.getPath(7, 5));

        assertEquals(2.0, shortestPaths.getWeight(9, 5), 1e-9);
        assertEquals(Arrays.asList(9, 6, 5), shortestPaths.getPath(9, 5));
    }

    @Test
    public void testMoreSourcesThanTargets2() {
        Graph<Integer, DefaultWeightedEdge> graph = getMultigraph();

        ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<Integer, DefaultWeightedEdge> shortestPaths
                = new CHManyToManyShortestPaths<>(graph).getManyTwoManyPaths(
                Arrays.asList(2, 3, 4, 5, 6),
                Collections.singletonList(1)
        );

        assertEquals(3.0, shortestPaths.getWeight(2, 1), 1e-9);
        assertEquals(Arrays.asList(2, 1), shortestPaths.getPath(2, 1));

        assertEquals(8.0, shortestPaths.getWeight(3, 1), 1e-9);
        assertEquals(Arrays.asList(3, 2, 1), shortestPaths.getPath(3, 1));

        assertEquals(29.0, shortestPaths.getWeight(4, 1), 1e-9);
        assertEquals(Arrays.asList(4, 3, 2, 1), shortestPaths.getPath(4, 1));

        assertEquals(40.0, shortestPaths.getWeight(5, 1), 1e-9);
        assertEquals(Arrays.asList(5, 6, 1), shortestPaths.getPath(5, 1));

        assertEquals(23.0, shortestPaths.getWeight(6, 1), 1e-9);
        assertEquals(Arrays.asList(6, 1), shortestPaths.getPath(6, 1));
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

            Pair<Graph<ContractionHierarchyAlgorithm.ContractionVertex<Integer>, ContractionHierarchyAlgorithm.ContractionEdge<DefaultWeightedEdge>>,
                    Map<Integer, ContractionHierarchyAlgorithm.ContractionVertex<Integer>>> contraction
                    = new ContractionHierarchyAlgorithm<>(graph, () -> new Random(SEED)).computeContractionHierarchy();
            ManyToManyShortestPathsAlgorithm<Integer, DefaultWeightedEdge> algorithm
                    = new CHManyToManyShortestPaths<>(graph, contraction.getFirst(), contraction.getSecond());


            List<Integer> sources = getRandomVertices(graph, numOfRandomVertices, random);
            List<Integer> targets = getRandomVertices(graph, numOfRandomVertices, random);
            test(algorithm, graph, sources, targets);
        }
    }
}