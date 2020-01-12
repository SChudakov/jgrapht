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

public class TransitNodeRouting<V, E> {

    private Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph;
    private Map<V, ContractionVertex<V>> contractionMapping;
    private int numberOfTransitVertices;

    private ManyToManyShortestPathsAlgorithm<V, E> manyToManyShortestPathsAlgorithm;
    private TransitVerticesSelection<V> transitVerticesSelection;

    public TransitNodeRouting(Graph<V, E> graph) {
        Pair<Graph<ContractionVertex<V>, ContractionEdge<E>>, Map<V, ContractionVertex<V>>> p
                = new ContractionHierarchy<>(graph).computeContractionHierarchy();
        init(graph, p.getFirst(), p.getSecond(), Math.max(1, (int) Math.sqrt(graph.vertexSet().size())),
                new TopKTransitVerticesSelection(p.getFirst()));
    }

    public TransitNodeRouting(Graph<V, E> graph, TransitVerticesSelection<V> transitVerticesSelection) {
        Pair<Graph<ContractionVertex<V>, ContractionEdge<E>>, Map<V, ContractionVertex<V>>> p
                = new ContractionHierarchy<>(graph).computeContractionHierarchy();
        init(graph, p.getFirst(), p.getSecond(), Math.max(1, (int) Math.sqrt(graph.vertexSet().size())), transitVerticesSelection);
    }

    public TransitNodeRouting(Graph<V, E> graph,
                              Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph,
                              Map<V, ContractionVertex<V>> contractionMapping) {
        init(graph, contractionGraph, contractionMapping, (int) Math.sqrt(graph.vertexSet().size()),
                new TopKTransitVerticesSelection(contractionGraph));
    }

    public TransitNodeRouting(Graph<V, E> graph,
                              Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph,
                              Map<V, ContractionVertex<V>> contractionMapping, int numberOfTransitVertices) {
        init(graph, contractionGraph, contractionMapping, numberOfTransitVertices, new TopKTransitVerticesSelection(contractionGraph));
    }

    public TransitNodeRouting(Graph<V, E> graph,
                              Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph,
                              Map<V, ContractionVertex<V>> contractionMapping, int numberOfTransitVertices,
                              TransitVerticesSelection<V> transitVerticesSelection) {
        init(graph, contractionGraph, contractionMapping, numberOfTransitVertices, transitVerticesSelection);
    }

    private void init(Graph<V, E> graph,
                      Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph,
                      Map<V, ContractionVertex<V>> contractionMapping, int numberOfTransitVertices,
                      TransitVerticesSelection<V> transitVerticesSelection) {
        if (numberOfTransitVertices > graph.vertexSet().size()) {
            throw new IllegalArgumentException("numberOfTransitVertices is larger than the number of vertices in the graph");
        }
        this.contractionGraph = contractionGraph;
        this.contractionMapping = contractionMapping;
        this.numberOfTransitVertices = numberOfTransitVertices;
        this.manyToManyShortestPathsAlgorithm = new CHManyToManyShortestPaths<>(graph, contractionGraph, contractionMapping);
        this.transitVerticesSelection = transitVerticesSelection;
    }


    public TransitNodeRoutingData<V, E> computeTransitNodeRoutingData() {
        Set<V> transitVertices = transitVerticesSelection.getTransitVertices(numberOfTransitVertices);
        Set<ContractionVertex<V>> contractedTransitVertices = transitVertices.stream()
                .map(v -> contractionMapping.get(v)).collect(Collectors.toCollection(HashSet::new));

        VoronoiDiagramComputation<V, E> voronoiDiagramComputation = new VoronoiDiagramComputation<>(
                contractionGraph, contractedTransitVertices);
        VoronoiDiagram<V> voronoiDiagram = voronoiDiagramComputation.computeVoronoiDiagram();

        // TODO: check possibility to compute both packed and unpacked transit nodes by @TransitNodesSelection
        Set<V> unpackedTransitVertices = contractedTransitVertices.stream().map(v -> v.vertex).collect(Collectors.toCollection(HashSet::new));
        ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<V, E>
                transitVerticesPaths = manyToManyShortestPathsAlgorithm.getManyToManyPaths(unpackedTransitVertices, unpackedTransitVertices);

        AVAndLFComputation<V, E> AVAndLFComputation = new AVAndLFComputation<>(contractionGraph,
                contractionMapping, contractedTransitVertices, voronoiDiagram, manyToManyShortestPathsAlgorithm, transitVerticesPaths);
        Pair<AccessVertices<V, E>, LocalityFiler<V>> p = AVAndLFComputation.computeAVAndLF();

        AccessVertices<V, E> accessVertices = p.getFirst();
        LocalityFiler<V> localityFiler = p.getSecond();

        return new TransitNodeRoutingData<>(contractionGraph, contractionMapping, contractedTransitVertices,
                transitVerticesPaths, localityFiler, accessVertices);
    }


    public static class TransitNodeRoutingData<V, E> {
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

        public TransitNodeRoutingData(Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph,
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

    public interface TransitVerticesSelection<V> {
        Set<V> getTransitVertices(int numOfTransitVertices);
    }

    private class TopKTransitVerticesSelection implements TransitVerticesSelection<V> {
        private Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph;

        TopKTransitVerticesSelection(Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph) {
            this.contractionGraph = contractionGraph;
        }

        public Set<V> getTransitVertices(int numOfTransitVertices) {
            int numOfVertices = contractionGraph.vertexSet().size();
            Set<V> result = new HashSet<>();
            for (ContractionVertex<V> vertex : contractionGraph.vertexSet()) {
                if (vertex.contractionLevel >= numOfVertices - numOfTransitVertices) {
                    result.add(vertex.vertex);
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

            CoveringSearch<V, E> forwardSearch = new CoveringSearch<>(new MaskSubgraph<>(contractionGraph,
                    v -> false, e -> !e.isUpward), transitVertices, voronoiDiagram);
            CoveringSearch<V, E> backwardSearch = new CoveringSearch<>(new MaskSubgraph<>(new EdgeReversedGraph<>(
                    contractionGraph), v -> false, e -> e.isUpward), transitVertices, voronoiDiagram);

            for (ContractionVertex<V> v : contractionGraph.vertexSet()) {
                Pair<Set<ContractionVertex<V>>, Set<Integer>> forwardSearchData = forwardSearch.runSearch(v);
                Pair<Set<ContractionVertex<V>>, Set<Integer>> backwardSearchData = backwardSearch.runSearch(v);

                accessVerticesBuilder.addForwardAccessVertices(v, forwardSearchData.getFirst());
                accessVerticesBuilder.addBackwardAccessVertices(v, backwardSearchData.getFirst());

                localityFilterBuilder.addForwardVisitedVoronoiCells(v, forwardSearchData.getSecond());
                localityFilterBuilder.addBackwardVisitedVoronoiCells(v, backwardSearchData.getSecond());
            }

            return Pair.of(accessVerticesBuilder.buildVertices(),
                    localityFilterBuilder.buildLocalityFilter(contractionMapping));
        }
    }

    // TODO: find better name to address access vertices and locality filter
    private static class CoveringSearch<V, E> {
        private Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph;

        private Set<ContractionVertex<V>> transitVertices;
        private VoronoiDiagram<V> voronoiDiagram;

        public CoveringSearch(Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph,
                              Set<ContractionVertex<V>> transitVertices,
                              VoronoiDiagram<V> voronoiDiagram) {
            this.contractionGraph = contractionGraph;

            this.transitVertices = transitVertices;
            this.voronoiDiagram = voronoiDiagram;
        }

        public Pair<Set<ContractionVertex<V>>, Set<Integer>> runSearch(ContractionVertex<V> vertex) {
            Set<ContractionVertex<V>> accessVertices = new HashSet<>();
            Set<Integer> visitedVoronoiCells = new HashSet<>();

            Set<Integer> visitedVerticesIds = new HashSet<>();
            Queue<ContractionVertex<V>> queue = new LinkedList<>();
            queue.add(vertex);

            while (!queue.isEmpty()) {
                ContractionVertex<V> v = queue.remove();
                visitedVerticesIds.add(v.vertexId);

                if (transitVertices.contains(v)) {
                    accessVertices.add(v);
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

        public void addForwardAccessVertices(ContractionVertex<V> v, Set<ContractionVertex<V>> vertices) {
            // TODO: avoid mapping vertices by extracting a dedicated method in @CHManyToManyShortestPaths
            Set<V> unpackedVertices = vertices.stream().map(av -> av.vertex).collect(Collectors.toCollection(HashSet::new));
            ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<V, E> manyToManyShortestPaths
                    = manyToManyShortestPathsAlgorithm.getManyToManyPaths(Collections.singleton(v.vertex), unpackedVertices);

            Set<V> prunedVertices = getPrunedAccessVertices(v.vertex, unpackedVertices, manyToManyShortestPaths, true);
            List<AccessVertex<V, E>> accessVerticesList = forwardAccessVertices.get(v.vertexId);
            for (V unpackedVertex : unpackedVertices) {
                if (!prunedVertices.contains(unpackedVertex)) {
                    accessVerticesList.add(new AccessVertex<>(unpackedVertex, manyToManyShortestPaths.getPath(v.vertex, unpackedVertex)));
                }
            }
        }


        public void addBackwardAccessVertices(ContractionVertex<V> v, Set<ContractionVertex<V>> vertices) {
            // TODO: avoid mapping vertices by extracting a dedicated method in @CHManyToManyShortestPaths
            Set<V> unpackedVertices = vertices.stream().map(av -> av.vertex).collect(Collectors.toCollection(HashSet::new));
            ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<V, E> manyToManyShortestPaths
                    = manyToManyShortestPathsAlgorithm.getManyToManyPaths(unpackedVertices, Collections.singleton(v.vertex));

            Set<V> prunedVertices = getPrunedAccessVertices(v.vertex, unpackedVertices, manyToManyShortestPaths, false);
            List<AccessVertex<V, E>> accessVerticesList = backwardAccessVertices.get(v.vertexId);
            for (V unpackedVertex : unpackedVertices) {
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
}
