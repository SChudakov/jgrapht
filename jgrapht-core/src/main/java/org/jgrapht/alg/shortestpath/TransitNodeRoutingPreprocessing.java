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

public class TransitNodeRoutingPreprocessing<V, E> {

    private Graph<V, E> graph;
    private Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph;
    private Map<V, ContractionVertex<V>> contractionMapping;
    private int numOfTransitNodes;

    private ManyToManyShortestPathsAlgorithm<V, E> manyToManyShortestPathsAlgorithm;

    public TransitNodeRoutingPreprocessing(Graph<V, E> graph,
                                           Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph,
                                           Map<V, ContractionVertex<V>> contractionMapping, int numOfTransitNodes) {
        this.graph = graph;
        this.contractionGraph = contractionGraph;
        this.contractionMapping = contractionMapping;
        this.numOfTransitNodes = numOfTransitNodes;

        this.manyToManyShortestPathsAlgorithm = new CHManyToManyShortestPaths<>(graph, contractionGraph, contractionMapping);
    }

    public TransitNodeRoutingData<V, E> computeTransitNodeRoutingData() {
        TransitNodeSelection<V, E> transitNodeSelection = new TransitNodeSelection<>(contractionGraph);
        Set<ContractionVertex<V>> transitNodes = transitNodeSelection.getTransitNodes(numOfTransitNodes);

        VoronoiDiagram<V, E> voronoiDiagram = new VoronoiDiagram<>(contractionGraph, transitNodes);

        Set<V> unpackedTransitNodes = transitNodes.stream().map(v -> v.vertex).collect(Collectors.toCollection(HashSet::new));
        ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<V, E>
                manyToManyShortestPaths = manyToManyShortestPathsAlgorithm.getManyToManyPaths(unpackedTransitNodes, unpackedTransitNodes);

        AccessVerticesDetermination<V, E> accessVerticesDetermination = new AccessVerticesDetermination<>(contractionGraph,
                contractionMapping, transitNodes, voronoiDiagram, manyToManyShortestPathsAlgorithm, manyToManyShortestPaths);
        Pair<AccessVertices<V, E>, LocalityFiler<V>> p = accessVerticesDetermination.performComputation();

        AccessVertices<V, E> accessVertices = p.getFirst();
        LocalityFiler<V> localityFiler = p.getSecond();


        return new TransitNodeRoutingData<>(contractionGraph, contractionMapping,
                manyToManyShortestPaths, localityFiler, accessVertices);
    }


    public static class TransitNodeRoutingData<V, E> {
        public Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph;
        public Map<V, ContractionVertex<V>> contractionMapping;

        public ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<V, E> manyToManyShortestPaths;
        public LocalityFiler<V> localityFiler;
        public AccessVertices<V, E> accessVertices;

        public TransitNodeRoutingData(Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph,
                                      Map<V, ContractionVertex<V>> contractionMapping,
                                      ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<V, E> manyToManyShortestPaths,
                                      LocalityFiler<V> localityFiler,
                                      AccessVertices<V, E> accessVertices) {
            this.contractionGraph = contractionGraph;
            this.contractionMapping = contractionMapping;
            this.manyToManyShortestPaths = manyToManyShortestPaths;
            this.localityFiler = localityFiler;
            this.accessVertices = accessVertices;
        }
    }


    public static class TransitNodeSelection<V, E> {
        private Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph;

        public TransitNodeSelection(Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph) {
            this.contractionGraph = contractionGraph;
        }

        Set<ContractionVertex<V>> getTransitNodes(int numOfTransitVertices) {
            int numOfVertices = contractionGraph.vertexSet().size();
            Set<ContractionVertex<V>> result = new HashSet<>();
            for (ContractionVertex<V> vertex : contractionGraph.vertexSet()) {
                if (vertex.contractionLevel >= numOfVertices - numOfTransitVertices) {
                    result.add(vertex);
                }
            }
            return result;
        }
    }

    private static class VoronoiDiagram<V, E> {
        private Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph;

        private AddressableHeap<Double, ContractionVertex<V>> heap;
        private Map<ContractionVertex<V>,
                AddressableHeap.Handle<Double, ContractionVertex<V>>> seen;

        private Set<ContractionVertex<V>> transitNodes;
        private List<Integer> parent;
        private List<Integer> voronoiCell;
        private List<Double> distanceToCenter;

        public VoronoiDiagram(Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph,
                              Set<ContractionVertex<V>> transitNodes) {
            this.contractionGraph = contractionGraph;
            this.transitNodes = transitNodes;

            parent = new ArrayList<>(contractionGraph.vertexSet().size());
            voronoiCell = new ArrayList<>(contractionGraph.vertexSet().size());
            distanceToCenter = new ArrayList<>(contractionGraph.vertexSet().size());
            for (int i = 0; i < transitNodes.size(); ++i) {
                parent.add(null);
                voronoiCell.add(null);
                distanceToCenter.add(null);
            }

            heap = new PairingHeap<>();
            seen = new HashMap<>();

            computeDiagram();
        }

        private Integer getVoronoiCellId(ContractionVertex<V> vertex) {
            return voronoiCell.get(vertex.vertexId);
        }


        private void computeDiagram() {
            Graph<ContractionVertex<V>, ContractionEdge<E>> searchGraph = new EdgeReversedGraph<>(contractionGraph);

            for (ContractionVertex<V> transitNode : transitNodes) {
                updateDistance(transitNode, transitNode, 0.0);
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
            int voronoiCell = (vertex.vertexId == parent.vertexId) ? vertex.vertexId : this.voronoiCell.get(parent.vertexId);
            this.voronoiCell.set(vertex.vertexId, voronoiCell);
            this.parent.set(vertex.vertexId, parent.vertexId);
            this.distanceToCenter.set(vertex.vertexId, distance);
        }
    }

    private static class AccessVerticesDetermination<V, E> {
        private Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph;
        private Map<V, ContractionVertex<V>> contractionMapping;

        private Set<ContractionVertex<V>> transitNodes;
        private VoronoiDiagram<V, E> voronoiDiagram;
        private ManyToManyShortestPathsAlgorithm<V, E> manyToManyShortestPathsAlgorithm;
        private ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<V, E> manyToManyShortestPaths;

        public AccessVerticesDetermination(Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph,
                                           Map<V, ContractionVertex<V>> contractionMapping,
                                           Set<ContractionVertex<V>> transitNodes,
                                           VoronoiDiagram<V, E> voronoiDiagram,
                                           ManyToManyShortestPathsAlgorithm<V, E> manyToManyShortestPathsAlgorithm,
                                           ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<V, E> manyToManyShortestPaths) {
            this.contractionGraph = contractionGraph;
            this.contractionMapping = contractionMapping;
            this.transitNodes = transitNodes;
            this.voronoiDiagram = voronoiDiagram;
            this.manyToManyShortestPathsAlgorithm = manyToManyShortestPathsAlgorithm;
            this.manyToManyShortestPaths = manyToManyShortestPaths;
        }

        public Pair<AccessVertices<V, E>, LocalityFiler<V>> performComputation() {
            LocalityFilterConstructor<V> localityFilterConstructor =
                    new LocalityFilterConstructor<>(contractionGraph.vertexSet().size());

            AccessVerticesConstructor<V, E> accessVerticesConstructor = new AccessVerticesConstructor<>(
                    manyToManyShortestPathsAlgorithm, manyToManyShortestPaths, contractionGraph.vertexSet().size());

            CoveringSearch<V, E> forwardSearch = new CoveringSearch<>(new MaskSubgraph<>(contractionGraph,
                    v -> false, e -> !e.isUpward), transitNodes, voronoiDiagram);
            CoveringSearch<V, E> backwardSearch = new CoveringSearch<>(new MaskSubgraph<>(new EdgeReversedGraph<>(
                    contractionGraph), v -> false, e -> e.isUpward), transitNodes, voronoiDiagram);

            for (ContractionVertex<V> v : contractionGraph.vertexSet()) {
                Pair<Set<ContractionVertex<V>>, Set<Integer>> forwardSearchData = forwardSearch.run(v);
                Pair<Set<ContractionVertex<V>>, Set<Integer>> backwardSearchData = backwardSearch.run(v);

                accessVerticesConstructor.addForwardAccessVertices(v, forwardSearchData.getFirst());
                accessVerticesConstructor.addBackwardAccessVertices(v, backwardSearchData.getFirst());

                localityFilterConstructor.addForwardVisitedVoronoiCells(v, forwardSearchData.getSecond());
                localityFilterConstructor.addBackwardVisitedVoronoiCells(v, backwardSearchData.getSecond());
            }

            return Pair.of(accessVerticesConstructor.buildVertices(),
                    localityFilterConstructor.buildLocalityFilter(contractionMapping));
        }
    }


    private static class CoveringSearch<V, E> {
        private Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph;

        private Set<ContractionVertex<V>> transitVertices;
        private VoronoiDiagram<V, E> voronoiDiagram;

        public CoveringSearch(Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph,
                              Set<ContractionVertex<V>> transitVertices,
                              VoronoiDiagram<V, E> voronoiDiagram) {
            this.contractionGraph = contractionGraph;

            this.transitVertices = transitVertices;
            this.voronoiDiagram = voronoiDiagram;
        }

        public Pair<Set<ContractionVertex<V>>, Set<Integer>> run(ContractionVertex<V> vertex) {
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

    private static class LocalityFilterConstructor<V> {
        private List<Set<Integer>> visitedForwardVoronoiCells;
        private List<Set<Integer>> visitedBackwardVoronoiCells;

        public LocalityFilterConstructor(int numberOfVertices) {
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
        V accessVertex;
        GraphPath<V, E> path;

        public AccessVertex(V accessVertex, GraphPath<V, E> path) {
            this.accessVertex = accessVertex;
            this.path = path;
        }
    }

    private static class AccessVerticesConstructor<V, E> {
        private ManyToManyShortestPathsAlgorithm<V, E> manyToManyShortestPathsAlgorithm;
        private ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<V, E> transitVerticesDistances;

        private List<List<AccessVertex<V, E>>> forwardAccessVertices;
        private List<List<AccessVertex<V, E>>> backwardAccessVertices;

        public AccessVerticesConstructor(ManyToManyShortestPathsAlgorithm<V, E> manyToManyShortestPathsAlgorithm,
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
                    accessVerticesList.add(new AccessVertex<>(v.vertex, manyToManyShortestPaths.getPath(v.vertex, unpackedVertex)));
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
                    accessVerticesList.add(new AccessVertex<>(v.vertex, manyToManyShortestPaths.getPath(unpackedVertex, v.vertex)));
                }
            }
        }


        private Set<V> getPrunedAccessVertices(V v, Set<V> vertices, ManyToManyShortestPathsAlgorithm
                .ManyToManyShortestPaths<V, E> manyToManyShortestPaths, boolean forwardAccessVertices) {
            Set<V> result = new HashSet<>();
            for (V v1 : vertices) {
                if (!result.contains(v1)) {
                    for (V v2 : vertices) {
                        if (!result.contains(v2)) {
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
