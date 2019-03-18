package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.generate.GnpRandomGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@Fork(value = 1, warmups = 0)
@Warmup(iterations = 3, time = 10)
@Measurement(iterations = 25, time = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class YenKShortestPathIteratorPerformance {

    @Benchmark
    public List<GraphPath<Integer, DefaultWeightedEdge>> testOldVersionGnp(GnpState state) {
        List<GraphPath<Integer, DefaultWeightedEdge>> paths = new ArrayList<>();
        YenOldShortestPathIterator<Integer, DefaultWeightedEdge> iterator =
                new YenOldShortestPathIterator<>(state.graph, state.source, state.target);
        for (int i = 0; i < 10 && iterator.hasNext(); i++) {
            paths.add(iterator.next());
        }
        return paths;
    }

    @Benchmark
    public List<GraphPath<Integer, DefaultWeightedEdge>> testNewVersionGnp(GnpState state) {
        List<GraphPath<Integer, DefaultWeightedEdge>> paths = new ArrayList<>();
        YenShortestPathIterator<Integer, DefaultWeightedEdge> iterator =
                new YenShortestPathIterator<>(state.graph, state.source, state.target);
        for (int i = 0; i < 10 && iterator.hasNext(); i++) {
            paths.add(iterator.next());
        }
        return paths;
    }


    @State(Scope.Benchmark)
    public abstract static class BaseState {
        DefaultUndirectedWeightedGraph<Integer, DefaultWeightedEdge> graph;
        Integer source;
        Integer target;

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
    public static class GnpState extends BaseState {
        @Param({"100", "1000"})
        int numOfVertices;
        @Param({"0.05", "0.1", "0.25", "0.5"})
        double p;

        @Setup(Level.Iteration)
        public void generateGraph() {
            graph = new DefaultUndirectedWeightedGraph<>(DefaultWeightedEdge.class);
            graph.setVertexSupplier(SupplierUtil.createIntegerSupplier());

            GraphGenerator<Integer, DefaultWeightedEdge, Integer> generator =
                    new GnpRandomGraphGenerator<>(numOfVertices, p);
            generator.generateGraph(graph);
            makeConnected(graph);
            addEdgeWeights(graph);
            source = (int) (Math.random() * numOfVertices);
            target = (int) (Math.random() * numOfVertices);
        }
    }
}
