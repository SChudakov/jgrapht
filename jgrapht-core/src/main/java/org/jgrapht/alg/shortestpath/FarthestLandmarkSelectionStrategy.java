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
import org.jgrapht.alg.util.Pair;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * ashdkjahk
 *
 * @param <V> asd asd
 * @param <E> sdjsf
 */
public class FarthestLandmarkSelectionStrategy<V, E> {
    /**
     * aksjdhakjshdk
     *
     * @param graph          ajsdkjahfkjadhf
     * @param numOfLandmarks ajsdkjahfkjadhf
     * @param externalVertex ajsdkjahfkjadhf
     * @return asfjadkfjkafnkjanf
     */
    public Set<V> getLandmarks(Graph<V, E> graph, int numOfLandmarks, V externalVertex) {
        if (numOfLandmarks < 1) {
            throw new IllegalArgumentException("num of landmarks should be positive");
        }
        if (graph.vertexSet().size() < numOfLandmarks) {
            throw new IllegalArgumentException("not enough vertices in the graph");
        }
        V v = getRandomVertex(graph);
        Set<V> vSet = Collections.singleton(v);
        Set<V> landmarks = new HashSet<>(numOfLandmarks);
        landmarks.add(getFarthestVertex(graph, vSet, externalVertex));
        for (int i = 0; i < numOfLandmarks - 1; i++) {
            V landmark = getFarthestVertex(graph, landmarks, externalVertex);
            landmarks.add(landmark);
        }
        return landmarks;
    }

    private V getRandomVertex(Graph<V, E> graph) {
        @SuppressWarnings("unchecked")
        V[] vertices = (V[]) graph.vertexSet().toArray();
        return vertices[(int) (Math.random() * vertices.length)];
    }

    private V getFarthestVertex(Graph<V, E> graph, Set<V> vertices, V externalVertex) {
        graph.addVertex(externalVertex);
        for (V vertex : vertices) {
            graph.addEdge(externalVertex, vertex);
        }

        DijkstraClosestFirstIterator<V, E> iterator =
                new DijkstraClosestFirstIterator<>(graph, externalVertex);
        while (iterator.hasNext()) {
            iterator.next();
        }

        Map<V, Pair<Double, E>> predecessorMap = iterator.getDistanceAndPredecessorMap();
        Map.Entry<V, Pair<Double, E>> entry = predecessorMap.entrySet().stream()
                .max(Comparator.comparingDouble(o -> o.getValue().getFirst())).get();
        V result = entry.getKey();
        System.out.println(result + " " + entry.getValue().getFirst());

        for (V vertex : vertices) {
            graph.removeEdge(externalVertex, vertex);
        }
        graph.removeVertex(externalVertex);

        return result;
    }
}
