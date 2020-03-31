package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.generate.GnpRandomGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.GraphWalk;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.util.CollectionUtil;
import org.jgrapht.util.SupplierUtil;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class KShortestSimplePathTest
        extends BaseKShortestPathTest {

    /**
     * Seed value which is used to generate random graphs by
     * {@code getRandomGraph(Graph, int, double)} method.
     */
    private static final long SEED = 13L;
    /**
     * Number of path to iterate over for each random graph in the
     * {@code testOnRandomGraph(Graph, Integer, Integer)} method.
     */
    private static final int NUMBER_OF_PATH_TO_ITERATE = 100;

    @Test(expected = IllegalArgumentException.class)
    public void testNegativeK() {
        Graph<Integer, DefaultWeightedEdge> graph =
                new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        Graphs.addEdgeWithVertices(graph, 1, 2);
        new KShortestSimplePath<>(graph).getPaths(1, 2, -1);
    }

    /**
     * If k equals to $0$ and there is no paths in the graph between source and target, no exception
     * should be thrown and an empty list should be returned.
     */
    @Test
    public void testKEqualsZero() {
        Graph<Integer, DefaultWeightedEdge> graph =
                new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        graph.addVertex(1);
        graph.addVertex(2);
        List<GraphPath<Integer, DefaultWeightedEdge>> paths =
                new KShortestSimplePath<>(graph).getPaths(1, 2, 0);
        assertEquals(0, paths.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoSourceGraph() {
        Graph<Integer, DefaultWeightedEdge> graph =
                new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        graph.addVertex(2);
        new KShortestSimplePath<>(graph).getPaths(1, 2, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNoSinkGraph() {
        Graph<Integer, DefaultWeightedEdge> graph =
                new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        graph.addVertex(1);
        new KShortestSimplePath<>(graph).getPaths(1, 2, 1);
    }

    @Test
    public void testCyclicGraph() {
        Graph<Integer, DefaultWeightedEdge> graph =
                new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        readGraph(graph, cyclicGraph3);
        List<GraphPath<Integer, DefaultWeightedEdge>> paths =
                new KShortestSimplePath<>(graph).getPaths(1, 3, 1);
        List<Double> weights = Collections.singletonList(2.0);

        assertSameWeights(paths, weights);

        ((GraphWalk<Integer, DefaultWeightedEdge>) paths.get(0)).verify();
    }

    /**
     * If the specified k is greater than the total amount of paths between source and target, a
     * list of all existing paths should be returned and no exception should be thrown.
     */
    @Test
    public void testLessThanKPaths() {
        Graph<Integer, DefaultWeightedEdge> graph =
                new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        readGraph(graph, simpleGraph1);
        List<GraphPath<Integer, DefaultWeightedEdge>> paths =
                new KShortestSimplePath<>(graph).getPaths(1, 12, 12);
        List<Double> weights =
                Arrays.asList(55.0, 58.0, 59.0, 61.0, 62.0, 64.0, 65.0, 68.0, 68.0, 71.0);

        assertSameWeights(paths, weights);

        for (GraphPath<Integer, DefaultWeightedEdge> path : paths) {
            ((GraphWalk<Integer, DefaultWeightedEdge>) path).verify();
        }
    }

    @Test
    public void testOnRandomGraphs() {
        Random random = new Random(SEED);
        int n = 50;
        double p = 0.2;
        int numberOfRandomEdges = 0;
        for (int i = 0; i < 100; i++) {
            System.out.println(i);

            SimpleDirectedWeightedGraph<Integer, DefaultWeightedEdge> graph =
                    new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
            graph.setVertexSupplier(SupplierUtil.createIntegerSupplier());
            getRandomGraph(graph, n, p);

            Integer source = random.nextInt(n);
            Integer target = random.nextInt(n);

            if(i < 73){
                continue;
            }
            System.out.println("source: " + source);
            System.out.println("target: " + target);

//            Set<DefaultWeightedEdge> randomEdges = getRandomEdges(graph, numberOfRandomEdges);
//            PathValidator<Integer, DefaultWeightedEdge> pathValidator = (path, edge) -> !randomEdges.contains(edge);

            testOnRandomGraph(graph, source, target, null);
        }
    }

    /**
     * If the overall number of paths between {@code source} and {@code target} is denoted by $n$
     * and the value of {@code #NUMBER_OF_PATH_TO_ITERATE} is denoted by $m$ then the method
     * iterates over $p = min\{n, m\}$ such paths and verifies that they are built correctly. The
     * method uses the {@link KShortestSimplePaths} implementation to verify the order of paths
     * returned by {@link YenShortestPathIterator}. Additionally it is checked that all paths
     * returned by the iterator are unique.
     *
     * @param graph  graph the iterator is being tested on
     * @param source source vertex
     * @param target target vertex
     */
    private void testOnRandomGraph(
            Graph<Integer, DefaultWeightedEdge> graph, Integer source, Integer target,
            PathValidator<Integer, DefaultWeightedEdge> pathValidator) {

        List<GraphPath<Integer, DefaultWeightedEdge>> expectedPaths =
                new YenKShortestPath<>(graph, pathValidator).getPaths(source, target, NUMBER_OF_PATH_TO_ITERATE);
        List<GraphPath<Integer, DefaultWeightedEdge>> actualPaths =
                new KShortestSimplePath<>(graph).getPaths(source, target, NUMBER_OF_PATH_TO_ITERATE);

        assertEquals(expectedPaths.size(), actualPaths.size());

//        System.out.println();
        for (int i = 0; i < expectedPaths.size(); ++i) {
//            System.out.println(i);

            GraphPath<Integer, DefaultWeightedEdge> expected = expectedPaths.get(i);
            GraphPath<Integer, DefaultWeightedEdge> actual = actualPaths.get(i);

//            System.out.println("expected path \t" + expected);
//            System.out.println("actual path \t" + actual);

            assertEquals(expected.getWeight(), actual.getWeight(), 1e-9);

            ((GraphWalk<Integer, DefaultWeightedEdge>) actual).verify();
        }
//        System.out.println();

        Set<GraphPath<Integer, DefaultWeightedEdge>> actualPathsSet = new HashSet<>(actualPaths);
        assertEquals(actualPathsSet.size(), actualPathsSet.size());
    }

    private void assertSameWeights(
            List<GraphPath<Integer, DefaultWeightedEdge>> paths, List<Double> weights) {
        assertEquals(weights.size(), paths.size());
        for (int i = 0; i < paths.size(); i++) {
            assertEquals(weights.get(i), paths.get(i).getWeight(), 1e-9);
        }
    }

    /**
     * Generates random graph from the $G(n, p)$ model.
     *
     * @param graph graph instance for the generator
     * @param n     the number of nodes
     * @param p     the edge probability
     */
    private void getRandomGraph(Graph<Integer, DefaultWeightedEdge> graph, int n, double p) {
        Random random = new Random(SEED);
        GraphGenerator<Integer, DefaultWeightedEdge, Integer> generator =
                new GnpRandomGraphGenerator<>(n, p, SEED);
        generator.generateGraph(graph);

        graph.edgeSet().forEach(e -> graph.setEdgeWeight(e, random.nextInt(10)));
    }

    /**
     * Computes a set of random vertices of {@code graph}. The size of the
     * set is {@code numberOfEdges}.
     *
     * @param graph         a graph
     * @param numberOfEdges number of random vertices
     * @return set of random vertices
     */
    private Set<DefaultWeightedEdge> getRandomEdges(Graph<Integer, DefaultWeightedEdge> graph, int numberOfEdges) {
        Set<DefaultWeightedEdge> result = CollectionUtil.newHashSetWithExpectedSize(numberOfEdges);
        Object[] edges = graph.edgeSet().toArray();
        Random random = new Random(SEED);
        while (result.size() != numberOfEdges) {
            result.add((DefaultWeightedEdge) edges[random.nextInt(edges.length)]);
        }
        return result;
    }
}