package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.GraphType;
import org.jgrapht.Graphs;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.MaskSubgraph;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.jheaps.AddressableHeap;
import org.jheaps.tree.PairingHeap;

import java.util.ArrayList;
import java.util.Collection;
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

    private ExecutorService executor;
    private ExecutorCompletionService<Void> completionService;
    private int parallelism;

    private Supplier<Random> randomSupplier;

    public ContractionHierarchyAlgorithm(Graph<V, E> graph) {
        this(graph, new PairingHeap<>());
    }

    public ContractionHierarchyAlgorithm(Graph<V, E> graph, AddressableHeap<VertexPriority, ContractionVertex<V>> contractionQueue) {
        init(graph, contractionQueue, Random::new, Runtime.getRuntime().availableProcessors());
    }

    public ContractionHierarchyAlgorithm(Graph<V, E> graph, AddressableHeap<VertexPriority, ContractionVertex<V>> contractionQueue,
                                         Supplier<Random> randomSupplier, int parallelism) {
        init(graph, contractionQueue, randomSupplier, parallelism);
    }

    private void init(Graph<V, E> graph,
                      AddressableHeap<VertexPriority, ContractionVertex<V>> contractionQueue,
                      Supplier<Random> randomSupplier,
                      int parallelism) {
        if (!graph.getType().isSimple()) {
            throw new IllegalArgumentException("Graph should be simple!");
        }
        this.graph = graph;
        this.contractionQueue = contractionQueue;
        this.parallelism = parallelism;
        this.contractionGraph = createContractionGraph();
        this.maskedContractionGraph = new MaskSubgraph<>(contractionGraph,
                v -> v.contracted,
                e -> contractionGraph.getEdgeSource(e).contracted || contractionGraph.getEdgeTarget(e).contracted);
        this.randomSupplier = randomSupplier;

        verticesArray = new Object[graph.vertexSet().size()];

        contractionMapping = new HashMap<>();
        prioritiesArray = new VertexPriority[graph.vertexSet().size()];
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        completionService = new ExecutorCompletionService<>(executor);
    }


    public Pair<Graph<ContractionVertex<V>, ContractionEdge<E>>,
            Map<V, ContractionVertex<V>>> computeContractionHierarchy() {
        fillContractionGraphAndVerticesQueue();
        computeInitialPriorities();
        contractVertices();
        return Pair.of(contractionGraph, contractionMapping);
    }

    private Graph<ContractionVertex<V>, ContractionEdge<E>> createContractionGraph() {
        GraphType graphType = graph.getType();
        GraphTypeBuilder<ContractionVertex<V>, ContractionEdge<E>> resultBuilder;

        if (graphType.isDirected()) {
            resultBuilder = GraphTypeBuilder.directed();
        } else if (graphType.isUndirected()) {
            resultBuilder = GraphTypeBuilder.undirected();
        } else {
            resultBuilder = GraphTypeBuilder.mixed();
        }

        return resultBuilder
                .weighted(true)
                .allowingMultipleEdges(graph.getType().isAllowingMultipleEdges())
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
            ContractionEdge<E> edge = new ContractionEdge<>(e);
            contractionGraph.addEdge(
                    contractionMapping.get(graph.getEdgeSource(e)),
                    contractionMapping.get(graph.getEdgeTarget(e)),
                    edge
            );
            contractionGraph.setEdgeWeight(edge, graph.getEdgeWeight(e));
        }
    }

    private void computeInitialPriorities() {
//        contractionGraph.vertexSet().forEach(vertex -> {
//            VertexPriority priority = getPriority(vertex, (int) (Math.random() * 1000000));
//            prioritiesArray.put(vertex, priority);
//            contractionQueue.insert(priority, vertex);
////            System.out.println(vertex.vertex + " " + p.getSecond().size() + " " + p.getFirst());
//        });

//      submit tasks
        for (int i = 0; i < parallelism; ++i) {
            completionService.submit(new ContractionWorker(i, randomSupplier.get()), null);
        }
//      take tasks
        for (int i = 0; i < parallelism; ++i) {
            try {
                completionService.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

//      shut down executor
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

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

            if (contractionQueue.isEmpty() ||
                    updatedPriority.compareToIgnoreRandom(contractionQueue.findMin().getKey()) <= 0) {
                contractVertex(vertex, contractionIndex, p.getSecond());
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
            ContractionVertex<V> shortcutSource = Graphs.getOppositeVertex(contractionGraph, shortcut.getFirst(), vertex);
            ContractionVertex<V> shortcutTarget = Graphs.getOppositeVertex(contractionGraph, shortcut.getSecond(), vertex);
            ContractionEdge<E> shortcutEdge = new ContractionEdge<>(shortcut);

            contractionGraph.addEdge(shortcutSource, shortcutTarget, shortcutEdge);
            contractionGraph.setEdgeWeight(contractionGraph.getEdge(shortcutSource, shortcutTarget),
                    contractionGraph.getEdgeWeight(shortcut.getFirst())
                            + contractionGraph.getEdgeWeight(shortcut.getSecond()));
        }

        // update neighbors data --multi-graph unsafe--
        Collection<ContractionVertex<V>> neighbours;
        if (graph.getType().isSimple()) {
            neighbours = Graphs.successorListOf(maskedContractionGraph, vertex);
        } else {
            neighbours = Graphs.neighborSetOf(maskedContractionGraph, vertex);
        }
        neighbours.forEach(v -> ++v.neighborsContracted);

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
        // --multi-graph usage--
        int result = Graphs.successorListOf(maskedContractionGraph, vertex).size();
        if (graph.getType().isDirected()) {
            result += Graphs.predecessorListOf(maskedContractionGraph, vertex).size();
        }
        return result;
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
                                  BiConsumer<ContractionEdge<E>, ContractionEdge<E>> shortcutsConsumer) {
        Set<ContractionVertex<V>> successors = new HashSet<>();

        double maxOutgoingEdgeWeight = Double.MIN_VALUE;
        for (ContractionEdge<E> outEdge : maskedContractionGraph.outgoingEdgesOf(vertex)) {
            ContractionVertex<V> successor = Graphs.getOppositeVertex(contractionGraph, outEdge, vertex);
            successors.add(successor);
            maxOutgoingEdgeWeight = Math.max(maxOutgoingEdgeWeight, contractionGraph.getEdgeWeight(outEdge));
        }


        for (ContractionEdge<E> inEdge : maskedContractionGraph.incomingEdgesOf(vertex)) {
            ContractionVertex<V> predecessor = Graphs.getOppositeVertex(contractionGraph, inEdge, vertex);

            boolean containedPredecessor = successors.remove(predecessor); // might contain the predecessor vertex itself

            AddressableHeap<Double, ContractionVertex<V>> nodesHeap = new PairingHeap<>();
            Map<ContractionVertex<V>, AddressableHeap.Handle<Double, ContractionVertex<V>>> distances =
                    iterateToSuccessors(maskedContractionGraph,
                            nodesHeap,
                            predecessor,
                            successors,
                            vertex,
                            maxOutgoingEdgeWeight + contractionGraph.getEdgeWeight(inEdge));


            for (ContractionVertex<V> successor : successors) {
                ContractionEdge<E> outEdge = contractionGraph.getEdge(vertex, successor);
                double pathWeight = contractionGraph.getEdgeWeight(inEdge) + contractionGraph.getEdgeWeight(outEdge);

                if (!distances.containsKey(successor) ||
                        distances.get(successor).getKey() > pathWeight) {
                    shortcutsConsumer.accept(inEdge, outEdge);
                }
            }

            if (containedPredecessor && graph.getType().isDirected()) {
                successors.add(predecessor);
            }
        }
    }


    private Map<ContractionVertex<V>, AddressableHeap.Handle<Double, ContractionVertex<V>>>
    iterateToSuccessors(Graph<ContractionVertex<V>, ContractionEdge<E>> graph,
                        AddressableHeap<Double, ContractionVertex<V>> heap,
                        ContractionVertex<V> source,
                        Set<ContractionVertex<V>> targets,
                        ContractionVertex<V> forbiddenVertex,
                        double radius) {
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
            ContractionVertex<V> successor = Graphs.getOppositeVertex(graph, edge, vertex);

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


    private class ToListConsumer implements BiConsumer<ContractionEdge<E>, ContractionEdge<E>> {
        List<Pair<ContractionEdge<E>, ContractionEdge<E>>> shortcuts;

        public ToListConsumer() {
            shortcuts = new ArrayList<>();
        }

        @Override
        public void accept(ContractionEdge<E> e1, ContractionEdge<E> e2) {
            shortcuts.add(Pair.of(e1, e2));
        }
    }

    private class CountingConsumer implements BiConsumer<ContractionEdge<E>, ContractionEdge<E>> {
        int amount;

        public CountingConsumer() {
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

        @Override
        public String toString() {
            return "ContractionVertex{" +
                    "vertex=" + vertex +
                    ", contractionIndex=" + contractionIndex +
                    ", neighborsContracted=" + neighborsContracted +
                    ", contracted=" + contracted +
                    '}';
        }

        public ContractionVertex(V1 vertex, int index) {
            this.index = index;
            this.vertex = vertex;
        }
    }

    public static class ContractionEdge<E1> {
        E1 edge;
        Pair<ContractionEdge<E1>, ContractionEdge<E1>> skippedEdges;

        public ContractionEdge(E1 edge) {
            this.edge = edge;
        }

        public ContractionEdge(Pair<ContractionEdge<E1>, ContractionEdge<E1>> skippedEdges) {
            this.skippedEdges = skippedEdges;
        }

        @Override
        public String toString() {
            return "ContractionEdge{" +
                    "edge=" + edge +
                    ", skippedEdges=" + skippedEdges +
                    '}';
        }
    }

    private class ContractionWorker implements Runnable {

        private int workerIndex;
        private Random random;

        ContractionWorker(int workerIndex, Random random) {
            this.workerIndex = workerIndex;
            this.random = random;
        }

        @Override
        public void run() {
            int start = (graph.vertexSet().size() * workerIndex) / parallelism;
            int end = (graph.vertexSet().size() * (workerIndex + 1)) / parallelism;
            for (int vertexIndex = start; vertexIndex < end; ++vertexIndex) {
                ContractionVertex<V> vertex = (ContractionVertex<V>) verticesArray[vertexIndex];
                prioritiesArray[vertexIndex] = getPriority(vertex, random.nextInt());
            }
        }
    }

    private static class VertexPriority implements Comparable<VertexPriority> {
        int edgeDifference;
        int neighborsContracted;
        int random;

        private VertexPriority(int edgeDifference, int neighborsContracted) {
            this(edgeDifference, neighborsContracted, 0);
        }

        VertexPriority(int edgeDifference, int neighborsContracted, int random) {
            this.edgeDifference = edgeDifference;
            this.neighborsContracted = neighborsContracted;
            this.random = random;
        }

        public int compareToIgnoreRandom(VertexPriority other) {
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
            @SuppressWarnings("unchecked")
            VertexPriority priority = (VertexPriority) o;
            return edgeDifference == priority.edgeDifference &&
                    neighborsContracted == priority.neighborsContracted &&
                    random == priority.random;
        }

        @Override
        public int hashCode() {
            return Objects.hash(edgeDifference, neighborsContracted, random);
        }

        @Override
        public String toString() {
            return "VertexPriority{" +
                    "edgeDifference=" + edgeDifference +
                    ", neighborsContracted=" + neighborsContracted +
                    ", random=" + random +
                    '}';
        }
    }
}
