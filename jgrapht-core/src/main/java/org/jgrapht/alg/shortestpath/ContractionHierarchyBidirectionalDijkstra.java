package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.graph.GraphWalk;
import org.jheaps.AddressableHeap;
import org.jheaps.tree.PairingHeap;

import java.util.LinkedList;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class ContractionHierarchyBidirectionalDijkstra<V, E> extends BidirectionalDijkstraShortestPath<V, E> {

    private final Graph<ContractionHierarchyAlgorithm.ContractionVertex<V>, ContractionHierarchyAlgorithm.ContractionEdge<E>> contractionGraph;
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
//        System.out.println(source + " " + sink + " \n");
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
        BiFunction<ContractionHierarchyAlgorithm.ContractionVertex<V>,
                ContractionHierarchyAlgorithm.ContractionVertex<V>, Boolean> forwardFunction
                = (sourceVertex, targetVertex) -> sourceVertex.contractionIndex < targetVertex.contractionIndex;
        ContractionSearchFrontier<ContractionHierarchyAlgorithm.ContractionVertex<V>,
                ContractionHierarchyAlgorithm.ContractionEdge<E>>
                forwardFrontier
                = new ContractionSearchFrontier<>(contractionGraph, contractionGraphHeapSupplier.get(), forwardFunction);


        BiFunction<ContractionHierarchyAlgorithm.ContractionVertex<V>,
                ContractionHierarchyAlgorithm.ContractionVertex<V>, Boolean> backwardFunction
                = (startVertex, endVertex) -> startVertex.contractionIndex < endVertex.contractionIndex;
        ContractionSearchFrontier<ContractionHierarchyAlgorithm.ContractionVertex<V>,
                ContractionHierarchyAlgorithm.ContractionEdge<E>>
                backwardFrontier;
        if (contractionGraph.getType().isDirected()) {
            backwardFrontier = new ContractionSearchFrontier<>(new EdgeReversedGraph<>(contractionGraph),
                    contractionGraphHeapSupplier.get(), backwardFunction);
        } else {
            backwardFrontier = new ContractionSearchFrontier<>(contractionGraph,
                    contractionGraphHeapSupplier.get(), backwardFunction);
        }


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
                    ContractionHierarchyAlgorithm.ContractionVertex<V> u = Graphs.getOppositeVertex(frontier.graph, e, v);

                    if (!frontier.isUpwardDirection.apply(v, u)) { // skip downward edges
                        continue;
                    }

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
                ContractionSearchFrontier<ContractionHierarchyAlgorithm.ContractionVertex<V>, ContractionHierarchyAlgorithm.ContractionEdge<E>>
                        tmpFrontier = frontier;
                frontier = otherFrontier;
                otherFrontier = tmpFrontier;
            }
        }

        // create path if found
        if (Double.isFinite(bestPath) && bestPath <= radius) {
//            GraphPath<V, E> result =  createPath(forwardFrontier, backwardFrontier,
//                    bestPath, contractedSource, bestPathCommonVertex, contractedSink);
//            GraphPath<ContractionHierarchyAlgorithm.ContractionVertex<V>, ContractionHierarchyAlgorithm.ContractionEdge<E>>
//                    contractedPath = new BidirectionalDijkstraShortestPath<>(contractionGraph).createPath(
//                    forwardFrontier,
//                    backwardFrontier,
//                    bestPath,
//                    contractedSource,
//                    bestPathCommonVertex,
//                    contractedSink
//            );
//            System.out.println("contracted path length: " + contractedPath.getLength());
//            System.out.println("unpacked path length: " + result.getLength());
//            return result;
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
            v = unpackForward(e, v, vertexList, edgeList);
        }

        // traverse reverse path
        v = commonVertex;
        while (true) {
            ContractionHierarchyAlgorithm.ContractionEdge<E> e = backwardFrontier.getTreeEdge(v);

            if (e == null) {
                break;
            }
            v = unpackBackward(e, v, vertexList, edgeList);
        }

        return new GraphWalk<>(graph, source.vertex, sink.vertex, vertexList, edgeList, weight);
    }

    // add to the lists all edges E and vertices V that are in between
    // of source and target of #edge
    private ContractionHierarchyAlgorithm.ContractionVertex<V> unpackForward(
            ContractionHierarchyAlgorithm.ContractionEdge<E> edge,
            ContractionHierarchyAlgorithm.ContractionVertex<V> vertex,
            LinkedList<V> vertexList,
            LinkedList<E> edgeList) {
        ContractionHierarchyAlgorithm.ContractionVertex<V> oppositeVertex;

        if (edge.skippedEdges == null) {
            oppositeVertex = Graphs.getOppositeVertex(contractionGraph, edge, vertex);
            vertexList.addFirst(oppositeVertex.vertex);
            edgeList.addFirst(edge.edge);
            return oppositeVertex;
        } else {
            if (graph.getType().isDirected()) {
                oppositeVertex = unpackForward(edge.skippedEdges.getSecond(), vertex, vertexList, edgeList);
                return unpackForward(edge.skippedEdges.getFirst(), oppositeVertex, vertexList, edgeList);
            } else {
                ContractionHierarchyAlgorithm.ContractionEdge<E> second = edge.skippedEdges.getSecond();
                if (contractionGraph.getEdgeTarget(second).equals(vertex) ||
                        contractionGraph.getEdgeSource(second).equals(vertex)) {
                    oppositeVertex = unpackForward(second, vertex, vertexList, edgeList);
                    return unpackForward(edge.skippedEdges.getFirst(), oppositeVertex, vertexList, edgeList);
                } else {
                    oppositeVertex = unpackForward(edge.skippedEdges.getFirst(), vertex, vertexList, edgeList);
                    return unpackForward(second, oppositeVertex, vertexList, edgeList);
                }
            }
        }
    }

    private ContractionHierarchyAlgorithm.ContractionVertex<V> unpackBackward(
            ContractionHierarchyAlgorithm.ContractionEdge<E> edge,
            ContractionHierarchyAlgorithm.ContractionVertex<V> vertex,
            LinkedList<V> vertexList,
            LinkedList<E> edgeList) {
        ContractionHierarchyAlgorithm.ContractionVertex<V> oppositeVertex;
        if (edge.skippedEdges == null) {
            oppositeVertex = Graphs.getOppositeVertex(contractionGraph, edge, vertex);
            vertexList.addLast(oppositeVertex.vertex);
            edgeList.addLast(edge.edge);
            return oppositeVertex;
        } else {
            if (graph.getType().isDirected()) {
                oppositeVertex = unpackBackward(edge.skippedEdges.getFirst(), vertex, vertexList, edgeList);
                return unpackBackward(edge.skippedEdges.getSecond(), oppositeVertex, vertexList, edgeList);
            } else {
                ContractionHierarchyAlgorithm.ContractionEdge<E> second = edge.skippedEdges.getSecond();
                if (contractionGraph.getEdgeTarget(second).equals(vertex) ||
                        contractionGraph.getEdgeSource(second).equals(vertex)) {
                    oppositeVertex = unpackBackward(second, vertex, vertexList, edgeList);
                    return unpackBackward(edge.skippedEdges.getFirst(), oppositeVertex, vertexList, edgeList);
                } else {
                    oppositeVertex = unpackBackward(edge.skippedEdges.getFirst(), vertex, vertexList, edgeList);
                    return unpackBackward(second, oppositeVertex, vertexList, edgeList);
                }
            }
        }
    }

    static class ContractionSearchFrontier<V1, E1>
            extends DijkstraSearchFrontier<V1, E1> {
        final BiFunction<V1, V1, Boolean> isUpwardDirection;
        boolean isFinished;

        ContractionSearchFrontier(Graph<V1, E1> graph,
                                  AddressableHeap<Double, Pair<V1, E1>> heap,
                                  BiFunction<V1, V1, Boolean> isUpwardDirection) {
            super(graph, heap);
            this.isUpwardDirection = isUpwardDirection;
        }
    }
}
