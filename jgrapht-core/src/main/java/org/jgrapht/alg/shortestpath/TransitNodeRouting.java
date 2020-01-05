package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.ManyToManyShortestPathsAlgorithm;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;

import static org.jgrapht.alg.shortestpath.TransitNodeRoutingPreprocessing.AccessVertex;
import static org.jgrapht.alg.shortestpath.TransitNodeRoutingPreprocessing.AccessVertices;
import static org.jgrapht.alg.shortestpath.TransitNodeRoutingPreprocessing.LocalityFiler;
import static org.jgrapht.alg.shortestpath.TransitNodeRoutingPreprocessing.TransitNodeRoutingData;

public class TransitNodeRouting<V, E> extends BaseShortestPathAlgorithm<V, E> {

    private LocalityFiler<V,E> localityFiler;
    private AccessVertices<V, E> accessVertices;
    private ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<V, E> manyToManyShortestPaths;
    private ShortestPathAlgorithm<V, E> localQueriesAlgorithm;

    /**
     * Constructs a new instance of the algorithm for a given graph.
     *
     * @param graph the graph
     */
    public TransitNodeRouting(Graph<V, E> graph, TransitNodeRoutingData<V, E> transitNodeRoutingData,
                              ShortestPathAlgorithm<V, E> localQueriesAlgorithm) {
        super(graph);
        this.localityFiler = transitNodeRoutingData.localityFiler;
        this.accessVertices = transitNodeRoutingData.accessVertices;
//        this.manyToManyShortestPaths = transitNodeRoutingData.manyToManyShortestPaths;
        this.localQueriesAlgorithm = localQueriesAlgorithm;
    }

    @Override
    public GraphPath<V, E> getPath(V source, V sink) {
        if (localityFiler.isLocal(source, sink)) {
            return localQueriesAlgorithm.getPath(source, sink);
        } else {
            AccessVertex<V, E> forwardAccessVertex = null;
            AccessVertex<V, E> backwardAccessVertex = null;
            double minimumWeight = Double.POSITIVE_INFINITY;

            for (AccessVertex<V, E> sourceAccessVertex : accessVertices.getAccessVertices(source)) {
                for (AccessVertex<V, E> sinkAccessVertex : accessVertices.getAccessVertices(sink)) {
                    double currentDistance = sourceAccessVertex.path.getWeight() +
                            manyToManyShortestPaths.getWeight(sourceAccessVertex.vertex, sinkAccessVertex.vertex) +
                            sinkAccessVertex.path.getWeight();
                    if (currentDistance < minimumWeight) {
                        minimumWeight = currentDistance;
                        forwardAccessVertex = sourceAccessVertex;
                        backwardAccessVertex = sinkAccessVertex;
                    }
                }
            }

            return combinePaths(forwardAccessVertex.path,
                    manyToManyShortestPaths.getPath(forwardAccessVertex.vertex, backwardAccessVertex.vertex),
                    backwardAccessVertex.path);
        }
    }

    private GraphPath<V, E> combinePaths(GraphPath<V, E> p1, GraphPath<V, E> p2, GraphPath<V, E> p3) {
        return null;
    }
}
