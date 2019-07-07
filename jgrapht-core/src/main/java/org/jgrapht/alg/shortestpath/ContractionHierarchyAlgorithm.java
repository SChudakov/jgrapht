package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.GraphType;
import org.jgrapht.Graphs;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.jheaps.AddressableHeap;
import org.jheaps.tree.PairingHeap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class ContractionHierarchyAlgorithm<V, E> {
    private Graph<V, E> graph;

    private Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph;
    private Map<V, ContractionVertex<V>> contractionMapping;

    private AddressableHeap<VertexPriority, ContractionVertex<V>> contractionQueue;
    private Map<ContractionVertex<V>, VertexPriority> vertexPriorityMap;

    private Queue<ContractionVertex<V>> verticesQueue;

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
        this.randomSupplier = randomSupplier;

        contractionMapping = new HashMap<>();
        verticesQueue = new ConcurrentLinkedQueue<>();
        vertexPriorityMap = new ConcurrentHashMap<>();
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
        // to be able to mark upward edges correctly
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
//                .vertexClass((Class<ContractionVertex<V>>) (Class<?>) ContractionVertex.class)
//                .edgeClass((Class<ContractionEdge<E>>) (Class<?>) ContractionEdge.class)
                .buildGraph();
    }

    private void fillContractionGraphAndVerticesQueue() {
        for (V v : graph.vertexSet()) {
            ContractionVertex<V> vertex = new ContractionVertex<>(v);

            contractionGraph.addVertex(vertex);
            contractionMapping.put(v, vertex);
            verticesQueue.add(vertex);
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
//            Pair<VertexPriority, List<Pair<ContractionEdge<E>, ContractionEdge<E>>>> p
//                    = getPriorityAndShortcuts(vertex, (int) (Math.random() * 10));
//            vertexPriorityMap.put(vertex, p.getFirst());
//            contractionQueue.insert(p.getFirst(), vertex);
//            System.out.println(vertex.vertex + " " + p.getSecond().size() + " " + p.getFirst());
//        });

//      submit tasks
        for (int i = 0; i < parallelism; ++i) {
            completionService.submit(new ContractionWorker(verticesQueue, randomSupplier.get()), null);
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

        for (Map.Entry<ContractionVertex<V>, VertexPriority> entry : vertexPriorityMap.entrySet()) {
            contractionQueue.insert(entry.getValue(), entry.getKey());
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
        System.out.println(vertex.vertex);
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

        // update vertex data
        vertex.contractionIndex = contractionIndex;
        vertex.contracted = true;

        // update neighbors data
        Graphs.successorListOf(contractionGraph, vertex).forEach(v -> ++v.neighborsContracted);
        Graphs.predecessorListOf(contractionGraph, vertex).forEach(v -> ++v.neighborsContracted);
    }

//    private void markUpwardEdges() {
//        contractionGraph.edgeSet().forEach(e -> e.upwardEdge =
//                contractionGraph.getEdgeSource(e).contractionIndex < contractionGraph.getEdgeTarget(e).contractionIndex);
//    }

//    private void recomputeNeiborsPriorities(Graph<ContractionVertex<V1>, ContractionEdge<E1>> contractionGraph,
//                                            ContractionVertex<V1> vertex) {
//
//    }

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


    private int getEdgeRemovedCount(ContractionVertex<V> vertex) {
        int result = (int) Graphs.successorListOf(contractionGraph, vertex).stream().filter(v -> !v.contracted).count();
        if (graph.getType().isDirected()) {
            result += Graphs.predecessorListOf(contractionGraph, vertex).stream().filter(v -> !v.contracted).count();
        }
        return result;
    }

    private List<Pair<ContractionEdge<E>, ContractionEdge<E>>> getShortcuts(ContractionVertex<V> vertex) {

        List<Pair<ContractionEdge<E>, ContractionEdge<E>>> shortcuts = new ArrayList<>();
        Set<ContractionVertex<V>> successors = new HashSet<>();

        double maxOutgoingEdgeWeight = Double.MIN_VALUE;
        for (ContractionEdge<E> outEdge : contractionGraph.outgoingEdgesOf(vertex)) {
            ContractionVertex<V> successor = Graphs.getOppositeVertex(contractionGraph, outEdge, vertex);
            if (!successor.contracted) { // do not consider contracted vertices
                successors.add(successor);
                maxOutgoingEdgeWeight = Math.max(maxOutgoingEdgeWeight, contractionGraph.getEdgeWeight(outEdge));
            }
        }


        for (ContractionEdge<E> inEdge : contractionGraph.incomingEdgesOf(vertex)) {
            ContractionVertex<V> predecessor = Graphs.getOppositeVertex(contractionGraph, inEdge, vertex);
            if (!predecessor.contracted) { // do not consider contracted vertices
                DijkstraClosestFirstIterator<ContractionVertex<V>, ContractionEdge<E>> it =
                        new DijkstraClosestFirstIterator<>(
                                contractionGraph,
                                predecessor,
                                contractionGraph.getEdgeWeight(inEdge) + maxOutgoingEdgeWeight);

                boolean containedPredecessor = successors.remove(predecessor); // might contain the predecessor vertex itself
                iterateToSuccessors(it, successors);
                ShortestPathAlgorithm.SingleSourcePaths<ContractionVertex<V>, ContractionEdge<E>> ssp = it.getPaths();

                for (ContractionVertex<V> successor : successors) {
                    GraphPath<ContractionVertex<V>, ContractionEdge<E>> successorPath = ssp.getPath(successor);
                    if (successorPath != null
                            && successorPath.getLength() == 2
                            && successorPath.getVertexList().get(1).equals(vertex)) {
                        List<ContractionEdge<E>> pathEdges = successorPath.getEdgeList();
                        shortcuts.add(Pair.of(pathEdges.get(0), pathEdges.get(1)));
                    }
                }
                if (containedPredecessor && graph.getType().isDirected()) {
                    successors.add(predecessor);
                }
            }
        }

        return shortcuts;
    }

    private void iterateToSuccessors(DijkstraClosestFirstIterator<ContractionVertex<V>, ContractionEdge<E>> it,
                                     Set<ContractionVertex<V>> successors) {
        int n = successors.size();
        int passedSuccessors = 0; // the source of the search is also a successor
        while (it.hasNext() && passedSuccessors < n) {
            ContractionVertex<V> next = it.next();
            if (successors.contains(next)) {
                ++passedSuccessors;
            }
        }
    }


    public static class ContractionVertex<V1> {
        V1 vertex;
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

        public ContractionVertex(V1 vertex) {
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

        private Queue<ContractionVertex<V>> verticesQueue;
        private Random random;

        ContractionWorker(Queue<ContractionVertex<V>> verticesQueue, Random random) {
            this.verticesQueue = verticesQueue;
            this.random = random;
        }

        @Override
        public void run() {
            ContractionVertex<V> vertex = verticesQueue.poll();
            while (vertex != null) {
                Pair<VertexPriority, List<Pair<ContractionEdge<E>, ContractionEdge<E>>>> p =
                        getPriorityAndShortcuts(vertex, random.nextInt());
                vertexPriorityMap.put(vertex, p.getFirst());
                vertex = verticesQueue.poll();
            }
        }
    }

    private class VertexPriority implements Comparable<VertexPriority> {
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
