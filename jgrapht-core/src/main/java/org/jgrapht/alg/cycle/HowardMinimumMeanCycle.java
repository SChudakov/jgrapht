package org.jgrapht.alg.cycle;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.interfaces.MinimumCycleMeanAlgorithm;
import org.jgrapht.alg.interfaces.StrongConnectivityAlgorithm;
import org.jgrapht.alg.util.Pair;
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

    boolean _curr_found;
    double _curr_cost;
    int _curr_size;
    V _curr_node;

    boolean _best_found;
    double _best_cost;
    int _best_size;
    V _best_node;

    Map<V, E> _best_policy;

    int _qback;
    int _qfront;
    List<V> _queue;

    public HowardMinimumMeanCycle(Graph<V, E> graph, StrongConnectivityAlgorithm<V, E> strongConnectivityAlgorithm, int maximumIterations) {
        this.graph = graph;
        this.strongConnectivityAlgorithm = strongConnectivityAlgorithm;
        this.maximumIterations = maximumIterations;
        this._queue = new ArrayList<>(Collections.nCopies(graph.vertexSet().size(), null));
    }

    @Override
    public double getCycleMean() {
        return getMean(getCycle());
    }

    @Override
    public GraphPath<V, E> getCycle() {
        for (Graph<V, E> scc : strongConnectivityAlgorithm.getStronglyConnectedComponents()) {
            Map<V, E> _policy = processStronglyConnectedComponent(scc);

            // Update the best cycle
            if (_curr_found && (!_best_found || _curr_cost * _best_size < _best_cost * _curr_size)) {
                _best_found = true;
                _best_cost = _curr_cost;
                _best_size = _curr_size;
                _best_node = _curr_node;
                _best_policy = _policy;
            }
        }

        return buildPath();
    }

    private double getMean(GraphPath<V, E> cycle) {
        if (cycle == null) {
            return Double.POSITIVE_INFINITY;
        }
        return cycle.getWeight() / cycle.getLength();
    }


    private Map<V, E> processStronglyConnectedComponent(Graph<V, E> scc) {
        // build policy graph
        Pair<Map<V, Double>, Map<V, E>> p = buildPolicyGraph(scc);
        Map<V, Double> _dist = p.getFirst();
        Map<V, E> _policy = p.getSecond();


        boolean improved = true;
        for (int i = 0; i < maximumIterations && improved; ++i) {
            // find policy cycle
            findPolicyCycle(scc, _policy);

            // compute node distance
            improved = computeNodeDistance(scc, _dist, _policy);
        }

        return _policy;
    }

    private Pair<Map<V, Double>, Map<V, E>> buildPolicyGraph(Graph<V, E> scc) {
        int numberOfVertices = scc.vertexSet().size();
        Map<V, Double> _dist = CollectionUtil.newHashMapWithExpectedSize(numberOfVertices);
        Map<V, E> _policy = CollectionUtil.newHashMapWithExpectedSize(numberOfVertices);

        for (V v : scc.vertexSet()) {
            _dist.put(v, Double.POSITIVE_INFINITY);
        }

        for (V v : scc.vertexSet()) {
            for (E e : scc.incomingEdgesOf(v)) {
                V u = Graphs.getOppositeVertex(scc, e, v);
                double eWeight = scc.getEdgeWeight(e);
                if (eWeight < _dist.get(u)) {
                    _dist.put(u, eWeight);
                    _policy.put(u, e);
                }
            }
        }

        return Pair.of(_dist, _policy);
    }

    private void findPolicyCycle(Graph<V, E> scc, Map<V, E> _policy) {
        int numberOfVertices = scc.vertexSet().size();
        Map<V, Integer> _level = CollectionUtil.newHashMapWithExpectedSize(numberOfVertices);
        for (V v : scc.vertexSet()) {
            _level.put(v, -1);
        }

        double ccost;
        int csize;

        _curr_found = false;
        int i = 0;
        for (V u : scc.vertexSet()) {
            if (_level.get(u) >= 0) {
                continue;
            }

            while (_level.get(u) < 0) {
                _level.put(u, i);
                u = Graphs.getOppositeVertex(scc, _policy.get(u), u);
            }

            if (_level.get(u) == i) {
                // A cycle is found
                ccost = scc.getEdgeWeight(_policy.get(u));
                csize = 1;

                for (V v = u; !(v = Graphs.getOppositeVertex(scc, _policy.get(v), v)).equals(u); ) {
                    ccost += scc.getEdgeWeight(_policy.get(v));
                    ++csize;
                }
                if (!_curr_found || (ccost * _curr_size < _curr_cost * csize)) {
                    _curr_found = true;
                    _curr_cost = ccost;
                    _curr_size = csize;
                    _curr_node = u;
                }
            }
            ++i;
        }
    }

    private boolean computeNodeDistance(Graph<V, E> scc, Map<V, Double> _dist, Map<V, E> _policy) {
        int numberOfVertices = scc.vertexSet().size();

        // Find the component of the main cycle and compute node distances using reverse BFS
        Map<V, Boolean> _reached = CollectionUtil.newHashMapWithExpectedSize(numberOfVertices);
        for (V v : scc.vertexSet()) {
            _reached.put(v, false);
        }

        _qfront = 0;
        _qback = 0;
        _queue.set(0, _curr_node);
        _reached.put(_curr_node, true);
        _dist.put(_curr_node, 0.0);

        while (_qfront <= _qback) {
            V v = _queue.get(_qfront);
            ++_qfront;
            for (E e : scc.incomingEdgesOf(v)) {
                V u = Graphs.getOppositeVertex(scc, e, v);
                if (_policy.get(u).equals(e) && !_reached.get(u)) {
                    _reached.put(u, true);
                    _dist.put(u, _dist.get(v) + scc.getEdgeWeight(e) * _curr_size - _curr_cost);
                    ++_qback;
                    _queue.set(_qback, u);
                }
            }
        }

        // Connect all other nodes to this component and compute node distances using reverse BFS
        _qfront = 0;
        while (_qback < numberOfVertices - 1) {
            V v = _queue.get(_qfront++);
            for (E e : scc.incomingEdgesOf(v)) {
                V u = Graphs.getOppositeVertex(scc, e, v);
                if (!_reached.get(u)) {
                    _reached.put(u, true);
                    _policy.put(u, e);
                    _dist.put(u, _dist.get(v) + scc.getEdgeWeight(e) * _curr_size - _curr_cost);
                    ++_qback;
                    _queue.set(_qback, u);
                }
            }
        }

        // Improve node distances
        boolean improved = false;
        for (V v : scc.vertexSet()) {
            for (E e : scc.incomingEdgesOf(v)) {
                V u = Graphs.getOppositeVertex(scc, e, v);
                double delta = _dist.get(v) + scc.getEdgeWeight(e) * _curr_size - _curr_cost;

                if (delta < _dist.get(u)) {
                    _dist.put(u, delta);
                    _policy.put(u, e);
                    improved = true;
                }
            }
        }
        return improved;
    }

    private GraphPath<V, E> buildPath() {
        if (!_best_found) {
            return null;
        }
        List<E> pathEdges = new ArrayList<>(_best_size);
        List<V> pathVertices = new ArrayList<>(_best_size + 1);

        V v = _best_node;
        pathVertices.add(_best_node);
        do {
            E e = _best_policy.get(v);
            v = Graphs.getOppositeVertex(graph, e, v);

            pathEdges.add(e);
            pathVertices.add(v);

        } while (!v.equals(_best_node));

        return new GraphWalk<>(graph, _best_node, _best_node, pathVertices, pathEdges, _best_cost);
    }
}
