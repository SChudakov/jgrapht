package org.jgrapht.alg.cycle;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.connectivity.GabowStrongConnectivityInspector;
import org.jgrapht.alg.interfaces.MinimumCycleMeanAlgorithm;
import org.jgrapht.alg.interfaces.StrongConnectivityAlgorithm;
import org.jgrapht.alg.util.ToleranceDoubleComparator;
import org.jgrapht.graph.GraphWalk;
import org.jgrapht.util.CollectionUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class HowardMinimumMeanCycle<V, E> implements MinimumCycleMeanAlgorithm<V, E> {
    private final Graph<V, E> graph;
    private final StrongConnectivityAlgorithm<V, E> strongConnectivityAlgorithm;
    private final int maximumIterations;
    private final ToleranceDoubleComparator comparator;

    private boolean isCurrentPathFound;
    private double currentPathWeight;
    private int currentPathSize;
    private V currentPathVertex;

    private boolean isBestPathFound;
    private double bestPathWeight;
    private int bestPathSize;
    private V bestPathVertex;

    private Map<V, E> policyGraph;
    private Map<V, Boolean> reachedVertices;
    private Map<V, Integer> vertexLevel;
    private Map<V, Double> vertexDistance;


    public HowardMinimumMeanCycle(Graph<V, E> graph) {
        this(graph, Integer.MAX_VALUE);
    }

    public HowardMinimumMeanCycle(Graph<V, E> graph, int maximumIterations) {
        this(graph, maximumIterations, new GabowStrongConnectivityInspector<>(graph), 1e-9);
    }

    public HowardMinimumMeanCycle(Graph<V, E> graph, int maximumIterations,
                                  StrongConnectivityAlgorithm<V, E> strongConnectivityAlgorithm, double toleranceEpsilon) {
        this.graph = graph;
        this.strongConnectivityAlgorithm = strongConnectivityAlgorithm;
        this.maximumIterations = maximumIterations;
        this.comparator = new ToleranceDoubleComparator(toleranceEpsilon);

        this.policyGraph = CollectionUtil.newHashMapWithExpectedSize(graph.vertexSet().size());
        this.reachedVertices = CollectionUtil.newHashMapWithExpectedSize(graph.vertexSet().size());
        this.vertexLevel = CollectionUtil.newHashMapWithExpectedSize(graph.vertexSet().size());
        this.vertexDistance = CollectionUtil.newHashMapWithExpectedSize(graph.vertexSet().size());

        this.isBestPathFound = false;
        this.bestPathWeight = 0.0;
        this.bestPathSize = 1;
    }

    @Override
    public double getCycleMean() {
        return getMean(getCycle());
    }

    private double getMean(GraphPath<V, E> cycle) {
        if (cycle == null) {
            return Double.POSITIVE_INFINITY;
        }
        return cycle.getWeight() / cycle.getLength();
    }

    @Override
    public GraphPath<V, E> getCycle() {
        boolean pathFound = findCycleMean();
        if (pathFound) {
            return buildPath();
        }
        return null;
    }

    private boolean findCycleMean() {
        int numberOfIterations = 0;
        boolean iterationsLimitReached = false;
        for (Graph<V, E> component : strongConnectivityAlgorithm.getStronglyConnectedComponents()) {
            if (!buildPolicyGraph(component)) {
                continue;
            }

            while (true) {
                if (++numberOfIterations > maximumIterations) {
                    iterationsLimitReached = true;
                    break;
                }
                findPolicyCycle(component, policyGraph);

                if (!computeNodeDistance(component)) {
                    break;
                }
            }

            if (isCurrentPathFound &&
                    (!isBestPathFound || currentPathWeight * bestPathSize < bestPathWeight * currentPathSize)) {
                isBestPathFound = true;
                bestPathWeight = currentPathWeight;
                bestPathSize = currentPathSize;
                bestPathVertex = currentPathVertex;
            }

            if (iterationsLimitReached) {
                break;
            }
        }

        return isBestPathFound;
    }


    private boolean buildPolicyGraph(Graph<V, E> component) {
        if (component.vertexSet().size() == 0) {
            return false;
        }
        if (component.vertexSet().size() == 1 &&
                component.incomingEdgesOf(component.vertexSet().iterator().next()).size() == 0) {
            return false;
        }

        for (V v : component.vertexSet()) {
            vertexDistance.put(v, Double.POSITIVE_INFINITY);
        }

        for (V v : component.vertexSet()) {
            for (E e : component.incomingEdgesOf(v)) {
                V u = Graphs.getOppositeVertex(component, e, v);

                double eWeight = component.getEdgeWeight(e);
                if (eWeight < vertexDistance.get(u)) {
                    vertexDistance.put(u, eWeight);
                    policyGraph.put(u, e);
                }
            }
        }

        return true;
    }

    private void findPolicyCycle(Graph<V, E> component, Map<V, E> _policy) {
        for (V v : component.vertexSet()) {
            vertexLevel.put(v, -1);
        }

        double currentWeight;
        int currentSize;
        isCurrentPathFound = false;
        int i = 0;
        for (V u : component.vertexSet()) {
            if (vertexLevel.get(u) >= 0) {
                continue;
            }

            while (vertexLevel.get(u) < 0) {
                vertexLevel.put(u, i);
                u = Graphs.getOppositeVertex(component, _policy.get(u), u);
            }

            if (vertexLevel.get(u) == i) {
                currentWeight = component.getEdgeWeight(_policy.get(u));
                currentSize = 1;

                for (V v = u; !(v = Graphs.getOppositeVertex(component, _policy.get(v), v)).equals(u); ) {
                    currentWeight += component.getEdgeWeight(_policy.get(v));
                    ++currentSize;
                }
                if (!isCurrentPathFound || (currentWeight * currentPathSize < currentPathWeight * currentSize)) {
                    isCurrentPathFound = true;
                    currentPathWeight = currentWeight;
                    currentPathSize = currentSize;
                    currentPathVertex = u;
                }
            }
            ++i;
        }
    }

    private boolean computeNodeDistance(Graph<V, E> component) {
        List<V> queue = new ArrayList<>(Collections.nCopies(graph.vertexSet().size(), null));

        for (V v : component.vertexSet()) {
            reachedVertices.put(v, false);
        }

        int queueFrontIndex = 0;
        int queueBackIndex = 0;
        queue.set(0, currentPathVertex);
        reachedVertices.put(currentPathVertex, true);
        vertexDistance.put(currentPathVertex, 0.0);

        while (queueFrontIndex <= queueBackIndex) {
            V v = queue.get(queueFrontIndex++);
            for (E e : component.incomingEdgesOf(v)) {
                V u = Graphs.getOppositeVertex(component, e, v);
                if (policyGraph.get(u).equals(e) && !reachedVertices.get(u)) {
                    reachedVertices.put(u, true);
                    vertexDistance.put(u, vertexDistance.get(v) + component.getEdgeWeight(e) * currentPathSize - currentPathWeight);
                    queue.set(++queueBackIndex, u);
                }
            }
        }

        queueFrontIndex = 0;
        while (queueBackIndex < component.vertexSet().size() - 1) {
            V v = queue.get(queueFrontIndex++);
            for (E e : component.incomingEdgesOf(v)) {
                V u = Graphs.getOppositeVertex(component, e, v);
                if (!reachedVertices.get(u)) {
                    reachedVertices.put(u, true);
                    policyGraph.put(u, e);
                    vertexDistance.put(u, vertexDistance.get(v) + component.getEdgeWeight(e) * currentPathSize - currentPathWeight);
                    ++queueBackIndex;
                    queue.set(queueBackIndex, u);
                }
            }
        }

        boolean improved = false;
        for (V v : component.vertexSet()) {
            for (E e : component.incomingEdgesOf(v)) {
                V u = Graphs.getOppositeVertex(component, e, v);

                double delta = vertexDistance.get(v) + component.getEdgeWeight(e) * currentPathSize - currentPathWeight;

                if (comparator.compare(delta, vertexDistance.get(u)) < 0) {
                    vertexDistance.put(u, delta);
                    policyGraph.put(u, e);
                    improved = true;
                }
            }
        }
        return improved;
    }

    private GraphPath<V, E> buildPath() {
        if (!isBestPathFound) {
            return null;
        }
        List<E> pathEdges = new ArrayList<>(bestPathSize);
        List<V> pathVertices = new ArrayList<>(bestPathSize + 1);

        V v = bestPathVertex;
        pathVertices.add(bestPathVertex);
        do {
            E e = policyGraph.get(v);
            v = Graphs.getOppositeVertex(graph, e, v);

            pathEdges.add(e);
            pathVertices.add(v);

        } while (!v.equals(bestPathVertex));

        return new GraphWalk<>(graph, bestPathVertex, bestPathVertex, pathVertices, pathEdges, bestPathWeight);
    }
}
