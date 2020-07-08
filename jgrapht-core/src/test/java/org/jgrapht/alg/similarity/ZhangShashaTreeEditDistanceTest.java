package org.jgrapht.alg.similarity;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ZhangShashaTreeEditDistanceTest {
    int[][] articleTree1 = new int[][]{{1, 2}, {1, 3}, {2, 4}, {2, 5}, {5, 6}};
    int[][] articleTree2 = new int[][]{{1, 2}, {1, 3}, {2, 4}, {4, 5}, {4, 6}};
    int[][] generatedTree = new int[][]{{1, 2}, {1, 4}, {2, 5}, {2, 1}, {3, 6}, {4, 1}, {4, 10}, {5, 6}, {5, 7}, {5, 9}, {5, 2}, {6, 3}, {6, 5}, {7, 8}, {7, 5}, {8, 7}, {9, 5}, {10, 4}};

    @Test
    public void testTED_BothTreesAreEmpty() {
        Graph<Integer, DefaultEdge> graph1 = new SimpleGraph<>(DefaultEdge.class);
        Graph<Integer, DefaultEdge> graph2 = new SimpleGraph<>(DefaultEdge.class);
        ZhangShashaTreeEditDistance<Integer, DefaultEdge> treeEditDistance
                = new ZhangShashaTreeEditDistance<>(graph1, null, graph2, null);
        double distance = treeEditDistance.getDistance();
        assertEquals(0.0, distance, 1e-9);
    }

    @Test
    public void testTED_FirstTreeIsEmpty() {
        Graph<Integer, DefaultEdge> graph1 = new SimpleGraph<>(DefaultEdge.class);
        Graph<Integer, DefaultEdge> graph2 = new SimpleGraph<>(DefaultEdge.class);
        readGraph(graph2, articleTree1);
        ZhangShashaTreeEditDistance<Integer, DefaultEdge> treeEditDistance
                = new ZhangShashaTreeEditDistance<>(graph1, null, graph2, 1);
        double distance = treeEditDistance.getDistance();
        assertEquals(6.0, distance, 1e-9);
    }

    @Test
    public void testTED_SecondTreeIsEmpty() {
        Graph<Integer, DefaultEdge> graph1 = new SimpleGraph<>(DefaultEdge.class);
        readGraph(graph1, articleTree1);
        Graph<Integer, DefaultEdge> graph2 = new SimpleGraph<>(DefaultEdge.class);
        ZhangShashaTreeEditDistance<Integer, DefaultEdge> treeEditDistance
                = new ZhangShashaTreeEditDistance<>(graph1, 1, graph2, null);
        double distance = treeEditDistance.getDistance();
        assertEquals(6.0, distance, 1e-9);
    }

    @Test
    public void testTED_TreesOfEqualSize() {
        Graph<Integer, DefaultEdge> graph1 = new SimpleGraph<>(DefaultEdge.class);
        readGraph(graph1, articleTree1);
        Graph<Integer, DefaultEdge> graph2 = new SimpleGraph<>(DefaultEdge.class);
        readGraph(graph2, articleTree2);
        ZhangShashaTreeEditDistance<Integer, DefaultEdge> treeEditDistance
                = new ZhangShashaTreeEditDistance<>(graph1, 1, graph2, 1);
        double distance = treeEditDistance.getDistance();
        assertEquals(2.0, distance, 1e-9);
    }

    @Test
    public void testTED_FirstTreeLarger() {
        Graph<Integer, DefaultEdge> graph1 = new SimpleGraph<>(DefaultEdge.class);
        readGraph(graph1, generatedTree);
        Graph<Integer, DefaultEdge> graph2 = new SimpleGraph<>(DefaultEdge.class);
        readGraph(graph2, articleTree2);
        ZhangShashaTreeEditDistance<Integer, DefaultEdge> treeEditDistance
                = new ZhangShashaTreeEditDistance<>(graph1, 1, graph2, 1);
        double distance = treeEditDistance.getDistance();
        assertEquals(2.0, distance, 1e-9);
    }

    @Test
    public void testTED_SecondTreeLarger() {
        Graph<Integer, DefaultEdge> graph1 = new SimpleGraph<>(DefaultEdge.class);
        readGraph(graph1, articleTree1);
        Graph<Integer, DefaultEdge> graph2 = new SimpleGraph<>(DefaultEdge.class);
        readGraph(graph2, generatedTree);
        ZhangShashaTreeEditDistance<Integer, DefaultEdge> treeEditDistance
                = new ZhangShashaTreeEditDistance<>(graph1, 1, graph2, 1);
        double distance = treeEditDistance.getDistance();
        assertEquals(2.0, distance, 1e-9);
    }

    protected static void readGraph(Graph<Integer, DefaultEdge> graph, int[][] representation) {
        for (int[] ints : representation) {
            Graphs.addEdgeWithVertices(graph, ints[0], ints[1]);
        }
    }
}
