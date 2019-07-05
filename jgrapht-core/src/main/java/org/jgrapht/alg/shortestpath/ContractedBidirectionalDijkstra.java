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

public class ContractedBidirectionalDijkstra<V, E> extends BidirectionalDijkstraShortestPath<V, E> {

    private final Graph<GraphContractor.ContractionVertex<V>, GraphContractor.ContractionEdge<E>> contractionGraph;
    private final Map<V, GraphContractor.ContractionVertex<V>> contractionMapping;

    private final Supplier<AddressableHeap<Double, Pair<GraphContractor.ContractionVertex<V>,
            GraphContractor.ContractionEdge<E>>>> contractionGraphHeapSupplier;

    public ContractedBidirectionalDijkstra(Graph<V, E> graph,
                                           Graph<GraphContractor.ContractionVertex<V>,
                                                   GraphContractor.ContractionEdge<E>> contractionGraph,
                                           Map<V, GraphContractor.ContractionVertex<V>> contractionMapping) {
        super(graph);
        this.contractionGraph = contractionGraph;
        this.contractionMapping = contractionMapping;
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

        // create frontiers
        ContractionSearchFrontier<GraphContractor.ContractionVertex<V>, GraphContractor.ContractionEdge<E>>
                forwardFrontier
                = new ContractionSearchFrontier<>(contractionGraph, contractionGraphHeapSupplier.get(),
                (edge, sourceVertex) -> sourceVertex.contractionIndex
                        < Graphs.getOppositeVertex(contractionGraph, edge, sourceVertex).contractionIndex);


        ContractionSearchFrontier<GraphContractor.ContractionVertex<V>, GraphContractor.ContractionEdge<E>>
                backwardFrontier
                = new ContractionSearchFrontier<>(new EdgeReversedGraph<>(contractionGraph),
                contractionGraphHeapSupplier.get(),
                (edge, sourceVertex) -> sourceVertex.contractionIndex
                        > Graphs.getOppositeVertex(contractionGraph, edge, sourceVertex).contractionIndex);

        GraphContractor.ContractionVertex<V> contractedSource = contractionMapping.get(source);
        GraphContractor.ContractionVertex<V> contractedSink = contractionMapping.get(sink);

        // initialize both frontiers
        forwardFrontier.updateDistance(contractedSource, null, 0d);
        backwardFrontier.updateDistance(contractedSink, null, 0d);

        // initialize best path
        double bestPath = Double.POSITIVE_INFINITY;
        GraphContractor.ContractionVertex<V> bestPathCommonVertex = null;

        ContractionSearchFrontier<GraphContractor.ContractionVertex<V>, GraphContractor.ContractionEdge<E>>
                frontier = forwardFrontier;
        ContractionSearchFrontier<GraphContractor.ContractionVertex<V>, GraphContractor.ContractionEdge<E>>
                otherFrontier = backwardFrontier;

        while (true) {
            // stopping condition
            if (frontier.heap.isEmpty() || otherFrontier.heap.isEmpty()
                    || frontier.heap.findMin().getKey()
                    + otherFrontier.heap.findMin().getKey() >= bestPath) {
                break;
            }

            // frontier scan
            AddressableHeap.Handle<Double, Pair<GraphContractor.ContractionVertex<V>,
                    GraphContractor.ContractionEdge<E>>> node = frontier.heap.deleteMin();
            GraphContractor.ContractionVertex<V> v = node.getValue().getFirst();
            double vDistance = node.getKey();

            for (GraphContractor.ContractionEdge<E> e : frontier.graph.outgoingEdgesOf(v)) {
                if (!frontier.isUpwardEdge.apply(e, v)) {
                    continue;
                }

                GraphContractor.ContractionVertex<V> u = Graphs.getOppositeVertex(frontier.graph, e, v);

                double eWeight = frontier.graph.getEdgeWeight(e);

                frontier.updateDistance(u, e, vDistance + eWeight);

                // check path with u's distance from the other frontier
                double pathDistance = vDistance + eWeight + otherFrontier.getDistance(u);

                if (pathDistance < bestPath) {
                    bestPath = pathDistance;
                    bestPathCommonVertex = u;
                }

            }

            // swap frontiers
            ContractionSearchFrontier<GraphContractor.ContractionVertex<V>, GraphContractor.ContractionEdge<E>>
                    tmpFrontier = frontier;
            frontier = otherFrontier;
            otherFrontier = tmpFrontier;

        }

        // create path if found
        if (Double.isFinite(bestPath) && bestPath <= radius) {
            return createPath(forwardFrontier, backwardFrontier,
                    bestPath, contractedSource, bestPathCommonVertex, contractedSink);
        } else {
            return createEmptyPath(source, sink);
        }
    }


    protected GraphPath<V, E> createPath(
            BaseSearchFrontier<GraphContractor.ContractionVertex<V>,
                    GraphContractor.ContractionEdge<E>> forwardFrontier,
            BaseSearchFrontier<GraphContractor.ContractionVertex<V>,
                    GraphContractor.ContractionEdge<E>> backwardFrontier,
            double weight,
            GraphContractor.ContractionVertex<V> source,
            GraphContractor.ContractionVertex<V> commonVertex,
            GraphContractor.ContractionVertex<V> sink) {

        LinkedList<E> edgeList = new LinkedList<>();
        LinkedList<V> vertexList = new LinkedList<>();

        // add common vertex
        vertexList.add(commonVertex.vertex);

        // traverse forward path
        GraphContractor.ContractionVertex<V> v = commonVertex;
        while (true) {
            GraphContractor.ContractionEdge<E> e = forwardFrontier.getTreeEdge(v);

            if (e == null) {
                break;
            }

            unpackForward(e, vertexList, edgeList);
            v = Graphs.getOppositeVertex(forwardFrontier.graph, e, v);
        }

        // traverse reverse path
        v = commonVertex;
        while (true) {
            GraphContractor.ContractionEdge<E> e = backwardFrontier.getTreeEdge(v);

            if (e == null) {
                break;
            }

            unpackBackward(e, vertexList, edgeList);
            v = Graphs.getOppositeVertex(backwardFrontier.graph, e, v);
        }

        return new GraphWalk<>(graph, source.vertex, sink.vertex, vertexList, edgeList, weight);
    }

    // add to the lists all edges E and vertices V that are in between
    // of source and target of #edge
    private void unpackForward(GraphContractor.ContractionEdge<E> edge,
                               LinkedList<V> vertexList,
                               LinkedList<E> edgeList) {
        if (edge.skippedEdges == null) {
            vertexList.addFirst(contractionGraph.getEdgeSource(edge).vertex);
            edgeList.addFirst(edge.edge);
        } else {
            unpackForward(edge.skippedEdges.getSecond(), vertexList, edgeList);
            unpackForward(edge.skippedEdges.getFirst(), vertexList, edgeList);
        }
    }

    private void unpackBackward(GraphContractor.ContractionEdge<E> edge,
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
        // TODO: change to determining upwarness with respect to source vertex (undirected graph case)
        final BiFunction<E1, V1, Boolean> isUpwardEdge;

        ContractionSearchFrontier(Graph<V1, E1> graph,
                                  AddressableHeap<Double, Pair<V1, E1>> heap,
                                  BiFunction<E1, V1, Boolean> isUpwardEdge) {
            super(graph, heap);
            this.isUpwardEdge = isUpwardEdge;
        }
    }
}
