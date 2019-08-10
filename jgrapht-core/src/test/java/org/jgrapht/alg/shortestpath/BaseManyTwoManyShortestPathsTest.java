package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.interfaces.ManyToManyShortestPathsAlgorithm;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.jgrapht.graph.DirectedWeightedPseudograph;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.util.SupplierUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class BaseManyTwoManyShortestPathsTest {
    protected static final long SEED = 17L;

    protected void testDifferentSourcesAndTargets1(ManyToManyShortestPathsAlgorithm<Integer, DefaultWeightedEdge> algorithm) {
        ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<Integer, DefaultWeightedEdge> shortestPaths
                = algorithm.getManyTwoManyPaths(
                Arrays.asList(4, 1, 2),
                Arrays.asList(8, 9, 6)
        );

        assertEquals(2.0, shortestPaths.getWeight(4, 8), 1e-9);
        assertEquals(Arrays.asList(4, 5, 8), shortestPaths.getPath(4, 8).getVertexList());

        assertEquals(3.0, shortestPaths.getWeight(4, 9), 1e-9);
        assertEquals(Arrays.asList(4, 5, 6, 9), shortestPaths.getPath(4, 9).getVertexList());

        assertEquals(2.0, shortestPaths.getWeight(4, 6), 1e-9);
        assertEquals(Arrays.asList(4, 5, 6), shortestPaths.getPath(4, 6).getVertexList());

        assertEquals(3.0, shortestPaths.getWeight(1, 8), 1e-9);
        assertEquals(Arrays.asList(1, 4, 5, 8), shortestPaths.getPath(1, 8).getVertexList());

        assertEquals(4.0, shortestPaths.getWeight(1, 9), 1e-9);
        assertEquals(Arrays.asList(1, 4, 5, 6, 9), shortestPaths.getPath(1, 9).getVertexList());

        assertEquals(3.0, shortestPaths.getWeight(1, 6), 1e-9);
        assertEquals(Arrays.asList(1, 4, 5, 6), shortestPaths.getPath(1, 6).getVertexList());

        assertEquals(2.0, shortestPaths.getWeight(2, 8), 1e-9);
        assertEquals(Arrays.asList(2, 5, 8), shortestPaths.getPath(2, 8).getVertexList());

        assertEquals(3.0, shortestPaths.getWeight(2, 9), 1e-9);
        assertEquals(Arrays.asList(2, 5, 6, 9), shortestPaths.getPath(2, 9).getVertexList());

        assertEquals(2.0, shortestPaths.getWeight(2, 6), 1e-9);
        assertEquals(Arrays.asList(2, 5, 6), shortestPaths.getPath(2, 6).getVertexList());
    }

    protected void testDifferentSourcesAndTargets2(ManyToManyShortestPathsAlgorithm<Integer, DefaultWeightedEdge> algorithm) {
        ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<Integer, DefaultWeightedEdge> shortestPaths
                = algorithm.getManyTwoManyPaths(
                Arrays.asList(1, 4),
                Arrays.asList(2, 5)
        );

        assertEquals(1.0, shortestPaths.getWeight(1, 2), 1e-9);
        assertEquals(Arrays.asList(1, 2), shortestPaths.getPath(1, 2).getVertexList());

        assertEquals(32, shortestPaths.getWeight(1, 5), 1e-9);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), shortestPaths.getPath(1, 5).getVertexList());

        assertEquals(16, shortestPaths.getWeight(4, 2), 1e-9);
        assertEquals(Arrays.asList(4, 3, 2), shortestPaths.getPath(4, 2).getVertexList());

        assertEquals(15, shortestPaths.getWeight(4, 5), 1e-9);
        assertEquals(Arrays.asList(4, 5), shortestPaths.getPath(4, 5).getVertexList());

    }

    protected void testSourcesEqualTargets1(ManyToManyShortestPathsAlgorithm<Integer, DefaultWeightedEdge> algorithm) {
        ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<Integer, DefaultWeightedEdge> shortestPaths
                = algorithm.getManyTwoManyPaths(
                Arrays.asList(1, 5, 9),
                Arrays.asList(1, 5, 9)
        );

        assertEquals(0.0, shortestPaths.getWeight(1, 1), 1e-9);
        assertEquals(Collections.singletonList(1), shortestPaths.getPath(1, 1).getVertexList());

        assertEquals(0.0, shortestPaths.getWeight(5, 5), 1e-9);
        assertEquals(Collections.singletonList(5), shortestPaths.getPath(5, 5).getVertexList());

        assertEquals(0.0, shortestPaths.getWeight(9, 9), 1e-9);
        assertEquals(Collections.singletonList(9), shortestPaths.getPath(9, 9).getVertexList());


        assertEquals(2.0, shortestPaths.getWeight(1, 5), 1e-9);
        assertEquals(Arrays.asList(1, 4, 5), shortestPaths.getPath(1, 5).getVertexList());
        assertEquals(2.0, shortestPaths.getWeight(5, 1), 1e-9);
        assertEquals(Arrays.asList(5, 4, 1), shortestPaths.getPath(5, 1).getVertexList());


        assertEquals(4.0, shortestPaths.getWeight(1, 9), 1e-9);
        assertEquals(Arrays.asList(1, 4, 5, 6, 9), shortestPaths.getPath(1, 9).getVertexList());
        assertEquals(4.0, shortestPaths.getWeight(9, 1), 1e-9);
        assertEquals(Arrays.asList(9, 6, 5, 4, 1), shortestPaths.getPath(9, 1).getVertexList());


        assertEquals(2.0, shortestPaths.getWeight(5, 9), 1e-9);
        assertEquals(Arrays.asList(5, 6, 9), shortestPaths.getPath(5, 9).getVertexList());
        assertEquals(2.0, shortestPaths.getWeight(9, 5), 1e-9);
        assertEquals(Arrays.asList(9, 6, 5), shortestPaths.getPath(9, 5).getVertexList());
    }

    protected void testSourcesEqualTargets2(ManyToManyShortestPathsAlgorithm<Integer, DefaultWeightedEdge> algorithm) {
        ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<Integer, DefaultWeightedEdge> shortestPaths
                = algorithm.getManyTwoManyPaths(
                Arrays.asList(2, 4, 6),
                Arrays.asList(2, 4, 6)
        );

        assertEquals(0.0, shortestPaths.getWeight(2, 2), 1e-9);
        assertEquals(Collections.singletonList(2), shortestPaths.getPath(2, 2).getVertexList());

        assertEquals(0.0, shortestPaths.getWeight(4, 4), 1e-9);
        assertEquals(Collections.singletonList(4), shortestPaths.getPath(4, 4).getVertexList());

        assertEquals(0.0, shortestPaths.getWeight(6, 6), 1e-9);
        assertEquals(Collections.singletonList(6), shortestPaths.getPath(6, 6).getVertexList());

        assertEquals(16.0, shortestPaths.getWeight(2, 4), 1e-9);
        assertEquals(Arrays.asList(2, 3, 4), shortestPaths.getPath(2, 4).getVertexList());
        assertEquals(16.0, shortestPaths.getWeight(4, 2), 1e-9);
        assertEquals(Arrays.asList(4, 3, 2), shortestPaths.getPath(4, 2).getVertexList());

        assertEquals(24.0, shortestPaths.getWeight(2, 6), 1e-9);
        assertEquals(Arrays.asList(2, 1, 6), shortestPaths.getPath(2, 6).getVertexList());
        assertEquals(24.0, shortestPaths.getWeight(6, 2), 1e-9);
        assertEquals(Arrays.asList(6, 1, 2), shortestPaths.getPath(6, 2).getVertexList());

        assertEquals(32.0, shortestPaths.getWeight(4, 6), 1e-9);
        assertEquals(Arrays.asList(4, 5, 6), shortestPaths.getPath(4, 6).getVertexList());
        assertEquals(32.0, shortestPaths.getWeight(6, 4), 1e-9);
        assertEquals(Arrays.asList(6, 5, 4), shortestPaths.getPath(6, 4).getVertexList());
    }


    protected void test(ManyToManyShortestPathsAlgorithm<Integer, DefaultWeightedEdge> algorithm,
                        Graph<Integer, DefaultWeightedEdge> graph, List<Integer> sources, List<Integer> targets) {
        ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<Integer, DefaultWeightedEdge> sourcesToTargetsPaths
                = algorithm.getManyTwoManyPaths(sources, targets);
        ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<Integer, DefaultWeightedEdge> sourcesToSourcesPaths
                = algorithm.getManyTwoManyPaths(sources, sources);

        assertCorrectPaths(graph, sourcesToTargetsPaths, sources, targets);
        assertCorrectPaths(graph, sourcesToSourcesPaths, sources, sources);
    }


    protected Graph<Integer, DefaultWeightedEdge> generateRandomGraph(int numOfVertices, int numOfEdges, Random random) {
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

    protected void makeConnected(Graph<Integer, DefaultWeightedEdge> graph) {
        Object[] vertices = graph.vertexSet().toArray();
        for (int i = 0; i < vertices.length - 1; ++i) {
            graph.addEdge((Integer) vertices[i], (Integer) vertices[i + 1]);
            graph.addEdge((Integer) vertices[i + 1], (Integer) vertices[i]);
        }
    }

    protected void addEdgeWeights(Graph<Integer, DefaultWeightedEdge> graph, Random random) {
        for (DefaultWeightedEdge edge : graph.edgeSet()) {
            graph.setEdgeWeight(edge, random.nextDouble());
        }
    }

    protected void assertCorrectPaths(Graph<Integer, DefaultWeightedEdge> graph,
                                      ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<Integer, DefaultWeightedEdge> paths,
                                      List<Integer> sources, List<Integer> targets
    ) {
        ShortestPathAlgorithm<Integer, DefaultWeightedEdge> dijkstra = new DijkstraShortestPath<>(graph);

        for (Integer source : sources) {
            ShortestPathAlgorithm.SingleSourcePaths<Integer, DefaultWeightedEdge> expectedPaths
                    = dijkstra.getPaths(source);
            for (Integer target : targets) {
                assertEquals(expectedPaths.getPath(target), paths.getPath(source, target));
            }
        }
    }

    protected List<Integer> getRandomVertices(
            Graph<Integer, DefaultWeightedEdge> graph, int numOfRandomVertices, Random random) {
        List<Integer> result = new ArrayList<>(numOfRandomVertices);
        Set<Integer> verticesSet = new HashSet<>();
        Integer[] graphVertices = graph.vertexSet().toArray(new Integer[0]);

        for (int i = 0; i < numOfRandomVertices; ++i) {
            int vertex = random.nextInt(graph.vertexSet().size());
            while (verticesSet.contains(vertex)) {
                vertex = graphVertices[random.nextInt(graph.vertexSet().size())];
            }

            verticesSet.add(vertex);
            result.add(vertex);
        }

        return result;
    }

    protected Graph<Integer, DefaultWeightedEdge> getSimpleGraph() {
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

        return graph;
    }

    protected Graph<Integer, DefaultWeightedEdge> getMultigraph() {
        Graph<Integer, DefaultWeightedEdge> graph = new DirectedWeightedMultigraph<>(DefaultWeightedEdge.class);

        Graphs.addEdgeWithVertices(graph, 1, 2, 1);
        Graphs.addEdgeWithVertices(graph, 1, 2, 2);
        Graphs.addEdgeWithVertices(graph, 2, 1, 3);
        Graphs.addEdgeWithVertices(graph, 2, 1, 4);

        Graphs.addEdgeWithVertices(graph, 2, 3, 8);
        Graphs.addEdgeWithVertices(graph, 2, 3, 7);
        Graphs.addEdgeWithVertices(graph, 3, 2, 6);
        Graphs.addEdgeWithVertices(graph, 3, 2, 5);

        Graphs.addEdgeWithVertices(graph, 3, 4, 9);
        Graphs.addEdgeWithVertices(graph, 3, 4, 10);
        Graphs.addEdgeWithVertices(graph, 4, 3, 11);
        Graphs.addEdgeWithVertices(graph, 4, 3, 12);

        Graphs.addEdgeWithVertices(graph, 4, 5, 16);
        Graphs.addEdgeWithVertices(graph, 4, 5, 15);
        Graphs.addEdgeWithVertices(graph, 5, 4, 14);
        Graphs.addEdgeWithVertices(graph, 5, 4, 13);

        Graphs.addEdgeWithVertices(graph, 5, 6, 17);
        Graphs.addEdgeWithVertices(graph, 5, 6, 18);
        Graphs.addEdgeWithVertices(graph, 6, 5, 19);
        Graphs.addEdgeWithVertices(graph, 6, 5, 20);

        Graphs.addEdgeWithVertices(graph, 6, 1, 24);
        Graphs.addEdgeWithVertices(graph, 6, 1, 23);
        Graphs.addEdgeWithVertices(graph, 1, 6, 22);
        Graphs.addEdgeWithVertices(graph, 1, 6, 21);

        return graph;
    }

}
