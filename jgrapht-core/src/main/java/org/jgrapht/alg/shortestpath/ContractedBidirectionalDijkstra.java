package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jheaps.AddressableHeap;

import java.util.LinkedList;
import java.util.function.Function;

public class ContractedBidirectionalDijkstra<V, E> extends BidirectionalDijkstraShortestPath<V, E> {

    private Graph<GraphContractor.ContractionVertex<V>, GraphContractor.ContractionEdge<E>> contractionGraph;

    public ContractedBidirectionalDijkstra(Graph<V, E> graph) {
        super(graph);
        GraphContractor<V, E> contractor = new GraphContractor<>(graph);
        contractionGraph = contractor.computeContractionHierarchy();
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
        ContractionSearchFrontier forwardFrontier = new ContractionSearchFrontier(graph,
                contractionGraph,
                edge -> contractionGraph.getEdgeSource(edge).contractionIndex
                        < contractionGraph.getEdgeTarget(edge).contractionIndex);
        ContractionSearchFrontier backwardFrontier = new ContractionSearchFrontier(new EdgeReversedGraph<>(graph),
                contractionGraph,
                edge -> contractionGraph.getEdgeSource(edge).contractionIndex
                        > contractionGraph.getEdgeTarget(edge).contractionIndex);


        // initialize both frontiers
        forwardFrontier.updateDistance(source, null, 0d);
        backwardFrontier.updateDistance(sink, null, 0d);

        // initialize best path
        double bestPath = Double.POSITIVE_INFINITY;
        V bestPathCommonVertex = null;

        DijkstraSearchFrontier frontier = forwardFrontier;
        DijkstraSearchFrontier otherFrontier = backwardFrontier;

        while (true) {
            // stopping condition
            if (frontier.heap.isEmpty() || otherFrontier.heap.isEmpty()
                    || frontier.heap.findMin().getKey()
                    + otherFrontier.heap.findMin().getKey() >= bestPath) {
                break;
            }

            // frontier scan
            AddressableHeap.Handle<Double, Pair<V, E>> node = frontier.heap.deleteMin();
            V v = node.getValue().getFirst();
            double vDistance = node.getKey();

            for (E e : frontier.graph.outgoingEdgesOf(v)) {
                V u = Graphs.getOppositeVertex(frontier.graph, e, v);

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
            DijkstraSearchFrontier tmpFrontier = frontier;
            frontier = otherFrontier;
            otherFrontier = tmpFrontier;

        }

        // create path if found
        if (Double.isFinite(bestPath) && bestPath <= radius) {
            return createPath(
                    forwardFrontier, backwardFrontier, bestPath, source, bestPathCommonVertex, sink);
        } else {
            return createEmptyPath(source, sink);
        }
    }

    @Override
    protected GraphPath<V, E> createPath(BaseSearchFrontier forwardFrontier,
                                         BaseSearchFrontier backwardFrontier,
                                         double weight, V source, V commonVertex, V sink) {
        return null;
    }

    private void unpack(GraphContractor.ContractionEdge edge, LinkedList<GraphContractor.ContractionEdge> path, boolean forward) {

    }

    class ContractionSearchFrontier extends DijkstraSearchFrontier {
        final Graph<GraphContractor.ContractionVertex<V>, GraphContractor.ContractionEdge<E>> contractionGraph;
        final Function<GraphContractor.ContractionEdge<E>, Boolean> isUpwardEdge;

        ContractionSearchFrontier(Graph<V, E> graph,
                                  Graph<GraphContractor.ContractionVertex<V>, GraphContractor.ContractionEdge<E>> contractionGraph,
                                  Function<GraphContractor.ContractionEdge<E>, Boolean> isUpwardEdge) {
            super(graph);
            this.contractionGraph = contractionGraph
            this.isUpwardEdge = isUpwardEdge;
        }
    }
}
