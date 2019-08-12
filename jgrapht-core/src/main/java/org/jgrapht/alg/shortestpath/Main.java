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
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.KShortestPathAlgorithm;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.jgrapht.graph.Multigraph;

import java.util.List;

/**
 * asjdkaj
 */
public class Main {
    /**
     * ajsdja
     * @param args asdasd
     */
    public static void main(String[] args) {
        Graph<String, DefaultEdge> graph = new Multigraph<>(DefaultEdge.class);

        graph.addVertex("19");
        graph.addVertex("1e");
        graph.addVertex("1c");
        graph.addVertex("1b");
        graph.addVertex("1d");
        graph.addVertex("1f");
        graph.addVertex("16");
        graph.addVertex("17");
        graph.addVertex("12");
        graph.addVertex("14");
        graph.addVertex("18");
        graph.addVertex("15");
        graph.addVertex("21");

        graph.addEdge("19", "1e");
        graph.addEdge("19", "1c");
        graph.addEdge("19", "1b");
        graph.addEdge("19", "1d");
        graph.addEdge("19", "1f");
        graph.addEdge("19", "16");
        graph.addEdge("12", "17");
        graph.addEdge("12", "14");
        graph.addEdge("12", "15");
        graph.addEdge("12", "16");
        graph.addEdge("12", "16");
        graph.addEdge("12", "18");
        graph.addEdge("12", "21");
        graph.addEdge("21", "1f");

        KShortestPathAlgorithm<String, DefaultEdge> yen = new YenKShortestPath<>(graph);
        KShortestPathAlgorithm<String, DefaultEdge> simple = new KShortestSimplePaths<>(graph);

        List<GraphPath<String, DefaultEdge>> path_yen = yen.getPaths("1e", "18", 7);
        List<GraphPath<String, DefaultEdge>> path_simple = simple.getPaths("1e", "18", 7);

        for (GraphPath<String, DefaultEdge> p : path_yen) {
            System.out.println(p + " " + p.getLength() + " " + p.getWeight());
        }
        System.out.print("====================\n");
        for (GraphPath<String, DefaultEdge> p : path_simple) {
            System.out.println(p);
        }
    }
}
