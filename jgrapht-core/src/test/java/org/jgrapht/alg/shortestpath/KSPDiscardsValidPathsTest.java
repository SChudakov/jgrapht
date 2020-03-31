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

    private int[][] simpleGraph2 = new int[][]{{1, 2, 10}, {2, 3, 10}, {1, 4, 1}, {4, 5, 1}, {5, 2, 1}};

    @Test
    public void testSimpleGraph1() {
        Graph<Integer, DefaultWeightedEdge> graph = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        readGraph(graph, simpleGraph1);
        int source = 12;
        int target = 6;
        int k = Integer.MAX_VALUE;
        KShortestPathAlgorithm<Integer, DefaultWeightedEdge> algorithm = new YenKShortestPath<>(graph);
        List<GraphPath<Integer, DefaultWeightedEdge>> paths = algorithm.getPaths(source, target, k);

        for (int i = 0; i < paths.size(); ++i) {
            GraphPath<Integer, DefaultWeightedEdge> path = paths.get(i);

            List<Integer> vertices = path.getVertexList();
            double weight = path.getWeight();
            System.out.println(i + ")\t" + weight + "\t" + vertices);
        }
    }


    @Test
    public void testSimpleGraph2() {
        Graph<Integer, DefaultWeightedEdge> graph = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        readGraph(graph, simpleGraph1);

        int source = 12;
        int target = 6;
        int k = 100;

        List<Integer> expectedVertices = Arrays.asList(12, 0, 7, 10, 13, 2, 1, 8, 9, 11, 6);
//        List<DefaultWeightedEdge> expectedEdges = new ArrayList<>();
//        double expectedWeight = 0.0;
//        for (int i = 0; i < expectedVertices.size() - 1; ++i) {
//            DefaultWeightedEdge edge = graph.getEdge(expectedVertices.get(i), expectedVertices.get(i + 1));
//            expectedEdges.add(edge);
//            expectedWeight += graph.getEdgeWeight(edge);
//        }
//        GraphWalk<Integer, DefaultWeightedEdge> walk = new GraphWalk<>(graph, 12, 6, expectedEdges, expectedWeight);
//        walk.verify();

//        List<Integer> verticesSublist = Arrays.asList(12, 0, 7, 10, 13, 2, 1);
//        PathValidator<Integer, DefaultWeightedEdge> validator = (path, edge) -> {
//            if (graph.getEdgeSource(edge).equals(1) && graph.getEdgeTarget(edge).equals(8)) {
//                if (path.getVertexList().equals(verticesSublist)) {
//                    System.out.println("needed path");
//                    System.out.println(edge);
//                }
//            }
//            return true;
//        };

        KShortestPathAlgorithm<Integer, DefaultWeightedEdge> alg = new KShortestSimplePaths<>(graph/*, validator*/);
        List<GraphPath<Integer, DefaultWeightedEdge>> paths = alg.getPaths(source, target, k);

        for (int i = 0; i < k; ++i) {
            System.out.println(i + ")\t" + paths.get(i).getWeight() + "\t" + paths.get(i).getVertexList());
        }

        assertEquals(29.0, paths.get(88).getWeight(), 1e-9);
        assertEquals(expectedVertices, paths.get(88).getVertexList());
    }

    @Test
    public void helper() {
        Graph<Integer, DefaultWeightedEdge> graph = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        readGraph(graph, simpleGraph1);

        int source = 12;
        int target = 9;
        int k = Integer.MAX_VALUE;

        Set<GraphPath<Integer, DefaultWeightedEdge>> uniquePaths = new HashSet<>();

        List<GraphPath<Integer, DefaultWeightedEdge>> paths = new YenKShortestPath<>(graph).getPaths(source, target, k);
        int pathIndex = 0;
        for (int i = 0; i < paths.size(); ++i) {
            GraphPath<Integer, DefaultWeightedEdge> path = paths.get(i);
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
