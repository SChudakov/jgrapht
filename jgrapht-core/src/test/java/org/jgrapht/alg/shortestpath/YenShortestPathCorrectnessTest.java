package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.generate.GnpRandomGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.util.SupplierUtil;
import org.junit.Test;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class YenShortestPathCorrectnessTest {

    @Test
    public void testCorrectness() {
        int n = 100;
        double p = 0.5;
        for (int i = 0; i < 1000; i++) {
            System.out.println(i);
            SimpleDirectedWeightedGraph<Integer, DefaultWeightedEdge> graph
                    = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
            graph.setVertexSupplier(SupplierUtil.createIntegerSupplier());
            getRandomGraph(graph, n, p);
            Integer source = (int) (Math.random() * n);
            Integer target = (int) (Math.random() * n);
            assertCorrectness(graph, source, target);
        }
    }


    private void assertCorrectness(Graph<Integer, DefaultWeightedEdge> graph,
                                   Integer source, Integer target) {
        int amountOfPathsToIterate = 10;

        YenShortestPathIterator<Integer, DefaultWeightedEdge> it1
                = new YenShortestPathIterator<>(graph, source, target);
        YenOldShortestPathIterator<Integer, DefaultWeightedEdge> it2
                = new YenOldShortestPathIterator<>(graph, source, target);

        GraphPath<Integer, DefaultWeightedEdge> newVersionPath;
        GraphPath<Integer, DefaultWeightedEdge> oldVersionPath;

        double previousPathWeight = 0.0;
        Set<GraphPath<Integer, DefaultWeightedEdge>> paths = new HashSet<>();
        int i = 0;
        for (; i < amountOfPathsToIterate && it1.hasNext() && it2.hasNext(); i++) {
            newVersionPath = it1.next();
            oldVersionPath = it2.next();

            assertEquals(newVersionPath, oldVersionPath);

            paths.add(newVersionPath);
            assertCorrectPath(graph, newVersionPath, source, target);
            assertTrue(previousPathWeight <= newVersionPath.getWeight());
            previousPathWeight = newVersionPath.getWeight();
        }
        assertEquals(it1.hasNext(), it2.hasNext());
        assertEquals(i, paths.size());
    }

    private void assertCorrectPath(Graph<Integer, DefaultWeightedEdge> graph,
                                   GraphPath<Integer, DefaultWeightedEdge> path,
                                   Integer source, Integer target) {
        List<DefaultWeightedEdge> edgeList = path.getEdgeList();

        double expectedWeight = path.getWeight();
        double actualWeight = edgeList.stream().mapToDouble(graph::getEdgeWeight).sum();
        assertEquals(expectedWeight, actualWeight, 1e-9);

        if (edgeList.size() == 0) {
            assertEquals(source, target);
        } else {
            assertEquals(graph.getEdgeSource(edgeList.get(0)), source);
            assertEquals(graph.getEdgeTarget(edgeList.get(edgeList.size() - 1)), target);

            if (edgeList.size() >= 2) {
                Iterator<DefaultWeightedEdge> it = edgeList.iterator();
                DefaultWeightedEdge curr = it.next();
                DefaultWeightedEdge next;
                while (it.hasNext()) {
                    next = it.next();
                    assertEquals(graph.getEdgeTarget(curr), graph.getEdgeSource(next));
                    curr = next;
                }
            }
        }
    }

    private void assertLooplessPath(GraphPath<Integer, DefaultWeightedEdge> path) {
        Set<Integer> uniqueVertices = new HashSet<>(path.getVertexList());
        assertEquals(path.getVertexList().size(), uniqueVertices.size());
    }

    private void getRandomGraph(Graph<Integer, DefaultWeightedEdge> graph, int n, double p) {
        GraphGenerator<Integer, DefaultWeightedEdge, Integer> generator
                = new GnpRandomGraphGenerator<>(n, p);
        generator.generateGraph(graph);

        graph.edgeSet().forEach(e -> graph.setEdgeWeight(e, Math.random()));
    }
}
