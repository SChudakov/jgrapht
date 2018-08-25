/*
 * (C) Copyright 2018-2018, by Semen Chudakov and Contributors.
 *
 * JGraphT : a free Java graph-theory library
 *
 * This program and the accompanying materials are dual-licensed under
 * either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation, or (at your option) any
 * later version.
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation.
 */
package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.alg.util.Triple;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * An implementation of <a href="https://people.csail.mit.edu/jshun/papers/MS03.pdf">delta-stepping
 * shortest path algorithm</a>.
 *
 * <p>
 * The $\delta$-stepping algorithm maintains eligible nodes with tentative distances
 * in the bucket structure each bucket of which represents a distance range of size
 * $\delta$. During each phase, the algorithm removes all nodes of the first nonempty
 * bucket and relaxes all outgoing edges of weight at most $\delta$. Edges of higher
 * weight are only relaxed after their respective starting nodes are surely settled.
 * The choice of $\delta$ should provide a good trade-off between too many node
 * re-considerations on the one hand and too many bucket traversals on the other hand.
 *
 * <p>
 * To prevent the necessity to synchronize threads the bucket structure is implemented here
 * as a map of vertices to their bucket indices. Furthermore, every time a vertex is inserted
 * in a bucket the tentative distance to the vertex and its predecessor should be updated in
 * order to reconstruct the shortest paths tree at the end of the computation. Therefore to be
 * able to update all the information safely it is kept in a single object which is wrapped
 * in {@link AtomicReference}.
 *
 * @param <V> the graph vertex type
 * @param <E> the graph edge type
 * @author Semen Chudakov
 * @see AtomicReference
 * @since August 2018
 */
public class DeltaSteppingShortestPath<V, E> extends BaseShortestPathAlgorithm<V, E> {
    /**
     * Error message for reporting the existence of an edge with negative weight.
     */
    private static final String NEGATIVE_EDGE_WEIGHT_NOT_ALLOWED = "Negative edge weight not allowed";
    /**
     * Error message for reporting that delta must be positive.
     */
    private static final String DELTA_MUST_BE_POSITIVE = "Delta must be positive";

    /**
     * The bucket width. A bucket with index $i$ therefore stores
     * a vertex v if and only if v is queued and tentative distance
     * to v $\in[i\cdot\delta,(i+1)\cdot\delta]$
     */
    private double delta;
    /**
     * Num of buckets in the bucket structure.
     */
    private int numOfBuckets;
    /**
     * Map with light edges for each vertex. An edge is considered
     * light if its weight is less than or equal to {@link #delta}.
     */
    private Map<V, Set<E>> light;
    /**
     * Map with light edges for each vertex. An edge is
     * considered heavy if its weight is greater than {@link #delta}.
     */
    private Map<V, Set<E>> heavy;

    /**
     * Map that stores information about each vertex.
     *
     * <p>
     * In each triple the first value stands for the bucket index of a
     * vertex or $-1$ if a vertex does not belong to any bucket. The second
     * value stands for the tentative distance to a vertex. The third value
     * of each triple stands for the predecessor of a vertex in the the
     * shortest path tree. The second and the third values of each triple will
     * be used at the end of the computation to construct shortest paths tree.
     *
     * <p>
     * Keeping vertex information in an {@link AtomicReference} objects allows
     * to avoid threads synchronisation. Thus a thread can safely update
     * the information using standard CAS function.
     */
    private Map<V, AtomicReference<Triple<Integer, Double, E>>> verticesDataMap;

    /**
     * Constructs a new instance of the algorithm for a given graph.
     * Initializes {@link #delta} to $0.0$ to preserve lazy computation stype.
     *
     * @param graph graph
     */
    public DeltaSteppingShortestPath(Graph<V, E> graph) {
        super(graph);
        delta = 0.0;
        initializeFields();
    }

    /**
     * Constructs a new instance of the algorithm for a given graph and delta.
     *
     * @param graph the graph
     * @param delta bucket width
     */
    public DeltaSteppingShortestPath(Graph<V, E> graph, double delta) {
        super(graph);
        if (delta <= 0) {
            throw new IllegalArgumentException(DELTA_MUST_BE_POSITIVE);
        }
        this.delta = delta;
        initializeFields();
    }

    /**
     * Initializes {@link #light}, {@link #heavy} and {@link #verticesDataMap} fields.
     */
    private void initializeFields() {
        light = new HashMap<>();
        heavy = new HashMap<>();
        verticesDataMap = new HashMap<>();
    }

    /**
     * Asserts that all edges in the {@link #graph} have positive weights.
     */
    private void assertPositiveWeights() {
        boolean allEdgesWithNonNegativeWeights = graph.edgeSet().stream()
                .map(graph::getEdgeWeight).allMatch(weight -> weight >= 0);
        if (!allEdgesWithNonNegativeWeights) {
            throw new IllegalArgumentException(NEGATIVE_EDGE_WEIGHT_NOT_ALLOWED);
        }
    }

    /**
     * Calculates max edge weight in the {@link #graph}.
     *
     * @return max edge weight
     */
    private Optional<Double> maxEdgeWeight() {
        return graph.edgeSet().stream().map(graph::getEdgeWeight).max(Double::compare);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GraphPath<V, E> getPath(V source, V sink) {
        if (!graph.containsVertex(source)) {
            throw new IllegalArgumentException(GRAPH_MUST_CONTAIN_THE_SOURCE_VERTEX);
        }
        if (!graph.containsVertex(sink)) {
            throw new IllegalArgumentException(GRAPH_MUST_CONTAIN_THE_SINK_VERTEX);
        }
        return getPaths(source).getPath(sink);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SingleSourcePaths<V, E> getPaths(V source) {
        if (!graph.containsVertex(source)) {
            throw new IllegalArgumentException(GRAPH_MUST_CONTAIN_THE_SOURCE_VERTEX);
        }
        assertPositiveWeights();

        if (delta == 0.0) {
            delta = findDelta();
        }
        fillMaps();
        numOfBuckets = numOfBuckets();

        computeShortestPaths(source);

        Map<V, Pair<Double, E>> distanceAndPredecessorMap = new HashMap<>();
        for (Map.Entry<V, AtomicReference<Triple<Integer, Double, E>>> entry : verticesDataMap.entrySet()) {
            distanceAndPredecessorMap.put(entry.getKey(),
                    Pair.of(entry.getValue().get().getSecond(),
                            entry.getValue().get().getThird()));
        }
        return new TreeSingleSourcePathsImpl<>(graph, source, distanceAndPredecessorMap);
    }

    /**
     * Calculates value of {@link #delta}. The value is calculated to
     * maximal edge weight divided by maximal out-degree in the {@link #graph}
     * or $1.0$ if edge set of the {@link #graph} is empty.
     *
     * @return bucket width
     */
    private double findDelta() {
        Optional<Double> maxEdgeWeight = maxEdgeWeight();
        if (maxEdgeWeight.isPresent()) {
            int maxOutDegree = graph.vertexSet().stream().map(graph::outDegreeOf).max(Integer::compare).orElse(0);
            return maxEdgeWeight.get() / maxOutDegree;
        } else {
            return 1.0;
        }
    }

    /**
     * Fills {@link #light}, {@link #heavy} and{@link #verticesDataMap} fields.
     */
    private void fillMaps() {
        graph.vertexSet().forEach(v -> {
            light.put(v, new HashSet<>());
            heavy.put(v, new HashSet<>());
            verticesDataMap.putIfAbsent(v, new AtomicReference<>(Triple.of(-1, Double.POSITIVE_INFINITY, null)));
        });
        graph.vertexSet().parallelStream().forEach(v -> {
            for (E e : graph.outgoingEdgesOf(v)) {
                if (graph.getEdgeWeight(e) > delta) {
                    heavy.get(v).add(e);
                } else {
                    light.get(v).add(e);
                }
            }
        });
    }

    /**
     * Performs shortest paths computation.
     *
     * @param source the source vertex
     */
    private void computeShortestPaths(V source) {
        relax(source, null, 0.0);

        int firstNonEmptyBucket = 0;
        while (firstNonEmptyBucket != -1) {
            List<V> removed = new ArrayList<>();
            List<V> bucketElements = bucketElements(firstNonEmptyBucket);
            while (!bucketElements.isEmpty()) {
                removed.addAll(bucketElements);
                clearBucket(firstNonEmptyBucket);

                findAndRelaxRequests(bucketElements, light);
                bucketElements = bucketElements(firstNonEmptyBucket);
            }
            findAndRelaxRequests(removed, heavy);
            firstNonEmptyBucket = firstNonEmptyBucket();
        }
    }

    /**
     * For each vertex v in {@code vertices} relaxes all edges emanating from v
     * that are present in {@code edgesKind#get(v)}.
     * Elements in {@code vertices} are processed in parallel.
     *
     * @param vertices  vertices
     * @param edgesKind vertex to edges map
     */
    private void findAndRelaxRequests(List<V> vertices, Map<V, Set<E>> edgesKind) {
        vertices.parallelStream().forEach(v -> {
            for (E e : edgesKind.get(v)) {
                relax(Graphs.getOppositeVertex(graph, e, v), e,
                        verticesDataMap.get(v).get().getSecond() + graph.getEdgeWeight(e));
            }
        });
    }

    /**
     * Performs relaxation in parallel-safe fashion. Vertex data {@code v} is
     * considered updated once either {@code distance} is greater than or equal
     * to tentative distance in {@link #verticesDataMap} or
     * {@link AtomicReference#compareAndSet(Object, Object)} returned {@code true}.
     *
     * @param v        vertex
     * @param e        edge to predecessor
     * @param distance distance
     */
    private void relax(V v, E e, double distance) {
        boolean updated = false;
        AtomicReference<Triple<Integer, Double, E>> dataReference = verticesDataMap.get(v);
        Triple<Integer, Double, E> updatedData = Triple.of(bucketIndex(distance), distance, e);
        while (!updated) {
            Triple<Integer, Double, E> oldData = dataReference.get();
            if (distance < oldData.getSecond()) {
                updated = dataReference.compareAndSet(oldData, updatedData);
            } else {
                updated = true;
            }
        }
    }

    /**
     * Calculates num of buckets in the bucket structure.
     *
     * @return num of buckets
     */
    private int numOfBuckets() {
        return maxEdgeWeight().map(maxWeight -> (int) (Math.ceil(maxWeight / delta) + 1)).orElse(1);
    }

    /**
     * Calculates bucket index for a given {@code distance}.
     *
     * @param distance distance
     * @return bucket index
     */
    private int bucketIndex(double distance) {
        return ((int) Math.round(distance / delta)) % numOfBuckets;
    }

    /**
     * Finds index of the first non-empty bucket.
     *
     * @return index of the first non-empty buckets
     */
    private int firstNonEmptyBucket() {
        return verticesDataMap.values().stream()
                .map(triple -> triple.get().getFirst())
                .filter(bucketIndex -> bucketIndex >= 0)
                .min(Integer::compare).orElse(-1);
    }

    /**
     * Finds all elements of the given {@code bucket}.
     *
     * @param bucket bucket index
     * @return bucket elements
     */
    private List<V> bucketElements(int bucket) {
        return verticesDataMap.entrySet().stream()
                .filter(entry -> entry.getValue().get().getFirst() == bucket)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Empty given {@code bucket}. Sets bucket index
     * to $-1$ for all vertices in the given {@code bucket}.
     *
     * @param bucket bucket index
     */
    private void clearBucket(int bucket) {
        List<V> bucketElements = bucketElements(bucket);
        for (V v : bucketElements) {
            Triple<Integer, Double, E> data = verticesDataMap.get(v).get();
            verticesDataMap.get(v).set(Triple.of(
                    -1,
                    data.getSecond(),
                    data.getThird()
            ));
        }
    }
}
