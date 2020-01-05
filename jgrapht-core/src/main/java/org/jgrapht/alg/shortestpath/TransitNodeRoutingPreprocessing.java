package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.interfaces.ManyToManyShortestPathsAlgorithm;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jheaps.AddressableHeap;
import org.jheaps.tree.PairingHeap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jgrapht.alg.shortestpath.ContractionHierarchy.ContractionEdge;
import static org.jgrapht.alg.shortestpath.ContractionHierarchy.ContractionVertex;

public class TransitNodeRoutingPreprocessing<V, E> {

    private Graph<V, E> graph;
    private Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph;
    private Map<V, ContractionVertex<V>> contractionMapping;
    private int numOfTransitNodes;

    private TransitNodeSelection transitNodeSelection;
    private ManyToManyShortestPathsAlgorithm<ContractionVertex<V>, ContractionEdge<E>> manyToManyShortestPathsAlgorithm;
    private AccessVerticesDetermination<V, E> accessVerticesDetermination;

    public TransitNodeRoutingData<V, E> computeTransitNodeRoutingData() {
        Set<ContractionVertex<V>> transitNodes = transitNodeSelection.getTransitNodes(numOfTransitNodes);

        VoronoiDiagram<V, E> voronoiDiagram = new VoronoiDiagram<>(transitNodes);


        ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<ContractionVertex<V>, ContractionEdge<E>> manyToManyShortestPaths =
                manyToManyShortestPathsAlgorithm.getManyToManyPaths(transitNodes, transitNodes);


        Pair<AccessVertices<V, E>, LocalityFiler<V, E>> p = accessVerticesDetermination.performComputation();

        AccessVertices<V, E> accessVertices = p.getFirst();
        LocalityFiler<V, E> localityFiler = p.getSecond();


        return new TransitNodeRoutingData<>(manyToManyShortestPaths, localityFiler, accessVertices);
    }


    public static class TransitNodeRoutingData<V, E> {
        public ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<
                ContractionVertex<V>, ContractionEdge<E>> manyToManyShortestPaths;
        public LocalityFiler<V, E> localityFiler;
        public AccessVertices<V, E> accessVertices;

        public TransitNodeRoutingData(ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<ContractionVertex<V>,
                ContractionEdge<E>> manyToManyShortestPaths,
                                      LocalityFiler<V, E> localityFiler,
                                      AccessVertices<V, E> accessVertices) {
            this.manyToManyShortestPaths = manyToManyShortestPaths;
            this.localityFiler = localityFiler;
            this.accessVertices = accessVertices;
        }
    }


    public class TransitNodeSelection {
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
        private Map<V, ContractionVertex<V>> contractionMapping;

        private AddressableHeap<Double, Pair<ContractionVertex<V>, ContractionEdge<E>>> heap = new PairingHeap<>();
        private Map<ContractionVertex<V>,
                AddressableHeap.Handle<Double, Pair<ContractionVertex<V>, ContractionEdge<E>>>> seen = new HashMap<>();

        private Set<ContractionVertex<V>> transitNodes;
        private List<Integer> parent;
        private List<Integer> voronoiCell;
        private List<Double> distanceToCenter;

        public VoronoiDiagram(Set<ContractionVertex<V>> transitNodes) {
            this.transitNodes = transitNodes;
            parent = new ArrayList<>(contractionGraph.vertexSet().size());
            voronoiCell = new ArrayList<>(contractionGraph.vertexSet().size());
            distanceToCenter = new ArrayList<>(contractionGraph.vertexSet().size());
            for (int i = 0; i < transitNodes.size(); ++i) {
                parent.add(null);
                voronoiCell.add(null);
                distanceToCenter.add(null);
            }
            computeDiagram();
        }

        private void computeDiagram() {
            Graph<ContractionVertex<V>, ContractionEdge<E>> searchGraph = new EdgeReversedGraph<>(contractionGraph);

            for (ContractionVertex<V> transitNode : transitNodes) {
                updateDistance(transitNode, transitNode, null, 0.0);
            }

            while (!heap.isEmpty()) {
                AddressableHeap.Handle<Double, Pair<ContractionVertex<V>, ContractionEdge<E>>> entry = heap.deleteMin();

                double distance = entry.getKey();
                ContractionVertex<V> v = entry.getValue().getFirst();

                for (ContractionEdge<E> edge : searchGraph.outgoingEdgesOf(v)) {
                    ContractionVertex<V> successor = Graphs.getOppositeVertex(searchGraph, edge, v);

                    double updatedDistance = distance + searchGraph.getEdgeWeight(edge);
                    if (updatedDistance < distanceToCenter.get(successor.vertexId)) {
                        updateDistance(successor, v, edge, updatedDistance);
                    }
                }
            }
        }

        private void updateDistance(ContractionVertex<V> v, ContractionVertex<V> predecessor,
                                    ContractionEdge<E> e, double distance) {
            AddressableHeap.Handle<Double, Pair<ContractionVertex<V>, ContractionEdge<E>>> handle = seen.get(v);
            if (handle == null) {
                handle = heap.insert(distance, Pair.of(v, e));
                seen.put(v, handle);
                visitVertex(v, distance, predecessor);
            } else if (distance < handle.getKey()) {
                handle.decreaseKey(distance);
                handle.setValue(Pair.of(handle.getValue().getFirst(), e));
                visitVertex(v, distance, predecessor);
            }
        }

        //      computes distanceToCenter & voronoiCell relations
        private void visitVertex(ContractionVertex<V> vertex, double distance, ContractionVertex<V> parent) {
            this.parent.set(vertex.vertexId, parent.vertexId);
            this.distanceToCenter.set(vertex.vertexId, distance);
            this.voronoiCell.set(vertex.vertexId, (vertex.vertexId == parent.vertexId) ? vertex.vertexId : this.voronoiCell.get(parent.vertexId));
        }
    }

    public static class AccessVerticesDetermination<V, E> {
        private Graph<ContractionVertex<V>, ContractionEdge<E>> graph;
        private Map<V, ContractionVertex<V>> contractionMapping;

        private Set<ContractionVertex<V>> transitNodes;
        private VoronoiDiagram voronoiDiagram;
        private double markingRadius;
        private ManyToManyShortestPathsAlgorithm
                .ManyToManyShortestPaths<ContractionVertex<V>, ContractionEdge<E>> manyToManyShortestPaths;

        public AccessVerticesDetermination(Graph<ContractionVertex<V>, ContractionEdge<E>> graph,
                                           Map<V, ContractionVertex<V>> contractionMapping,
                                           Set<ContractionVertex<V>> transitNodes,
                                           VoronoiDiagram voronoiDiagram,
                                           double markingRadius,
                                           ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<
                                                   ContractionVertex<V>, ContractionEdge<E>> manyToManyShortestPaths) {
            this.graph = graph;
            this.contractionMapping = contractionMapping;
            this.transitNodes = transitNodes;
            this.voronoiDiagram = voronoiDiagram;
            this.markingRadius = markingRadius;
            this.manyToManyShortestPaths = manyToManyShortestPaths;
        }

        public Pair<AccessVertices<V, E>, LocalityFiler<V, E>> performComputation() {
            MarkedVertices<V, E> markedVertices = new MarkedVertices<>(graph, contractionMapping, transitNodes);
            Set<ContractionVertex<V>> markedVerticesSet = markedVertices.computeMarkingVertices(markingRadius);


            LocalityFilterConstructor<V, E> constructor = new LocalityFilterConstructor<>(graph.vertexSet().size(), voronoiDiagram);

            CoveringNodesToAccessNodesConverter<V, E> converter =
                    new CoveringNodesToAccessNodesConverter<>(transitNodes, markedVertices, manyToManyShortestPaths);


            CoveringSearch<V, E> search = new CoveringSearch<>(graph, contractionMapping, markedVerticesSet, constructor);


            for (ContractionVertex<V> v : graph.vertexSet()) {
                search.run(v);
                constructor.addSearchSpace(v, search);
                converter.computeAccessNodes(v, search.getCoveringNodes());
            }

            return Pair.of(converter.getAccessVertices(), constructor.buildLocalityFilter());
        }
    }


    private static class MarkedVertices<V, E> {
        private Graph<ContractionVertex<V>, ContractionEdge<E>> graph;
        private Map<V, ContractionVertex<V>> contractionMapping;
        private Set<ContractionVertex<V>> transitNodes;

        public MarkedVertices(Graph<ContractionVertex<V>,
                ContractionEdge<E>> graph, Map<V, ContractionVertex<V>> contractionMapping,
                              Set<ContractionVertex<V>> transitNodes) {
            this.graph = graph;
            this.contractionMapping = contractionMapping;
            this.transitNodes = transitNodes;
        }

        public Set<ContractionVertex<V>> computeMarkingVertices(double radius) {
            throw new UnsupportedOperationException();
        }
    }

    private static class LocalityFilterConstructor<V, E> {
        private int numberOfVertices;
        private VoronoiDiagram<V, E> voronoiDiagram;

        public LocalityFilterConstructor(int numberOfVertices, VoronoiDiagram<V, E> voronoiDiagram) {
            this.numberOfVertices = numberOfVertices;
            this.voronoiDiagram = voronoiDiagram;
        }

        public void addSearchSpace(ContractionVertex<V> v, CoveringSearch search) {
            throw new UnsupportedOperationException();
        }

        public LocalityFiler<V, E> buildLocalityFilter() {
            throw new UnsupportedOperationException();
        }
    }

    private static class CoveringNodesToAccessNodesConverter<V, E> {
        private Set<ContractionVertex<V>> transitNodes;
        private MarkedVertices<V, E> markedVertices;
        private ManyToManyShortestPathsAlgorithm
                .ManyToManyShortestPaths<ContractionVertex<V>, ContractionEdge<E>> manyToManyShortestPaths;

        public CoveringNodesToAccessNodesConverter(Set<ContractionVertex<V>> transitNodes,
                                                   MarkedVertices<V, E> markedVertices,
                                                   ManyToManyShortestPathsAlgorithm
                                                           .ManyToManyShortestPaths<ContractionVertex<V>,
                                                           ContractionEdge<E>> manyToManyShortestPaths) {
            this.transitNodes = transitNodes;
            this.markedVertices = markedVertices;
            this.manyToManyShortestPaths = manyToManyShortestPaths;
        }

        public void computeAccessNodes(ContractionVertex<V> v, List<Pair<ContractionVertex<V>, Double>> coveringNodes) {
            throw new UnsupportedOperationException();
        }

        public AccessVertices<V, E> getAccessVertices() {
            pruneAccessNodes();
            throw new UnsupportedOperationException();
        }

        private void pruneAccessNodes() {
            throw new UnsupportedOperationException();
        }
    }

    private static class CoveringSearch<V, E> {
        private Graph<ContractionVertex<V>, ContractionEdge<E>> graph;
        private Map<V, ContractionVertex<V>> contractionMapping;
        private Set<ContractionVertex<V>> markedVerticesSet;
        private LocalityFilterConstructor<V, E> constructor;

        public CoveringSearch(Graph<ContractionVertex<V>, ContractionEdge<E>> graph,
                              Map<V, ContractionVertex<V>> contractionMapping,
                              Set<ContractionVertex<V>> markedVerticesSet,
                              LocalityFilterConstructor<V, E> constructor) {
            this.graph = graph;
            this.contractionMapping = contractionMapping;
            this.markedVerticesSet = markedVerticesSet;
            this.constructor = constructor;
        }

        public void run(ContractionVertex<V> v) {
            throw new UnsupportedOperationException();
        }

        public List<Pair<ContractionVertex<V>, Double>> getCoveringNodes() {
            throw new UnsupportedOperationException();
        }
    }


    public static class AccessVertices<V, E> {
        List<List<AccessVertex>> accessVerticesArray;

        public List<AccessVertex<V, E>> getAccessVertices(V vertex) {
            return null;
        }
    }

    public static class AccessVertex<V, E> {
        V vertex;
        int vertexIndex;
        GraphPath<V, E> path;
    }


    public static class LocalityFiler<V, E> {
        public boolean isLocal(V source, V sink) {
            return false;
        }
    }
}
