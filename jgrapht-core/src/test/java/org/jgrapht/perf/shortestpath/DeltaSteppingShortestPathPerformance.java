/*
 * (C) Copyright 2018-2018, by Semen Chudakov and Contributors.
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
package org.jgrapht.perf.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.BellmanFordShortestPath;
import org.jgrapht.alg.shortestpath.DeltaSteppingShortestPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.generate.BarabasiAlbertGraphGenerator;
import org.jgrapht.generate.CompleteGraphGenerator;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.jgrapht.generate.GnpRandomGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.generate.WattsStrogatzGraphGenerator;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.util.SupplierUtil;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * A benchmark comparing {@link DeltaSteppingShortestPath} to {@link org.jgrapht.alg.shortestpath.DijkstraShortestPath}
 * and {@link org.jgrapht.alg.shortestpath.BellmanFordShortestPath}.
 * The benchmark test the algorithms on random, dense and sparse graphs.
 *
 * @author Semen Chudakov
 */
@BenchmarkMode(Mode.AverageTime)
@Fork(value = 1, warmups = 0)
@Warmup(iterations = 3, time = 10)
@Measurement(iterations = 8, time = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class DeltaSteppingShortestPathPerformance {

    @Benchmark
    public ShortestPathAlgorithm.SingleSourcePaths<Integer, DefaultWeightedEdge> testDeltaSteppingGnm(GnmState data) {
        return new DeltaSteppingShortestPath<>(data.graph, 1.0 / data.edgeDegree).getPaths(0);
    }

    @Benchmark
    public ShortestPathAlgorithm.SingleSourcePaths<Integer, DefaultWeightedEdge> testDijkstraGnm(GnmState data) {
        return new DijkstraShortestPath<>(data.graph).getPaths(0);
    }

    @Benchmark
    public ShortestPathAlgorithm.SingleSourcePaths<Integer, DefaultWeightedEdge> testBellmanFordGnm(GnmState data) {
        return new BellmanFordShortestPath<>(data.graph).getPaths(0);
    }

    @Benchmark
    public ShortestPathAlgorithm.SingleSourcePaths<Integer, DefaultWeightedEdge> testDeltaSteppingGnp(GnpState data) {
        return new DeltaSteppingShortestPath<>(data.graph, 1.0 / (1 + (data.p * data.numOfVertices))).getPaths(0);
    }

    @Benchmark
    public ShortestPathAlgorithm.SingleSourcePaths<Integer, DefaultWeightedEdge> testDijkstraGnp(GnpState data) {
        return new DijkstraShortestPath<>(data.graph).getPaths(0);
    }

    @Benchmark
    public ShortestPathAlgorithm.SingleSourcePaths<Integer, DefaultWeightedEdge> testBellmanFordGnp(GnpState data) {
        return new BellmanFordShortestPath<>(data.graph).getPaths(0);
    }

    @Benchmark
    public ShortestPathAlgorithm.SingleSourcePaths<Integer, DefaultWeightedEdge> testDeltaSteppingBarabasiAlbert(BarabasiAlbertState data) {
        return new DeltaSteppingShortestPath<>(data.graph, 1.0 / data.m0).getPaths(0);
    }

    @Benchmark
    public ShortestPathAlgorithm.SingleSourcePaths<Integer, DefaultWeightedEdge> testDijkstraBarabasiAlbert(BarabasiAlbertState data) {
        return new DijkstraShortestPath<>(data.graph).getPaths(0);
    }

    @Benchmark
    public ShortestPathAlgorithm.SingleSourcePaths<Integer, DefaultWeightedEdge> testBellmanFordBarabasiAlbert(BarabasiAlbertState data) {
        return new BellmanFordShortestPath<>(data.graph).getPaths(0);
    }

    @Benchmark
    public ShortestPathAlgorithm.SingleSourcePaths<Integer, DefaultWeightedEdge> testDeltaSteppingWattsStogatz(WattsStogatzState data) {
        return new DeltaSteppingShortestPath<>(data.graph, 1.0 / data.k).getPaths(0);
    }

    @Benchmark
    public ShortestPathAlgorithm.SingleSourcePaths<Integer, DefaultWeightedEdge> testDijkstraWattsStogatz(WattsStogatzState data) {
        return new DijkstraShortestPath<>(data.graph).getPaths(0);
    }

    @Benchmark
    public ShortestPathAlgorithm.SingleSourcePaths<Integer, DefaultWeightedEdge> testBellmanFordWattsStogatz(WattsStogatzState data) {
        return new BellmanFordShortestPath<>(data.graph).getPaths(0);
    }

    @Benchmark
    public ShortestPathAlgorithm.SingleSourcePaths<Integer, DefaultWeightedEdge> testDeltaSteppingDense(CompleteGraphState data) {
        return new DeltaSteppingShortestPath<>(data.graph, 1.0 / data.numOfVertices).getPaths(0);
    }

    @Benchmark
    public ShortestPathAlgorithm.SingleSourcePaths<Integer, DefaultWeightedEdge> testDijkstraDense(CompleteGraphState data) {
        return new DijkstraShortestPath<>(data.graph).getPaths(0);
    }

    @Benchmark
    public ShortestPathAlgorithm.SingleSourcePaths<Integer, DefaultWeightedEdge> testBellmanFordDense(CompleteGraphState data) {
        return new BellmanFordShortestPath<>(data.graph).getPaths(0);
    }


    @State(Scope.Benchmark)
    public abstract static class BaseState {
        DefaultUndirectedWeightedGraph<Integer, DefaultWeightedEdge> graph;

        public abstract void generateGraph();

        void makeConnected(Graph<Integer, DefaultWeightedEdge> graph) {
            Object[] vertices = graph.vertexSet().toArray();
            for (int i = 0; i < vertices.length - 1; i++) {
                graph.addEdge((Integer) vertices[i], (Integer) vertices[i + 1]);
            }
        }

        void addEdgeWeights(Graph<Integer, DefaultWeightedEdge> graph) {
            for (DefaultWeightedEdge edge : graph.edgeSet()) {
                graph.setEdgeWeight(edge, Math.random());
            }
        }
    }

    @State(Scope.Benchmark)
    public static class GnmState extends BaseState {
        @Param({"10000"})
        int numOfVertices;
        @Param({"50", "500"})
        int edgeDegree;

        @Setup(Level.Trial)
        public void generateGraph() {
            graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
            graph.setVertexSupplier(SupplierUtil.createIntegerSupplier());

            GraphGenerator<Integer, DefaultWeightedEdge, Integer> generator =
                    new GnmRandomGraphGenerator<>(numOfVertices, numOfVertices * edgeDegree - numOfVertices + 1);
            generator.generateGraph(graph);
            makeConnected(graph);
            addEdgeWeights(graph);
        }
    }

    @State(Scope.Benchmark)
    public static class GnpState extends BaseState {
        @Param({"10000"})
        int numOfVertices;
        @Param({"0.01", "0.05"})
        double p;

        @Setup(Level.Trial)
        public void generateGraph() {
            graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
            graph.setVertexSupplier(SupplierUtil.createIntegerSupplier());

            GraphGenerator<Integer, DefaultWeightedEdge, Integer> generator =
                    new GnpRandomGraphGenerator<>(numOfVertices, p);
            generator.generateGraph(graph);
            makeConnected(graph);
            addEdgeWeights(graph);
        }
    }

    @State(Scope.Benchmark)
    public static class BarabasiAlbertState extends BaseState {
        @Param({"1000"})
        int m0;
        @Param({"10000"})
        int numOfVertices;
        @Param({"50", "500"})
        int m;

        @Setup(Level.Trial)
        public void generateGraph() {
            graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
            graph.setVertexSupplier(SupplierUtil.createIntegerSupplier());

            GraphGenerator<Integer, DefaultWeightedEdge, Integer> generator =
                    new BarabasiAlbertGraphGenerator<>(m0, m, numOfVertices);
            generator.generateGraph(graph);
            makeConnected(graph);
            addEdgeWeights(graph);
        }
    }

    @State(Scope.Benchmark)
    public static class WattsStogatzState extends BaseState {
        @Param({"10000"})
        int numOfVertices;
        @Param({"100", "1000"})
        int k;
        @Param({"0.05", "0.5"})
        double p;

        @Setup(Level.Trial)
        public void generateGraph() {
            graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
            graph.setVertexSupplier(SupplierUtil.createIntegerSupplier());

            GraphGenerator<Integer, DefaultWeightedEdge, Integer> generator =
                    new WattsStrogatzGraphGenerator<>(numOfVertices, k, p);
            generator.generateGraph(graph);
            addEdgeWeights(graph);
        }
    }

    @State(Scope.Benchmark)
    public static class CompleteGraphState extends BaseState{
        @Param({"1000", "2000", "3000"})
        int numOfVertices;

        @Setup(Level.Trial)
        public void generateGraph() {
            graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
            graph.setVertexSupplier(SupplierUtil.createIntegerSupplier());
            CompleteGraphGenerator<Integer, DefaultWeightedEdge> generator = new CompleteGraphGenerator<>(numOfVertices);

            generator.generateGraph(graph);

            addEdgeWeights(graph);
        }
    }
}