package org.jgrapht.alg.similarity;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.jgrapht.alg.similarity.ZhangShashaTreeEditDistance.Operation;
import static org.jgrapht.alg.similarity.ZhangShashaTreeEditDistance.OperationType;
import static org.junit.Assert.assertEquals;

public class ZhangShashaTreeEditDistanceTest {
    int[][] articleTree1 = {{1, 2}, {1, 3}, {2, 4}, {2, 5}, {5, 6}};
    int[][] articleTree2 = {{1, 5}, {1, 3}, {5, 2}, {2, 4}, {2, 6}};
    int[][] tree1 = {{1, 2}, {1, 4}, {2, 5}, {3, 6}, {4, 10}, {5, 6}, {5, 7}, {5, 9}, {7, 8}, {10, 4}};
    int[][] tree2 = {{0, 1}, {0, 3}, {1, 4}, {1, 0}, {2, 5}, {3, 0}, {3, 9}, {4, 5}, {4, 6}, {4, 8}, {4, 1}, {5, 2}, {5, 4}, {6, 7}, {6, 4}, {7, 6}, {8, 4}, {9, 3}};
    int[][] tree3 = {{0, 3}, {0, 6}, {0, 4}, {0, 9}, {1, 8}, {1, 2}, {2, 5}, {2, 1}, {2, 4}, {3, 0}, {4, 7}, {4, 2}, {4, 0}, {5, 2}, {6, 0}, {7, 4}, {8, 1}, {9, 0}};


    private static void testOnTrees(Graph<Integer, DefaultEdge> tree1, int root1,
                                    Graph<Integer, DefaultEdge> tree2, int root2,
                                    double expectedDistance, Set<Operation<Integer>> expectedOperations) {
        ZhangShashaTreeEditDistance<Integer, DefaultEdge> treeEditDistance
                = new ZhangShashaTreeEditDistance<>(tree1, root1, tree2, root2);

        double distance = treeEditDistance.getDistance();
        List<Operation<Integer>> actualOperations = treeEditDistance.getOperationLists();

        assertEquals(expectedDistance, distance, 1e-9);
        assertEquals(expectedOperations, new HashSet<>(actualOperations));
    }

    protected static Graph<Integer, DefaultEdge> readGraph(int[][] representation) {
        Graph<Integer, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);
        for (int[] ints : representation) {
            Graphs.addEdgeWithVertices(graph, ints[0], ints[1]);
        }
        return graph;
    }

    @Test
    public void testTED_treeWithOneVertex_to_treeWithOneVertex() {
        Set<Operation<Integer>> expectedOperations = Collections.singleton(
                new Operation<>(OperationType.CHANGE, 1, 1));
        testOnTrees(getGraphWithOneVertex(), 1, getGraphWithOneVertex(), 1, 0.0, expectedOperations);
    }

    @Test
    public void testTED_treeWithOneVertex_to_articleTree2() {
        Set<Operation<Integer>> expectedOperations = new HashSet<>(
                Arrays.asList(
                        new Operation<>(OperationType.CHANGE, 1, 1),
                        new Operation<>(OperationType.INSERT, 2, null),
                        new Operation<>(OperationType.INSERT, 3, null),
                        new Operation<>(OperationType.INSERT, 4, null),
                        new Operation<>(OperationType.INSERT, 5, null),
                        new Operation<>(OperationType.INSERT, 6, null)
                )
        );
        testOnTrees(getGraphWithOneVertex(), 1, readGraph(articleTree2), 1, 5.0, expectedOperations);
    }

    @Test
    public void testTED_articleTree1_to_treeWithOneVertex() {
        Set<Operation<Integer>> expectedOperations = new HashSet<>(
                Arrays.asList(
                        new Operation<>(OperationType.CHANGE, 1, 1),
                        new Operation<>(OperationType.REMOVE, 2, null),
                        new Operation<>(OperationType.REMOVE, 3, null),
                        new Operation<>(OperationType.REMOVE, 4, null),
                        new Operation<>(OperationType.REMOVE, 5, null),
                        new Operation<>(OperationType.REMOVE, 6, null)
                )
        );
        testOnTrees(readGraph(articleTree1), 1, getGraphWithOneVertex(), 1, 5.0, expectedOperations);
    }

    @Test
    public void testTED_articleTree1_to_articleTree2() {
        Set<Operation<Integer>> expectedOperations = new HashSet<>(
                Arrays.asList(
                        new Operation<>(OperationType.REMOVE, 5, null),
                        new Operation<>(OperationType.INSERT, 5, null),
                        new Operation<>(OperationType.CHANGE, 1, 1),
                        new Operation<>(OperationType.CHANGE, 2, 2),
                        new Operation<>(OperationType.CHANGE, 3, 3),
                        new Operation<>(OperationType.CHANGE, 4, 4),
                        new Operation<>(OperationType.CHANGE, 6, 6)
                )
        );
        testOnTrees(readGraph(articleTree1), 1, readGraph(articleTree2), 1, 2.0, expectedOperations);
    }

    @Test
    public void testTED_tree1_to_articleTree2() {
        Set<Operation<Integer>> expectedOperations = new HashSet<>(
                Arrays.asList(
                        new Operation<>(OperationType.CHANGE, 1, 1),
                        new Operation<>(OperationType.REMOVE, 4, null),
                        new Operation<>(OperationType.CHANGE, 10, 3),
                        new Operation<>(OperationType.REMOVE, 2, null),
                        new Operation<>(OperationType.CHANGE, 5, 5),
                        new Operation<>(OperationType.REMOVE, 9, null),
                        new Operation<>(OperationType.REMOVE, 7, null),
                        new Operation<>(OperationType.REMOVE, 8, null),
                        new Operation<>(OperationType.INSERT, 2, null),
                        new Operation<>(OperationType.CHANGE, 6, 6),
                        new Operation<>(OperationType.REMOVE, 3, null),
                        new Operation<>(OperationType.INSERT, 4, null)
                )
        );
        testOnTrees(readGraph(tree1), 1, readGraph(articleTree2), 1, 9.0, expectedOperations);
    }

    @Test
    public void testTED_articleTree1_to_tree1() {
        Set<Operation<Integer>> expectedOperations = new HashSet<>(
                Arrays.asList(
                        new Operation<>(OperationType.CHANGE, 1, 1),
                        new Operation<>(OperationType.INSERT, 4, null),
                        new Operation<>(OperationType.CHANGE, 3, 10),
                        new Operation<>(OperationType.CHANGE, 2, 2),
                        new Operation<>(OperationType.CHANGE, 5, 5),
                        new Operation<>(OperationType.INSERT, 9, null),
                        new Operation<>(OperationType.INSERT, 7, null),
                        new Operation<>(OperationType.INSERT, 8, null),
                        new Operation<>(OperationType.CHANGE, 6, 6),
                        new Operation<>(OperationType.INSERT, 3, null),
                        new Operation<>(OperationType.REMOVE, 4, null)
                )
        );
        testOnTrees(readGraph(articleTree1), 1, readGraph(tree1), 1, 7.0, expectedOperations);
    }

    @Test
    public void testTED_tree2_to_tree3() {
        Set<Operation<Integer>> expectedOperations = new HashSet<>(
                Arrays.asList(
                        new Operation<>(OperationType.CHANGE, 0, 0),
                        new Operation<>(OperationType.REMOVE, 3, null),
                        new Operation<>(OperationType.CHANGE, 9, 9),
                        new Operation<>(OperationType.REMOVE, 1, null),
                        new Operation<>(OperationType.CHANGE, 4, 4),
                        new Operation<>(OperationType.REMOVE, 8, null),
                        new Operation<>(OperationType.REMOVE, 6, null),
                        new Operation<>(OperationType.CHANGE, 7, 7),
                        new Operation<>(OperationType.REMOVE, 5, null),
                        new Operation<>(OperationType.CHANGE, 2, 2),
                        new Operation<>(OperationType.INSERT, 5, null),
                        new Operation<>(OperationType.INSERT, 1, null),
                        new Operation<>(OperationType.INSERT, 8, null),
                        new Operation<>(OperationType.INSERT, 6, null),
                        new Operation<>(OperationType.INSERT, 3, null)
                )
        );
        testOnTrees(readGraph(tree2), 0, readGraph(tree3), 0, 10.0, expectedOperations);
    }

    private static Graph<Integer, DefaultEdge> getGraphWithOneVertex() {
        Graph<Integer, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);
        graph.addVertex(1);
        return graph;
    }
}
