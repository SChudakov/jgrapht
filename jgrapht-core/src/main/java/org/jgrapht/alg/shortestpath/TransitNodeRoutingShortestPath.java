package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.ManyToManyShortestPathsAlgorithm;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;

import java.util.Map;

import static org.jgrapht.alg.shortestpath.ContractionHierarchy.ContractionVertex;
import static org.jgrapht.alg.shortestpath.TransitNodeRouting.AccessVertex;
import static org.jgrapht.alg.shortestpath.TransitNodeRouting.AccessVertices;
import static org.jgrapht.alg.shortestpath.TransitNodeRouting.LocalityFiler;
import static org.jgrapht.alg.shortestpath.TransitNodeRouting.TransitNodeRoutingData;

public class TransitNodeRoutingShortestPath<V, E> extends BaseShortestPathAlgorithm<V, E> {

    private Map<V, ContractionVertex<V>> contractionMapping;

    private ShortestPathAlgorithm<V, E> localQueriesAlgorithm;

    private ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<V, E> manyToManyShortestPaths;
    private AccessVertices<V, E> accessVertices;
    private LocalityFiler<V> localityFiler;

    /**
     * Constructs a new instance of the algorithm for a given graph.
     *
     * @param graph the graph
     */
    public TransitNodeRoutingShortestPath(Graph<V, E> graph, TransitNodeRoutingData<V, E> transitNodeRoutingData) {
        super(graph);
        this.contractionMapping = transitNodeRoutingData.getContractionMapping();
        this.localityFiler = transitNodeRoutingData.getLocalityFiler();
        this.accessVertices = transitNodeRoutingData.getAccessVertices();
        this.manyToManyShortestPaths = transitNodeRoutingData.getTransitVerticesPaths();
        this.localQueriesAlgorithm = new ContractionHierarchyBidirectionalDijkstra<>(graph,
                transitNodeRoutingData.getContractionGraph(), transitNodeRoutingData.getContractionMapping());
    }

    @Override
    public GraphPath<V, E> getPath(V source, V sink) {
        if (localityFiler.isLocal(source, sink)) {
            return localQueriesAlgorithm.getPath(source, sink);
        } else {
            ContractionVertex<V> contractedSource = contractionMapping.get(source);
            ContractionVertex<V> contractedSink = contractionMapping.get(sink);

            AccessVertex<V, E> forwardAccessVertex = null;
            AccessVertex<V, E> backwardAccessVertex = null;
            double minimumWeight = Double.POSITIVE_INFINITY;

            for (AccessVertex<V, E> sourceAccessVertex : accessVertices.getForwardAccessVertices(contractedSource)) {
                for (AccessVertex<V, E> sinkAccessVertex : accessVertices.getBackwardAccessVertices(contractedSink)) {
                    double currentDistance = sourceAccessVertex.path.getWeight() +
                            manyToManyShortestPaths.getWeight(source, sink) +
                            sinkAccessVertex.path.getWeight();
                    if (currentDistance < minimumWeight) {
                        minimumWeight = currentDistance;
                        forwardAccessVertex = sourceAccessVertex;
                        backwardAccessVertex = sinkAccessVertex;
                    }
                }
            }

            return combinePaths(forwardAccessVertex.path, manyToManyShortestPaths.getPath(source, sink), backwardAccessVertex.path);
        }
    }

    private GraphPath<V, E> combinePaths(GraphPath<V, E> p1, GraphPath<V, E> p2, GraphPath<V, E> p3) {
        return null;
    }
}
