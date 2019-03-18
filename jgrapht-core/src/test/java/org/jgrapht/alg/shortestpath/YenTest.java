package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.generate.GnpRandomGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultDirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.util.SupplierUtil;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Stack;

public class YenTest {
    private static final String GRAPH_DIR_PATH = "C:\\Users\\Semen\\D\\workspace.java\\yen_test\\graphs";
    private static final String MY_RESULT_DIR_PATH = "C:\\Users\\Semen\\D\\workspace.java\\yen_test\\my";
    private static final String OTHERS_RESULT_DIR_PATH = "C:\\Users\\Semen\\D\\workspace.java\\yen_test\\my";

    @Test
    public void testCorrectness() {
        int n = 100;
        double p = 0.25;
        for (int i = 0; i < 10000; i++) {
            System.out.println(i);
            DefaultDirectedWeightedGraph<Integer, DefaultWeightedEdge> graph = new DefaultDirectedWeightedGraph<>(DefaultWeightedEdge.class);
            graph.setVertexSupplier(SupplierUtil.createIntegerSupplier());
            getRandomGraph(graph, n, p);
            writeGraph(graph, Paths.get(GRAPH_DIR_PATH, i + ".txt"));
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(Paths.get(MY_RESULT_DIR_PATH, i + ".txt").toFile()))) {
                int source = (int) (Math.random() * n);
                int target;
                do {
                    target = (int) (Math.random() * n);
                } while (source == target);
                writer.write(source + " " + target);
                writer.newLine();
                YenShortestPathIterator<Integer, DefaultWeightedEdge> iterator =
                        new YenShortestPathIterator<>(graph, source, target);
                for (int j = 0; j < 10 && iterator.hasNext(); j++) {
                    GraphPath<Integer, DefaultWeightedEdge> graphPath = iterator.next();
//                    writer.write(String.format("%.6f", graphPath.getWeight()));
//                    writer.newLine();
                    writer.write(graphPath.getVertexList().toString());
                    writer.newLine();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void getRandomGraph(Graph<Integer, DefaultWeightedEdge> graph, int n, double p) {
        GraphGenerator<Integer, DefaultWeightedEdge, Integer> generator
                = new GnpRandomGraphGenerator<>(n, p);
        generator.generateGraph(graph);

        graph.edgeSet().forEach(e -> graph.setEdgeWeight(e, Math.random()));
    }

    private void writeGraph(Graph<Integer, DefaultWeightedEdge> graph, Path path) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(path.toFile()))) {
            writer.write(String.valueOf(graph.vertexSet().size()));
            writer.newLine();
            for (DefaultWeightedEdge edge : graph.edgeSet()) {
                writer.write(graph.getEdgeSource(edge) + " " + graph.getEdgeTarget(edge) + " " + graph.getEdgeWeight(edge));
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
