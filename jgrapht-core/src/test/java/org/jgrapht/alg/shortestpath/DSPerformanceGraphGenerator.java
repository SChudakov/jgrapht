/*
 * (C) Copyright 2019-2019, by Semen Chudakov and Contributors.
 *
 * JGraphT : a free Java graph-theory library
 *
 * See the CONTRIBUTORS.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the
 * GNU Lesser General Public License v2.1 or later
 * which is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1-standalone.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR LGPL-2.1-or-later
 */
package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.generate.BarabasiAlbertGraphGenerator;
import org.jgrapht.generate.CompleteGraphGenerator;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.jgrapht.generate.GnpRandomGraphGenerator;
import org.jgrapht.generate.WattsStrogatzGraphGenerator;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.util.SupplierUtil;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * asuhdkjasd
 */
public class DSPerformanceGraphGenerator {
    private static final int NUM_OF_GRAPHS = 1;

    private static final String TXT_EXTENSION = ".txt";
    private static final String BASE_PATH = "C:\\Users\\Semen\\D\\workspace.cpp\\pbgl-benchmarks\\graphs\\";

    private static final String GNM = "Gnm";
    private static final String GNP = "Gnp";
    private static final String WATTS_STOGATZ = "WattsStogatz";
    private static final String BARABASI_ALBERT = "BarabasiAlbert";
    private static final String COMPLETE = "comlete";

    /**
     * asdasd
     * @param args asdhausd
     */
    public static void main(String[] args) {
        generate();
//        cleanDirs();
    }

    private static void generate() {
        for (int i = 0; i < NUM_OF_GRAPHS; i++) {
            writeGraph(generateGnm(10000, 50), filePath(BASE_PATH, GNM, new Parameter("n", 10000), new Parameter("m", 50), i));
            writeGraph(generateGnm(10000, 500), filePath(BASE_PATH, GNM, new Parameter("n", 10000), new Parameter("m", 500), i));
            System.out.println("GNM_PATH done");
            writeGraph(generateGnp(10000, 0.01), filePath(BASE_PATH, GNP, new Parameter("n", 10000), new Parameter("p", 0.01), i));
            writeGraph(generateGnp(10000, 0.05), filePath(BASE_PATH, GNP, new Parameter("n", 10000), new Parameter("p", 0.05), i));
            System.out.println("GNP_PATH done");
            writeGraph(generateBarabasiAlbert(1000, 10000, 50), filePath(BASE_PATH, BARABASI_ALBERT, new Parameter("m0", 1000), new Parameter("n", 10000), new Parameter("m", 50), i));
            writeGraph(generateBarabasiAlbert(1000, 10000, 500), filePath(BASE_PATH, BARABASI_ALBERT, new Parameter("m0", 1000), new Parameter("n", 10000), new Parameter("m", 500), i));
            System.out.println("BarabasiAlbert done");
            writeGraph(generateWattsStogatz(10000, 100, 0.05), filePath(BASE_PATH, WATTS_STOGATZ, new Parameter("n", 10000), new Parameter("k", 100), new Parameter("k", 0.05), i));
            writeGraph(generateWattsStogatz(10000, 100, 0.5), filePath(BASE_PATH, WATTS_STOGATZ, new Parameter("n", 10000), new Parameter("k", new Parameter("k", 0.5)), 0.5, i));
            writeGraph(generateWattsStogatz(10000, 1000, 0.05), filePath(BASE_PATH, WATTS_STOGATZ, new Parameter("n", 10000), new Parameter("k", 1000), new Parameter("k", 0.05), i));
            writeGraph(generateWattsStogatz(10000, 1000, 0.5), filePath(BASE_PATH, WATTS_STOGATZ, new Parameter("n", 10000), new Parameter("k", 1000), new Parameter("k", 0.5), i));
            System.out.println("WattsStogatz done");
            writeGraph(generateComplete(1000), filePath(BASE_PATH, COMPLETE, new Parameter("n", 1000), i));
            writeGraph(generateComplete(2000), filePath(BASE_PATH, COMPLETE, new Parameter("n", 2000), i));
            writeGraph(generateComplete(3000), filePath(BASE_PATH, COMPLETE, new Parameter("n", 3000), i));
            System.out.println("complete done");
        }
    }


    private static Graph<Integer, DefaultWeightedEdge> generateGnm(int n, int m) {
        DefaultUndirectedWeightedGraph<Integer, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        graph.setVertexSupplier(SupplierUtil.createIntegerSupplier());

        org.jgrapht.generate.GraphGenerator<Integer, DefaultWeightedEdge, Integer> generator =
                new GnmRandomGraphGenerator<>(n, n * m - n + 1);
        generator.generateGraph(graph);
        makeConnected(graph);
        addEdgeWeights(graph);
        return graph;
    }

    private static Graph<Integer, DefaultWeightedEdge> generateGnp(int n, double p) {
        DefaultUndirectedWeightedGraph<Integer, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        graph.setVertexSupplier(SupplierUtil.createIntegerSupplier());

        org.jgrapht.generate.GraphGenerator<Integer, DefaultWeightedEdge, Integer> generator =
                new GnpRandomGraphGenerator<>(n, p);
        generator.generateGraph(graph);
        makeConnected(graph);
        addEdgeWeights(graph);
        return graph;
    }

    private static Graph<Integer, DefaultWeightedEdge> generateBarabasiAlbert(int m0, int n, int m) {
        DefaultUndirectedWeightedGraph<Integer, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        graph.setVertexSupplier(SupplierUtil.createIntegerSupplier());

        org.jgrapht.generate.GraphGenerator<Integer, DefaultWeightedEdge, Integer> generator =
                new BarabasiAlbertGraphGenerator<>(m0, m, n);
        generator.generateGraph(graph);
        makeConnected(graph);
        addEdgeWeights(graph);
        return graph;
    }

    private static Graph<Integer, DefaultWeightedEdge> generateWattsStogatz(int n, int k, double p) {
        DefaultUndirectedWeightedGraph<Integer, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        graph.setVertexSupplier(SupplierUtil.createIntegerSupplier());

        org.jgrapht.generate.GraphGenerator<Integer, DefaultWeightedEdge, Integer> generator =
                new WattsStrogatzGraphGenerator<>(n, k, p);
        generator.generateGraph(graph);
        addEdgeWeights(graph);
        return graph;
    }

    private static Graph<Integer, DefaultWeightedEdge> generateComplete(int n) {
        DefaultUndirectedWeightedGraph<Integer, DefaultWeightedEdge> graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
        graph.setVertexSupplier(SupplierUtil.createIntegerSupplier());
        CompleteGraphGenerator<Integer, DefaultWeightedEdge> generator = new CompleteGraphGenerator<>(n);

        generator.generateGraph(graph);
        addEdgeWeights(graph);
        return graph;
    }

    private static void writeGraph(Graph<Integer, DefaultWeightedEdge> graph, String file) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (DefaultWeightedEdge edge : graph.edgeSet()) {
                writer.write(graph.getEdgeSource(edge) + " " + graph.getEdgeTarget(edge) + " " + graph.getEdgeWeight(edge));
                writer.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String filePath(String base, Object... attrs) {
        StringBuilder builder = new StringBuilder(base);
        for (Object attr : attrs) {
            builder.append(attr).append("_");
        }
        builder.deleteCharAt(builder.length() - 1);
        builder.append(TXT_EXTENSION);
        return builder.toString();
    }

    private static void makeConnected(Graph<Integer, DefaultWeightedEdge> graph) {
        Object[] vertices = graph.vertexSet().toArray();
        for (int i = 0; i < vertices.length - 1; i++) {
            graph.addEdge((Integer) vertices[i], (Integer) vertices[i + 1]);
        }
    }

    private static void addEdgeWeights(Graph<Integer, DefaultWeightedEdge> graph) {
        for (DefaultWeightedEdge edge : graph.edgeSet()) {
            graph.setEdgeWeight(edge, Math.random());
        }
    }

    private static void cleanDirs() {
        clearDir(BASE_PATH);
    }

    private static void clearDir(String dir) {
        for (File file : new File(dir).listFiles()) {
            file.delete();
        }
    }

    static class Parameter {
        String name;
        Object value;

        public Parameter(String name, Object value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public String toString() {
            return name + "=" + value;
        }
    }
}
