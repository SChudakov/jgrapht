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
import org.jgrapht.Graphs;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.MaskSubgraph;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.jheaps.AddressableHeap;
import org.jheaps.tree.PairingHeap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Implementation of the <a href="https://en.wikipedia.org/wiki/Contraction_hierarchies">
 * contraction hierarchy route planning precomputation technique.
 *
 * <p>
 * The algorithm for computing the hierarchy is originally described the article: Robert Geisberger,
 * Peter Sanders, Dominik Schultes, and Daniel Delling. 2008. Contraction hierarchies: faster and simpler
 * hierarchical routing in  road networks. In Proceedings of the 7th international conference on Experimental
 * algorithms (WEA'08), Catherine C. McGeoch (Ed.). Springer-Verlag, Berlin, Heidelberg, 319-333.
 *
 * <p>
 * The vertices are first ordered by "importance". A hierarchy is then generated by iteratively contracting
 * the least important vertex. Every vertex gets its vertexId which is its position in the hierarchical ordering.
 * A node v is contracted by removing it from the graph in such a way that shortest paths in the remaining
 * graph are preserved. This property is achieved by replacing paths of the form $(u, v, w)$ by a shortcut edge
 * $(u, w)$. Note that the shortcut $(u, w)$ is only required if $(u, v, w)$ is the only shortest path from $u$
 * to $w$.
 *
 * <p>
 * the graph remains very sparse throughout the contraction pro-
 * cess using rather simple heuristics for ordering the nodes.
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 * @author Semen Chudakov
 * @since July 2019
 */
public class ContractionHierarchyAlgorithm<V, E> {
    private Graph<V, E> graph;

    private Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph;
    private Map<V, ContractionVertex<V>> contractionMapping;
    private Graph<ContractionVertex<V>, ContractionEdge<E>> maskedContractionGraph;

    private Object[] verticesArray;
    private VertexData[] dataArray;

    private AtomicInteger contractionLevelCounter;
    private Function<Integer, Integer> hashFunction;

    private Supplier<AddressableHeap<Double, ContractionVertex<V>>> shortcutsSearchHeapSupplier;

    private ExecutorService executor;
    private ExecutorCompletionService<Void> completionService;
    private int parallelism;

    private List<IndependentSet> independentSetWorkers;
    private List<VerticesContractor> contractorWorkers;


    public ContractionHierarchyAlgorithm(Graph<V, E> graph) {
        this(graph, Runtime.getRuntime().availableProcessors());
    }

    public ContractionHierarchyAlgorithm(Graph<V, E> graph, int parallelism) {
        this(graph, parallelism, Random::new, PairingHeap::new);
    }

    public ContractionHierarchyAlgorithm(Graph<V, E> graph, Supplier<Random> randomSupplier) {
        this(graph, Runtime.getRuntime().availableProcessors(), randomSupplier, PairingHeap::new);
    }

    public ContractionHierarchyAlgorithm(Graph<V, E> graph, int parallelism,Supplier<Random> randomSupplier) {
        this(graph, parallelism, randomSupplier, PairingHeap::new);
    }

    public ContractionHierarchyAlgorithm(Graph<V, E> graph, int parallelism,
                                         Supplier<Random> randomSupplier,
                                         Supplier<AddressableHeap<Double, ContractionVertex<V>>> shortcutsSearchHeapSupplier) {
        this.graph = graph;
        this.contractionGraph = createContractionGraph();
        this.parallelism = parallelism;
        this.shortcutsSearchHeapSupplier = shortcutsSearchHeapSupplier;

        verticesArray = new Object[graph.vertexSet().size()];
        dataArray = new VertexData[graph.vertexSet().size()];

        contractionLevelCounter = new AtomicInteger();
        hashFunction = new HashFunction(randomSupplier.get());

        maskedContractionGraph = new MaskSubgraph<>(contractionGraph,
                v -> dataArray[v.vertexId] != null && dataArray[v.vertexId].isContracted, e -> false);
        contractionMapping = new HashMap<>();
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        completionService = new ExecutorCompletionService<>(executor);


        independentSetWorkers = new ArrayList<>(parallelism);
        contractorWorkers = new ArrayList<>(parallelism);
        for (int i = 0; i < parallelism; ++i) {
            independentSetWorkers.add(new IndependentSet(i));
            contractorWorkers.add(new VerticesContractor(i));
        }
    }


    public Pair<Graph<ContractionVertex<V>, ContractionEdge<E>>,
            Map<V, ContractionVertex<V>>> computeContractionHierarchy() {
        fillContractionGraphAndVerticesArray();
        computeInitialPriorities();

        contractVertices();

        markUpwardEdges();
        shutdownExecutor();
        return Pair.of(contractionGraph, contractionMapping);
    }

    private Graph<ContractionVertex<V>, ContractionEdge<E>> createContractionGraph() {
        GraphTypeBuilder<ContractionVertex<V>, ContractionEdge<E>> resultBuilder = GraphTypeBuilder.directed();

        return resultBuilder
                .weighted(true)
                .allowingMultipleEdges(false)
                .allowingSelfLoops(false)
                .buildGraph();
    }

    private void fillContractionGraphAndVerticesArray() {
        int vertexIndex = 0;
        for (V vertex : graph.vertexSet()) {
            ContractionVertex<V> contractionVertex = new ContractionVertex<>(vertex, vertexIndex);
            verticesArray[vertexIndex] = contractionVertex;
            ++vertexIndex;

            contractionGraph.addVertex(contractionVertex);
            contractionMapping.put(vertex, contractionVertex);
        }

        for (E e : graph.edgeSet()) {
            V source = graph.getEdgeSource(e);
            V target = graph.getEdgeTarget(e);
            if (!source.equals(target)) {

                ContractionVertex<V> contractionSource = contractionMapping.get(source);
                ContractionVertex<V> contractionTarget = contractionMapping.get(target);
                double eWeight = graph.getEdgeWeight(e);

                ContractionEdge<E> oldEdge = contractionGraph.getEdge(contractionSource, contractionTarget);
                if (oldEdge == null) {
                    ContractionEdge<E> forward = new ContractionEdge<>(e);
                    contractionGraph.addEdge(contractionSource, contractionTarget, forward);
                    contractionGraph.setEdgeWeight(forward, eWeight);

                    if (graph.getType().isUndirected()) {
                        ContractionEdge<E> backward = new ContractionEdge<>(e);
                        contractionGraph.addEdge(contractionTarget, contractionSource, backward);
                        contractionGraph.setEdgeWeight(backward, eWeight);
                    }
                } else {
                    double oldWeight = contractionGraph.getEdgeWeight(oldEdge);
                    if (eWeight < oldWeight) {
                        contractionGraph.setEdgeWeight(oldEdge, eWeight);
                    }
                    if (graph.getType().isUndirected()) {
                        contractionGraph.setEdgeWeight(
                                contractionGraph.getEdge(contractionTarget, contractionSource), eWeight);
                    }
                }
            }
        }
    }

    private void computeInitialPriorities() {
//        contractionGraph.vertexSet().forEach(vertex -> dataArray[vertex.vertexId] = getPriority(vertex));
        for (int i = 0; i < parallelism; ++i) {
            completionService.submit(new InitialPriorities(i), null);
        }
        takeTasks(parallelism);
    }

    private void contractVertices() {
        int notContractedVerticesEnd = graph.vertexSet().size();
        int independentSetStart;

        int cnt = 0;
        while (notContractedVerticesEnd != 0) {
            computeIndependentSet(notContractedVerticesEnd);

            independentSetStart = partitionIndependentSet(notContractedVerticesEnd);
            System.out.println(cnt++ + " " + independentSetStart + " " + notContractedVerticesEnd);
            contractIndependentSet(independentSetStart, notContractedVerticesEnd);

            notContractedVerticesEnd = independentSetStart;
        }
    }

    private void computeIndependentSet(int notContractedVerticesEnd) {
//        Arrays.asList(verticesArray).subList(0, notContractedVerticesEnd).forEach(o -> {
//            @SuppressWarnings("unchecked")
//            ContractionVertex<V> vertex = (ContractionVertex<V>) o;
//            dataArray[vertex.vertexId].isIndependent = vertexIsIndependent(vertex);
//        });

        for (IndependentSet worker : independentSetWorkers) {
            worker.notContractedVerticesEnd = notContractedVerticesEnd;
            completionService.submit(worker, null);
        }
        takeTasks(parallelism);
    }

    private boolean vertexIsIndependent(ContractionVertex<V> vertex) {
        double vertexPriority = dataArray[vertex.vertexId].priority;
        for (ContractionVertex<V> firstLevelNeighbour : Graphs.neighborSetOf(maskedContractionGraph, vertex)) {
            double firstLevelPriority = dataArray[firstLevelNeighbour.vertexId].priority;
            if (isGreater(vertexPriority, firstLevelPriority,
                    vertex.vertexId, firstLevelNeighbour.vertexId)) {
                return false;
            }

            for (ContractionVertex<V> secondLevelNeighbour :
                    Graphs.neighborSetOf(maskedContractionGraph, firstLevelNeighbour)) {
                if (!secondLevelNeighbour.equals(vertex)) {
                    double secondLevelPriority = dataArray[firstLevelNeighbour.vertexId].priority;
                    if (isGreater(vertexPriority, secondLevelPriority,
                            vertex.vertexId, secondLevelNeighbour.vertexId)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    private boolean isGreater(double priority1, double priority2, int vertexId1, int vertexId2) {
        return priority1 > priority2 || (Math.abs(priority1 - priority2) < Math.ulp(1.0)
                && tieBreaking(vertexId1, vertexId2));
    }

    private boolean tieBreaking(int vertexId1, int vertexId2) {
//        System.out.println("tie breaking");
        return vertexId1 > vertexId2;
//        return dataArray[vertexId1].random > dataArray[vertexId2].random;
//        int hash1 = hashFunction.apply(vertexId1);
//        int hash2 = hashFunction.apply(vertexId2);
//
//        if (hash1 != hash2) {
//            return hash1 < hash2;
//        }
//        return vertexId1 < vertexId2;
    }

    private int partitionIndependentSet(int notContractedVerticesEnd) {
        int left = 0;
        int right = notContractedVerticesEnd - 1;
        while (left <= right) {
            while (!dataArray[left].isIndependent) {
                ++left;
            }
            while (right >= 0 && dataArray[right].isIndependent) {
                --right;
            }
            if (left <= right) {
                // swap only vertices, because data is identified by vertex id
                swap(dataArray, left, right);
                swap(verticesArray, left, right);
                @SuppressWarnings("unchecked")
                ContractionVertex<V> leftVertex = (ContractionVertex<V>) verticesArray[left];
                @SuppressWarnings("unchecked")
                ContractionVertex<V> rightVertex = (ContractionVertex<V>) verticesArray[right];
                int tmpId = leftVertex.vertexId;
                leftVertex.vertexId = rightVertex.vertexId;
                rightVertex.vertexId = tmpId;
            }
        }
        return left;
    }

    private void swap(Object[] array, int i, int j) {
        Object tmp = array[i];
        array[i] = array[j];
        array[j] = tmp;
    }

    private void contractIndependentSet(int independentSetStart, int independentSetEnd) {
//        Arrays.asList(verticesArray).subList(independentSetStart, independentSetEnd).forEach(o -> {
//            @SuppressWarnings("unchecked")
//            ContractionVertex<V> vertex = (ContractionVertex<V>) o;
//            contractVertex(vertex, contractionLevelCounter.getAndIncrement());
//        });

        for (VerticesContractor worker : contractorWorkers) {
            worker.independentSetStart = independentSetStart;
            worker.independentSetEnd = independentSetEnd;
            completionService.submit(worker, null);
        }
        takeTasks(parallelism);
    }

    private void contractVertex(ContractionVertex<V> vertex, int contractionLevel) {
        // compute shortcuts
        List<Pair<ContractionEdge<E>, ContractionEdge<E>>> shortcuts = getShortcuts(vertex);

        // add shortcuts
        for (Pair<ContractionEdge<E>, ContractionEdge<E>> shortcut : shortcuts) {
            ContractionVertex<V> shortcutSource = contractionGraph.getEdgeSource(shortcut.getFirst());
            ContractionVertex<V> shortcutTarget = contractionGraph.getEdgeTarget(shortcut.getSecond());
            ContractionEdge<E> shortcutEdge = new ContractionEdge<>(shortcut);

            boolean added = contractionGraph.addEdge(shortcutSource, shortcutTarget, shortcutEdge);
            double weight = contractionGraph.getEdgeWeight(shortcut.getFirst())
                    + contractionGraph.getEdgeWeight(shortcut.getSecond());
            if (added) {
                contractionGraph.setEdgeWeight(shortcutEdge, weight);
            } else {
                contractionGraph.setEdgeWeight(contractionGraph.getEdge(shortcutSource, shortcutTarget), weight);
            }
        }

        Set<ContractionVertex<V>> neighbours = Graphs.neighborSetOf(maskedContractionGraph, vertex);

        // update vertex data
        vertex.contractionLevel = contractionLevel;
        VertexData data = dataArray[vertex.vertexId];
        data.isContracted = true;

        updateVertexDataNeighboursPriorities(data, neighbours);
    }

    private void updateVertexDataNeighboursPriorities(VertexData vertexData,
                                                      Set<ContractionVertex<V>> neighbours) {
        for (ContractionVertex<V> neighbour : neighbours) {
            VertexData neighbourData = dataArray[neighbour.vertexId];
            vertexData.depth = Math.max(vertexData.depth + 1, neighbourData.depth);
            updatePriority(neighbour, neighbourData);
        }
    }


    private VertexData getPriority(ContractionVertex<V> vertex) {
        VertexData result = new VertexData();
        updatePriority(vertex, result);
        return result;
    }

    private void updatePriority(ContractionVertex<V> vertex, VertexData data) {
        VertexStatistics statistics = getStatistics(vertex);
        if (statistics.removedContractionEdges * statistics.removeOriginalEdges == 0) {
            data.priority = data.depth;
        } else {
            data.priority = 4.0 * statistics.addedContractionEdges / statistics.removedContractionEdges +
                    2.0 * statistics.addedOriginalEdges / statistics.removeOriginalEdges +
                    1.0 * data.depth;
        }

    }

    private VertexStatistics getStatistics(ContractionVertex<V> vertex) {
        ToShortcutsStatisticsConsumer consumer = new ToShortcutsStatisticsConsumer();
        iterateShortcuts(vertex, consumer);
        addRemovedEdgesStatistics(vertex, consumer.statistics);
        return consumer.statistics;
    }

    private void addRemovedEdgesStatistics(ContractionVertex<V> vertex, VertexStatistics statistics) {
        maskedContractionGraph.edgesOf(vertex).forEach(e -> {
            ++statistics.removedContractionEdges;
            statistics.removeOriginalEdges += statistics.removeOriginalEdges;
        });
    }


    private List<Pair<ContractionEdge<E>, ContractionEdge<E>>> getShortcuts(ContractionVertex<V> vertex) {
        ToListConsumer consumer = new ToListConsumer();
        iterateShortcuts(vertex, consumer);
        return consumer.shortcuts;
    }


    private void iterateShortcuts(ContractionVertex<V> vertex,
                                  BiConsumer<ContractionEdge<E>, ContractionEdge<E>> shortcutConsumer) {
        Set<ContractionVertex<V>> successors = new HashSet<>();

        double maxOutgoingEdgeWeight = Double.MIN_VALUE;
        for (ContractionEdge<E> outEdge : maskedContractionGraph.outgoingEdgesOf(vertex)) {
            successors.add(contractionGraph.getEdgeTarget(outEdge));
            maxOutgoingEdgeWeight = Math.max(maxOutgoingEdgeWeight, contractionGraph.getEdgeWeight(outEdge));
        }


        for (ContractionEdge<E> inEdge : maskedContractionGraph.incomingEdgesOf(vertex)) {
            ContractionVertex<V> predecessor = contractionGraph.getEdgeSource(inEdge);

            boolean containedPredecessor = successors.remove(predecessor); // might contain the predecessor vertex itself

            Map<ContractionVertex<V>, AddressableHeap.Handle<Double, ContractionVertex<V>>> distances =
                    iterateToSuccessors(maskedContractionGraph,
                            predecessor,
                            successors,
                            vertex,
                            contractionGraph.getEdgeWeight(inEdge) + maxOutgoingEdgeWeight);


            for (ContractionVertex<V> successor : successors) {
                ContractionEdge<E> outEdge = contractionGraph.getEdge(vertex, successor);
                double pathWeight = contractionGraph.getEdgeWeight(inEdge) + contractionGraph.getEdgeWeight(outEdge);

                if (!distances.containsKey(successor) ||
                        distances.get(successor).getKey() > pathWeight) {
                    shortcutConsumer.accept(inEdge, outEdge);
                    if (graph.getType().isUndirected()) {
                        shortcutConsumer.accept(
                                contractionGraph.getEdge(successor, vertex),
                                contractionGraph.getEdge(vertex, predecessor)
                        );
                    }
                }
            }

            if (containedPredecessor && graph.getType().isDirected()) {
                successors.add(predecessor);
            }
        }
    }


    private Map<ContractionVertex<V>, AddressableHeap.Handle<Double, ContractionVertex<V>>>
    iterateToSuccessors(Graph<ContractionVertex<V>, ContractionEdge<E>> graph,
                        ContractionVertex<V> source,
                        Set<ContractionVertex<V>> targets,
                        ContractionVertex<V> forbiddenVertex,
                        double radius) {
        AddressableHeap<Double, ContractionVertex<V>> heap = shortcutsSearchHeapSupplier.get();
        Map<ContractionVertex<V>, AddressableHeap.Handle<Double, ContractionVertex<V>>> distanceMap = new HashMap<>();
        updateDistance(source, 0.0, heap, distanceMap);

        int numOfSuccessors = targets.size();
        int passedSuccessors = 0;

        while (!heap.isEmpty()) {
            AddressableHeap.Handle<Double, ContractionVertex<V>> min = heap.deleteMin();
            ContractionVertex<V> vertex = min.getValue();
            double distance = min.getKey();

            if (distance > radius) {
                break;
            }

            if (targets.contains(vertex)) {
                ++passedSuccessors;
                if (passedSuccessors == numOfSuccessors) {
                    break;
                }
            }

            relaxNode(graph, heap, distanceMap, vertex, distance, forbiddenVertex);
        }
        return distanceMap;
    }

    private void relaxNode(Graph<ContractionVertex<V>, ContractionEdge<E>> graph,
                           AddressableHeap<Double, ContractionVertex<V>> heap,
                           Map<ContractionVertex<V>, AddressableHeap.Handle<Double, ContractionVertex<V>>> map,
                           ContractionVertex<V> vertex,
                           double vertexDistance,
                           ContractionVertex<V> forbiddenVertex) {

        for (ContractionEdge<E> edge : graph.outgoingEdgesOf(vertex)) {
            ContractionVertex<V> successor = graph.getEdgeTarget(edge);

            if (successor.equals(forbiddenVertex)) {
                continue;
            }

            double updatedDistance = vertexDistance + graph.getEdgeWeight(edge);

            updateDistance(successor, updatedDistance, heap, map);
        }
    }

    private void updateDistance(ContractionVertex<V> v, double distance,
                                AddressableHeap<Double, ContractionVertex<V>> heap,
                                Map<ContractionVertex<V>, AddressableHeap.Handle<Double, ContractionVertex<V>>> map) {
        AddressableHeap.Handle<Double, ContractionVertex<V>> node = map.get(v);
        if (node == null) {
            node = heap.insert(distance, v);
            map.put(v, node);
        } else if (distance < node.getKey()) {
            node.decreaseKey(distance);
        }
    }


    private void markUpwardEdges() {
        for (int i = 0; i < parallelism; ++i) {
            completionService.submit(new UpwardEdgesMarker(i), null);
        }
        takeTasks(parallelism);
    }

    private void takeTasks(int numOfTasks) {
        for (int i = 0; i < numOfTasks; ++i) {
            try {
                completionService.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void shutdownExecutor() {
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private int workerSegmentStart(int start, int end, int workerId) {
        return start + ((end - start) * workerId) / parallelism;
    }

    private int workerSegmentEnd(int start, int end, int workerId) {
        return start + ((end - start) * (workerId + 1)) / parallelism;
    }


    public static class ContractionVertex<V1> {
        int vertexId;
        V1 vertex;
        int contractionLevel;

        ContractionVertex(V1 vertex, int vertexId) {
            this.vertexId = vertexId;
            this.vertex = vertex;
        }
    }

    public static class ContractionEdge<E1> {
        E1 edge;
        Pair<ContractionEdge<E1>, ContractionEdge<E1>> skippedEdges;
        boolean isUpward;
        int originalEdges;

        ContractionEdge(E1 edge) {
            this.edge = edge;
            this.originalEdges = 1;
        }

        ContractionEdge(Pair<ContractionEdge<E1>, ContractionEdge<E1>> skippedEdges) {
            this.skippedEdges = skippedEdges;
            this.originalEdges = skippedEdges.getFirst().originalEdges + skippedEdges.getSecond().originalEdges;
        }
    }


    private class ToListConsumer implements BiConsumer<ContractionEdge<E>, ContractionEdge<E>> {
        List<Pair<ContractionEdge<E>, ContractionEdge<E>>> shortcuts;

        ToListConsumer() {
            shortcuts = new ArrayList<>();
        }

        @Override
        public void accept(ContractionEdge<E> e1, ContractionEdge<E> e2) {
            shortcuts.add(Pair.of(e1, e2));
        }
    }

    private class ToShortcutsStatisticsConsumer implements BiConsumer<ContractionEdge<E>, ContractionEdge<E>> {
        VertexStatistics statistics;

        ToShortcutsStatisticsConsumer() {
            this.statistics = new VertexStatistics();
        }

        @Override
        public void accept(ContractionEdge<E> e1, ContractionEdge<E> e2) {
            ++statistics.addedContractionEdges;
            statistics.addedOriginalEdges += e1.originalEdges + e2.originalEdges;
        }
    }


    private class InitialPriorities implements Runnable {
        private int workerId;

        InitialPriorities(int workerId) {
            this.workerId = workerId;
        }

        @Override
        public void run() {
            int numOfVertices = graph.vertexSet().size();
            int start = workerSegmentStart(0, numOfVertices, workerId);
            int end = workerSegmentEnd(0, numOfVertices, workerId);

            for (int vertexIndex = start; vertexIndex < end; ++vertexIndex) {
                @SuppressWarnings("unchecked")
                ContractionVertex<V> vertex = (ContractionVertex<V>) verticesArray[vertexIndex];
                dataArray[vertex.vertexId] = getPriority(vertex);
            }
        }
    }

    private class IndependentSet implements Runnable {
        int workerId;
        int notContractedVerticesEnd;

        IndependentSet(int workerId) {
            this.workerId = workerId;
        }

        @Override
        public void run() {
            int start = workerSegmentStart(0, notContractedVerticesEnd, workerId);
            int end = workerSegmentEnd(0, notContractedVerticesEnd, workerId);
            for (int vertexIndex = start; vertexIndex < end; ++vertexIndex) {
                @SuppressWarnings("unchecked")
                ContractionVertex<V> vertex = (ContractionVertex<V>) verticesArray[vertexIndex];
                dataArray[vertex.vertexId].isIndependent = vertexIsIndependent(vertex);
            }
        }
    }

    private class VerticesContractor implements Runnable {
        int workerId;
        int independentSetStart;
        int independentSetEnd;

        VerticesContractor(int workerId) {
            this.workerId = workerId;
        }

        @Override
        public void run() {
            int start = workerSegmentStart(independentSetStart, independentSetEnd, workerId);
            int end = workerSegmentEnd(independentSetStart, independentSetEnd, workerId);
            for (int vertexIndex = start; vertexIndex < end; ++vertexIndex) {
                @SuppressWarnings("unchecked")
                ContractionVertex<V> vertex = (ContractionVertex<V>) verticesArray[vertexIndex];
                contractVertex(vertex, contractionLevelCounter.getAndIncrement());
            }
        }
    }

    private class UpwardEdgesMarker implements Runnable {
        private int workerId;

        UpwardEdgesMarker(int workerId) {
            this.workerId = workerId;
        }

        @Override
        public void run() {
            int numOfVertices = graph.vertexSet().size();
            int start = workerSegmentStart(0, numOfVertices, workerId);
            int end = workerSegmentEnd(0, numOfVertices, workerId);
            for (int vertexIndex = start; vertexIndex < end; ++vertexIndex) {
                @SuppressWarnings("unchecked")
                ContractionVertex<V> vertex = (ContractionVertex<V>) verticesArray[vertexIndex];
                contractionGraph.outgoingEdgesOf(vertex).forEach(
                        e -> e.isUpward = contractionGraph.getEdgeSource(e).contractionLevel <
                                contractionGraph.getEdgeTarget(e).contractionLevel
                );
            }
        }
    }


    private static class VertexData {
        int depth;
        double priority;
        boolean isContracted;
        boolean isIndependent;
        int random;

        public VertexData() {
            random = ThreadLocalRandom.current().nextInt();
        }
    }

    private static class VertexStatistics {
        int addedContractionEdges;
        int removedContractionEdges;
        int addedOriginalEdges;
        int removeOriginalEdges;
    }


    private static class HashFunction implements Function<Integer, Integer> {
        Random random;

        private short[] firstLookup;
        private short[] secondLookup;

        HashFunction(Random random) {
            this.random = random;
            int size = 1 << 16;
            firstLookup = new short[size];
            secondLookup = new short[size];
            for (int i = 0; i < size; ++i) {
                firstLookup[i] = (short) i;
                secondLookup[i] = (short) i;
            }
            randomShuffle(firstLookup);
            randomShuffle(secondLookup);
        }

        private void randomShuffle(short[] array) {
            for (int i = array.length - 1; i >= 0; i--) {
                int position = random.nextInt(i + 1);

                short tmp = array[position];
                array[position] = array[i];
                array[i] = tmp;
            }
        }

        @Override
        public Integer apply(Integer integer) {
            int firstPart = (integer & 0xffff);
            int secondPart = (integer >> 16);


            return firstLookup[firstPart] ^ secondLookup[secondPart];
        }
    }
}
