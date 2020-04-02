/*
 * (C) Copyright 2010-2020, by France Telecom and Contributors.
 *
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
import org.jgrapht.alg.interfaces.KShortestPathAlgorithm;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.GraphWalk;
import org.jgrapht.graph.MaskSubgraph;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.graph.WeightedMultigraph;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class KSPDiscardsValidPathsTest {
    // ~ Methods ----------------------------------------------------------------

    private int[][] simpleGraph1 = {
            {0, 2, 2}, {0, 7, 5}, {0, 11, 8}, {0, 12, 3},
            {1, 8, 6}, {1, 10, 4}, {1, 11, 0},
            {2, 1, 3}, {2, 7, 2}, {2, 8, 4}, {2, 9, 4},
            {3, 0, 0}, {3, 5, 4}, {3, 7, 8}, {3, 9, 0}, {3, 12, 7},
            {5, 3, 3}, {5, 9, 5}, {5, 12, 8}, {5, 13, 3},
            {6, 9, 5}, {6, 10, 4}, {6, 11, 1}, {6, 12, 7},
            {7, 1, 3}, {7, 3, 2}, {7, 10, 0},
            {8, 3, 9}, {8, 9, 3}, {8, 11, 0}, {8, 13, 7},
            {9, 7, 8}, {9, 10, 6}, {9, 11, 3},
            {10, 4, 2}, {10, 8, 1}, {10, 9, 8}, {10, 13, 1},
            {11, 1, 6}, {11, 2, 9}, {11, 5, 1}, {11, 6, 8}, {11, 10, 6}, {11, 13, 1},
            {12, 0, 0}, {12, 1, 5}, {12, 4, 0}, {12, 6, 9},
            {13, 1, 5}, {13, 2, 0}, {13, 3, 4}, {13, 12, 0}
    };

    private int[][] simpleGraph2 = {{0, 7, 5}, {0, 20, 0}, {0, 23, 3}, {0, 30, 3}, {0, 31, 6}, {0, 42, 6}, {1, 11, 5},
            {1, 14, 5}, {1, 21, 4}, {1, 25, 9}, {1, 29, 0}, {1, 31, 3}, {1, 33, 8}, {1, 40, 2}, {1, 49, 0}, {2, 13, 4},
            {2, 27, 2}, {2, 39, 5}, {2, 47, 1}, {3, 0, 0}, {3, 8, 8}, {3, 10, 5}, {3, 31, 7}, {3, 35, 9}, {3, 38, 8},
            {3, 40, 3}, {3, 45, 1}, {4, 8, 7}, {4, 38, 1}, {5, 6, 1}, {5, 7, 0}, {5, 13, 3}, {5, 23, 5}, {5, 35, 9},
            {5, 46, 5}, {5, 49, 8}, {6, 19, 5}, {6, 23, 7}, {6, 44, 0}, {6, 47, 5}, {7, 10, 8}, {7, 12, 9}, {7, 21, 9},
            {7, 27, 1}, {7, 38, 9}, {7, 41, 4}, {7, 48, 9}, {8, 2, 7}, {8, 4, 6}, {8, 15, 8}, {8, 22, 5}, {8, 31, 4},
            {8, 37, 0}, {8, 38, 3}, {9, 3, 3}, {9, 15, 9}, {9, 17, 7}, {9, 25, 3}, {9, 35, 3}, {10, 32, 9}, {10, 44, 3},
            {10, 47, 7}, {11, 8, 6}, {11, 14, 1}, {11, 26, 0}, {11, 30, 7}, {11, 31, 6}, {12, 0, 8}, {12, 4, 8},
            {12, 16, 3}, {12, 18, 7}, {12, 32, 9}, {13, 3, 4}, {13, 5, 2}, {13, 7, 9}, {13, 16, 4}, {13, 27, 2},
            {13, 29, 8}, {13, 48, 0}, {14, 7, 8}, {14, 20, 2}, {14, 24, 2}, {14, 31, 1}, {14, 32, 3}, {14, 36, 9},
            {14, 41, 9}, {14, 48, 2}, {15, 20, 7}, {15, 29, 1}, {15, 45, 0}, {15, 47, 1}, {16, 14, 8}, {16, 19, 0},
            {16, 23, 7}, {16, 37, 7}, {16, 41, 5}, {17, 19, 8}, {17, 29, 0}, {17, 31, 6}, {17, 42, 0}, {17, 48, 7},
            {18, 24, 6}, {19, 0, 3}, {19, 8, 7}, {19, 14, 3}, {19, 34, 6}, {19, 44, 0}, {20, 1, 2}, {20, 3, 1},
            {20, 6, 2}, {20, 47, 1}, {21, 1, 4}, {21, 12, 1}, {21, 13, 2}, {21, 15, 4}, {21, 19, 3}, {21, 34, 9},
            {21, 37, 7}, {21, 42, 9}, {21, 43, 4}, {21, 44, 4}, {22, 18, 2}, {22, 24, 3}, {22, 31, 3}, {22, 33, 3},
            {22, 36, 3}, {22, 47, 8}, {23, 31, 4}, {23, 42, 9}, {23, 47, 1}, {24, 8, 8}, {24, 21, 1}, {24, 22, 1},
            {24, 46, 5}, {25, 23, 0}, {25, 33, 3}, {26, 3, 8}, {26, 5, 2}, {26, 15, 1}, {27, 12, 9}, {27, 16, 7},
            {27, 20, 5}, {27, 23, 2}, {27, 28, 0}, {27, 33, 5}, {27, 40, 5}, {28, 11, 3}, {28, 14, 3}, {28, 24, 4},
            {28, 31, 4}, {28, 37, 9}, {28, 44, 7}, {28, 49, 3}, {29, 37, 1}, {29, 41, 8}, {29, 44, 8}, {30, 1, 4},
            {30, 9, 4}, {31, 47, 3}, {32, 22, 5}, {32, 24, 2}, {32, 33, 3}, {32, 34, 8}, {32, 43, 9}, {33, 14, 6},
            {33, 19, 2}, {33, 20, 4}, {33, 34, 5}, {33, 41, 9}, {34, 4, 3}, {34, 9, 7}, {34, 14, 5}, {34, 35, 3},
            {34, 37, 7}, {34, 40, 2}, {34, 44, 1}, {35, 2, 0}, {35, 4, 6}, {35, 22, 4}, {35, 27, 6}, {35, 43, 1},
            {36, 0, 4}, {36, 14, 6}, {36, 15, 9}, {36, 33, 5}, {36, 39, 8}, {36, 45, 3}, {37, 14, 8}, {37, 17, 7},
            {37, 24, 1}, {37, 31, 0}, {37, 36, 9}, {37, 47, 4}, {38, 3, 0}, {38, 8, 0}, {38, 15, 0}, {38, 31, 8},
            {39, 13, 8}, {39, 14, 6}, {39, 15, 2}, {39, 23, 2}, {40, 14, 0}, {40, 22, 8}, {40, 23, 7}, {40, 38, 9},
            {40, 42, 9}, {40, 44, 5}, {40, 46, 7}, {41, 0, 0}, {41, 5, 1}, {41, 21, 3}, {41, 30, 3}, {41, 49, 2},
            {42, 6, 3}, {42, 8, 7}, {42, 12, 8}, {42, 25, 3}, {42, 33, 8}, {42, 45, 2}, {43, 1, 9}, {43, 18, 0},
            {43, 22, 9}, {43, 23, 2}, {43, 35, 0}, {44, 25, 8}, {44, 30, 0}, {44, 40, 0}, {44, 47, 5}, {45, 3, 0},
            {45, 6, 3}, {45, 9, 4}, {45, 16, 9}, {45, 18, 2}, {45, 23, 3}, {45, 41, 6}, {46, 14, 1}, {46, 21, 2},
            {46, 22, 9}, {46, 29, 4}, {46, 36, 1}, {46, 38, 2}, {47, 10, 0}, {47, 12, 5}, {47, 16, 7}, {47, 25, 2},
            {47, 36, 2}, {47, 37, 2}, {48, 11, 2}, {48, 33, 6}, {49, 14, 7}, {49, 25, 4}, {49, 43, 0}, {49, 44, 7}};


    @Test
    public void testSimpleGraph1() {
        Graph<Integer, DefaultWeightedEdge> graph = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        readGraph(graph, simpleGraph1);

        int source = 6;
        int target = 4;
        int k = 100;

        KShortestPathAlgorithm<Integer, DefaultWeightedEdge> kssp = new KShortestSimplePaths<>(graph);
        List<GraphPath<Integer, DefaultWeightedEdge>> actualPaths = kssp.getPaths(source, target, k);
        KShortestPathAlgorithm<Integer, DefaultWeightedEdge> yen = new YenKShortestPath<>(graph);
        List<GraphPath<Integer, DefaultWeightedEdge>> expectedPaths = yen.getPaths(source, target, k);

        assertEquals(expectedPaths.size(), actualPaths.size());
        for (int i = 0; i < actualPaths.size(); ++i) {
            GraphPath<Integer, DefaultWeightedEdge> expected = expectedPaths.get(i);
            GraphPath<Integer, DefaultWeightedEdge> actual = actualPaths.get(i);

            System.out.println(i + ")");

            List<Integer> expectedVertices = expected.getVertexList();
            double expectedWeight = expected.getWeight();
            System.out.println("expected:\t" + expectedWeight + "\t" + expectedVertices);

            List<Integer> actualVertices = actual.getVertexList();
            double actualWeight = actual.getWeight();
            System.out.println("actual:\t\t" + actualWeight + "\t" + actualVertices);

            assertEquals(expected.getWeight(), actual.getWeight(), 1e-9);
        }
    }


    @Test
    public void testSimpleGraph2() {
        Graph<Integer, DefaultWeightedEdge> graph = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        readGraph(graph, simpleGraph1);

        int source = 6;
        int target = 4;
        int k = 100;

        List<Integer> expectedVertices = Arrays.asList(6, 10, 8, 11, 13, 2, 7, 3, 0, 12, 4);

        List<DefaultWeightedEdge> expectedEdges = new ArrayList<>();
        double expectedWeight = 0.0;
        for (int i = 0; i < expectedVertices.size() - 1; ++i) {
            DefaultWeightedEdge edge = graph.getEdge(expectedVertices.get(i), expectedVertices.get(i + 1));
            expectedEdges.add(edge);
            expectedWeight += graph.getEdgeWeight(edge);
        }
        GraphWalk<Integer, DefaultWeightedEdge> walk = new GraphWalk<>(graph, 6, 4, expectedEdges, expectedWeight);
        walk.verify();

        List<Integer> verticesSublist = Arrays.asList(6, 10, 8, 11, 13, 2, 7, 3, 0, 12);
        PathValidator<Integer, DefaultWeightedEdge> validator = (path, edge) -> {
            if (graph.getEdgeSource(edge).equals(12) && graph.getEdgeTarget(edge).equals(4)) {
                if (path.getVertexList().equals(verticesSublist)) {
                    System.out.println("needed path");
                    System.out.println(edge);
                }
            }
            return true;
        };

        KShortestPathAlgorithm<Integer, DefaultWeightedEdge> algorithm = new KShortestSimplePaths<>(graph, validator);
        List<GraphPath<Integer, DefaultWeightedEdge>> paths = algorithm.getPaths(source, target, k);

        for (int i = 0; i < k; ++i) {
            System.out.println(i + ")\t" + paths.get(i).getWeight() + "\t" + paths.get(i).getVertexList());
        }

        assertEquals(13.0, paths.get(53).getWeight(), 1e-9);
//        assertEquals(expectedVertices, paths.get(88).getVertexList());
    }

    @Test
    public void helper1() {
        Graph<Integer, DefaultWeightedEdge> graph = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        readGraph(graph, simpleGraph1);

        int source = 6;
        int target = 4;
        int k = 100;

        List<GraphPath<Integer, DefaultWeightedEdge>> paths = new YenKShortestPath<>(graph).getPaths(source, target, k);
        for (int i = 0; i < paths.size(); ++i) {
            GraphPath<Integer, DefaultWeightedEdge> path = paths.get(i);
            double weight = path.getWeight();
            List<Integer> vertices = path.getVertexList();
            System.out.println(i + ")\t" + weight + "\t" + vertices);
        }
    }

    @Test
    public void helper2() {
        Graph<Integer, DefaultWeightedEdge> graph = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        readGraph(graph, simpleGraph1);

        int source = 12;
        int target = 9;
        int k = 100;

        List<GraphPath<Integer, DefaultWeightedEdge>> paths = new YenKShortestPath<>(graph).getPaths(source, target, k);
        int pathIndex = 0;
        for (GraphPath<Integer, DefaultWeightedEdge> path : paths) {
            GraphPath<Integer, DefaultWeightedEdge> spurPath = spurPath(graph, path, 6);
            if (spurPath != null) {
                double weight = path.getWeight();
                double totalWeight = weight + spurPath.getWeight();
                List<Integer> vertices = path.getVertexList();
                List<Integer> spurVertices = spurPath.getVertexList();
                System.out.println(pathIndex++ + ")\t" + weight + "\t" + totalWeight + "\t" + vertices + " " + spurVertices);
            }
        }
    }

    @Test
    public void helper3() {
        Graph<Integer, DefaultWeightedEdge> graph = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        readGraph(graph, simpleGraph1);

        List<Integer> maskedVertices = Arrays.asList(6, 10, 8, 3);
        Graph<Integer, DefaultWeightedEdge> maskSubgraph = new MaskSubgraph<>(graph, maskedVertices::contains, e -> false);
        GraphPath<Integer, DefaultWeightedEdge> graphPath = DijkstraShortestPath.findPathBetween(maskSubgraph, 9, 4);
        System.out.println(graphPath.getWeight());
    }

    private GraphPath<Integer, DefaultWeightedEdge> spurPath(Graph<Integer, DefaultWeightedEdge> graph,
                                                             GraphPath<Integer, DefaultWeightedEdge> path,
                                                             int target) {

        Set<Integer> vertices = new HashSet<>(path.getVertexList());
        vertices.remove(path.getEndVertex());

        if (vertices.contains(target)) {
            return null;
        }

        MaskSubgraph<Integer, DefaultWeightedEdge> mask = new MaskSubgraph<>(graph, vertices::contains, e -> false);
        DijkstraShortestPath<Integer, DefaultWeightedEdge> shortestPath = new DijkstraShortestPath<>(mask);

        return shortestPath.getPath(path.getEndVertex(), target);
    }


    private void readGraph(Graph<Integer, DefaultWeightedEdge> graph, int[][] representation) {
        for (int[] ints : representation) {
            Graphs.addEdgeWithVertices(graph, ints[0], ints[1], ints[2]);
        }
    }

    /**
     * Example with a biconnected graph but not 3-connected. With a graph not 3-connected, the start
     * vertex and the end vertex can be disconnected by 2 paths.
     */
    @Test
    public void testNot3connectedGraph() {
        WeightedMultigraph<String, DefaultWeightedEdge> graph;
        KShortestSimplePaths<String, DefaultWeightedEdge> paths;

        graph = new WeightedMultigraph<>(DefaultWeightedEdge.class);
        graph.addVertex("S");
        graph.addVertex("T");
        graph.addVertex("A");
        graph.addVertex("B");
        graph.addVertex("C");
        graph.addVertex("D");
        graph.addVertex("E");
        graph.addVertex("F");
        graph.addVertex("G");
        graph.addVertex("H");
        graph.addVertex("I");
        graph.addVertex("J");
        graph.addVertex("K");
        graph.addVertex("L");

        this.addGraphEdge(graph, "S", "A", 1.0);
        this.addGraphEdge(graph, "A", "T", 1.0);
        this.addGraphEdge(graph, "A", "B", 1.0);
        this.addGraphEdge(graph, "B", "T", 1.0);
        this.addGraphEdge(graph, "B", "C", 1.0);

        this.addGraphEdge(graph, "C", "D", 1.0);
        this.addGraphEdge(graph, "C", "E", 1.0);
        this.addGraphEdge(graph, "C", "F", 1.0);
        this.addGraphEdge(graph, "D", "G", 1.0);
        this.addGraphEdge(graph, "E", "G", 1.0);
        this.addGraphEdge(graph, "F", "G", 1.0);

        this.addGraphEdge(graph, "G", "H", 1.0);
        this.addGraphEdge(graph, "H", "I", 1.0);
        this.addGraphEdge(graph, "I", "J", 1.0);
        this.addGraphEdge(graph, "J", "K", 1.0);
        this.addGraphEdge(graph, "K", "L", 1.0);
        this.addGraphEdge(graph, "L", "S", 1.0);

        paths = new KShortestSimplePaths<>(graph);

        assertTrue(paths.getPaths("S", "T", 3).size() == 3);
    }

    /**
     * JUnit test for the bug reported by Bruno Maoili. Example with a connected graph but not
     * 2-connected. With a graph not 2-connected, the start vertex and the end vertex can be
     * disconnected by 1 path.
     */
    @Test
    public void testBrunoMaoili() {
        WeightedMultigraph<String, DefaultWeightedEdge> graph;
        KShortestSimplePaths<String, DefaultWeightedEdge> paths;

        graph = new WeightedMultigraph<>(DefaultWeightedEdge.class);
        graph.addVertex("A");
        graph.addVertex("B");
        graph.addVertex("C");
        graph.addVertex("D");
        graph.addVertex("E");

        this.addGraphEdge(graph, "A", "B", 1.0);
        this.addGraphEdge(graph, "A", "C", 2.0);
        this.addGraphEdge(graph, "B", "D", 1.0);
        this.addGraphEdge(graph, "B", "D", 1.0);
        this.addGraphEdge(graph, "B", "D", 1.0);
        this.addGraphEdge(graph, "B", "E", 1.0);
        this.addGraphEdge(graph, "C", "D", 1.0);

        paths = new KShortestSimplePaths<>(graph);
        assertTrue(paths.getPaths("A", "E", 2).size() == 2);

        paths = new KShortestSimplePaths<>(graph);
        assertTrue(paths.getPaths("A", "E", 3).size() == 3);

        paths = new KShortestSimplePaths<>(graph);
        assertTrue(paths.getPaths("A", "E", 4).size() == 4);
    }

    private void addGraphEdge(
            WeightedMultigraph<String, DefaultWeightedEdge> graph, String sourceVertex,
            String targetVertex, double weight) {
        DefaultWeightedEdge edge = new DefaultWeightedEdge();

        graph.addEdge(sourceVertex, targetVertex, edge);
        graph.setEdgeWeight(edge, weight);
    }
}
