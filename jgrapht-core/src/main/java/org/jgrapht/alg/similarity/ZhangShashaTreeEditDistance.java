package org.jgrapht.alg.similarity;

import org.jgrapht.Graph;
import org.jgrapht.GraphTests;
import org.jgrapht.Graphs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
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
    private List<List<List<Operation<V>>>> operationLists;

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
        this.root1 = Objects.requireNonNull(root1, "root1 cannot be null!");
        this.graph2 = Objects.requireNonNull(graph2, "graph2 cannot be null!");
        this.root2 = Objects.requireNonNull(root2, "root2 cannot be null!");
        this.insertCost = Objects.requireNonNull(insertCost, "insertCost cannot be null!");
        this.removeCost = Objects.requireNonNull(removeCost, "removeCost cannot be null!");
        this.changeCost = Objects.requireNonNull(changeCost, "changeCost cannot be null!");
        if (!GraphTests.isTree(graph1)) {
            throw new IllegalArgumentException("graph1 must be a tree!");
        }
        if (!GraphTests.isTree(graph2)) {
            throw new IllegalArgumentException("graph2 must be a tree!");
        }

        int m = graph1.vertexSet().size();
        int n = graph2.vertexSet().size();
        treeDistance = new double[m][n];
        operationLists = new ArrayList<>(m);
        for (int i = 0; i < m; ++i) {
            operationLists.add(new ArrayList<>(Collections.nCopies(n, null)));
        }
    }

    public double getDistance() {
        lazyComputeEditDistance();
        int m = graph1.vertexSet().size();
        int n = graph2.vertexSet().size();
        return treeDistance[m - 1][n - 1];
    }

    public List<Operation<V>> getOperationLists() {
        lazyComputeEditDistance();
        int m = graph1.vertexSet().size();
        int n = graph2.vertexSet().size();
        return Collections.unmodifiableList(operationLists.get(m - 1).get(n - 1));
    }

    private void lazyComputeEditDistance() {
        if (!algorithmExecuted) {
            TreeOrdering ordering1 = new TreeOrdering(graph1, root1);
            TreeOrdering ordering2 = new TreeOrdering(graph2, root2);

            for (int keyroot1 : ordering1.keyRoots) {
                for (Integer keyroot2 : ordering2.keyRoots) {
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
        List<List<CacheEntry>> cachedOperations = new ArrayList<>(m);
        for (int k = 0; k < m; ++k) {
            cachedOperations.add(new ArrayList<>(Collections.nCopies(n, null)));
        }

        int iOffset = li - 1;
        int jOffset = lj - 1;

        for (int i1 = li; i1 <= i; ++i1) {
            V i1Vertex = ordering1.indexToVertexMap.get(i1);
            int iIndex = i1 - iOffset;
            forestdist[iIndex][0] = forestdist[iIndex - 1][0] + removeCost.applyAsDouble(i1Vertex);
            CacheEntry entry = new CacheEntry(iIndex - 1, 0, new Operation<>(OperationType.REMOVE, i1Vertex, null));
            cachedOperations.get(iIndex).set(0, entry);
        }
        for (int j1 = lj; j1 <= j; ++j1) {
            V j1Vertex = ordering2.indexToVertexMap.get(j1);
            int jIndex = j1 - jOffset;
            forestdist[0][jIndex] = forestdist[0][jIndex - 1] + removeCost.applyAsDouble(j1Vertex);
            CacheEntry entry = new CacheEntry(0, jIndex - 1, new Operation<>(OperationType.INSERT, j1Vertex, null));
            cachedOperations.get(0).set(jIndex, entry);
        }

        for (int i1 = li; i1 <= i; ++i1) {
            V i1Vertex = ordering1.indexToVertexMap.get(i1);
            int li1 = ordering1.indexToLMap.get(i1);

            for (int j1 = lj; j1 <= j; ++j1) {
                V j1Vertex = ordering2.indexToVertexMap.get(j1);
                int lj1 = ordering2.indexToLMap.get(j1);

                int iIndex = i1 - iOffset;
                int jIndex = j1 - jOffset;
                if (li1 == li && lj1 == lj) {
                    double dist1 = forestdist[iIndex - 1][jIndex] + removeCost.applyAsDouble(i1Vertex);
                    double dist2 = forestdist[iIndex][jIndex - 1] + insertCost.applyAsDouble(j1Vertex);
                    double dist3 = forestdist[iIndex - 1][jIndex - 1] + changeCost.applyAsDouble(i1Vertex, j1Vertex);
                    double result = Math.min(dist1, Math.min(dist2, dist3));

                    CacheEntry entry;
                    if (result == dist1) { // remove operation
                        entry = new CacheEntry(iIndex - 1, jIndex, new Operation<>(OperationType.REMOVE, i1Vertex, null));
                    } else if (result == dist2) { // insert operation
                        entry = new CacheEntry(iIndex, jIndex - 1, new Operation<>(OperationType.INSERT, j1Vertex, null));
                    } else { // result == dist3 => change operation
                        entry = new CacheEntry(iIndex - 1, jIndex - 1, new Operation<>(OperationType.CHANGE, i1Vertex, j1Vertex));
                    }
                    cachedOperations.get(iIndex).set(jIndex, entry);

                    forestdist[iIndex][jIndex] = result;
                    treeDistance[i1 - 1][j1 - 1] = result;
                    operationLists.get(i1 - 1).set(j1 - 1, restoreOperationsList(cachedOperations, iIndex, jIndex));
                } else {
                    int i2 = li1 - 1 - iOffset;
                    int j2 = lj1 - 1 - jOffset;
                    double dist1 = forestdist[iIndex - 1][jIndex] + removeCost.applyAsDouble(i1Vertex);
                    double dist2 = forestdist[iIndex][jIndex - 1] + insertCost.applyAsDouble(j1Vertex);
                    double dist3 = forestdist[i2][j2] + treeDistance[i1 - 1][j1 - 1];
                    double result = Math.min(dist1, Math.min(dist2, dist3));
                    forestdist[iIndex][jIndex] = result;

                    CacheEntry entry;
                    if (result == dist1) {
                        entry = new CacheEntry(iIndex - 1, jIndex, new Operation<>(OperationType.REMOVE, i1Vertex, null));
                    } else if (result == dist2) {
                        entry = new CacheEntry(iIndex, jIndex - 1, new Operation<>(OperationType.INSERT, j1Vertex, null));
                    } else {
                        entry = new CacheEntry(i2, j2, null);
                        entry.treeDistanceI = i1 - 1;
                        entry.treeDistanceJ = j1 - 1;
                    }
                    cachedOperations.get(iIndex).set(jIndex, entry);
                }
            }
        }
    }

    private List<Operation<V>> restoreOperationsList(List<List<CacheEntry>> cachedOperations, int i, int j) {
        List<Operation<V>> result = new ArrayList<>();

        CacheEntry it = cachedOperations.get(i).get(j);
        while (it != null) {
            if (it.operation == null) {
                result.addAll(operationLists.get(it.treeDistanceI).get(it.treeDistanceJ));
            } else {
                result.add(it.operation);
            }
            it = cachedOperations.get(it.cachePreviousPosI).get(it.cachePreviousPosJ);
        }

        return result;
    }

    private class TreeOrdering {
        final Graph<V, E> tree;
        final V treeRoot;

        List<Integer> keyRoots;

        List<V> indexToVertexMap;
        List<Integer> indexToLMap;
        int currentIndex;

        public TreeOrdering(Graph<V, E> tree, V treeRoot) {
            this.tree = tree;
            this.treeRoot = treeRoot;

            int numberOfVertices = tree.vertexSet().size();
            keyRoots = new ArrayList<>();
            indexToVertexMap = new ArrayList<>(Collections.nCopies(numberOfVertices + 1, null));
            indexToLMap = new ArrayList<>(Collections.nCopies(numberOfVertices + 1, null));
            currentIndex = 1;

            computeKeyRootsAndMapping(treeRoot);
        }

        private class StackEntry {
            V v;
            boolean isKeyRoot;

            V vParent;
            boolean isKeyRootArg;
            int lValue;
            Iterator<V> vChildIterator;
            V vChild;
            int lVChild;

            int state;

            public StackEntry(V v, boolean isKeyRoot) {
                this.v = v;
                this.isKeyRoot = isKeyRoot;
                this.lValue = -1;
            }
        }

        private void computeKeyRootsAndMapping(V treeRoot) {
            List<StackEntry> stack = new ArrayList<>();
            stack.add(new StackEntry(treeRoot, true));

            while (!stack.isEmpty()) {
                StackEntry entry = stack.get(stack.size() - 1);
                if (entry.state == 0) {
                    if (stack.size() > 1) {
                        entry.vParent = stack.get(stack.size() - 2).v;
                    }
                    entry.vChildIterator = Graphs.successorListOf(tree, entry.v).iterator();
                    entry.state = 1;
                } else if (entry.state == 1) {
                    if (entry.vChildIterator.hasNext()) {
                        entry.vChild = entry.vChildIterator.next();
                        if (entry.vParent == null || !entry.vChild.equals(entry.vParent)) {
                            stack.add(new StackEntry(entry.vChild, entry.isKeyRootArg));
                            entry.state = 2;
                        }
                    } else {
                        entry.state = 3;
                    }
                } else if (entry.state == 2) {
                    entry.isKeyRootArg = true;
                    if (entry.lValue == -1) {
                        entry.lValue = entry.lVChild;
                    }
                    entry.state = 1;
                } else if (entry.state == 3) {
                    if (entry.lValue == -1) {
                        entry.lValue = currentIndex;
                    }
                    if (entry.isKeyRoot) {
                        keyRoots.add(currentIndex);
                    }
                    indexToVertexMap.set(currentIndex, entry.v);
                    indexToLMap.set(currentIndex, entry.lValue);
                    ++currentIndex;
                    if (stack.size() > 1) {
                        stack.get(stack.size() - 2).lVChild = entry.lValue;
                    }
                    stack.remove(stack.size() - 1);
                }
            }
        }
    }

    public static class Operation<V> {
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Operation<?> operation = (Operation<?>) o;

            if (type != operation.type) return false;
            if (!firstOperand.equals(operation.firstOperand)) return false;
            return secondOperand != null ? secondOperand.equals(operation.secondOperand) : operation.secondOperand == null;
        }

        @Override
        public int hashCode() {
            int result = type.hashCode();
            result = 31 * result + firstOperand.hashCode();
            result = 31 * result + (secondOperand != null ? secondOperand.hashCode() : 0);
            return result;
        }

        @Override
        public String toString() {
            if (type.equals(OperationType.INSERT) || type.equals(OperationType.REMOVE)) {
                return type + " " + firstOperand;
            }
            return type + " " + firstOperand + " -> " + secondOperand;
        }
    }

    public enum OperationType {
        INSERT,
        REMOVE,
        CHANGE
    }

    private class CacheEntry {
        int cachePreviousPosI;
        int cachePreviousPosJ;
        Operation<V> operation;
        int treeDistanceI;
        int treeDistanceJ;

        public CacheEntry(int cachePreviousPosI, int cachePreviousPosJ, Operation<V> operation) {
            this.cachePreviousPosI = cachePreviousPosI;
            this.cachePreviousPosJ = cachePreviousPosJ;
            this.operation = operation;
        }
    }
}
