package org.jgrapht.alg.similarity;

import org.jgrapht.Graph;
import org.jgrapht.GraphTests;
import org.jgrapht.Graphs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;

public class ZhangShashaTreeEditDistance<V, E> {

    private Graph<V, E> graph1;
    private V root1;

    private Graph<V, E> graph2;
    private V root2;

    private ToDoubleFunction<V> insertCost;
    private ToDoubleFunction<V> removeCost;
    private ToDoubleBiFunction<V, V> changeCost;

    private double[][] treeDistance;
    private List<Operation> operationsList;

    private boolean algorithmExecuted;

    public ZhangShashaTreeEditDistance(Graph<V, E> graph1, V root1, Graph<V, E> graph2, V root2) {
        this(graph1, root1, graph2, root2, v -> 1.0, v -> 1.0, (v1, v2) -> {
            if (v1.equals(v2)) {
                return 0.0;
            }
            return 1.0;
        });
    }

    public ZhangShashaTreeEditDistance(Graph<V, E> graph1, V root1, Graph<V, E> graph2, V root2,
                                       ToDoubleFunction<V> insertCost, ToDoubleFunction<V> removeCost,
                                       ToDoubleBiFunction<V, V> changeCost) {
        this.graph1 = Objects.requireNonNull(graph1, "graph1 cannot be null!");
        this.root1 = root1;
        this.graph2 = Objects.requireNonNull(graph2, "graph2 cannot be null!");
        this.root2 = root2;
        if (!GraphTests.isTree(graph1)) {
            throw new IllegalArgumentException("graph1 must be a tree!");
        }
        if (!GraphTests.isTree(graph2)) {
            throw new IllegalArgumentException("graph2 must be a tree!");
        }
        if (root1 == null) {
            if (graph1.vertexSet().size() != 0) {
                throw new IllegalArgumentException("root1 can only be null if graph1 has not vertices!");
            }
        }
        if (root2 == null) {
            if (graph2.vertexSet().size() != 0) {
                throw new IllegalArgumentException("root2 can only be null if graph2 has not vertices!");
            }
        }
        this.insertCost = Objects.requireNonNull(insertCost, "insertCost cannot be null!");
        this.removeCost = Objects.requireNonNull(removeCost, "removeCost cannot be null!");
        this.changeCost = Objects.requireNonNull(changeCost, "changeCost cannot be null!");
        int m = graph1.vertexSet().size() + 1;
        int n = graph2.vertexSet().size() + 1;
        treeDistance = new double[m][n];
    }

    public double getDistance() {
        lazyComputeEditDistance();

        int s1 = graph1.vertexSet().size();
        int s2 = graph2.vertexSet().size();
        return treeDistance[s1][s2];
    }

    public List<Operation> getOperationsList() {
        lazyComputeEditDistance();
        return operationsList;
    }

    private void lazyComputeEditDistance() {
        if (!algorithmExecuted) {
            TreeOrdering ordering1 = new TreeOrdering(graph1, root1);
            TreeOrdering ordering2 = new TreeOrdering(graph2, root2);

            for (int keyroot1 : ordering1.keyroots) {
                for (Integer keyroot2 : ordering2.keyroots) {
                    treeDistance(keyroot1, keyroot2, ordering1, ordering2);
                }
            }

            algorithmExecuted = true;
        }
    }

    private void treeDistance(int i, int j, TreeOrdering ordering1, TreeOrdering ordering2) {
        int li = ordering1.indexToLMap.get(i);
        int lj = ordering2.indexToLMap.get(j);
        int m = i - li + 2;
        int n = j - lj + 2;

        double[][] forestdist = new double[m][n];

        for (int i1 = li; i1 < i; ++i1) {
            V i1Vertex = ordering1.indexToVertexMap.get(i1);
            forestdist[i1][0] = forestdist[i1 - 1][0] + removeCost.applyAsDouble(i1Vertex);
        }
        for (int j1 = lj; j1 < j; ++j1) {
            V j1Vertex = ordering2.indexToVertexMap.get(j1);
            forestdist[0][j1] = forestdist[j1 - 1][0] + removeCost.applyAsDouble(j1Vertex);
        }

        for (int i1 = li; i1 < i; ++i1) {
            V i1Vertex = ordering1.indexToVertexMap.get(i1);
            int li1 = ordering1.indexToLMap.get(i1);
            for (int j1 = lj; j1 < j; ++j1) {
                V j1Vertex = ordering2.indexToVertexMap.get(j1);
                int lj1 = ordering2.indexToLMap.get(j1);
                if (li1 == li && lj1 == lj) {
                    double dist1 = forestdist[i1 - 1][j1] + removeCost.applyAsDouble(i1Vertex);
                    double dist2 = forestdist[i1][j1 - 1] + insertCost.applyAsDouble(j1Vertex);
                    double dist3 = forestdist[i1 - 1][j1 - 1] + changeCost.applyAsDouble(i1Vertex, j1Vertex);
                    double result = Math.min(dist1, Math.min(dist2, dist3));

                    Operation op;
                    if (result == dist1) { // remove operation
                        op = new Operation(OperationType.REMOVE, i1Vertex, null);
                    } else if (result == dist2) { // insert operation
                        op = new Operation(OperationType.INSERT, j1Vertex, null);
                    } else { // result == dist3 => change operation
                        op = new Operation(OperationType.CHANGE, i1Vertex, j1Vertex);
                    }
                    operationsList.add(op);

                    forestdist[i1][j1] = result;
                    treeDistance[i1][j1] = result;
                } else {
                    double dist1 = forestdist[i1 - 1][j1] + removeCost.applyAsDouble(i1Vertex);
                    double dist2 = forestdist[i1][j1 - 1] + insertCost.applyAsDouble(j1Vertex);
                    double dist3 = forestdist[i1 - 1][j1 - 1] + treeDistance[i1][j1];
                    forestdist[i1][j1] = Math.min(dist1, Math.min(dist2, dist3));
                }
            }
        }
    }

    private class TreeOrdering {
        final Graph<V, E> tree;
        final V treeRoot;

        List<Integer> keyroots;

        List<V> indexToVertexMap;
        List<Integer> indexToLMap;
        int currentIndex;

        public TreeOrdering(Graph<V, E> tree, V treeRoot) {
            this.tree = tree;
            this.treeRoot = treeRoot;

            int numberOfVertices = tree.vertexSet().size();
            keyroots = new ArrayList<>();
            indexToVertexMap = new ArrayList<>(Collections.nCopies(numberOfVertices + 1, null));
            indexToLMap = new ArrayList<>(Collections.nCopies(numberOfVertices + 1, null));
            currentIndex = 1;

            List<V> stack = new ArrayList<>();
            computeKeyrootsAndMapping(treeRoot, stack, true);
        }


        private int computeKeyrootsAndMapping(V v, List<V> stack, boolean isKeyroot) {
            if (treeRoot == null) {
                return -1;
            }
            stack.add(v);

            V vParent = null;
            if (stack.size() > 1) {
                vParent = stack.get(stack.size() - 2);
            }

            boolean isKeyrootArg = false;
            int lValue = -1;
            for (V vChild : Graphs.successorListOf(tree, v)) {
                if (vParent == null || vChild.equals(vParent)) {
                    int lVChild = computeKeyrootsAndMapping(vChild, stack, isKeyrootArg);
                    isKeyrootArg = true;
                    if (lValue == -1) {
                        lValue = lVChild;
                    }
                }
            }

            if (lValue == -1) { // this is a leaf
                lValue = currentIndex;
            }

            if (isKeyroot) {
                keyroots.add(currentIndex);
            }
            indexToVertexMap.set(currentIndex, v);
            indexToLMap.set(currentIndex, lValue);
            ++currentIndex;
            stack.remove(stack.size() - 1);

            return lValue;
        }
    }

    public class Operation {
        private final OperationType type;
        private final V firstOperand;
        private final V secondOperand;

        public OperationType getType() {
            return type;
        }

        public V getFirstOperand() {
            return firstOperand;
        }

        public V getSecondOperand() {
            return secondOperand;
        }

        public Operation(OperationType type, V firstOperand, V secondOperand) {
            this.type = type;
            this.firstOperand = firstOperand;
            this.secondOperand = secondOperand;
        }
    }

    public enum OperationType {
        INSERT,
        REMOVE,
        CHANGE
    }
}
