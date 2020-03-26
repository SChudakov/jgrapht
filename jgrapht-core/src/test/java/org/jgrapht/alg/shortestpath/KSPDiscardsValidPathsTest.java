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
import org.jgrapht.graph.*;
import org.junit.*;

import java.util.List;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

public class KSPDiscardsValidPathsTest
{
    // ~ Methods ----------------------------------------------------------------

    private int[][] pseudograph1 = {{0, 2, 2}, {3, 0, 0}, {0, 7, 5}, {12, 0, 8}, {19, 0, 3}, {0, 20, 0}, {0, 23, 3},
            {1, 7, 3}, {1, 8, 6}, {13, 1, 4}, {18, 1, 0}, {1, 19, 6}, {2, 14, 5}, {2, 17, 5}, {23, 2, 2}, {2, 24, 4},
            {24, 2, 4}, {3, 7, 9}, {3, 11, 0}, {12, 3, 4}, {3, 13, 3}, {3, 15, 8}, {3, 22, 2}, {5, 4, 9}, {4, 11, 0},
            {17, 4, 7}, {4, 22, 4}, {5, 17, 2}, {7, 6, 0}, {6, 11, 5}, {6, 19, 1}, {7, 9, 8}, {10, 7, 3}, {7, 11, 5},
            {14, 7, 4}, {21, 7, 1}, {11, 8, 8}, {8, 16, 7}, {8, 20, 9}, {8, 23, 8}, {23, 8, 0}, {9, 10, 3}, {9, 15, 1},
            {15, 9, 0}, {9, 23, 7}, {23, 9, 6}, {13, 10, 8}, {22, 11, 3}, {23, 11, 6}, {12, 14, 1}, {13, 15, 1},
            {13, 16, 0}, {13, 22, 3}, {22, 13, 2}, {14, 22, 5}, {16, 15, 2}, {16, 17, 9}, {23, 16, 1}, {17, 21, 5},
            {17, 24, 8}, {20, 22, 5}, {23, 20, 2}, {21, 23, 7}};

    @Test
    public void testPseudograph1() {
        Graph<Integer, DefaultWeightedEdge> graph = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
        readGraph(graph, pseudograph1);
        int source = 2;
        int target = 6;
        int k = 100;
        KShortestPathAlgorithm<Integer, DefaultWeightedEdge> algorithm = new KShortestSimplePaths<>(graph);

        List<GraphPath<Integer, DefaultWeightedEdge>> paths = algorithm.getPaths(source, target, k);
        assertEquals(paths.size(), 86);
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
    public void testNot3connectedGraph()
    {
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
    public void testBrunoMaoili()
    {
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
        String targetVertex, double weight)
    {
        DefaultWeightedEdge edge = new DefaultWeightedEdge();

        graph.addEdge(sourceVertex, targetVertex, edge);
        graph.setEdgeWeight(edge, weight);
    }
}
