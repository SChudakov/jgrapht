package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.AStarAdmissibleHeuristic;
import org.jgrapht.alg.interfaces.ManyToManyShortestPathsAlgorithm;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Warmup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;

@BenchmarkMode(Mode.AverageTime)
@Fork(value = 1, warmups = 0)
@Warmup(iterations = 3, time = 10)
@Measurement(iterations = 5, time = 10)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
public class ShortestPathPerformance {
    private static final double EARTH_RADIUS = 6371000.0;

    private static String path = "/home/semen/drive/osm/final/liechtenstein.txt";
    private static int numOfLandmarks = 8;
    private static int numOfQueries = 200;
    private static int numManyTwoOfVertices = 1000;

    private static Random random = new Random();
    private static long SEED = 17L;

    private static Graph<Node, DefaultWeightedEdge> graph;
    private static List<Pair<Node, Node>> queries;

    private static AStarAdmissibleHeuristic<Node> heuristic;
    private static Set<Node> landmarks;

    private static Pair<Graph<ContractionHierarchyAlgorithm.ContractionVertex<Node>,
            ContractionHierarchyAlgorithm.ContractionEdge<DefaultWeightedEdge>>,
            Map<Node, ContractionHierarchyAlgorithm.ContractionVertex<Node>>> contraction;

    private static Pair<List<Node>, List<Node>> many2ManyQueries;

    static {
        setup();
    }

    private static void setup() {
        graph = new SimpleDirectedWeightedGraph<>(DefaultWeightedEdge.class);
//        System.out.println("reading graph");
        OSMReader reader = new OSMReader();
        reader.readGraph(graph, path, ShortestPathPerformance::greatCircleDistance);

//        System.out.println("selecting queries");
        Node[] nodes = graph.vertexSet().toArray(new Node[0]);
        queries = selectQueries(nodes);
        many2ManyQueries = Pair.of(getRandomNodes(nodes, numManyTwoOfVertices),
                getRandomNodes(nodes, numManyTwoOfVertices));

//        System.out.println("computing contraction");
        contraction = getContraction(Runtime.getRuntime().availableProcessors());

        heuristic = new GreatCircleHeuristic();
//        System.out.println("selecting landmarks");
//        landmarks = selectRandomLandmarks();
//        heuristic = new ALTAdmissibleHeuristic<>(graph, landmarks);
    }


    private static List<Pair<Node, Node>> selectQueries(Node[] nodes) {
        List<Pair<Node, Node>> result = new ArrayList<>();
        List<Node> sources = getRandomNodes(nodes, numOfQueries);
        List<Node> targets = getRandomNodes(nodes, numOfQueries);
        for (int i = 0; i < numOfQueries; ++i) {
            result.add(Pair.of(sources.get(i), targets.get(i)));
        }
        return result;
    }

    private static List<Node> getRandomNodes(Node[] nodes, int numOfNodes) {
        List<Node> result = new ArrayList<>(numOfNodes);
        for (int i = 0; i < numOfNodes; ++i) {
            result.add(nodes[random.nextInt(nodes.length)]);
        }
        return result;
    }

    private static Set<Node> selectRandomLandmarks() {
        Set<Node> landmarks = new HashSet<>();
        Node[] nodes = graph.vertexSet().toArray(new Node[0]);
        while (landmarks.size() < numOfLandmarks) {
            int position = random.nextInt(graph.vertexSet().size());
            if (landmarks.contains(nodes[position])) {
                continue;
            }
            landmarks.add(nodes[position]);
        }
        return landmarks;
    }

    private static Pair<Graph<ContractionHierarchyAlgorithm.ContractionVertex<Node>,
            ContractionHierarchyAlgorithm.ContractionEdge<DefaultWeightedEdge>>,
            Map<Node, ContractionHierarchyAlgorithm.ContractionVertex<Node>>> getContraction(int parallelism) {
        return new ContractionHierarchyAlgorithm<>(
                graph,
                parallelism/*,
                () -> new Random(SEED)*/
        ).computeContractionHierarchy();
    }


    private static Set<Node> selectFarthestLandmarks() {
        FarthestLandmarkSelectionStrategy<Node, DefaultWeightedEdge> selection =
                new FarthestLandmarkSelectionStrategy<>();
        return selection.getLandmarks(graph, numOfLandmarks, new Node(0, 0.0, 0.0));
    }

    private static double getFarthestDistance() {
        DijkstraShortestPath<Node, DefaultWeightedEdge> paths1 = new DijkstraShortestPath<>(graph);
        double maxDistance = 0;
        for (Node node : graph.vertexSet()) {
            ShortestPathAlgorithm.SingleSourcePaths<Node, DefaultWeightedEdge> paths =
                    paths1.getPaths(node);
            for (Node node1 : graph.vertexSet()) {
                GraphPath<Node, DefaultWeightedEdge> path = paths.getPath(node1);
                maxDistance = Math.max(maxDistance, path.getWeight());
            }
        }
        return maxDistance;
    }

    //    @Benchmark
    public List<GraphPath<Node, DefaultWeightedEdge>> testBidirectionalAStar() {
        List<GraphPath<Node, DefaultWeightedEdge>> result = new ArrayList<>(queries.size());
        BidirectionalAStarShortestPath<Node, DefaultWeightedEdge> s =
                new BidirectionalAStarShortestPath<>(graph, heuristic);
        for (Pair<Node, Node> query : queries) {
            GraphPath<Node, DefaultWeightedEdge> path = s.getPath(query.getFirst(), query.getSecond());
            result.add(path);
        }
        return result;
    }

    //    @Benchmark
    public List<GraphPath<Node, DefaultWeightedEdge>> testBidirectionalDijkstra() {
        List<GraphPath<Node, DefaultWeightedEdge>> result = new ArrayList<>(queries.size());
        BidirectionalDijkstraShortestPath<Node, DefaultWeightedEdge> s =
                new BidirectionalDijkstraShortestPath<>(graph);
        for (Pair<Node, Node> query : queries) {
            result.add(s.getPath(query.getFirst(), query.getSecond()));
        }
        return result;
    }

    //    @Benchmark
    public List<GraphPath<Node, DefaultWeightedEdge>> testDijkstra() {
        List<GraphPath<Node, DefaultWeightedEdge>> result = new ArrayList<>(queries.size());
        DijkstraShortestPath<Node, DefaultWeightedEdge> s = new DijkstraShortestPath<>(graph);
        for (Pair<Node, Node> query : queries) {
            result.add(s.getPath(query.getFirst(), query.getSecond()));
        }
        return result;
    }

    //    @Benchmark
    public List<GraphPath<Node, DefaultWeightedEdge>> testAStar() {
        List<GraphPath<Node, DefaultWeightedEdge>> result = new ArrayList<>(queries.size());
        AStarShortestPath<Node, DefaultWeightedEdge> s = new AStarShortestPath<>(graph, heuristic);
        for (Pair<Node, Node> query : queries) {
            result.add(s.getPath(query.getFirst(), query.getSecond()));
        }
        return result;
    }

    //    @Benchmark
    public List<GraphPath<Node, DefaultWeightedEdge>> testAStarNoHeuristic() {
        List<GraphPath<Node, DefaultWeightedEdge>> result = new ArrayList<>(queries.size());
        AStarShortestPath<Node, DefaultWeightedEdge> s = new AStarShortestPath<>(graph, new AStarAdmissibleHeuristic<Node>() {
            @Override
            public double getCostEstimate(Node sourceVertex, Node targetVertex) {
                return 0.0;
            }

            @Override
            public <E> boolean isConsistent(Graph<Node, E> graph) {
                return true;
            }
        });
        for (Pair<Node, Node> query : queries) {
            result.add(s.getPath(query.getFirst(), query.getSecond()));
        }
        return result;
    }

    //    @Benchmark
    public List<GraphPath<Node, DefaultWeightedEdge>> testBidirectionalAStarNoHeuristic() {
        List<GraphPath<Node, DefaultWeightedEdge>> result = new ArrayList<>(queries.size());
        BidirectionalAStarShortestPath<Node, DefaultWeightedEdge> s =
                new BidirectionalAStarShortestPath<>(graph, new AStarAdmissibleHeuristic<Node>() {
                    @Override
                    public double getCostEstimate(Node sourceVertex, Node targetVertex) {
                        return 0.0;
                    }

                    @Override
                    public <E> boolean isConsistent(Graph<Node, E> graph) {
                        return true;
                    }
                });
        for (Pair<Node, Node> query : queries) {
            result.add(s.getPath(query.getFirst(), query.getSecond()));
        }
        return result;
    }

    //    @Benchmark
    public List<GraphPath<Node, DefaultWeightedEdge>> testContractionHierarchyBidirectionalDijkstra() {
        List<GraphPath<Node, DefaultWeightedEdge>> result = new ArrayList<>(queries.size());
        ContractionHierarchyBidirectionalDijkstra<Node, DefaultWeightedEdge> s =
                new ContractionHierarchyBidirectionalDijkstra<>(graph, contraction.getFirst(), contraction.getSecond());
        for (Pair<Node, Node> query : queries) {
            result.add(s.getPath(query.getFirst(), query.getSecond()));
        }
        return result;
    }

    //    @Benchmark
    public AStarAdmissibleHeuristic<Node> testALTPrecomputation() {
        return new ALTAdmissibleHeuristic<>(graph, selectRandomLandmarks());
    }

    //    @Benchmark
    public Pair<Graph<ContractionHierarchyAlgorithm.ContractionVertex<Node>,
            ContractionHierarchyAlgorithm.ContractionEdge<DefaultWeightedEdge>>,
            Map<Node, ContractionHierarchyAlgorithm.ContractionVertex<Node>>> testContractionHierarchyPrecomputation() {
        return getContraction(12);
    }

    @Benchmark
    public List<GraphPath<Node, DefaultWeightedEdge>> testCHMany2Many() {
        return testMany2ManyAlgorithm(new CHManyToManyShortestPaths<>(graph, contraction.getFirst(), contraction.getSecond()));
    }

    @Benchmark
    public List<GraphPath<Node, DefaultWeightedEdge>> testDefaultMany2Many() {
        return testMany2ManyAlgorithm(new DefaultManyTwoManyShortestPaths<>(graph));
    }

    private List<GraphPath<Node, DefaultWeightedEdge>> testMany2ManyAlgorithm(
            ManyToManyShortestPathsAlgorithm<Node, DefaultWeightedEdge> algorithm){
        List<GraphPath<Node, DefaultWeightedEdge>> result = new ArrayList<>(numManyTwoOfVertices * numManyTwoOfVertices);
        ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<Node, DefaultWeightedEdge> shortestPaths
                = algorithm.getManyTwoManyPaths(many2ManyQueries.getFirst(), many2ManyQueries.getSecond());

        for (Node source : many2ManyQueries.getFirst()) {
            for (Node target : many2ManyQueries.getSecond()) {
                result.add(shortestPaths.getPath(source, target));
            }
        }

        return result;
    }

    public static double greatCircleDistance(Node source, Node target) {
        double endLat = target.latitude;
        double startLat = source.latitude;
        double endLong = target.longitude;
        double startLong = source.longitude;

        double dLat = Math.toRadians((endLat - startLat));
        double dLong = Math.toRadians((endLong - startLong));

        startLat = Math.toRadians(startLat);
        endLat = Math.toRadians(endLat);

        double a = haversin(dLat) + cos(startLat) * cos(endLat) * haversin(dLong);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
//          return centralAngle(sourceVertex.longitude,
//                    sourceVertex.latitude,
//                    targetVertex.longitude,
//                    targetVertex.latitude) * EARTH_RADIUS;
    }

    private static double haversin(double val) {
        return Math.pow(sin(val / 2), 2);
    }

    private static double centralAngle(double lambda1, double fi1, double lambda2, double fi2) {
        double deltaLambda = Math.abs(lambda1 - lambda2);
        double a = cos(fi2) * sin(deltaLambda);
        double b = cos(fi1) * sin(fi2) - sin(fi1) * cos(fi2) * cos(deltaLambda);
        double c = sin(fi1) * sin(fi2);
        double d = cos(fi1) * cos(fi2) * cos(deltaLambda);
        double nominator = sqrt(a * a + b * b);
        double denominator = c + d;
        return Math.atan(nominator / denominator);
    }

    static class GreatCircleHeuristic implements AStarAdmissibleHeuristic<Node> {

        @Override
        public double getCostEstimate(Node source, Node target) {
            return greatCircleDistance(source, target);
        }


        @Override
        public <E> boolean isConsistent(Graph<Node, E> graph) {
            return true;
        }
    }

    private class EuclideanDistance implements AStarAdmissibleHeuristic<Node> {
        @Override
        public double getCostEstimate(Node sourceVertex, Node targetVertex) {
            return sqrt(
                    Math.pow(sourceVertex.longitude - targetVertex.longitude, 2)
                            + Math.pow(sourceVertex.latitude - targetVertex.latitude, 2));
        }

        @Override
        public <E> boolean isConsistent(Graph<Node, E> graph) {
            return true;
        }
    }

    public static class Node {
        long id;
        double longitude;
        double latitude;

        Node(long id, double longitude, double latitude) {
            this.id = id;
            this.longitude = longitude;
            this.latitude = latitude;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return id == node.id &&
                    Double.compare(node.longitude, longitude) == 0 &&
                    Double.compare(node.latitude, latitude) == 0;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, longitude, latitude);
        }

        @Override
        public String toString() {
            return id + " " + longitude + " " + latitude;
        }
    }

//    public static void main(String[] args) throws RunnerException {
//        Options opt = new OptionsBuilder()
//                .include(ShortestPathPerformance.class.getSimpleName())
//
//                .forks(1)
//                .warmupForks(0)
//
//                .warmupIterations(3)
//                .warmupBatchSize(5)
//                .warmupTime(new TimeValue(10, TimeUnit.MINUTES))
//
//                .measurementIterations(5)
//                .measurementTime(new TimeValue(10, TimeUnit.MINUTES))
//                .measurementBatchSize(5)
//
//                .timeUnit(TimeUnit.MINUTES)
//
//                .jvmArgs("-Xms2048m", "-Xmx8192m")
//
////                .output("~/ch benchmarks/liechtenstein_50")
//                .build();
//        new Runner(opt).run();
//    }
}