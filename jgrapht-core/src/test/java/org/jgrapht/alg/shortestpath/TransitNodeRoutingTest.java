package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedPseudograph;
import org.jgrapht.util.SupplierUtil;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.jgrapht.alg.shortestpath.ContractionHierarchy.ContractionEdge;
import static org.jgrapht.alg.shortestpath.ContractionHierarchy.ContractionVertex;
import static org.jgrapht.alg.shortestpath.TransitNodeRouting.AccessVertex;
import static org.jgrapht.alg.shortestpath.TransitNodeRouting.AccessVertices;
import static org.jgrapht.alg.shortestpath.TransitNodeRouting.TransitNodeRoutingData;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TransitNodeRoutingTest {
    private static final long SEED = 19L;

    @Test
    public void name() {

    }

    @Test
    public void testEmptyGraph() {
        Graph<Integer, DefaultWeightedEdge> graph = new DirectedWeightedPseudograph<>(DefaultWeightedEdge.class);

        Pair<Graph<ContractionVertex<Integer>, ContractionEdge<DefaultWeightedEdge>>,
                Map<Integer, ContractionVertex<Integer>>> p
                = new ContractionHierarchy<>(graph, () -> new Random(SEED)).computeContractionHierarchy();

        TransitNodeRouting<Integer, DefaultWeightedEdge> routing = new TransitNodeRouting<>(graph, p.getFirst(), p.getSecond(), 0);
        TransitNodeRoutingData<Integer, DefaultWeightedEdge> data = routing.computeTransitNodeRoutingData();
    }

    @Test
    public void testOneVertex() {
        // initialisation
        Integer vertex = 1;
        Graph<Integer, DefaultWeightedEdge> graph = new DirectedWeightedPseudograph<>(DefaultWeightedEdge.class);
        graph.addVertex(vertex);

        Pair<Graph<ContractionVertex<Integer>, ContractionEdge<DefaultWeightedEdge>>, Map<Integer, ContractionVertex<Integer>>> p
                = new ContractionHierarchy<>(graph, () -> new Random(SEED)).computeContractionHierarchy();

        // computation
        TransitNodeRouting<Integer, DefaultWeightedEdge> routing =
                new TransitNodeRouting<>(graph, p.getFirst(), p.getSecond(), 1);
        TransitNodeRoutingData<Integer, DefaultWeightedEdge> data = routing.computeTransitNodeRoutingData();

        Map<Integer, ContractionVertex<Integer>> contractionMapping = data.getContractionMapping();
        ContractionVertex<Integer> contractionVertex = contractionMapping.get(vertex);

        // transit vertices
        assertTrue(data.getTransitVertices().contains(contractionVertex));

        // access vertices
        AccessVertices<Integer, DefaultWeightedEdge> accessVertices = data.getAccessVertices();
        List<AccessVertex<Integer, DefaultWeightedEdge>> forwardAccessVertices = accessVertices.getForwardAccessVertices(contractionVertex);
        List<AccessVertex<Integer, DefaultWeightedEdge>> backwardAccessVertices = accessVertices.getBackwardAccessVertices(contractionVertex);
        assertEquals(forwardAccessVertices.size(), 1);
        assertEquals(forwardAccessVertices.get(0).vertex, vertex);
        assertEquals(forwardAccessVertices.get(0).path.getStartVertex(), vertex);
        assertEquals(forwardAccessVertices.get(0).path.getEndVertex(), vertex);
        assertEquals(forwardAccessVertices.get(0).path.getWeight(), 0.0, 1e-9);
        assertEquals(forwardAccessVertices.get(0).path.getLength(), 0);
        assertEquals(forwardAccessVertices.get(0).path.getVertexList(), Collections.singletonList(vertex));
        assertEquals(forwardAccessVertices.get(0).path.getEdgeList(), Collections.emptyList());
        assertEquals(backwardAccessVertices.size(), 1);
        assertEquals(backwardAccessVertices.get(0).vertex, vertex);
        assertEquals(backwardAccessVertices.get(0).path.getStartVertex(), vertex);
        assertEquals(backwardAccessVertices.get(0).path.getEndVertex(), vertex);
        assertEquals(backwardAccessVertices.get(0).path.getWeight(), 0.0, 1e-9);
        assertEquals(backwardAccessVertices.get(0).path.getLength(), 0);
        assertEquals(backwardAccessVertices.get(0).path.getVertexList(), Collections.singletonList(vertex));
        assertEquals(backwardAccessVertices.get(0).path.getEdgeList(), Collections.emptyList());

        // locality filter
        assertFalse(data.getLocalityFiler().isLocal(vertex, vertex));
    }

    @Test
    public void testThreeVertices() {
        Integer v1 = 1;
        Integer v2 = 2;
        Integer v3 = 3;
        Graph<Integer, DefaultWeightedEdge> graph = new DirectedWeightedPseudograph<>(DefaultWeightedEdge.class);
        graph.addVertex(v1);
        graph.addVertex(v2);
        graph.addVertex(v3);
        Graphs.addEdgeWithVertices(graph, v1, v2, 1.0);
        Graphs.addEdgeWithVertices(graph, v2, v1, 1.0);
        Graphs.addEdgeWithVertices(graph, v2, v3, 2.0); // to ensure Voronoi diagram correctness
        Graphs.addEdgeWithVertices(graph, v3, v2, 2.0);

        Pair<Graph<ContractionVertex<Integer>, ContractionEdge<DefaultWeightedEdge>>, Map<Integer, ContractionVertex<Integer>>> p
                = new ContractionHierarchy<>(graph, () -> new Random(SEED)).computeContractionHierarchy();

        // computation
        TransitNodeRouting<Integer, DefaultWeightedEdge> routing = new TransitNodeRouting<>(
                graph, p.getFirst(), p.getSecond(), 2,
                numOfTransitVertices -> new HashSet<>(Arrays.asList(v1, v3)));
        TransitNodeRoutingData<Integer, DefaultWeightedEdge> data = routing.computeTransitNodeRoutingData();

        Map<Integer, ContractionVertex<Integer>> contractionMapping = data.getContractionMapping();
        ContractionVertex<Integer> cv1 = contractionMapping.get(v1);
        ContractionVertex<Integer> cv2 = contractionMapping.get(v2);
        ContractionVertex<Integer> cv3 = contractionMapping.get(v3);


        // transit vertices
        assertTrue(data.getTransitVertices().contains(cv1));
        assertTrue(data.getTransitVertices().contains(cv3));

        // access vertices
        AccessVertices<Integer, DefaultWeightedEdge> accessVertices = data.getAccessVertices();
        List<AccessVertex<Integer, DefaultWeightedEdge>> cv1ForwardAccessVertices = accessVertices.getForwardAccessVertices(cv1);
        List<AccessVertex<Integer, DefaultWeightedEdge>> cv1BackwardAccessVertices = accessVertices.getBackwardAccessVertices(cv1);
        assertEquals(cv1ForwardAccessVertices.size(), 1);
        assertEquals(cv1ForwardAccessVertices.get(0).vertex, v1);
        assertEquals(cv1ForwardAccessVertices.get(0).path.getStartVertex(), v1);
        assertEquals(cv1ForwardAccessVertices.get(0).path.getEndVertex(), v1);
        assertEquals(cv1ForwardAccessVertices.get(0).path.getWeight(), 0.0, 1e-9);
        assertEquals(cv1ForwardAccessVertices.get(0).path.getLength(), 0);
        assertEquals(cv1ForwardAccessVertices.get(0).path.getVertexList(), Collections.singletonList(v1));
        assertEquals(cv1ForwardAccessVertices.get(0).path.getEdgeList(), Collections.emptyList());
        assertEquals(cv1BackwardAccessVertices.size(), 1);
        assertEquals(cv1BackwardAccessVertices.get(0).vertex, v1);
        assertEquals(cv1BackwardAccessVertices.get(0).path.getStartVertex(), v1);
        assertEquals(cv1BackwardAccessVertices.get(0).path.getEndVertex(), v1);
        assertEquals(cv1BackwardAccessVertices.get(0).path.getWeight(), 0.0, 1e-9);
        assertEquals(cv1BackwardAccessVertices.get(0).path.getLength(), 0);
        assertEquals(cv1BackwardAccessVertices.get(0).path.getVertexList(), Collections.singletonList(v1));
        assertEquals(cv1BackwardAccessVertices.get(0).path.getEdgeList(), Collections.emptyList());

        List<AccessVertex<Integer, DefaultWeightedEdge>> cv2ForwardAccessVertices = accessVertices.getForwardAccessVertices(cv2);
        List<AccessVertex<Integer, DefaultWeightedEdge>> cv2BackwardAccessVertices = accessVertices.getBackwardAccessVertices(cv2);
        assertEquals(cv2ForwardAccessVertices.size(), 0);
//        assertEquals(cv2ForwardAccessVertices.get(0).vertex, v1);
//        assertEquals(cv2ForwardAccessVertices.get(0).path.getStartVertex(), v2);
//        assertEquals(cv2ForwardAccessVertices.get(0).path.getEndVertex(), v1);
//        assertEquals(cv2ForwardAccessVertices.get(0).path.getWeight(), 1.0, 1e-9);
//        assertEquals(cv2ForwardAccessVertices.get(0).path.getLength(), 1);
//        assertEquals(cv2ForwardAccessVertices.get(0).path.getVertexList(), Arrays.asList(v2, v1));
//        assertEquals(cv2ForwardAccessVertices.get(0).path.getEdgeList(), Collections.singletonList(graph.getEdge(v2, v1)));
//        assertEquals(cv2ForwardAccessVertices.get(1).vertex, v3);
//        assertEquals(cv2ForwardAccessVertices.get(1).path.getStartVertex(), v2);
//        assertEquals(cv2ForwardAccessVertices.get(1).path.getEndVertex(), v3);
//        assertEquals(cv2ForwardAccessVertices.get(1).path.getWeight(), 1.0, 1e-9);
//        assertEquals(cv2ForwardAccessVertices.get(1).path.getLength(), 1);
//        assertEquals(cv2ForwardAccessVertices.get(1).path.getVertexList(), Arrays.asList(v2, v3));
//        assertEquals(cv2ForwardAccessVertices.get(1).path.getEdgeList(), Collections.singletonList(graph.getEdge(v2, v3)));
        assertEquals(cv2BackwardAccessVertices.size(), 0);
//        assertEquals(cv2BackwardAccessVertices.get(0).vertex, v1);
//        assertEquals(cv2BackwardAccessVertices.get(0).path.getStartVertex(), v1);
//        assertEquals(cv2BackwardAccessVertices.get(0).path.getEndVertex(), v2);
//        assertEquals(cv2BackwardAccessVertices.get(0).path.getWeight(), 1.0, 1e-9);
//        assertEquals(cv2BackwardAccessVertices.get(0).path.getLength(), 1);
//        assertEquals(cv2BackwardAccessVertices.get(0).path.getVertexList(), Arrays.asList(v1, v2));
//        assertEquals(cv2BackwardAccessVertices.get(0).path.getEdgeList(), Collections.singletonList(graph.getEdge(v1, v2)));
//        assertEquals(cv2BackwardAccessVertices.get(1).vertex, v3);
//        assertEquals(cv2BackwardAccessVertices.get(1).path.getStartVertex(), v3);
//        assertEquals(cv2BackwardAccessVertices.get(1).path.getEndVertex(), v2);
//        assertEquals(cv2BackwardAccessVertices.get(1).path.getWeight(), 1.0, 1e-9);
//        assertEquals(cv2BackwardAccessVertices.get(1).path.getLength(), 1);
//        assertEquals(cv2BackwardAccessVertices.get(1).path.getVertexList(), Arrays.asList(v3, v2));
//        assertEquals(cv2BackwardAccessVertices.get(1).path.getEdgeList(), Collections.singletonList(graph.getEdge(v3, v2)));

        List<AccessVertex<Integer, DefaultWeightedEdge>> cv3ForwardAccessVertices = accessVertices.getForwardAccessVertices(cv3);
        List<AccessVertex<Integer, DefaultWeightedEdge>> cv3BackwardAccessVertices = accessVertices.getBackwardAccessVertices(cv3);
        assertEquals(cv3ForwardAccessVertices.size(), 1);
        assertEquals(cv3ForwardAccessVertices.get(0).vertex, v3);
        assertEquals(cv3ForwardAccessVertices.get(0).path.getStartVertex(), v3);
        assertEquals(cv3ForwardAccessVertices.get(0).path.getEndVertex(), v3);
        assertEquals(cv3ForwardAccessVertices.get(0).path.getWeight(), 0.0, 1e-9);
        assertEquals(cv3ForwardAccessVertices.get(0).path.getLength(), 0);
        assertEquals(cv3ForwardAccessVertices.get(0).path.getVertexList(), Collections.singletonList(v3));
        assertEquals(cv3ForwardAccessVertices.get(0).path.getEdgeList(), Collections.emptyList());
        assertEquals(cv3BackwardAccessVertices.size(), 1);
        assertEquals(cv3BackwardAccessVertices.get(0).vertex, v3);
        assertEquals(cv3BackwardAccessVertices.get(0).path.getStartVertex(), v3);
        assertEquals(cv3BackwardAccessVertices.get(0).path.getEndVertex(), v3);
        assertEquals(cv3BackwardAccessVertices.get(0).path.getWeight(), 0.0, 1e-9);
        assertEquals(cv3BackwardAccessVertices.get(0).path.getLength(), 0);
        assertEquals(cv3BackwardAccessVertices.get(0).path.getVertexList(), Collections.singletonList(v3));
        assertEquals(cv3BackwardAccessVertices.get(0).path.getEdgeList(), Collections.emptyList());

        // locality filter
        assertFalse(data.getLocalityFiler().isLocal(v1, v1));
        assertFalse(data.getLocalityFiler().isLocal(v1, v2));
        assertFalse(data.getLocalityFiler().isLocal(v1, v3));
        assertTrue(data.getLocalityFiler().isLocal(v2, v2));
        assertFalse(data.getLocalityFiler().isLocal(v2, v3));
        assertFalse(data.getLocalityFiler().isLocal(v3, v3));
    }

    @Test
    public void testOnRandomGraphs() {
        int numOfVertices = 100;
        int vertexDegree = 5;
        int numOfIterations = 100;

        Random random = new Random(SEED);

        for (int i = 0; i < numOfIterations; ++i) {
            System.out.println(i);
            Graph<Integer, DefaultWeightedEdge> graph =
                    generateRandomGraph(numOfVertices, vertexDegree * numOfVertices, random);
            TransitNodeRouting<Integer, DefaultWeightedEdge> routing = new TransitNodeRouting<>(graph);
            TransitNodeRoutingData<Integer, DefaultWeightedEdge> data = routing.computeTransitNodeRoutingData();
        }
    }

    /**
     * Generates a graph instance from the $G(n,M)$ random graphs model with {@code numOfVertices} vertices
     * and {@code numOfEdges} edges.
     *
     * @param numOfVertices number of vertices in a graph
     * @param numOfEdges    number of edges in a graph
     * @param random        random generator
     * @return random graph
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
     * @param graph a graph
     */
    private void makeConnected(Graph<Integer, DefaultWeightedEdge> graph) {
        Object[] vertices = graph.vertexSet().toArray();
        for (int i = 0; i < vertices.length - 1; ++i) {
            graph.addEdge((Integer) vertices[i], (Integer) vertices[i + 1]);
            graph.addEdge((Integer) vertices[i + 1], (Integer) vertices[i]);
        }
    }

    /**
     * Sets weight for every edge in the {@code graph}.
     *
     * @param graph  a graph
     * @param random random generator instance
     */
    private void addEdgeWeights(Graph<Integer, DefaultWeightedEdge> graph, Random random) {
        for (DefaultWeightedEdge edge : graph.edgeSet()) {
            graph.setEdgeWeight(edge, random.nextDouble());
        }
    }
}