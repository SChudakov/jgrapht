package org.jgrapht.alg.interfaces;

import org.jgrapht.GraphPath;

import java.util.List;

public interface ManyToManyShortestPathsAlgorithm<V, E> {

    ManyToManyShortestPaths<V, E> getManyTwoManyPaths(List<V> sources, List<V> targets);

    interface ManyToManyShortestPaths<V, E> {

        GraphPath<V, E> getPath(V source, V target);

        double getWeight(V source, V target);
    }
}
