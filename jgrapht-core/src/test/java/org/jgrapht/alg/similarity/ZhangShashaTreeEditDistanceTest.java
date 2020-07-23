package org.jgrapht.alg.similarity;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ZhangShashaTreeEditDistanceTest {
    int[][] articleTree1 = new int[][]{{1, 2}, {1, 3}, {2, 4}, {2, 5}, {5, 6}};
    int[][] articleTree2 = new int[][]{{1, 5}, {1, 3}, {5, 2}, {2, 4}, {2, 6}};
    int[][] generatedTree = new int[][]{{1, 2}, {1, 4}, {2, 5}, {2, 1}, {3, 6}, {4, 1}, {4, 10}, {5, 6}, {5, 7}, {5, 9}, {5, 2}, {6, 3}, {6, 5}, {7, 8}, {7, 5}, {8, 7}, {9, 5}, {10, 4}};

    private static void testOnTrees(Graph<Integer, DefaultEdge> tree1, int root1,
                                    Graph<Integer, DefaultEdge> tree2, int root2,
                                    double expectedDistance) {
        ZhangShashaTreeEditDistance<Integer, DefaultEdge> treeEditDistance
                = new ZhangShashaTreeEditDistance<>(tree1, root1, tree2, root2);
        double distance = treeEditDistance.getDistance();

        assertEquals(expectedDistance, distance, 1e-9);
    }

    protected static Graph<Integer, DefaultEdge> readGraph(int[][] representation) {
        Graph<Integer, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);
        for (int[] ints : representation) {
            Graphs.addEdgeWithVertices(graph, ints[0], ints[1]);
        }
        return graph;
    }

    @Test
    public void testTED_BothTreesAreEmpty() {
        testOnTrees(getGraphWithOneVertex(), 1, getGraphWithOneVertex(), 1, 0.0);
    }

    @Test
    public void testTED_FirstTreeIsEmpty() {
        testOnTrees(getGraphWithOneVertex(), 1, readGraph(articleTree1), 1, 5.0);
    }

    @Test
    public void testTED_SecondTreeIsEmpty() {
        testOnTrees(readGraph(articleTree1), 1, getGraphWithOneVertex(), 1, 5.0);
    }

    @Test
    public void testTED_TreesOfEqualSize() {
        testOnTrees(readGraph(articleTree1), 1, readGraph(articleTree2), 1, 2.0);
    }

    @Test
    public void testTED_FirstTreeLarger() {
        testOnTrees(readGraph(generatedTree), 1, readGraph(articleTree2), 1, 9.0);
    }

    @Test
    public void testTED_SecondTreeLarger() {
        testOnTrees(readGraph(articleTree1), 1, readGraph(generatedTree), 1, 9.0);
    }


    private static Graph<Integer, DefaultEdge> getGraphWithOneVertex() {
        Graph<Integer, DefaultEdge> graph = new SimpleGraph<>(DefaultEdge.class);
        graph.addVertex(1);
        return graph;
    }


}
