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
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

public class ContractionHierarchyAlgorithm<V, E> {
    private Graph<V, E> graph;

    private Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph;
    private Map<V, ContractionVertex<V>> contractionMapping;
    private Graph<ContractionVertex<V>, ContractionEdge<E>> maskedContractionGraph;

    private AddressableHeap<VertexPriority, ContractionVertex<V>> contractionQueue;

    private Object[] verticesArray;
    private VertexPriority[] prioritiesArray;

    private Supplier<Random> randomSupplier;
    private Supplier<AddressableHeap<Double, ContractionVertex<V>>> dijkstraSearchHeapSupplier;

    private ExecutorService executor;
    private ExecutorCompletionService<Void> completionService;
    private int parallelism;


    public ContractionHierarchyAlgorithm(Graph<V, E> graph) {
        this(graph, Runtime.getRuntime().availableProcessors());
    }

    public ContractionHierarchyAlgorithm(Graph<V, E> graph, int parallelism) {
        this(graph, parallelism, Random::new, PairingHeap::new, PairingHeap::new);
    }

    public ContractionHierarchyAlgorithm(Graph<V, E> graph, Supplier<Random> randomSupplier) {
        this(graph, Runtime.getRuntime().availableProcessors(), randomSupplier, PairingHeap::new, PairingHeap::new);
    }

    public ContractionHierarchyAlgorithm(Graph<V, E> graph,
                                         int parallelism,
                                         Supplier<Random> randomSupplier,
                                         Supplier<AddressableHeap<VertexPriority, ContractionVertex<V>>> queueHeapSupplier,
                                         Supplier<AddressableHeap<Double, ContractionVertex<V>>> dijkstraSearchHeapSupplier) {
        this.graph = graph;
        this.contractionGraph = createContractionGraph();
        this.parallelism = parallelism;
        this.randomSupplier = randomSupplier;
        this.contractionQueue = queueHeapSupplier.get();
        this.dijkstraSearchHeapSupplier = dijkstraSearchHeapSupplier;

        verticesArray = new Object[graph.vertexSet().size()];
        prioritiesArray = new VertexPriority[graph.vertexSet().size()];
        maskedContractionGraph = new MaskSubgraph<>(contractionGraph,
                v -> v.contracted,
                e -> false/* contractionGraph.getEdgeSource(e).contracted || contractionGraph.getEdgeTarget(e).contracted*/);
        contractionMapping = new HashMap<>();
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        completionService = new ExecutorCompletionService<>(executor);
    }


    public Pair<Graph<ContractionVertex<V>, ContractionEdge<E>>,
            Map<V, ContractionVertex<V>>> computeContractionHierarchy() {
        fillContractionGraphAndVerticesQueue();
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

    private void fillContractionGraphAndVerticesQueue() {
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
        for (int i = 0; i < parallelism; ++i) {
            completionService.submit(new ContractionWorker(i), null);
        }

        takeTasks(parallelism);

        int n = graph.vertexSet().size();
        for (int vertexIndex = 0; vertexIndex < n; ++vertexIndex) {
            contractionQueue.insert(prioritiesArray[vertexIndex], (ContractionVertex<V>) verticesArray[vertexIndex]);
        }
    }

    private void contractVertices() {
        int contractionIndex = 0;

        while (!contractionQueue.isEmpty()) {
            AddressableHeap.Handle<VertexPriority, ContractionVertex<V>> handle = contractionQueue.deleteMin();

            ContractionVertex<V> vertex = handle.getValue();
            VertexPriority oldPriority = handle.getKey();

            Pair<VertexPriority, List<Pair<ContractionEdge<E>, ContractionEdge<E>>>> p =
                    getPriorityAndShortcuts(vertex, oldPriority.random);
            VertexPriority updatedPriority = p.getFirst();
            List<Pair<ContractionEdge<E>, ContractionEdge<E>>> shortcuts = p.getSecond();

            if (contractionQueue.isEmpty() ||
                    updatedPriority.compareToIgnoreRandom(contractionQueue.findMin().getKey()) <= 0) {
                contractVertex(vertex, contractionIndex, shortcuts);
                ++contractionIndex;
            } else {
                contractionQueue.insert(updatedPriority, vertex);
            }
        }
    }

    private void contractVertex(ContractionVertex<V> vertex, int contractionIndex,
                                List<Pair<ContractionEdge<E>, ContractionEdge<E>>> shortcuts) {
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

        Graphs.neighborSetOf(maskedContractionGraph, vertex).forEach(v -> ++v.neighborsContracted);

        // update vertex data
        vertex.contractionIndex = contractionIndex;
        vertex.contracted = true;
    }


    private Pair<VertexPriority, List<Pair<ContractionEdge<E>, ContractionEdge<E>>>> getPriorityAndShortcuts(
            ContractionVertex<V> vertex, int random) {

        List<Pair<ContractionEdge<E>, ContractionEdge<E>>> shortcuts = getShortcuts(vertex);
        VertexPriority priority = new VertexPriority(
                shortcuts.size() - getEdgeRemovedCount(vertex),
                vertex.neighborsContracted,
                random
        );
        return Pair.of(priority, shortcuts);
    }

    private VertexPriority getPriority(ContractionVertex<V> vertex, int random) {
        return new VertexPriority(
                countShortcuts(vertex) - getEdgeRemovedCount(vertex),
                vertex.neighborsContracted,
                random
        );
    }


    private int getEdgeRemovedCount(ContractionVertex<V> vertex) {
        return maskedContractionGraph.edgesOf(vertex).size();
    }


    private List<Pair<ContractionEdge<E>, ContractionEdge<E>>> getShortcuts(ContractionVertex<V> vertex) {
        ToListConsumer consumer = new ToListConsumer();
        iterateShortcuts(vertex, consumer);
        return consumer.shortcuts;
    }

    private int countShortcuts(ContractionVertex<V> vertex) {
        CountingConsumer consumer = new CountingConsumer();
        iterateShortcuts(vertex, consumer);
        return consumer.amount;
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
        AddressableHeap<Double, ContractionVertex<V>> heap = dijkstraSearchHeapSupplier.get();
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
            completionService.submit(new UpwardEdgesMarkerWorker(i), null);
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

    private class CountingConsumer implements BiConsumer<ContractionEdge<E>, ContractionEdge<E>> {
        int amount;

        CountingConsumer() {
            amount = 0;
        }

        @Override
        public void accept(ContractionEdge<E> e1, ContractionEdge<E> e2) {
            ++amount;
        }
    }


    public static class ContractionVertex<V1> {
        V1 vertex;
        int index;
        int contractionIndex;
        int neighborsContracted;
        boolean contracted;

        ContractionVertex(V1 vertex, int index) {
            this.index = index;
            this.vertex = vertex;
        }
    }

    public static class ContractionEdge<E1> {
        E1 edge;
        Pair<ContractionEdge<E1>, ContractionEdge<E1>> skippedEdges;
        boolean isUpward;

        public ContractionEdge(E1 edge) {
            this.edge = edge;
        }

        public ContractionEdge(Pair<ContractionEdge<E1>, ContractionEdge<E1>> skippedEdges) {
            this.skippedEdges = skippedEdges;
        }
    }

    private class ContractionWorker implements Runnable {

        private int workerIndex;
        private Random random;

        ContractionWorker(int workerIndex) {
            this.workerIndex = workerIndex;
            this.random = randomSupplier.get();
        }

        @Override
        public void run() {
            int start = (graph.vertexSet().size() * workerIndex) / parallelism;
            int end = (graph.vertexSet().size() * (workerIndex + 1)) / parallelism;
            for (int vertexIndex = start; vertexIndex < end; ++vertexIndex) {
                @SuppressWarnings("unchecked")
                ContractionVertex<V> vertex = (ContractionVertex<V>) verticesArray[vertexIndex];
                prioritiesArray[vertexIndex] = getPriority(vertex, random.nextInt());
            }
        }
    }

    private class UpwardEdgesMarkerWorker implements Runnable {
        private int workerIndex;

        UpwardEdgesMarkerWorker(int workerIndex) {
            this.workerIndex = workerIndex;
        }

        @Override
        public void run() {
            int start = (graph.vertexSet().size() * workerIndex) / parallelism;
            int end = (graph.vertexSet().size() * (workerIndex + 1)) / parallelism;
            for (int vertexIndex = start; vertexIndex < end; ++vertexIndex) {
                @SuppressWarnings("unchecked")
                ContractionVertex<V> vertex = (ContractionVertex<V>) verticesArray[vertexIndex];
                contractionGraph.outgoingEdgesOf(vertex).forEach(
                        e -> e.isUpward = contractionGraph.getEdgeSource(e).contractionIndex <
                                contractionGraph.getEdgeTarget(e).contractionIndex
                );
            }
        }
    }

    private static class VertexPriority implements Comparable<VertexPriority> {
        int edgeDifference;
        int neighborsContracted;
        int random;


        VertexPriority(int edgeDifference, int neighborsContracted, int random) {
            this.edgeDifference = edgeDifference;
            this.neighborsContracted = neighborsContracted;
            this.random = random;
        }

        int compareToIgnoreRandom(VertexPriority other) {
            int compareByEdgeDifference = Integer.compare(edgeDifference, other.edgeDifference);
            if (compareByEdgeDifference == 0) {
                return Integer.compare(neighborsContracted, other.neighborsContracted);
            }
            return compareByEdgeDifference;
        }

        @Override
        public int compareTo(VertexPriority other) {
            int compareIgnoreRandom = compareToIgnoreRandom(other);
            if (compareIgnoreRandom == 0) {
                return Integer.compare(random, other.random);
            }
            return compareIgnoreRandom;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            VertexPriority priority = (VertexPriority) o;
            return edgeDifference == priority.edgeDifference &&
                    neighborsContracted == priority.neighborsContracted &&
                    random == priority.random;
        }

        @Override
        public int hashCode() {
            return Objects.hash(edgeDifference, neighborsContracted, random);
        }
    }
}
