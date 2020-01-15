package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.interfaces.ManyToManyShortestPathsAlgorithm;
import org.jgrapht.alg.interfaces.ShortestPathAlgorithm;
import org.jgrapht.graph.GraphWalk;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.jgrapht.alg.shortestpath.ContractionHierarchy.ContractionVertex;
import static org.jgrapht.alg.shortestpath.TransitNodeRoutingPrecomputation.AccessVertex;
import static org.jgrapht.alg.shortestpath.TransitNodeRoutingPrecomputation.AccessVertices;
import static org.jgrapht.alg.shortestpath.TransitNodeRoutingPrecomputation.LocalityFiler;
import static org.jgrapht.alg.shortestpath.TransitNodeRoutingPrecomputation.TransitNodeRouting;

public class TransitNodeRoutingShortestPath<V, E> extends BaseShortestPathAlgorithm<V, E> {

    private Map<V, ContractionVertex<V>> contractionMapping;

    private ShortestPathAlgorithm<V, E> localQueriesAlgorithm;

    private ManyToManyShortestPathsAlgorithm.ManyToManyShortestPaths<V, E> manyToManyShortestPaths;
    private AccessVertices<V, E> accessVertices;
    private LocalityFiler<V> localityFiler;

    public int isLocalCount;

    public TransitNodeRoutingShortestPath(Graph<V, E> graph) {
        super(graph);
        init(new TransitNodeRoutingPrecomputation<>(graph).computeTransitNodeRouting());
    }

    /**
     * Constructs a new instance of the algorithm for a given graph.
     *
     * @param graph the graph
     */
    public TransitNodeRoutingShortestPath(Graph<V, E> graph, TransitNodeRouting<V, E> transitNodeRouting) {
        super(graph);
        init(transitNodeRouting);
    }

    private void init(TransitNodeRouting<V, E> transitNodeRouting) {
        this.contractionMapping = transitNodeRouting.getContractionMapping();
        this.localityFiler = transitNodeRouting.getLocalityFiler();
        this.accessVertices = transitNodeRouting.getAccessVertices();
        this.manyToManyShortestPaths = transitNodeRouting.getTransitVerticesPaths();
        this.localQueriesAlgorithm = new ContractionHierarchyBidirectionalDijkstra<>(graph,
                transitNodeRouting.getContractionGraph(), transitNodeRouting.getContractionMapping());
    }

    @Override
    public GraphPath<V, E> getPath(V source, V sink) {
        if (localityFiler.isLocal(source, sink)) {
            ++isLocalCount;
            return localQueriesAlgorithm.getPath(source, sink);
        } else {
            ContractionVertex<V> contractedSource = contractionMapping.get(source);
            ContractionVertex<V> contractedSink = contractionMapping.get(sink);

            AccessVertex<V, E> forwardAccessVertex = null;
            AccessVertex<V, E> backwardAccessVertex = null;
            double minimumWeight = Double.POSITIVE_INFINITY;

            for (AccessVertex<V, E> sourceAccessVertex : accessVertices.getForwardAccessVertices(contractedSource)) {
                for (AccessVertex<V, E> sinkAccessVertex : accessVertices.getBackwardAccessVertices(contractedSink)) {
                    double currentWeight = sourceAccessVertex.path.getWeight() +
                            manyToManyShortestPaths.getWeight(sourceAccessVertex.vertex, sinkAccessVertex.vertex) +
                            sinkAccessVertex.path.getWeight();
                    if (currentWeight < minimumWeight) {
                        minimumWeight = currentWeight;
                        forwardAccessVertex = sourceAccessVertex;
                        backwardAccessVertex = sinkAccessVertex;
                    }
                }
            }

            if (minimumWeight == Double.POSITIVE_INFINITY) {
                return createEmptyPath(source, sink);
            }

            return combinePaths(forwardAccessVertex.path,
                    manyToManyShortestPaths.getPath(forwardAccessVertex.vertex, backwardAccessVertex.vertex),
                    backwardAccessVertex.path);
        }
    }

    private GraphPath<V, E> combinePaths(GraphPath<V, E> first, GraphPath<V, E> second, GraphPath<V, E> third) {
        V startVertex = first.getStartVertex();
        V endVertex = third.getEndVertex();
        double totalWeight = first.getWeight() + second.getWeight() + third.getWeight();

        int vertexListSize = first.getVertexList().size() + second.getVertexList().size() + third.getVertexList().size() - 2;
        List<V> vertexList = new ArrayList<>(vertexListSize);
        int edgeListSize = first.getLength() + second.getLength() + third.getLength();
        List<E> edgeList = new ArrayList<>(edgeListSize);

        int transitVerticesPathLength = second.getEdgeList().size();
//        System.out.println("transit vertices length: " + transitVerticesPathLength);
//        System.out.println("total path length: " + edgeListSize);
//        System.out.println("ratio: " + (double) transitVerticesPathLength / edgeListSize);

        // form vertex list
        Iterator<V> firstIt = first.getVertexList().iterator();
        while (firstIt.hasNext()) {
            V element = firstIt.next();
            if (firstIt.hasNext()) {
                vertexList.add(element);
            }
        }
        vertexList.addAll(second.getVertexList());
        Iterator<V> thirdIt = third.getVertexList().iterator();
        thirdIt.next();
        while (thirdIt.hasNext()) {
            vertexList.add(thirdIt.next());
        }

        // form edge list
        edgeList.addAll(first.getEdgeList());
        edgeList.addAll(second.getEdgeList());
        edgeList.addAll(third.getEdgeList());

        return new GraphWalk<>(graph, startVertex, endVertex, vertexList, edgeList, totalWeight);
    }
}
