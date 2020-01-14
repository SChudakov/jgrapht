package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedPseudograph;
import org.jgrapht.graph.GraphWalk;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.util.SupplierUtil;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.jgrapht.alg.shortestpath.ContractionHierarchy.ContractionEdge;
import static org.jgrapht.alg.shortestpath.ContractionHierarchy.ContractionVertex;
import static org.jgrapht.alg.shortestpath.TransitNodeRoutingPrecomputation.TransitNodeRouting;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TransitNodeRoutingShortestPathTest {
    private static final long SEED = 19L;

    @Test
    public void testOneVertex() {
        Integer vertex = 1;
        Graph<Integer, DefaultWeightedEdge> graph = new DirectedWeightedPseudograph<>(DefaultWeightedEdge.class);
        graph.addVertex(vertex);

        TransitNodeRoutingShortestPath<Integer, DefaultWeightedEdge> shortestPath = new TransitNodeRoutingShortestPath<>(graph);

        GraphPath<Integer, DefaultWeightedEdge> path = shortestPath.getPath(vertex, vertex);
        GraphWalk<Integer, DefaultWeightedEdge> expectedPath = new GraphWalk<>(graph, vertex, vertex,
                Collections.singletonList(vertex), Collections.emptyList(), 0.0);
        assertEquals(expectedPath, path);
    }

    @Test
    public void testTwoVertices() {
        Integer v1 = 1;
        Integer v2 = 2;

        Graph<Integer, DefaultWeightedEdge> graph = new DirectedWeightedPseudograph<>(DefaultWeightedEdge.class);
        DefaultWeightedEdge edge1 = Graphs.addEdgeWithVertices(graph, v1, v2, 1.0);
        DefaultWeightedEdge edge2 = Graphs.addEdgeWithVertices(graph, v1, v2, 2.0);
        DefaultWeightedEdge edge3 = Graphs.addEdgeWithVertices(graph, v2, v1, 1.0);

        Pair<Graph<ContractionVertex<Integer>, ContractionEdge<DefaultWeightedEdge>>,
                Map<Integer, ContractionVertex<Integer>>> p =
                new ContractionHierarchy<>(graph, () -> new Random(SEED)).computeContractionHierarchy();

        TransitNodeRouting<Integer, DefaultWeightedEdge> routing = new TransitNodeRoutingPrecomputation<>(
                graph, p.getFirst(), p.getSecond(), 1).computeTransitNodeRouting();
        TransitNodeRoutingShortestPath<Integer, DefaultWeightedEdge> shortestPath
                = new TransitNodeRoutingShortestPath<>(graph, routing);

        GraphPath<Integer, DefaultWeightedEdge> expectedPath1 = new GraphWalk<>(
                graph, v1, v2, Arrays.asList(v1, v2), Collections.singletonList(edge1), 1.0);
        assertEquals(expectedPath1, shortestPath.getPath(v1, v2));

        GraphPath<Integer, DefaultWeightedEdge> expectedPath2 = new GraphWalk<>(
                graph, v2, v1, Arrays.asList(v2, v1), Collections.singletonList(edge3), 1.0);
        assertEquals(expectedPath2, shortestPath.getPath(v2, v1));
    }

    @Test
    public void testThreeVertices() {
        Integer v1 = 1;
        Integer v2 = 2;
        Integer v3 = 3;

        Graph<Integer, DefaultWeightedEdge> graph = new DirectedWeightedPseudograph<>(DefaultWeightedEdge.class);
        DefaultWeightedEdge edge1 = Graphs.addEdgeWithVertices(graph, v1, v2, 1.0);
        DefaultWeightedEdge edge2 = Graphs.addEdgeWithVertices(graph, v2, v3, 2.0);
        DefaultWeightedEdge edge3 = Graphs.addEdgeWithVertices(graph, v3, v2, 1.0);

        Pair<Graph<ContractionVertex<Integer>, ContractionEdge<DefaultWeightedEdge>>,
                Map<Integer, ContractionVertex<Integer>>> p =
                new ContractionHierarchy<>(graph, () -> new Random(SEED)).computeContractionHierarchy();

        TransitNodeRouting<Integer, DefaultWeightedEdge> routing = new TransitNodeRoutingPrecomputation<>(
                graph, p.getFirst(), p.getSecond(), 1).computeTransitNodeRouting();
        TransitNodeRoutingShortestPath<Integer, DefaultWeightedEdge> shortestPath
                = new TransitNodeRoutingShortestPath<>(graph, routing);

        GraphPath<Integer, DefaultWeightedEdge> expectedPath1 = new GraphWalk<>(
                graph, v1, v2, Arrays.asList(v1, v2), Collections.singletonList(edge1), 1.0);
        assertEquals(expectedPath1, shortestPath.getPath(v1, v2));
        assertNull(shortestPath.getPath(v2, v1));

        GraphPath<Integer, DefaultWeightedEdge> expectedPath2 = new GraphWalk<>(
                graph, v2, v3, Arrays.asList(v2, v3), Collections.singletonList(edge2), 2.0);
        assertEquals(expectedPath2, shortestPath.getPath(v2, v3));
        GraphPath<Integer, DefaultWeightedEdge> expectedPath3 = new GraphWalk<>(
                graph, v3, v2, Arrays.asList(v3, v2), Collections.singletonList(edge3), 1.0);
        assertEquals(expectedPath3, shortestPath.getPath(v3, v2));

        GraphPath<Integer, DefaultWeightedEdge> expectedPath4 = new GraphWalk<>(
                graph, v1, v3, Arrays.asList(v1, v2, v3), Arrays.asList(edge1, edge2), 3.0);
        assertEquals(expectedPath4, shortestPath.getPath(v1, v3));
        assertNull(shortestPath.getPath(v3, v1));
    }

    @Test
    public void testOnRandomGraphs() {
        int numOfVertices = 100;
        int vertexDegree = 5;
        int numOfIterations = 100;
        int source = 0;
        Random random = new Random(SEED);
        for (int i = 0; i < numOfIterations; ++i) {
            System.out.println(i);
            test(generateRandomGraph(numOfVertices, vertexDegree * numOfVertices, random), source);
        }
    }

    /**
     * Test correctness of {@link ContractionHierarchyBidirectionalDijkstra} on
     * {@code graph} starting at {@code source}.
     *
     * @param graph  graph
     * @param source vertex in {@code graph}
     */
    private <T> void test(Graph<T, DefaultWeightedEdge> graph, T source) {
        ShortestPathAlgorithm.SingleSourcePaths<T, DefaultWeightedEdge> dijkstraShortestPaths =
                new DijkstraShortestPath<>(graph).getPaths(source);

        Pair<Graph<ContractionVertex<T>, ContractionHierarchy.ContractionEdge<DefaultWeightedEdge>>,
                Map<T, ContractionVertex<T>>> p
                = new ContractionHierarchy<>(graph, () -> new Random(SEED)).computeContractionHierarchy();

        TransitNodeRouting<T, DefaultWeightedEdge> routing = new TransitNodeRoutingPrecomputation<>(
                graph, p.getFirst(), p.getSecond()).computeTransitNodeRouting();

        TransitNodeRoutingShortestPath<T, DefaultWeightedEdge> transitNodeRoutingShortestPath
                = new TransitNodeRoutingShortestPath<>(graph, routing);
        ShortestPathAlgorithm.SingleSourcePaths<T, DefaultWeightedEdge> tnrShortestPaths = transitNodeRoutingShortestPath.getPaths(source);


        assertEqualPaths(dijkstraShortestPaths, tnrShortestPaths, graph.vertexSet());

        System.out.println("LOCAL QUERIES COUNT: " + transitNodeRoutingShortestPath.isLocalCount);
    }

    /**
     * Generates an instance or random graph with {@code numOfVertices} vertices
     * and {@code numOfEdges} edges.
     *
     * @param numOfVertices number of vertices
     * @param numOfEdges    number of edges
     * @return generated graph
     */
    private Graph<Integer, DefaultWeightedEdge> generateRandomGraph(int numOfVertices, int numOfEdges, Random random) {
        DirectedWeightedPseudograph<Integer, DefaultWeightedEdge> graph =
                new DirectedWeightedPseudograph<>(DefaultWeightedEdge.class);
        graph.setVertexSupplier(SupplierUtil.createIntegerSupplier());

        GraphGenerator<Integer, DefaultWeightedEdge, Integer> generator =
                new GnmRandomGraphGenerator<>(numOfVertices, numOfEdges - numOfVertices + 1, SEED);
        generator.generateGraph(graph);
        makeConnected(graph);
        addEdgeWeights(graph, random);

        return graph;
    }

    /**
     * Makes {@code graph} connected.
     *
     * @param graph graph
     */
    private void makeConnected(Graph<Integer, DefaultWeightedEdge> graph) {
        Object[] vertices = graph.vertexSet().toArray();
        for (int i = 0; i < vertices.length - 1; ++i) {
            graph.addEdge((Integer) vertices[i], (Integer) vertices[i + 1]);
            graph.addEdge((Integer) vertices[i + 1], (Integer) vertices[i]);
        }
    }

    /**
     * Sets edge weights to edges in {@code graph}.
     *
     * @param graph  graph
     * @param random random numbers generator
     */
    private void addEdgeWeights(Graph<Integer, DefaultWeightedEdge> graph, Random random) {
        for (DefaultWeightedEdge edge : graph.edgeSet()) {
            graph.setEdgeWeight(edge, random.nextDouble());
        }
    }

    /**
     * Checks computed single source shortest paths tree for equality,
     *
     * @param expected  expected paths
     * @param actual    actual paths
     * @param vertexSet vertices
     */
    private <T> void assertEqualPaths(
            ShortestPathAlgorithm.SingleSourcePaths<T, DefaultWeightedEdge> expected,
            ShortestPathAlgorithm.SingleSourcePaths<T, DefaultWeightedEdge> actual,
            Set<T> vertexSet) {
        int i = 0;
        for (T sink : vertexSet) {
            System.out.println(i++);
            GraphPath<T, DefaultWeightedEdge> expectedPath = expected.getPath(sink);
            GraphPath<T, DefaultWeightedEdge> actualPath = actual.getPath(sink);
//            System.out.println(expectedPath);
//            System.out.println(actualPath);
//            System.out.println();
            assertEquals(expectedPath, actualPath);
        }
    }

    @Test
    public void testRoadMap() {
        String path = "/home/semen/drive/osm/final/andorra.txt";

        Graph<ShortestPathPerformance.Node, DefaultWeightedEdge> graph
                = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);

        System.out.println("reading graph");
        OSMReader reader = new OSMReader();
        reader.readGraph(graph, path, ShortestPathPerformance::greatCircleDistance);

        test(graph, new ShortestPathPerformance.Node(51552736, 1.5137007, 42.546934));
    }
}