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

public class ContractionHierarchyBidirectionalDijkstra<V, E> extends BidirectionalDijkstraShortestPath<V, E> {

    private final Graph<ContractionHierarchyAlgorithm.ContractionVertex<V>,
            ContractionHierarchyAlgorithm.ContractionEdge<E>> contractionGraph;
    private final Map<V, ContractionHierarchyAlgorithm.ContractionVertex<V>> contractionMapping;

    private final Supplier<AddressableHeap<Double, Pair<ContractionHierarchyAlgorithm.ContractionVertex<V>,
            ContractionHierarchyAlgorithm.ContractionEdge<E>>>> contractionGraphHeapSupplier;

    public ContractionHierarchyBidirectionalDijkstra(Graph<V, E> graph) {
        this(graph, new ContractionHierarchyAlgorithm<>(graph).computeContractionHierarchy());
    }

    public ContractionHierarchyBidirectionalDijkstra(Graph<V, E> graph,
                                                     Pair<Graph<ContractionHierarchyAlgorithm.ContractionVertex<V>,
                                                             ContractionHierarchyAlgorithm.ContractionEdge<E>>,
                                                             Map<V, ContractionHierarchyAlgorithm.ContractionVertex<V>>> p) {
        super(graph);
        this.contractionGraph = p.getFirst();
        this.contractionMapping = p.getSecond();
        this.contractionGraphHeapSupplier = PairingHeap::new;
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

        ContractionHierarchyAlgorithm.ContractionVertex<V> contractedSource = contractionMapping.get(source);
        ContractionHierarchyAlgorithm.ContractionVertex<V> contractedSink = contractionMapping.get(sink);

        // create frontiers
        ContractionSearchFrontier<ContractionHierarchyAlgorithm.ContractionVertex<V>,
                ContractionHierarchyAlgorithm.ContractionEdge<E>> forwardFrontier
                = new ContractionSearchFrontier<>(contractionGraph,
                contractionGraphHeapSupplier, e -> e.isUpward);


        ContractionSearchFrontier<ContractionHierarchyAlgorithm.ContractionVertex<V>,
                ContractionHierarchyAlgorithm.ContractionEdge<E>> backwardFrontier
                = new ContractionSearchFrontier<>(new EdgeReversedGraph<>(contractionGraph),
                contractionGraphHeapSupplier, e -> !e.isUpward);


        // initialize both frontiers
        forwardFrontier.updateDistance(contractedSource, null, 0d);
        backwardFrontier.updateDistance(contractedSink, null, 0d);

        // initialize best path
        double bestPath = Double.POSITIVE_INFINITY;
        ContractionHierarchyAlgorithm.ContractionVertex<V> bestPathCommonVertex = null;

        ContractionSearchFrontier<ContractionHierarchyAlgorithm.ContractionVertex<V>,
                ContractionHierarchyAlgorithm.ContractionEdge<E>> frontier = forwardFrontier;
        ContractionSearchFrontier<ContractionHierarchyAlgorithm.ContractionVertex<V>,
                ContractionHierarchyAlgorithm.ContractionEdge<E>> otherFrontier = backwardFrontier;

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
                AddressableHeap.Handle<Double, Pair<ContractionHierarchyAlgorithm.ContractionVertex<V>,
                        ContractionHierarchyAlgorithm.ContractionEdge<E>>> node = frontier.heap.deleteMin();
                ContractionHierarchyAlgorithm.ContractionVertex<V> v = node.getValue().getFirst();
                double vDistance = node.getKey();

                for (ContractionHierarchyAlgorithm.ContractionEdge<E> e : frontier.graph.outgoingEdgesOf(v)) {
                    if (!frontier.isUpwardDirection.apply(e)) { // skip downward edges
                        continue;
                    }

                    ContractionHierarchyAlgorithm.ContractionVertex<V> u = frontier.graph.getEdgeTarget(e);

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
                ContractionSearchFrontier<ContractionHierarchyAlgorithm.ContractionVertex<V>,
                        ContractionHierarchyAlgorithm.ContractionEdge<E>> tmpFrontier = frontier;
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
            BaseSearchFrontier<ContractionHierarchyAlgorithm.ContractionVertex<V>,
                    ContractionHierarchyAlgorithm.ContractionEdge<E>> forwardFrontier,
            BaseSearchFrontier<ContractionHierarchyAlgorithm.ContractionVertex<V>,
                    ContractionHierarchyAlgorithm.ContractionEdge<E>> backwardFrontier,
            double weight,
            ContractionHierarchyAlgorithm.ContractionVertex<V> source,
            ContractionHierarchyAlgorithm.ContractionVertex<V> commonVertex,
            ContractionHierarchyAlgorithm.ContractionVertex<V> sink) {

        LinkedList<E> edgeList = new LinkedList<>();
        LinkedList<V> vertexList = new LinkedList<>();

        // add common vertex
        vertexList.add(commonVertex.vertex);

        // traverse forward path
        ContractionHierarchyAlgorithm.ContractionVertex<V> v = commonVertex;
        while (true) {
            ContractionHierarchyAlgorithm.ContractionEdge<E> e = forwardFrontier.getTreeEdge(v);

            if (e == null) {
                break;
            }

            unpackBackward(e, vertexList, edgeList);
            v = contractionGraph.getEdgeSource(e);
        }

        // traverse reverse path
        v = commonVertex;
        while (true) {
            ContractionHierarchyAlgorithm.ContractionEdge<E> e = backwardFrontier.getTreeEdge(v);

            if (e == null) {
                break;
            }

            unpackForward(e, vertexList, edgeList);
            v = contractionGraph.getEdgeTarget(e);
        }

        return new GraphWalk<>(graph, source.vertex, sink.vertex, vertexList, edgeList, weight);
    }

    // add to the lists all edges E and vertices V that are in between
    // of source and target of #edge
    private void unpackBackward(
            ContractionHierarchyAlgorithm.ContractionEdge<E> edge,
            LinkedList<V> vertexList,
            LinkedList<E> edgeList) {
        if (edge.skippedEdges == null) {
            vertexList.addFirst(contractionGraph.getEdgeSource(edge).vertex);
            edgeList.addFirst(edge.edge);
        } else {
            unpackBackward(edge.skippedEdges.getSecond(), vertexList, edgeList);
            unpackBackward(edge.skippedEdges.getFirst(), vertexList, edgeList);
        }
    }

    private void unpackForward(
            ContractionHierarchyAlgorithm.ContractionEdge<E> edge,
            LinkedList<V> vertexList,
            LinkedList<E> edgeList) {
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
