package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.interfaces.ManyToManyShortestPathsAlgorithm;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.graph.MaskSubgraph;
import org.jheaps.AddressableHeap;
import org.jheaps.tree.PairingHeap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import static org.jgrapht.alg.shortestpath.ContractionHierarchy.ContractionEdge;
import static org.jgrapht.alg.shortestpath.ContractionHierarchy.ContractionVertex;

public class TransitNodeRoutingPrecomputation<V, E> {

    private Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph;
    private Map<V, ContractionVertex<V>> contractionMapping;
    private int numberOfTransitVertices;

    private ManyToManyShortestPathsAlgorithm<V, E> manyToManyShortestPathsAlgorithm;

    public TransitNodeRoutingPrecomputation(Graph<V, E> graph) {
        Pair<Graph<ContractionVertex<V>, ContractionEdge<E>>, Map<V, ContractionVertex<V>>> p
                = new ContractionHierarchy<>(graph).computeContractionHierarchy();
        init(graph, p.getFirst(), p.getSecond(), Math.max(1, (int) Math.sqrt(graph.vertexSet().size())));
    }

    public TransitNodeRoutingPrecomputation(Graph<V, E> graph, TransitVerticesSelection<V> transitVerticesSelection) {
        Pair<Graph<ContractionVertex<V>, ContractionEdge<E>>, Map<V, ContractionVertex<V>>> p
                = new ContractionHierarchy<>(graph).computeContractionHierarchy();
        init(graph, p.getFirst(), p.getSecond(), Math.max(1, (int) Math.sqrt(graph.vertexSet().size())));
    }

    public TransitNodeRoutingPrecomputation(Graph<V, E> graph,
                                            Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph,
                                            Map<V, ContractionVertex<V>> contractionMapping) {
        init(graph, contractionGraph, contractionMapping, (int) Math.sqrt(graph.vertexSet().size()));
    }

    public TransitNodeRoutingPrecomputation(Graph<V, E> graph,
                                            Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph,
                                            Map<V, ContractionVertex<V>> contractionMapping, int numberOfTransitVertices) {
        init(graph, contractionGraph, contractionMapping, numberOfTransitVertices);
    }

    public TransitNodeRoutingPrecomputation(Graph<V, E> graph,
                                            Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph,
                                            Map<V, ContractionVertex<V>> contractionMapping, int numberOfTransitVertices,
                                            TransitVerticesSelection<V> transitVerticesSelection) {
        init(graph, contractionGraph, contractionMapping, numberOfTransitVertices);
    }

    private void init(Graph<V, E> graph,
                      Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph,
                      Map<V, ContractionVertex<V>> contractionMapping, int numberOfTransitVertices) {
        if (numberOfTransitVertices > graph.vertexSet().size()) {
            throw new IllegalArgumentException("numberOfTransitVertices is larger than the number of vertices in the graph");
        }
        this.contractionGraph = contractionGraph;
        this.contractionMapping = contractionMapping;
        this.numberOfTransitVertices = numberOfTransitVertices;
        this.manyToManyShortestPathsAlgorithm = new CHManyToManyShortestPaths<>(graph, contractionGraph, contractionMapping);
        System.out.println("number of transit vertices: " + numberOfTransitVertices);
    }


    public TransitNodeRouting<V, E> computeTransitNodeRouting() {
        TransitVerticesSelection<V> transitVerticesSelection = new TransitVerticesSelection<>(contractionGraph);
        Set<ContractionVertex<V>> contractedTransitVertices = transitVerticesSelection.getTransitVertices(numberOfTransitVertices);
        Set<V> transitVertices = contractedTransitVertices.stream().map(v -> v.vertex)
                .collect(Collectors.toCollection(HashSet::new));

        VoronoiDiagramComputation<V, E> voronoiDiagramComputation = new VoronoiDiagramComputation<>(
                contractionGraph, contractedTransitVertices);
        VoronoiDiagram<V> voronoiDiagram = voronoiDiagramComputation.computeVoronoiDiagram();
        voronoiDiagramStatistics(voronoiDiagram);

        ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<V, E> transitVerticesPaths
                = manyToManyShortestPathsAlgorithm.getManyToManyPaths(transitVertices, transitVertices);

        AVAndLFComputation<V, E> AVAndLFComputation = new AVAndLFComputation<>(contractionGraph, contractionMapping,
                contractedTransitVertices, voronoiDiagram, manyToManyShortestPathsAlgorithm, transitVerticesPaths);
        Pair<AccessVertices<V, E>, LocalityFiler<V>> avAndLf = AVAndLFComputation.computeAVAndLF();

        AccessVertices<V, E> accessVertices = avAndLf.getFirst();
        LocalityFiler<V> localityFiler = avAndLf.getSecond();
        accessVerticesStatistics(accessVertices);
        localityFilterStatistics(localityFiler);

        return new TransitNodeRouting<>(contractionGraph, contractionMapping, contractedTransitVertices,
                transitVerticesPaths, localityFiler, accessVertices);
    }


    public static class TransitNodeRouting<V, E> {
        private Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph;
        private Map<V, ContractionVertex<V>> contractionMapping;

        private Set<ContractionVertex<V>> transitVertices;
        private ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<V, E> transitVerticesPaths;
        private LocalityFiler<V> localityFiler;
        private AccessVertices<V, E> accessVertices;

        public Graph<ContractionVertex<V>, ContractionEdge<E>> getContractionGraph() {
            return contractionGraph;
        }

        public Map<V, ContractionVertex<V>> getContractionMapping() {
            return contractionMapping;
        }

        public Set<ContractionVertex<V>> getTransitVertices() {
            return transitVertices;
        }

        public ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<V, E> getTransitVerticesPaths() {
            return transitVerticesPaths;
        }

        public LocalityFiler<V> getLocalityFiler() {
            return localityFiler;
        }

        public AccessVertices<V, E> getAccessVertices() {
            return accessVertices;
        }

        public TransitNodeRouting(Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph,
                                  Map<V, ContractionVertex<V>> contractionMapping,
                                  Set<ContractionVertex<V>> transitVertices,
                                  ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<V, E> transitVerticesPaths,
                                  LocalityFiler<V> localityFiler,
                                  AccessVertices<V, E> accessVertices) {
            this.contractionGraph = contractionGraph;
            this.contractionMapping = contractionMapping;
            this.transitVertices = transitVertices;
            this.transitVerticesPaths = transitVerticesPaths;
            this.localityFiler = localityFiler;
            this.accessVertices = accessVertices;
        }
    }


    private class TransitVerticesSelection<V> {
        private Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph;

        TransitVerticesSelection(Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph) {
            this.contractionGraph = contractionGraph;
        }

        public Set<ContractionVertex<V>> getTransitVertices(int numOfTransitVertices) {
            int numOfVertices = contractionGraph.vertexSet().size();
            Set<ContractionVertex<V>> result = new HashSet<>(numOfTransitVertices);
            for (ContractionVertex<V> vertex : contractionGraph.vertexSet()) {
                if (vertex.contractionLevel >= numOfVertices - numOfTransitVertices) {
                    result.add(vertex);
                }
            }
            return result;
        }
    }


    private static class VoronoiDiagramComputation<V, E> {
        private Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph;
        private Set<ContractionVertex<V>> transitVertices;

        // TODO: provide supplier for heap implementation
        private AddressableHeap<Double, ContractionVertex<V>> heap;
        private Map<ContractionVertex<V>, AddressableHeap.Handle<Double, ContractionVertex<V>>> seen;

        private List<Integer> voronoiCells;
        private List<Double> distanceToCenter;

        VoronoiDiagramComputation(Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph, Set<ContractionVertex<V>> transitVertices) {
            this.contractionGraph = contractionGraph;
            this.transitVertices = transitVertices;
            this.heap = new PairingHeap<>();
            this.seen = new HashMap<>();
        }

        VoronoiDiagram<V> computeVoronoiDiagram() {
            int numberOfVertices = contractionGraph.vertexSet().size();
            voronoiCells = new ArrayList<>(numberOfVertices);
            distanceToCenter = new ArrayList<>(numberOfVertices);
            for (int i = 0; i < numberOfVertices; ++i) {
                voronoiCells.add(null);
                distanceToCenter.add(Double.POSITIVE_INFINITY);
            }

            Graph<ContractionVertex<V>, ContractionEdge<E>> searchGraph = new EdgeReversedGraph<>(contractionGraph);

            for (ContractionVertex<V> transitVertex : transitVertices) {
                updateDistance(transitVertex, transitVertex, 0.0);
            }

            while (!heap.isEmpty()) {
                AddressableHeap.Handle<Double, ContractionVertex<V>> entry = heap.deleteMin();

                double distance = entry.getKey();
                ContractionVertex<V> v = entry.getValue();

                for (ContractionEdge<E> edge : searchGraph.outgoingEdgesOf(v)) {
                    ContractionVertex<V> successor = Graphs.getOppositeVertex(searchGraph, edge, v);

                    double updatedDistance = distance + searchGraph.getEdgeWeight(edge);
                    if (updatedDistance < distanceToCenter.get(successor.vertexId)) {
                        updateDistance(successor, v, updatedDistance);
                    }
                }
            }

            return new VoronoiDiagram<>(voronoiCells);
        }

        private void updateDistance(ContractionVertex<V> v, ContractionVertex<V> predecessor, double distance) {
            AddressableHeap.Handle<Double, ContractionVertex<V>> handle = seen.get(v);
            if (handle == null) {
                handle = heap.insert(distance, v);
                seen.put(v, handle);
                visitVertex(v, predecessor, distance);
            } else if (distance < handle.getKey()) {
                handle.decreaseKey(distance);
                handle.setValue(handle.getValue());
                visitVertex(v, predecessor, distance);
            }
        }

        private void visitVertex(ContractionVertex<V> vertex, ContractionVertex<V> parent, double distance) {
            int updatedVoronoiCell;
            if (vertex.vertexId == parent.vertexId) {
                updatedVoronoiCell = vertex.vertexId;
            } else {
                updatedVoronoiCell = this.voronoiCells.get(parent.vertexId);
            }
            this.voronoiCells.set(vertex.vertexId, updatedVoronoiCell);
            this.distanceToCenter.set(vertex.vertexId, distance);
        }
    }

    private static class VoronoiDiagram<V> {
        private List<Integer> voronoiCells;

        public VoronoiDiagram(List<Integer> voronoiCells) {
            this.voronoiCells = voronoiCells;
        }

        Integer getVoronoiCellId(ContractionVertex<V> vertex) {
            return voronoiCells.get(vertex.vertexId);
        }
    }


    private static class AVAndLFComputation<V, E> {
        private Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph;
        private Map<V, ContractionVertex<V>> contractionMapping;

        private Set<ContractionVertex<V>> transitVertices;
        private VoronoiDiagram<V> voronoiDiagram;
        private ManyToManyShortestPathsAlgorithm<V, E> manyToManyShortestPathsAlgorithm;
        private ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<V, E> transitVerticesPaths;

        public AVAndLFComputation(Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph,
                                  Map<V, ContractionVertex<V>> contractionMapping,
                                  Set<ContractionVertex<V>> transitVertices,
                                  VoronoiDiagram<V> voronoiDiagram,
                                  ManyToManyShortestPathsAlgorithm<V, E> manyToManyShortestPathsAlgorithm,
                                  ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<V, E> transitVerticesPaths) {
            this.contractionGraph = contractionGraph;
            this.contractionMapping = contractionMapping;
            this.transitVertices = transitVertices;
            this.voronoiDiagram = voronoiDiagram;
            this.manyToManyShortestPathsAlgorithm = manyToManyShortestPathsAlgorithm;
            this.transitVerticesPaths = transitVerticesPaths;
        }

        public Pair<AccessVertices<V, E>, LocalityFiler<V>> computeAVAndLF() {
            LocalityFilterBuilder<V> localityFilterBuilder =
                    new LocalityFilterBuilder<>(contractionGraph.vertexSet().size());

            AccessVerticesBuilder<V, E> accessVerticesBuilder = new AccessVerticesBuilder<>(
                    manyToManyShortestPathsAlgorithm, transitVerticesPaths, contractionGraph.vertexSet().size());

            ContractionHierarchyBFS<V, E> forwardBFS = new ContractionHierarchyBFS<>(new MaskSubgraph<>(contractionGraph,
                    v -> false, e -> !e.isUpward), transitVertices, voronoiDiagram);
            ContractionHierarchyBFS<V, E> backwardBFS = new ContractionHierarchyBFS<>(new MaskSubgraph<>(new EdgeReversedGraph<>(
                    contractionGraph), v -> false, e -> e.isUpward), transitVertices, voronoiDiagram);

//            int vertex = 0;
            for (ContractionVertex<V> v : contractionGraph.vertexSet()) {
//                System.out.println(++vertex);
                Pair<Set<V>, Set<Integer>> forwardData = forwardBFS.runSearch(v);
                Pair<Set<V>, Set<Integer>> backwardData = backwardBFS.runSearch(v);

                accessVerticesBuilder.addForwardAccessVertices(v, forwardData.getFirst());
                accessVerticesBuilder.addBackwardAccessVertices(v, backwardData.getFirst());

                localityFilterBuilder.addForwardVisitedVoronoiCells(v, forwardData.getSecond());
                localityFilterBuilder.addBackwardVisitedVoronoiCells(v, backwardData.getSecond());
            }

            return Pair.of(accessVerticesBuilder.buildVertices(),
                    localityFilterBuilder.buildLocalityFilter(contractionMapping));
        }
    }

    private static class ContractionHierarchyBFS<V, E> {
        private Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph;

        private Set<ContractionVertex<V>> transitVertices;
        private VoronoiDiagram<V> voronoiDiagram;

        public ContractionHierarchyBFS(Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph,
                                       Set<ContractionVertex<V>> transitVertices,
                                       VoronoiDiagram<V> voronoiDiagram) {
            this.contractionGraph = contractionGraph;

            this.transitVertices = transitVertices;
            this.voronoiDiagram = voronoiDiagram;
        }

        public Pair<Set<V>, Set<Integer>> runSearch(ContractionVertex<V> vertex) {
            Set<V> accessVertices = new HashSet<>();
            Set<Integer> visitedVoronoiCells = new HashSet<>();

            Set<Integer> visitedVerticesIds = new HashSet<>();
            Queue<ContractionVertex<V>> queue = new LinkedList<>();
            queue.add(vertex);

            while (!queue.isEmpty()) {
                ContractionVertex<V> v = queue.remove();
                visitedVerticesIds.add(v.vertexId);

                if (transitVertices.contains(v)) {
                    accessVertices.add(v.vertex);
                } else {
                    visitedVoronoiCells.add(voronoiDiagram.getVoronoiCellId(v));

                    for (ContractionEdge<E> e : contractionGraph.outgoingEdgesOf(v)) {
                        ContractionVertex<V> successor = Graphs.getOppositeVertex(contractionGraph, e, v);
                        if (!visitedVerticesIds.contains(successor.vertexId)) {
                            queue.add(successor);
                        }
                    }
                }
            }

            return Pair.of(accessVertices, visitedVoronoiCells);
        }
    }


    public static class LocalityFiler<V> {
        private Map<V, ContractionVertex<V>> contractionMapping;

        private List<Set<Integer>> visitedForwardVoronoiCells;
        private List<Set<Integer>> visitedBackwardVoronoiCells;

        public LocalityFiler(Map<V, ContractionVertex<V>> contractionMapping,
                             List<Set<Integer>> visitedForwardVoronoiCells,
                             List<Set<Integer>> visitedBackwardVoronoiCells) {
            this.contractionMapping = contractionMapping;
            this.visitedForwardVoronoiCells = visitedForwardVoronoiCells;
            this.visitedBackwardVoronoiCells = visitedBackwardVoronoiCells;
        }

        public boolean isLocal(V source, V sink) {
            ContractionVertex<V> contractedSource = contractionMapping.get(source);
            ContractionVertex<V> contractedSink = contractionMapping.get(sink);

            Set<Integer> sourceVisitedVoronoiCells = visitedForwardVoronoiCells.get(contractedSource.vertexId);
            Set<Integer> sinkVisitedVoronoiCells = visitedBackwardVoronoiCells.get(contractedSink.vertexId);

            if (sourceVisitedVoronoiCells.contains(null) || sinkVisitedVoronoiCells.contains(null)) {
                return true;
            }

            Set<Integer> smallerSet;
            Set<Integer> largerSet;
            if (sourceVisitedVoronoiCells.size() <= sinkVisitedVoronoiCells.size()) {
                smallerSet = sourceVisitedVoronoiCells;
                largerSet = sinkVisitedVoronoiCells;
            } else {
                smallerSet = sinkVisitedVoronoiCells;
                largerSet = sourceVisitedVoronoiCells;
            }

            for (Integer visitedVoronoiCell : smallerSet) {
                if (largerSet.contains(visitedVoronoiCell)) {
                    return true;
                }
            }

            return false;
        }
    }

    private static class LocalityFilterBuilder<V> {
        private List<Set<Integer>> visitedForwardVoronoiCells;
        private List<Set<Integer>> visitedBackwardVoronoiCells;

        public LocalityFilterBuilder(int numberOfVertices) {
            this.visitedForwardVoronoiCells = new ArrayList<>(numberOfVertices);
            this.visitedBackwardVoronoiCells = new ArrayList<>(numberOfVertices);
            for (int i = 0; i < numberOfVertices; ++i) {
                visitedForwardVoronoiCells.add(null);
                visitedBackwardVoronoiCells.add(null);
            }
        }


        public void addForwardVisitedVoronoiCells(ContractionVertex<V> vertex, Set<Integer> visitedVoronoiCells) {
            this.visitedForwardVoronoiCells.set(vertex.vertexId, visitedVoronoiCells);
        }

        public void addBackwardVisitedVoronoiCells(ContractionVertex<V> vertex, Set<Integer> visitedVoronoiCells) {
            this.visitedBackwardVoronoiCells.set(vertex.vertexId, visitedVoronoiCells);
        }


        public LocalityFiler<V> buildLocalityFilter(Map<V, ContractionVertex<V>> contractionMapping) {
            return new LocalityFiler<>(contractionMapping, visitedForwardVoronoiCells, visitedBackwardVoronoiCells);
        }
    }


    public static class AccessVertices<V, E> {
        private List<List<AccessVertex<V, E>>> forwardAccessVertices;
        private List<List<AccessVertex<V, E>>> backwardAccessVertices;

        public AccessVertices(List<List<AccessVertex<V, E>>> forwardAccessVertices, List<List<AccessVertex<V, E>>> backwardAccessVertices) {
            this.forwardAccessVertices = forwardAccessVertices;
            this.backwardAccessVertices = backwardAccessVertices;
        }

        public List<AccessVertex<V, E>> getForwardAccessVertices(ContractionVertex<V> vertex) {
            return forwardAccessVertices.get(vertex.vertexId);
        }

        public List<AccessVertex<V, E>> getBackwardAccessVertices(ContractionVertex<V> vertex) {
            return backwardAccessVertices.get(vertex.vertexId);
        }
    }

    public static class AccessVertex<V, E> {
        V vertex;
        GraphPath<V, E> path;

        public AccessVertex(V vertex, GraphPath<V, E> path) {
            this.vertex = vertex;
            this.path = path;
        }
    }

    private static class AccessVerticesBuilder<V, E> {
        private ManyToManyShortestPathsAlgorithm<V, E> manyToManyShortestPathsAlgorithm;
        private ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<V, E> transitVerticesDistances;

        private List<List<AccessVertex<V, E>>> forwardAccessVertices;
        private List<List<AccessVertex<V, E>>> backwardAccessVertices;

        public AccessVerticesBuilder(ManyToManyShortestPathsAlgorithm<V, E> manyToManyShortestPathsAlgorithm,
                                     ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<V, E>
                                             transitVerticesDistances, int numberOfVertices) {
            this.manyToManyShortestPathsAlgorithm = manyToManyShortestPathsAlgorithm;
            this.transitVerticesDistances = transitVerticesDistances;

            this.forwardAccessVertices = new ArrayList<>(numberOfVertices);
            this.backwardAccessVertices = new ArrayList<>(numberOfVertices);
            for (int i = 0; i < numberOfVertices; ++i) {
                forwardAccessVertices.add(new ArrayList<>());
                backwardAccessVertices.add(new ArrayList<>());
            }
        }


        public AccessVertices<V, E> buildVertices() {
            return new AccessVertices<>(forwardAccessVertices, backwardAccessVertices);
        }

        public void addForwardAccessVertices(ContractionVertex<V> v, Set<V> vertices) {
            ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<V, E> manyToManyShortestPaths
                    = manyToManyShortestPathsAlgorithm.getManyToManyPaths(Collections.singleton(v.vertex), vertices);

            Set<V> prunedVertices = getPrunedAccessVertices(v.vertex, vertices, manyToManyShortestPaths, true);
            List<AccessVertex<V, E>> accessVerticesList = forwardAccessVertices.get(v.vertexId);
            for (V unpackedVertex : vertices) {
                if (!prunedVertices.contains(unpackedVertex)) {
                    accessVerticesList.add(new AccessVertex<>(unpackedVertex, manyToManyShortestPaths.getPath(v.vertex, unpackedVertex)));
                }
            }
        }


        public void addBackwardAccessVertices(ContractionVertex<V> v, Set<V> vertices) {
            ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<V, E> manyToManyShortestPaths
                    = manyToManyShortestPathsAlgorithm.getManyToManyPaths(vertices, Collections.singleton(v.vertex));

            Set<V> prunedVertices = getPrunedAccessVertices(v.vertex, vertices, manyToManyShortestPaths, false);
            List<AccessVertex<V, E>> accessVerticesList = backwardAccessVertices.get(v.vertexId);
            for (V unpackedVertex : vertices) {
                if (!prunedVertices.contains(unpackedVertex)) {
                    accessVerticesList.add(new AccessVertex<>(unpackedVertex, manyToManyShortestPaths.getPath(unpackedVertex, v.vertex)));
                }
            }
        }


        private Set<V> getPrunedAccessVertices(V v, Set<V> vertices, ManyToManyShortestPathsAlgorithm
                .ManyToManyShortestPaths<V, E> manyToManyShortestPaths, boolean forwardAccessVertices) {
            Set<V> result = new HashSet<>();
            for (V v1 : vertices) {
                if (!result.contains(v1)) {
                    for (V v2 : vertices) {
                        if (!v1.equals(v2) && !result.contains(v2)) {
                            if (forwardAccessVertices) {
                                if (manyToManyShortestPaths.getWeight(v, v1) + transitVerticesDistances.getWeight(v1, v2)
                                        <= manyToManyShortestPaths.getWeight(v, v2)) {
                                    result.add(v2);
                                }
                            } else {
                                if (transitVerticesDistances.getWeight(v2, v1) + manyToManyShortestPaths.getWeight(v1, v)
                                        <= manyToManyShortestPaths.getWeight(v2, v)) {
                                    result.add(v2);
                                }
                            }
                        }
                    }
                }
            }
            return result;
        }
    }

    private void voronoiDiagramStatistics(VoronoiDiagram<V> voronoiDiagram) {
        Map<Integer, Integer> counts = new HashMap<>();

        for (Integer id : voronoiDiagram.voronoiCells) {
            counts.compute(id, (key, value) -> {
                if (value == null) {
                    return 1;
                }
                return value + 1;
            });
        }

        List<Integer> sizes = new ArrayList<>(counts.values());
        sizes.sort(Integer::compareTo);
        System.out.println("voronoi cells sizes: " + sizes);

        int max = sizes.stream().max(Integer::compareTo).get();
        int min = sizes.stream().min(Integer::compareTo).get();
        double avg = sizes.stream().reduce((i1, i2) -> i1 + i2).get() / 149;

        System.out.println("max cell size: " + max);
        System.out.println("min cell size: " + min);
        System.out.println("avg cell size: " + avg);
    }

    private void accessVerticesStatistics(AccessVertices<V, E> accessVertices) {
        List<Integer> forwardAccessVerticesSizes = accessVertices.forwardAccessVertices.stream().map(av -> av.size()).collect(Collectors.toList());
        forwardAccessVerticesSizes.sort(Integer::compareTo);
        System.out.println("forward access vertices sizes: " + forwardAccessVerticesSizes);

        int forwardMax = forwardAccessVerticesSizes.stream().max(Integer::compareTo).get();
        int forwardMin = forwardAccessVerticesSizes.stream().min(Integer::compareTo).get();
        double forwardAvg = forwardAccessVerticesSizes.stream().reduce((i1, i2) -> i1 + i2).get() / (double) contractionGraph.vertexSet().size();

        System.out.println("max forward access vertices size: " + forwardMax);
        System.out.println("min forward access vertices size: " + forwardMin);
        System.out.println("avg forward access vertices size: " + forwardAvg);

        List<Integer> backwardAccessVerticesSizes = accessVertices.backwardAccessVertices.stream().map(av -> av.size()).collect(Collectors.toList());
        backwardAccessVerticesSizes.sort(Integer::compareTo);
        System.out.println("backward access vertices sizes: " + forwardAccessVerticesSizes);
        int backwardMax = backwardAccessVerticesSizes.stream().max(Integer::compareTo).get();
        int backwardMin = backwardAccessVerticesSizes.stream().min(Integer::compareTo).get();
        double backwardAvg = backwardAccessVerticesSizes.stream().reduce((i1, i2) -> i1 + i2).get() / (double) contractionGraph.vertexSet().size();

        System.out.println("max backward access vertices size: " + backwardMax);
        System.out.println("min backward access vertices size: " + backwardMin);
        System.out.println("avg backward access vertices size: " + backwardAvg);
    }

    private void localityFilterStatistics(LocalityFiler<V> localityFiler) {
        List<Integer> forwardVisitedVoronoiCells = localityFiler.visitedForwardVoronoiCells.stream().map(av -> av.size()).collect(Collectors.toList());
        forwardVisitedVoronoiCells.sort(Integer::compareTo);
        System.out.println("forward visited voronoi cells: " + forwardVisitedVoronoiCells);
        int forwardMax = forwardVisitedVoronoiCells.stream().max(Integer::compareTo).get();
        int forwardMin = forwardVisitedVoronoiCells.stream().min(Integer::compareTo).get();
        double forwardAvg = forwardVisitedVoronoiCells.stream().reduce((i1, i2) -> i1 + i2).get() / (double) contractionGraph.vertexSet().size();

        System.out.println("max forward visited voronoi cells size: " + forwardMax);
        System.out.println("min forward visited voronoi cells size: " + forwardMin);
        System.out.println("avg forward visited voronoi cells size: " + forwardAvg);

        List<Integer> backwardVisitedVoronoiCells = localityFiler.visitedBackwardVoronoiCells.stream().map(av -> av.size()).collect(Collectors.toList());
        backwardVisitedVoronoiCells.sort(Integer::compareTo);
        System.out.println("backward visited voronoi cells sizes: " + backwardVisitedVoronoiCells);
        int backwardMax = backwardVisitedVoronoiCells.stream().max(Integer::compareTo).get();
        int backwardMin = backwardVisitedVoronoiCells.stream().min(Integer::compareTo).get();
        double backwardAvg = backwardVisitedVoronoiCells.stream().reduce((i1, i2) -> i1 + i2).get() / (double) contractionGraph.vertexSet().size();

        System.out.println("max forward visited voronoi cells size: " + backwardMax);
        System.out.println("min forward visited voronoi cells size: " + backwardMin);
        System.out.println("avg forward visited voronoi cells size: " + backwardAvg);
    }
}
