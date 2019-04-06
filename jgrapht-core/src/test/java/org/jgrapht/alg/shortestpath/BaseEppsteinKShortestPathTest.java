/*
 * (C) Copyright 2019-2019, by Semen Chudakov and Contributors.
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
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultWeightedEdge;

/**
 * Base test for {@link EppsteinKShortestPath} and
 * {@link EppsteinShortestPathIterator}.
 *
 * @author Semen Chudakov
 */
public class BaseEppsteinKShortestPathTest {
    final int[][] simpleGraph1 = {
            {1, 2, 2}, {2, 3, 20}, {3, 4, 14},
            {1, 5, 13}, {2, 6, 27}, {3, 7, 14}, {4, 8, 15},
            {5, 6, 9}, {6, 7, 10}, {7, 8, 25},
            {5, 9, 15}, {6, 10, 20}, {7, 11, 12}, {8, 12, 7},
            {9, 10, 18}, {10, 11, 8}, {11, 12, 11}
    };
    final int[][] simpleGraph2 = {{1, 2, 5}, {1, 3, 6}, {2, 3, 7}, {2, 4, 8}, {3, 4, 9}};
    final int[][] simpleGraph3 = {
            {0, 1, 6}, {2, 0, 9}, {4, 0, 4}, {0, 5, 6}, {0, 6, 5},
            {2, 1, 1}, {1, 4, 9}, {4, 1, 2}, {1, 5, 7}, {1, 6, 5},
            {2, 4, 1}, {2, 5, 0}, {3, 4, 4}, {4, 3, 4}, {4, 5, 6},
            {5, 4, 8}, {4, 6, 3}, {6, 5, 0}
    };

    final int[][] cyclicGraph1 = {{1, 2, 1}, {2, 1, 1}};

    final int[][] cyclicGraph2 = {
            {1, 2, 1}, {2, 3, 1}, {3, 4, 1}, {4, 1, 1},
            {1, 5, 2}, {5, 6, 2}, {6, 7, 2}, {7, 1, 2},
            {3, 6, 2}, {6, 3, 2}
    };

    final int[][] cyclicGraph3 = {
            {1, 2, 1}, {2, 3, 1}, {3, 4, 1},
            {3, 4, 1}, {4, 3, 1}, {4, 5, 1}, {5, 4, 1}
    };

    final int[][] restHeapGraph = {
            {1, 2, 2}, {1, 3, 3}, {1, 4, 4}, {1, 5, 5}, {1, 6, 6}, {1, 7, 7}, {1, 8, 8}, {1, 9, 9},
            {2, 10, 1}, {3, 10, 1}, {4, 10, 1}, {5, 10, 1}, {6, 10, 1}, {7, 10, 1}, {8, 10, 1}, {9, 10, 1}
    };
    final int[][] notShortestPathEdgesGraph = {
            {1, 2, 1},
            {1, 3, 3}, {1, 4, 4}, {1, 5, 5}, {1, 6, 6}, {1, 7, 7}, {1, 8, 8}, {1, 9, 9}
    };

    void readGraph(Graph<Integer, DefaultWeightedEdge> graph, int[][] representation) {
        for (int[] ints : representation) {
            Graphs.addEdgeWithVertices(graph, ints[0], ints[1], ints[2]);
        }
    }
}
