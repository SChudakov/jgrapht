package org.jgrapht.alg.shortestpath;

import org.jgrapht.GraphPath;

import java.util.Iterator;

public class KShortestSimplePathIterator<V, E>
        implements Iterator<GraphPath<V, E>> {


    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public GraphPath<V, E> next() {
        return null;
    }
}
