package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.ManyToManyShortestPathsAlgorithm;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;

import java.util.Set;

public abstract class BaseManyTwoManyShortestPaths<V, E> implements ManyToManyShortestPathsAlgorithm<V, E> {

    protected static <V, E> ShortestPathAlgorithm.SingleSourcePaths<V, E>
    getShortestPathsTree(Graph<V, E> graph, V source, Set<V> targets) {
        DijkstraClosestFirstIterator<V, E> iterator = new DijkstraClosestFirstIterator<>(graph, source);

        int reachedTargets = 0;
        while (iterator.hasNext() && reachedTargets < targets.size()) {
            if (targets.contains(iterator.next())) {
                ++reachedTargets;
            }
        }

        return iterator.getPaths();
    }
}
