package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.graph.GraphWalk;
import org.jheaps.AddressableHeap;
import org.jheaps.tree.PairingHeap;

import java.util.LinkedList;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.jgrapht.alg.shortestpath.BidirectionalDijkstraShortestPath.DijkstraSearchFrontier;
import static org.jgrapht.alg.shortestpath.ContractionHierarchyAlgorithm.ContractionEdge;
import static org.jgrapht.alg.shortestpath.ContractionHierarchyAlgorithm.ContractionVertex;

public class ContractionHierarchyBidirectionalDijkstra<V, E> extends BaseShortestPathAlgorithm<V, E> {

    private Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph;
    private Map<V, ContractionVertex<V>> contractionMapping;

    private Supplier<AddressableHeap<Double, Pair<ContractionVertex<V>,
            ContractionEdge<E>>>> contractionGraphHeapSupplier;

    private double radius;

    public ContractionHierarchyBidirectionalDijkstra(Graph<V, E> graph) {
        super(graph);
        Pair<Graph<ContractionVertex<V>, ContractionEdge<E>>, Map<V, ContractionVertex<V>>> p
                = new ContractionHierarchyAlgorithm<>(graph).computeContractionHierarchy();
        init(p.getFirst(), p.getSecond(), Double.POSITIVE_INFINITY, PairingHeap::new);
    }

    public ContractionHierarchyBidirectionalDijkstra(Graph<V, E> graph,
                                                     Graph<ContractionVertex<V>, ContractionEdge<E>> contractedGraph,
                                                     Map<V, ContractionVertex<V>> contractionMapping) {
        this(graph, contractedGraph, contractionMapping, Double.POSITIVE_INFINITY, PairingHeap::new);
    }

    public ContractionHierarchyBidirectionalDijkstra(Graph<V, E> graph,
                                                     Graph<ContractionVertex<V>, ContractionEdge<E>> contractedGraph,
                                                     Map<V, ContractionVertex<V>> contractionMapping,
                                                     double radius,
                                                     Supplier<AddressableHeap<Double, Pair<ContractionVertex<V>,
                                                             ContractionEdge<E>>>> heapSupplier) {
        super(graph);
        init(contractedGraph, contractionMapping, radius, heapSupplier);
    }

    private void init(Graph<ContractionVertex<V>, ContractionEdge<E>> contractedGraph,
                      Map<V, ContractionVertex<V>> contractionMapping,
                      double radius,
                      Supplier<AddressableHeap<Double, Pair<ContractionVertex<V>, ContractionEdge<E>>>> heapSupplier) {
        this.contractionGraph = contractedGraph;
        this.contractionMapping = contractionMapping;
        this.radius = radius;
        this.contractionGraphHeapSupplier = heapSupplier;
    }

    @Override
    public GraphPath<V, E> getPath(V source, V sink) {
        if (!graph.containsVertex(source)) {
            throw new IllegalArgumentException(GRAPH_MUST_CONTAIN_THE_SOURCE_VERTEX);
        }
        if (!graph.containsVertex(sink)) {
            throw new IllegalArgumentException(GRAPH_MUST_CONTAIN_THE_SINK_VERTEX);
        }

        // handle special case if source equals target
        if (source.equals(sink)) {
            return createEmptyPath(source, sink);
        }

        ContractionVertex<V> contractedSource = contractionMapping.get(source);
        ContractionVertex<V> contractedSink = contractionMapping.get(sink);

        // create frontiers
        ContractionSearchFrontier<ContractionVertex<V>, ContractionEdge<E>> forwardFrontier
                = new ContractionSearchFrontier<>(contractionGraph,
                contractionGraphHeapSupplier, e -> e.isUpward);


        ContractionSearchFrontier<ContractionVertex<V>, ContractionEdge<E>> backwardFrontier
                = new ContractionSearchFrontier<>(new EdgeReversedGraph<>(contractionGraph),
                contractionGraphHeapSupplier, e -> !e.isUpward);


        // initialize both frontiers
        forwardFrontier.updateDistance(contractedSource, null, 0d);
        backwardFrontier.updateDistance(contractedSink, null, 0d);

        // initialize best path
        double bestPath = Double.POSITIVE_INFINITY;
        ContractionVertex<V> bestPathCommonVertex = null;

        ContractionSearchFrontier<ContractionVertex<V>, ContractionEdge<E>> frontier = forwardFrontier;
        ContractionSearchFrontier<ContractionVertex<V>, ContractionEdge<E>> otherFrontier = backwardFrontier;

        while (true) {
            if (frontier.heap.isEmpty()) {
                frontier.isFinished = true;
            }
            if (otherFrontier.heap.isEmpty()) {
                otherFrontier.isFinished = true;
            }

            // stopping condition for search
            if (frontier.isFinished && otherFrontier.isFinished) {
                break;
            }

            // stopping condition for current frontier
            if (frontier.heap.findMin().getKey() >= bestPath) {
                frontier.isFinished = true;
            } else {
                // frontier scan
                AddressableHeap.Handle<Double, Pair<ContractionVertex<V>, ContractionEdge<E>>> node
                        = frontier.heap.deleteMin();
                ContractionVertex<V> v = node.getValue().getFirst();
                double vDistance = node.getKey();

                for (ContractionEdge<E> e : frontier.graph.outgoingEdgesOf(v)) {
                    if (!frontier.isUpwardDirection.apply(e)) { // skip downward edges
                        continue;
                    }

                    ContractionVertex<V> u = frontier.graph.getEdgeTarget(e);

                    double eWeight = frontier.graph.getEdgeWeight(e);

                    frontier.updateDistance(u, e, vDistance + eWeight);

                    // check path with u's distance from the other frontier
                    double pathDistance = vDistance + eWeight + otherFrontier.getDistance(u);

                    if (pathDistance < bestPath) {
                        bestPath = pathDistance;
                        bestPathCommonVertex = u;
                    }
                }
            }

            // swap frontiers
            if (!otherFrontier.isFinished) {
                ContractionSearchFrontier<ContractionVertex<V>,
                        ContractionEdge<E>> tmpFrontier = frontier;
                frontier = otherFrontier;
                otherFrontier = tmpFrontier;
            }
        }

        // create path if found
        if (Double.isFinite(bestPath) && bestPath <= radius) {
            return createPath(forwardFrontier, backwardFrontier,
                    bestPath, contractedSource, bestPathCommonVertex, contractedSink);
        } else {
            return createEmptyPath(source, sink);
        }
    }

    private GraphPath<V, E> createPath(
            ContractionSearchFrontier<ContractionVertex<V>, ContractionEdge<E>> forwardFrontier,
            ContractionSearchFrontier<ContractionVertex<V>, ContractionEdge<E>> backwardFrontier,
            double weight,
            ContractionVertex<V> source,
            ContractionVertex<V> commonVertex,
            ContractionVertex<V> sink) {

        LinkedList<E> edgeList = new LinkedList<>();
        LinkedList<V> vertexList = new LinkedList<>();

        // add common vertex
        vertexList.add(commonVertex.vertex);

        // traverse forward path
        ContractionVertex<V> v = commonVertex;
        while (true) {
            ContractionEdge<E> e = forwardFrontier.getTreeEdge(v);

            if (e == null) {
                break;
            }

            unpackBackward(e, vertexList, edgeList);
            v = contractionGraph.getEdgeSource(e);
        }

        // traverse reverse path
        v = commonVertex;
        while (true) {
            ContractionEdge<E> e = backwardFrontier.getTreeEdge(v);

            if (e == null) {
                break;
            }

            unpackForward(e, vertexList, edgeList);
            v = contractionGraph.getEdgeTarget(e);
        }

        return new GraphWalk<>(graph, source.vertex, sink.vertex, vertexList, edgeList, weight);
    }

    private void unpackBackward(ContractionEdge<E> edge, LinkedList<V> vertexList, LinkedList<E> edgeList) {
        if (edge.skippedEdges == null) {
            vertexList.addFirst(contractionGraph.getEdgeSource(edge).vertex);
            edgeList.addFirst(edge.edge);
        } else {
            unpackBackward(edge.skippedEdges.getSecond(), vertexList, edgeList);
            unpackBackward(edge.skippedEdges.getFirst(), vertexList, edgeList);
        }
    }

    private void unpackForward(ContractionEdge<E> edge, LinkedList<V> vertexList, LinkedList<E> edgeList) {
        if (edge.skippedEdges == null) {
            vertexList.addLast(contractionGraph.getEdgeTarget(edge).vertex);
            edgeList.addLast(edge.edge);
        } else {
            unpackForward(edge.skippedEdges.getFirst(), vertexList, edgeList);
            unpackForward(edge.skippedEdges.getSecond(), vertexList, edgeList);
        }
    }

    static class ContractionSearchFrontier<V1, E1>
            extends DijkstraSearchFrontier<V1, E1> {
        final Function<E1, Boolean> isUpwardDirection;
        boolean isFinished;

        ContractionSearchFrontier(Graph<V1, E1> graph,
                                  Supplier<AddressableHeap<Double, Pair<V1, E1>>> heapSupplier,
                                  Function<E1, Boolean> isUpwardDirection) {
            super(graph, heapSupplier);
            this.isUpwardDirection = isUpwardDirection;
        }
    }
}
