/*
 * (C) Copyright 2018-2018, by Semen Chudakov and Contributors.
 *
 * JGraphT : a free Java graph-theory library
 *
 * This program and the accompanying materials are dual-licensed under
 * either
 *
 * (a) the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation, or (at your option) any
 * later version.
 *
 * or (per the licensee's choosing)
 *
 * (b) the terms of the Eclipse Public License v1.0 as published by
 * the Eclipse Foundation.
 */
package org.jgrapht.perf.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.shortestpath.BellmanFordShortestPath;
import org.jgrapht.alg.shortestpath.DeltaSteppingShortestPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.generate.CompleteGraphGenerator;
import org.jgrapht.graph.DefaultUndirectedWeightedGraph;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.util.SupplierUtil;
import org.openjdk.jmh.annotations.*;

import java.util.concurrent.TimeUnit;

/**
 * A benchmark comparing {@link DeltaSteppingShortestPath} to {@link org.jgrapht.alg.shortestpath.DijkstraShortestPath}
 * and {@link org.jgrapht.alg.shortestpath.BellmanFordShortestPath}.
 * The benchmark test the algorithms on dense and sparse random graphs.
 *
 * @author Semen Chudakov
 */
@BenchmarkMode(Mode.SampleTime)
@Fork(value = 1, warmups = 0)
@Warmup(iterations = 3, time = 10)
@Measurement(iterations = 8, time = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class DeltaSteppingShortestPathPerformance {

    @Benchmark
    public ShortestPathAlgorithm.SingleSourcePaths<Integer, DefaultWeightedEdge> testSparseDeltaStepping(SparseGraphData data) {
        return new DeltaSteppingShortestPath<>(data.graph, 1.0).getPaths(0);
    }

    @Benchmark
    public ShortestPathAlgorithm.SingleSourcePaths<Integer, DefaultWeightedEdge> testSparseDijkstra(SparseGraphData data) {
        return new DijkstraShortestPath<>(data.graph).getPaths(0);
    }

    @Benchmark
    public ShortestPathAlgorithm.SingleSourcePaths<Integer, DefaultWeightedEdge> testSparseBellmanFord(SparseGraphData data) {
        return new BellmanFordShortestPath<>(data.graph).getPaths(0);
    }

    @Benchmark
    public ShortestPathAlgorithm.SingleSourcePaths<Integer, DefaultWeightedEdge> testDenseDeltaStepping(DenseGraphData data) {
        return new DeltaSteppingShortestPath<>(data.graph, 1.0 / data.graphSize).getPaths(0);
    }

    @Benchmark
    public ShortestPathAlgorithm.SingleSourcePaths<Integer, DefaultWeightedEdge> testDijkstraDense(DenseGraphData data) {
        return new DijkstraShortestPath<>(data.graph).getPaths(0);
    }

    @Benchmark
    public ShortestPathAlgorithm.SingleSourcePaths<Integer, DefaultWeightedEdge> testBellmanFordDense(DenseGraphData data) {
        return new BellmanFordShortestPath<>(data.graph).getPaths(0);
    }

    @State(Scope.Benchmark)
    public static class SparseGraphData {
        @Param({"10000"})
        int graphSize;
        @Param({"50"})
        int edgeDegree;
        Graph<Integer, DefaultWeightedEdge> graph;

        @Setup(Level.Iteration)
        public void generate() {
            this.graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);

            for (int i = 0; i < graphSize; i++) {
                graph.addVertex(i);
            }
            for (int i = 0; i < graphSize; i++) {
                for (int j = 0; j < edgeDegree; j++) {
                    Graphs.addEdge(graph, i, (i + j) % graphSize, Math.random());
                }
            }
        }
    }

    @State(Scope.Benchmark)
    public static class DenseGraphData {
        @Param({"1000"})
        public int graphSize;
        public DefaultUndirectedWeightedGraph<Integer, DefaultWeightedEdge> graph;

        @Setup(Level.Iteration)
        public void generateGraph() {
            this.graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
            graph.setVertexSupplier(SupplierUtil.createIntegerSupplier());
            CompleteGraphGenerator<Integer, DefaultWeightedEdge> generator = new CompleteGraphGenerator<>(graphSize);
            generator.generateGraph(graph);
            graph.edgeSet().forEach(e -> graph.setEdgeWeight(e, Math.random()));
        }
    }
}