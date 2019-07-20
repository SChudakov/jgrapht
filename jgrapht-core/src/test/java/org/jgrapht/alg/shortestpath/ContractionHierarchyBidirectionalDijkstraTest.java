/*
 * (C) Copyright 2019-2019, by Semen Chudakov and Contributors.
d *
 * JGraphT : a free Java graph-theory library
 *
 * See the CONTRIBUTORS.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the
 * GNU Lesser General Public License v2.1 or later
 * which is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1-standalone.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR LGPL-2.1-or-later
 */
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
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.util.SupplierUtil;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static org.jgrapht.alg.shortestpath.ContractionHierarchyAlgorithm.ContractionEdge;
import static org.jgrapht.alg.shortestpath.ContractionHierarchyAlgorithm.ContractionVertex;
import static org.jgrapht.alg.shortestpath.ShortestPathPerformance.Node;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ContractionHierarchyBidirectionalDijkstraTest {
    private static final long SEED = 19L;

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
    public void testOnRandomGraphs() {
        int numOfVertices = 100;
        int vertexDegree = 5;
        int numOfIterations = 10000;
        int source = 0;
        for (int i = 0; i < numOfIterations; i++) {
            System.out.println(i);
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


    @Test
    public void testRingGraph() {
        int size = 10000;
        Graph<Integer, DefaultWeightedEdge> graph = new SimpleWeightedGraph<>(DefaultWeightedEdge.class);
        fillLineGraph(graph, size);
        test(graph, 0);
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
    }

    private void test(Graph<Integer, DefaultWeightedEdge> graph, Integer source) {
        ShortestPathAlgorithm.SingleSourcePaths<Integer, DefaultWeightedEdge> dijkstraShortestPaths =
                new DijkstraShortestPath<>(graph).getPaths(source);

        Pair<Graph<ContractionVertex<Integer>, ContractionHierarchyAlgorithm.ContractionEdge<DefaultWeightedEdge>>,
                Map<Integer, ContractionVertex<Integer>>> p
                = new ContractionHierarchyAlgorithm<>(graph, 1, () -> new Random(SEED)).computeContractionHierarchy();

        ShortestPathAlgorithm.SingleSourcePaths<Integer, DefaultWeightedEdge> contractionDijkstra =
                new ContractionHierarchyBidirectionalDijkstra<>(graph, p.getFirst(), p.getSecond()).getPaths(source);

        assertEqualPaths(dijkstraShortestPaths, contractionDijkstra, graph.vertexSet());
    }

    private Graph<Integer, DefaultWeightedEdge> generateRandomGraph(
            int numOfVertices, int numOfEdges) {
        DirectedWeightedPseudograph<Integer, DefaultWeightedEdge> graph =
                new DirectedWeightedPseudograph<>(DefaultWeightedEdge.class);
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
        for (int i = 0; i < vertices.length - 1; ++i) {
            graph.addEdge((Integer) vertices[i], (Integer) vertices[i + 1]);
            graph.addEdge((Integer) vertices[i + 1], (Integer) vertices[i]);
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
            Set<Integer> vertexSet) {
        for (Integer sink : vertexSet) {
            GraphPath<Integer, DefaultWeightedEdge> expectedPath = expected.getPath(sink);
            GraphPath<Integer, DefaultWeightedEdge> actualPath = actual.getPath(sink);
            if (expectedPath == null || actualPath == null) {
                assertEquals(expectedPath, actualPath);
            } else {
                assertEquals(
                        expected.getPath(sink).getWeight(), actual.getPath(sink).getWeight(), 1e-9);
            }
        }
    }

    @Test
    public void testRoadNetwork() {
        String path = "/home/semen/drive/osm/final/andorra.txt";

        Graph<Node, DefaultWeightedEdge> graph = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        OSMReader reader = new OSMReader();
        reader.readGraph(graph, path, ShortestPathPerformance::greatCircleDistance);
        System.out.println("graph read");

//        System.out.println(graph.containsEdge(new Node(1203775436L, 1.5557865, 42.6276914), new Node(5044381870L, 1.5562043, 42.6278807)));

        List<Pair<Node, Node>> queries = getQueries(graph);

        ContractionHierarchyAlgorithm<Node, DefaultWeightedEdge> algorithm
                = new ContractionHierarchyAlgorithm<>(graph, 12, () -> new Random(SEED));
        Pair<Graph<ContractionVertex<Node>, ContractionEdge<DefaultWeightedEdge>>, Map<Node, ContractionVertex<Node>>> p
                = algorithm.computeContractionHierarchy();
        ShortestPathAlgorithm<Node, DefaultWeightedEdge> dijkstraShortestPaths =
                new DijkstraShortestPath<>(graph);
        ContractionHierarchyBidirectionalDijkstra<Node, DefaultWeightedEdge> contractionDijkstra =
                new ContractionHierarchyBidirectionalDijkstra<>(graph, p.getFirst(), p.getSecond());

        for (Pair<Node, Node> query : queries) {
            System.out.println(query);
            Node source = query.getFirst();
            Node target = query.getSecond();

            test(dijkstraShortestPaths, contractionDijkstra, source, target);
        }
    }

    private void unpack(List<ContractionHierarchyAlgorithm.ContractionEdge<DefaultWeightedEdge>> edges,
                        Graph<ContractionHierarchyAlgorithm.ContractionVertex<Node>,
                                ContractionHierarchyAlgorithm.ContractionEdge<DefaultWeightedEdge>> graph) {
        System.out.println("\n\nContraction indices\n\n");
        for (ContractionHierarchyAlgorithm.ContractionEdge<DefaultWeightedEdge> e : edges) {
            System.out.println(graph.getEdgeSource(e).contractionLevel > graph.getEdgeTarget(e).contractionLevel);
        }
        for (ContractionHierarchyAlgorithm.ContractionEdge<DefaultWeightedEdge> e : edges) {
            unpack(e);
        }
    }

    private void unpack(ContractionHierarchyAlgorithm.ContractionEdge<DefaultWeightedEdge> edge) {
        if (edge.edge != null) {
            System.out.print(edge.edge + ", ");
        } else {
            unpack(edge.skippedEdges.getFirst());
            unpack(edge.skippedEdges.getSecond());
        }
    }


    private void test(ShortestPathAlgorithm<Node, DefaultWeightedEdge> expected,
                      ShortestPathAlgorithm<Node, DefaultWeightedEdge> actual,
                      Node source, Node target) {
        GraphPath<Node, DefaultWeightedEdge> expectedPath = expected.getPath(source, target);
        GraphPath<Node, DefaultWeightedEdge> actualPath = actual.getPath(source, target);

        if (actualPath == null) {
            assertNull(expectedPath);
        } else {
            assertEquals(expectedPath.getWeight(), actualPath.getWeight(), 1e-9);
        }
    }

    private List<Pair<Node, Node>> getQueries(
            Graph<Node, DefaultWeightedEdge> graph) {
        int numOfQueries = 100;
        Random random = new Random(SEED);

        List<Pair<Node, Node>> queries = new ArrayList<>();
        Node[] nodes = graph.vertexSet().toArray(new Node[0]);
        for (int i = 0; i < numOfQueries; i++) {
            int sourcePosition = random.nextInt(graph.vertexSet().size());
            int targetPosition = random.nextInt(graph.vertexSet().size());
            queries.add(Pair.of(nodes[sourcePosition], nodes[targetPosition]));
        }
        return queries;
    }
}