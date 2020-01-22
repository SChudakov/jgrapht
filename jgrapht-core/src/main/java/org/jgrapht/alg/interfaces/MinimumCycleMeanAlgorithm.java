package org.jgrapht.alg.interfaces;

import org.jgrapht.GraphPath;

public interface MinimumCycleMeanAlgorithm<V, E> {
    double getCycleMean();

    GraphPath<V, E> getCycle();
}
