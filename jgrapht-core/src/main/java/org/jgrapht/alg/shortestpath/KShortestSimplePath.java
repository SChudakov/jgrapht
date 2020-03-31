package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.alg.interfaces.KShortestPathAlgorithm;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.GraphWalk;
import org.jgrapht.util.CollectionUtil;
import org.jheaps.AddressableHeap;
import org.jheaps.tree.PairingHeap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class KShortestSimplePath<V, E> implements KShortestPathAlgorithm<V, E> {

    private final Graph<V, E> graph;

    private AddressableHeap<Double, Pair<GraphPath<V, E>, Set<V>>> pathsHeap;
    private Map<V, Integer> pathsCount;


    public KShortestSimplePath(Graph<V, E> graph) {
        this.graph = graph;
        this.pathsHeap = new PairingHeap<>();
        this.pathsCount = CollectionUtil.newHashMapWithExpectedSize(graph.vertexSet().size());
        for (V v : graph.vertexSet()) {
            pathsCount.put(v, 0);
        }
    }

    public KShortestSimplePath(Graph<V, E> graph, PathValidator<V, E> pathValidator) {
        this.graph = graph;
        this.pathsHeap = new PairingHeap<>();
        this.pathsCount = CollectionUtil.newHashMapWithExpectedSize(graph.vertexSet().size());
        for (V v : graph.vertexSet()) {
            pathsCount.put(v, 0);
        }
    }


    @Override
    public List<GraphPath<V, E>> getPaths(V source, V sink, int k) {
        if (!graph.containsVertex(source)) {
            throw new IllegalArgumentException("graph must contain source vertex!");
        }
        if (!graph.containsVertex(sink)) {
            throw new IllegalArgumentException("graph must contain sink vertex!");
        }
        if (k < 0) {
            throw new IllegalArgumentException("k should be positive");
        }
        List<GraphPath<V, E>> result = new ArrayList<>();

        GraphPath<V, E> initialPath = new GraphWalk<>(graph, source, source,
                Collections.singletonList(source), Collections.emptyList(), 0.0);
        pathsHeap.insert(0.0, Pair.of(initialPath, Collections.singleton(source)));

        while (!pathsHeap.isEmpty() && pathsCount.get(sink) < k) {
            Pair<GraphPath<V, E>, Set<V>> p = pathsHeap.deleteMin().getValue();
            GraphPath<V, E> currentPath = p.getFirst();
            Set<V> currentVertices = p.getSecond();

            V endVertex = currentPath.getEndVertex();
            pathsCount.compute(endVertex, (key, value) -> value + 1);

            if (endVertex.equals(sink)) {
                result.add(currentPath);
            } else {
                if (pathsCount.get(endVertex) <= k) {
                    for (E outEdge : graph.outgoingEdgesOf(endVertex)) {
                        V oppositeVertex = Graphs.getOppositeVertex(graph, outEdge, currentPath.getEndVertex());
                        if (currentVertices.contains(oppositeVertex)) {
                            continue;
                        }

                        GraphPath<V, E> concatenatedPath = concatenate(currentPath, oppositeVertex, outEdge);

                        Set<V> concatenatedVertices = new HashSet<>(currentVertices);
                        concatenatedVertices.add(oppositeVertex);
//                        System.out.println(concatenatedVertices.size());
                        pathsHeap.insert(concatenatedPath.getWeight(), Pair.of(concatenatedPath, concatenatedVertices));
                    }
                }
            }
        }

        return result;
    }

    private GraphPath<V, E> concatenate(GraphPath<V, E> path, V vertex, E edge) {
        List<V> vertices = new ArrayList<>(Collections.nCopies(path.getVertexList().size() + 1, null));
        Collections.copy(vertices, path.getVertexList());
        vertices.set(vertices.size() - 1, vertex);

        List<E> edges = new ArrayList<>(Collections.nCopies(path.getEdgeList().size() + 1, null));
        Collections.copy(edges, path.getEdgeList());
        edges.set(edges.size() - 1, edge);

        double weight = path.getWeight() + graph.getEdgeWeight(edge);

        return new GraphWalk<>(graph, path.getStartVertex(), vertex, vertices, edges, weight);
    }
}
