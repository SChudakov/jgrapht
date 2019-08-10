package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class DefaultManyTwoManyShortestPaths<V, E> extends BaseManyTwoManyShortestPaths<V, E> {
    private final Graph<V, E> graph;

    public DefaultManyTwoManyShortestPaths(Graph<V, E> graph) {
        this.graph = graph;
    }

    @Override
    public ManyToManyShortestPaths<V, E> getManyTwoManyPaths(List<V> sources, List<V> targets) {
        Objects.requireNonNull(sources, "sources cannot be null!");
        Objects.requireNonNull(targets, "targets cannot be null!");

        Map<V, ShortestPathAlgorithm.SingleSourcePaths<V, E>> searchSpaces = new HashMap<>();

        Set<V> targetsSet = new HashSet<>(targets);

        for (V source : sources) {
            searchSpaces.put(source, getShortestPathsTree(graph, source, targetsSet));
        }

        return new DefaultManyToManyShortestPathsImpl(searchSpaces);
    }

    private class DefaultManyToManyShortestPathsImpl implements ManyToManyShortestPaths<V, E> {

        private final Map<V, ShortestPathAlgorithm.SingleSourcePaths<V, E>> searchSpaces;

        private DefaultManyToManyShortestPathsImpl(Map<V, ShortestPathAlgorithm.SingleSourcePaths<V, E>> searchSpaces) {
            this.searchSpaces = searchSpaces;
        }

        @Override
        public GraphPath<V, E> getPath(V source, V target) {
            return searchSpaces.get(source).getPath(target);
        }

        @Override
        public double getWeight(V source, V target) {
            return searchSpaces.get(source).getWeight(target);
        }
    }
}
