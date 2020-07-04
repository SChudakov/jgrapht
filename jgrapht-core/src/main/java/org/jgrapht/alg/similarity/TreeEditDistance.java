package org.jgrapht.alg.similarity;

import org.jgrapht.Graph;

import java.util.Objects;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;

public class TreeEditDistance<V, E> {

    private Graph<V, E> graph1;
    private V root1;

    private Graph<V, E> graph2;
    private V root2;

    private ToDoubleFunction<V> insertCost;
    private ToDoubleFunction<V> removeCost;
    private ToDoubleBiFunction<V, V> changeCost;

    public TreeEditDistance(Graph<V, E> graph1, V root1, Graph<V, E> graph2, V root2,
                            ToDoubleFunction<V> insertCost, ToDoubleFunction<V> removeCost,
                            ToDoubleBiFunction<V, V> changeCost) {
        this.graph1 = Objects.requireNonNull(graph1, "graph1 cannot be null!");
        this.root1 = Objects.requireNonNull(root1, "root1 cannot be null!");
        this.graph2 = Objects.requireNonNull(graph2, "graph2 cannot be null!");
        this.root2 = Objects.requireNonNull(root2, "root2 cannot be null");
        this.insertCost = Objects.requireNonNull(insertCost, "insertCost cannot be null!");
        this.removeCost = Objects.requireNonNull(removeCost, "removeCost cannot be null!");
        this.changeCost = Objects.requireNonNull(changeCost, "changeCost cannot be null!");
    }

    public double getDistance() {
        return 0.0;
    }

    private class AnnotatedTree {
        
    }
}
