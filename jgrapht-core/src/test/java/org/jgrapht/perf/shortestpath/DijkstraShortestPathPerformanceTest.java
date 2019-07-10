/*
 * (C) Copyright 2016-2018, by Dimitrios Michail and Contributors.
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

import org.jgrapht.*;
import org.jgrapht.alg.interfaces.*;
import org.jgrapht.alg.shortestpath.*;
import org.jgrapht.generate.*;
import org.jgrapht.graph.*;
import org.jgrapht.graph.builder.*;
import org.jgrapht.traverse.*;
import org.jgrapht.util.*;
import org.junit.*;
import org.omg.CORBA.INTERNAL;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.*;

/**
 * A small benchmark comparing Dijkstra like algorithms. The benchmark creates a random graph and
 * computes all-pairs shortest paths.
 *
 * @author Dimitrios Michail
 */
public class DijkstraShortestPathPerformanceTest
{
    private static final int PERF_BENCHMARK_VERTICES_COUNT = 250;
    private static final double PERF_BENCHMARK_EDGES_PROP = 0.02;
    private static final int WARMUP_REPEAT = 5;
    private static final int REPEAT = 10;
    private static final long SEED = 13l;

    private static abstract class BenchmarkBase
    {
        protected Random rng = new Random(SEED);
        protected GraphGenerator<Integer, DefaultWeightedEdge, Integer> generator = null;
        protected Graph<Integer, DefaultWeightedEdge> graph;
        protected ShortestPathAlgorithm<Integer, DefaultWeightedEdge> algorithm;

        abstract void createSolver();

        public void setup()
        {
            if (generator == null) {
                // lazily construct generator
                generator = new GnpRandomGraphGenerator<>(
                    PERF_BENCHMARK_VERTICES_COUNT, PERF_BENCHMARK_EDGES_PROP, rng, false);
            }

            this.graph = GraphTypeBuilder
                .directed().weighted(true).edgeClass(DefaultWeightedEdge.class)
                .vertexSupplier(SupplierUtil.createIntegerSupplier()).allowingMultipleEdges(false)
                .allowingSelfLoops(false).buildGraph();

            generator.generateGraph(graph);

            for (DefaultWeightedEdge e : graph.edgeSet()) {
                graph.setEdgeWeight(e, rng.nextDouble());
            }
        }

        public void run()
        {
            for (Integer v : graph.vertexSet()) {
                for (Integer u : graph.vertexSet()) {
                    algorithm.getPath(v, u);
                }
            }
        }
    }

    public static class DijkstraBenchmark
        extends
        BenchmarkBase
    {
        @Override
        void createSolver()
        {
            algorithm = new DijkstraShortestPath<>(graph);
        }

        @Override
        public String toString()
        {
            return "Dijkstra";
        }
    }

    public static class BFSShortestPathBenchmark
        extends
        BenchmarkBase
    {
        @Override
        void createSolver()
        {
            algorithm = new BFSShortestPath<>(graph);
        }

        @Override
        public String toString()
        {
            return "BFSShortestPath";
        }
    }

    public static class ClosestFirstIteratorBenchmark
        extends
        BenchmarkBase
    {
        @Override
        void createSolver()
        {
            algorithm = new ShortestPathAlgorithm<Integer, DefaultWeightedEdge>()
            {

                @Override
                public GraphPath<Integer, DefaultWeightedEdge> getPath(Integer source, Integer sink)
                {
                    /*
                     * We do not really return a result here, just reach the target.
                     */
                    ClosestFirstIterator<Integer, DefaultWeightedEdge> iter =
                        new ClosestFirstIterator<>(graph, source, Double.POSITIVE_INFINITY);
                    while (iter.hasNext()) {
                        Integer vertex = iter.next();
                        if (vertex.equals(sink)) {
                            return null;
                        }
                    }
                    return null;
                }

                @Override
                public double getPathWeight(Integer source, Integer sink)
                {
                    GraphPath<Integer, DefaultWeightedEdge> p = getPath(source, sink);
                    if (p == null) {
                        return Double.POSITIVE_INFINITY;
                    } else {
                        return p.getWeight();
                    }
                }

                public org.jgrapht.alg.interfaces.ShortestPathAlgorithm.SingleSourcePaths<Integer,
                    DefaultWeightedEdge> getPaths(Integer source)
                {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public String toString()
        {
            return "Dijkstra with ClosestFirstIterator";
        }
    }

    public static class BidirectionalDijkstraBenchmark
        extends
        BenchmarkBase
    {
        @Override
        void createSolver()
        {
            algorithm = new BidirectionalDijkstraShortestPath<>(graph);

        }

        @Override
        public String toString()
        {
            return "Bidirectional Dijkstra";
        }
    }

    public static class AStarNoHeuristicBenchmark
        extends
        BenchmarkBase
    {
        @Override
        void createSolver()
        {
            algorithm = new AStarShortestPath<>(graph, (u, t) -> 0d);

        }

        @Override
        public String toString()
        {
            return "A* no heuristic";
        }
    }

    public static class AStarALTBenchmark
        extends
        BenchmarkBase
    {
        private int totalLandmarks;

        AStarALTBenchmark(int totalLandmarks)
        {
            this.totalLandmarks = totalLandmarks;
        }

        @Override
        void createSolver()
        {
            Integer[] vertices = graph.vertexSet().toArray(new Integer[0]);
            Set<Integer> landmarks = new HashSet<>();
            while (landmarks.size() < totalLandmarks) {
                landmarks.add(vertices[rng.nextInt(graph.vertexSet().size())]);
            }
            algorithm = new AStarShortestPath<>(graph, new ALTAdmissibleHeuristic<>(graph, landmarks));
        }

        @Override
        public String toString()
        {
            return "A* with ALT heuristic (" + totalLandmarks + " random landmarks)";
        }
    }

    public static class BidirectionalAStarNoHeuristicBenchmark
        extends
        BenchmarkBase
    {
        @Override
        void createSolver()
        {
            algorithm = new BidirectionalAStarShortestPath<>(graph, (u, t) -> 0d);
        }

        @Override
        public String toString()
        {
            return "Bidirectional A* no heuristic";
        }
    }

    public static class BidirectionalAStarALTBenchmark
        extends
        BenchmarkBase
    {
        private int totalLandmarks;

        BidirectionalAStarALTBenchmark(int totalLandmarks)
        {
            this.totalLandmarks = totalLandmarks;
        }

        @Override
        void createSolver()
        {
            Integer[] vertices = graph.vertexSet().toArray(new Integer[0]);
            Set<Integer> landmarks = new HashSet<>();
            while (landmarks.size() < totalLandmarks) {
                landmarks.add(vertices[rng.nextInt(graph.vertexSet().size())]);
            }
            AStarAdmissibleHeuristic<Integer> heuristic =
                new ALTAdmissibleHeuristic<>(graph, landmarks);
            algorithm = new BidirectionalAStarShortestPath<>(graph, heuristic);
        }

        @Override
        public String toString()
        {
            return "Bidirectional A* with ALT heuristic (" + totalLandmarks + " random landmarks)";
        }
    }

    public static class ContractionHierarchyBenchmark extends BenchmarkBase{

        @Override
        void createSolver() {
            algorithm = new ContractionHierarchyBidirectionalDijkstra<>(graph);
        }

        @Override
        public String toString() {
            return "Contraction Hierarchy Algorithm";
        }
    }

    @Test
    public void testBenchmark()
    {
        System.out.println("All-Pairs Shortest Paths Benchmark");
        System.out.println("---------");
        System.out.println(
            "Using G(n,p) random graph with n = " + PERF_BENCHMARK_VERTICES_COUNT + ", p = "
                + PERF_BENCHMARK_EDGES_PROP);
        System.out.println("Warmup phase " + WARMUP_REPEAT + " executions");
        System.out.println("Averaging results over " + REPEAT + " executions");

        List<Supplier<BenchmarkBase>> algFactory = new ArrayList<>();
        algFactory.add(() -> new ClosestFirstIteratorBenchmark());
        algFactory.add(() -> new DijkstraBenchmark());
        algFactory.add(() -> new AStarNoHeuristicBenchmark());
        algFactory.add(() -> new AStarALTBenchmark(1));
        algFactory.add(() -> new AStarALTBenchmark(5));
        algFactory.add(() -> new BidirectionalDijkstraBenchmark());
        algFactory.add(() -> new BFSShortestPathBenchmark());
        algFactory.add(() -> new BidirectionalAStarALTBenchmark(1));
        algFactory.add(() -> new BidirectionalAStarALTBenchmark(5));
        algFactory.add(() -> new BidirectionalAStarNoHeuristicBenchmark());
        algFactory.add(() -> new ContractionHierarchyBenchmark());

        for (Supplier<BenchmarkBase> alg : algFactory) {

            System.gc();
            StopWatch watch = new StopWatch();

            BenchmarkBase benchmark = alg.get();
            System.out.printf("%-50s :", benchmark.toString());

            for (int i = 0; i < WARMUP_REPEAT; i++) {
                System.out.print("-");
                System.out.flush();
                benchmark.setup();
                benchmark.createSolver();
                benchmark.run();
            }
            double avgGraphCreate = 0d;
            double avgPrecompute = 0d;
            double avgExecution = 0d;
            for (int i = 0; i < REPEAT; i++) {
                System.out.print("+");
                System.out.flush();
                watch.start();
                benchmark.setup();
                avgGraphCreate += watch.getElapsed(TimeUnit.MILLISECONDS);
                watch.start();
                benchmark.createSolver();
                avgPrecompute += watch.getElapsed(TimeUnit.MILLISECONDS);
                watch.start();
                benchmark.run();
                avgExecution += watch.getElapsed(TimeUnit.MILLISECONDS);
            }
            avgGraphCreate /= REPEAT;
            avgExecution /= REPEAT;

            System.out.print(" -> ");
            System.out
                .printf("setup %.3f (ms) | precompute %.3f (ms) | execution %.3f (ms)\n", avgGraphCreate, avgPrecompute, avgExecution);
        }

    }

}
