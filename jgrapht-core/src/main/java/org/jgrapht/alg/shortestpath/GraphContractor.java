package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.GraphType;
import org.jgrapht.Graphs;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.builder.GraphTypeBuilder;
import org.jheaps.AddressableHeap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class GraphContractor<V, E> {
    private final Graph<V, E> graph;
    private Graph<ContractionVertex, ContractionEdge> contractionGraph;

    private AddressableHeap<VertexPriority, ContractionVertex> contractionQueue;
    private Map<ContractionVertex, VertexPriority> vertexPriorityMap;

    private Queue<ContractionVertex> verticesQueue;

    private ExecutorService executor;
    private ExecutorCompletionService<Void> completionService;
    private int parallelism;

    public GraphContractor(Graph<V, E> graph, AddressableHeap<VertexPriority, ContractionVertex> contractionQueue) {
        if (!graph.getType().isDirected()) {
            throw new IllegalArgumentException("Graph should be directed!");
        }
        this.graph = graph;
        this.contractionQueue = contractionQueue;
        verticesQueue = new ConcurrentLinkedQueue<>();
        vertexPriorityMap = new ConcurrentHashMap<>();
        executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        completionService = new ExecutorCompletionService<>(executor);
        parallelism = Runtime.getRuntime().availableProcessors();
    }

    public Graph<ContractionVertex, ContractionEdge> computeContractionHierarchy() {
        createContractionGraph();
        fillContractionGraphAndVerticesQueue();
        computeInitialPriorities();
        contractVertices();
        markUpwardEdges();
        return contractionGraph;
    }

    private void createContractionGraph() {
        GraphType type = graph.getType();
        GraphTypeBuilder<ContractionVertex, ContractionEdge> resultBuilder;

        if (type.isDirected()) {
            resultBuilder = GraphTypeBuilder.directed();
        } else if (type.isUndirected()) {
            resultBuilder = GraphTypeBuilder.undirected();
        } else {
            resultBuilder = GraphTypeBuilder.mixed();
        }

        contractionGraph = resultBuilder
                .weighted(true)
                .allowingMultipleEdges(false)
                .allowingSelfLoops(false)
                .vertexClass(ContractionVertex.class)
                .edgeClass(ContractionEdge.class)
                .buildGraph();
    }

    private void fillContractionGraphAndVerticesQueue() {
        Map<V, ContractionVertex> vertexMap = new HashMap<>();

        for (V v : graph.vertexSet()) {
            ContractionVertex vertex = new ContractionVertex(v);

            contractionGraph.addVertex(vertex);
            vertexMap.put(v, vertex);
            verticesQueue.add(vertex);
        }

        for (E e : graph.edgeSet()) {
            ContractionEdge edge = new ContractionEdge(e);
            contractionGraph.addEdge(
                    vertexMap.get(graph.getEdgeSource(e)),
                    vertexMap.get(graph.getEdgeTarget(e)),
                    edge
            );
            contractionGraph.setEdgeWeight(edge, graph.getEdgeWeight(e));
        }
    }

    private void computeInitialPriorities() {
        Runnable runnable = new ContractionWorker(contractionGraph, verticesQueue);
        // submit tasks
        for (int i = 0; i < parallelism; ++i) {
            completionService.submit(runnable, null);
        }
        // take tasks
        for (int i = 0; i < parallelism; ++i) {
            try {
                completionService.take();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // shut down executor
        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        for (Map.Entry<ContractionVertex, VertexPriority> entry : vertexPriorityMap.entrySet()) {
            contractionQueue.insert(entry.getValue(), entry.getKey());
        }
    }

    private void contractVertices() {
        int contractionIndex = 0;

        while (!contractionQueue.isEmpty()) {
            AddressableHeap.Handle<VertexPriority, ContractionVertex> handle = contractionQueue.deleteMin();

            ContractionVertex vertex = handle.getValue();
            VertexPriority oldPriority = handle.getKey();
            Pair<VertexPriority, List<Pair<ContractionEdge, ContractionEdge>>> p =
                    getPriorityAndShortcuts(vertex, oldPriority.random);
            VertexPriority updatedPriority = p.getFirst();

            if (contractionQueue.isEmpty() ||
                    updatedPriority.compareTo(contractionQueue.findMin().getKey()) >= 0) {
                contractVertex(vertex, contractionIndex, p.getSecond());
                ++contractionIndex;
            } else {
                contractionQueue.insert(updatedPriority, vertex);
            }
        }
    }

    private void contractVertex(ContractionVertex vertex, int contractionIndex,
                                List<Pair<ContractionEdge, ContractionEdge>> shortcuts) {

        // add shortcuts
        for (Pair<ContractionEdge, ContractionEdge> shortcut : shortcuts) {
            ContractionVertex shortcutSource = contractionGraph.getEdgeSource(shortcut.getFirst());
            ContractionVertex shortcutTarget = contractionGraph.getEdgeSource(shortcut.getSecond());


            ContractionEdge shortcutEdge = new ContractionEdge(shortcut);
            contractionGraph.addEdge(shortcutSource, shortcutTarget, shortcutEdge);
            contractionGraph.setEdgeWeight(shortcutEdge,
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

    private void markUpwardEdges() {
        contractionGraph.edgeSet().forEach(e -> e.upwardEdge =
                contractionGraph.getEdgeSource(e).contractionIndex < contractionGraph.getEdgeTarget(e).contractionIndex);
    }

//    private void recomputeNeiborsPriorities(Graph<ContractionVertex, ContractionEdge> contractionGraph,
//                                            ContractionVertex vertex) {
//
//    }

    private Pair<VertexPriority, List<Pair<ContractionEdge, ContractionEdge>>> getPriorityAndShortcuts(
            ContractionVertex vertex, int random) {

        List<Pair<ContractionEdge, ContractionEdge>> shortcuts = getShortcuts(vertex);
        VertexPriority priority = new VertexPriority(
                shortcuts.size() - getEdgeRemovedCount(vertex),
                vertex.neighborsContracted,
                random
        );
        return Pair.of(priority, shortcuts);
    }


    private int getEdgeRemovedCount(ContractionVertex vertex) {
        return (int) (Graphs.successorListOf(contractionGraph, vertex).stream().filter(v -> v.contracted).count() +
                Graphs.predecessorListOf(contractionGraph, vertex).stream().filter(v -> v.contracted).count());
    }

    private List<Pair<ContractionEdge, ContractionEdge>> getShortcuts(ContractionVertex vertex) {

        List<Pair<ContractionEdge, ContractionEdge>> shortcuts = new ArrayList<>();
        Set<ContractionVertex> successors = new HashSet<>();

        double maxOutgoingEdgeWeight = Double.MIN_VALUE;
        for (ContractionEdge outEdge : contractionGraph.outgoingEdgesOf(vertex)) {
            ContractionVertex successor = contractionGraph.getEdgeTarget(outEdge);
            if (!successor.contracted) { // do not consider contracted vertices
                successors.add(successor);
                maxOutgoingEdgeWeight = Math.max(maxOutgoingEdgeWeight, contractionGraph.getEdgeWeight(outEdge));
            }
        }


        for (ContractionEdge inEdge : contractionGraph.incomingEdgesOf(vertex)) {
            ContractionVertex predecessor = contractionGraph.getEdgeSource(inEdge);
            if (!predecessor.contracted) { // do not consider contracted vertices
                DijkstraClosestFirstIterator<ContractionVertex, ContractionEdge> it =
                        new DijkstraClosestFirstIterator<>(
                                contractionGraph,
                                predecessor,
                                contractionGraph.getEdgeWeight(inEdge) + maxOutgoingEdgeWeight);

                iterateToSuccessors(it, successors);
                ShortestPathAlgorithm.SingleSourcePaths<ContractionVertex, ContractionEdge> ssp = it.getPaths();

                for (ContractionVertex successor : successors) {
                    GraphPath<ContractionVertex, ContractionEdge> successorPath = ssp.getPath(successor);
                    if (successorPath != null
                            && successorPath.getLength() == 2
                            && successorPath.getVertexList().get(1).equals(vertex)) {
                        List<ContractionEdge> pathEdges = successorPath.getEdgeList();
                        shortcuts.add(Pair.of(pathEdges.get(0), pathEdges.get(1)));
                    }
                }
            }
        }

        return shortcuts;
    }

    private void iterateToSuccessors(DijkstraClosestFirstIterator<ContractionVertex, ContractionEdge> it,
                                     Set<ContractionVertex> successors) {
        int n = successors.size();
        int passedSuccessors = 1; // the source of the search is also a successor
        while (it.hasNext() && passedSuccessors < n) {
            if (successors.contains(it.next())) {
                ++passedSuccessors;
            }
        }
    }


    private class ContractionWorker implements Runnable {

        private Graph<ContractionVertex, ContractionEdge> contractionGraph;
        private Queue<ContractionVertex> verticesQueue;
        private ThreadLocalRandom random;

        ContractionWorker(Graph<ContractionVertex, ContractionEdge> contractionGraph, Queue<ContractionVertex> verticesQueue) {
            this.contractionGraph = contractionGraph;
            this.verticesQueue = verticesQueue;
            this.random = ThreadLocalRandom.current();
        }

        @Override
        public void run() {
            ContractionVertex vertex = verticesQueue.poll();
            while (vertex != null) {
                Pair<VertexPriority, List<Pair<ContractionEdge, ContractionEdge>>> p =
                        getPriorityAndShortcuts(vertex, random.nextInt());
                vertexPriorityMap.putIfAbsent(vertex, p.getFirst());
                vertex = verticesQueue.poll();
            }
        }
    }

    public class ContractionVertex {
        V vertex;
        int contractionIndex;
        int neighborsContracted;
        boolean contracted;

        public ContractionVertex(V vertex) {
            this.vertex = vertex;
        }
    }

    public class ContractionEdge {
        E edge;
        Pair<ContractionEdge, ContractionEdge> skippedEdges;
        boolean upwardEdge;

        public ContractionEdge(E edge) {
            this.edge = edge;
        }

        public ContractionEdge(Pair<ContractionEdge, ContractionEdge> skippedEdges) {
            this.skippedEdges = skippedEdges;
        }
    }

    private class VertexPriority implements Comparable<VertexPriority> {
        int edgeDifference;
        int neighborsContracted;
        int random;

        private VertexPriority(int edgeDifference, int neighborsContracted) {
            this(edgeDifference, neighborsContracted, 0);
        }

        public VertexPriority(int edgeDifference, int neighborsContracted, int random) {
            this.edgeDifference = edgeDifference;
            this.neighborsContracted = neighborsContracted;
            this.random = random;
        }


        @Override
        public int compareTo(VertexPriority other) {
            int compareByEdgeDifference = -Integer.compare(edgeDifference, other.edgeDifference);
            if (compareByEdgeDifference == 0) {
                int compareByNeighborsContracted = -Integer.compare(neighborsContracted, other.neighborsContracted);
                if (compareByNeighborsContracted == 0) {
                    return Integer.compare(random, other.random);
                }
                return compareByNeighborsContracted;
            }
            return compareByEdgeDifference;
        }
    }
}
