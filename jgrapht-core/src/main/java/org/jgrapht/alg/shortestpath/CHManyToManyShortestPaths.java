package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.util.Pair;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.graph.GraphWalk;
import org.jgrapht.graph.MaskSubgraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.jgrapht.alg.shortestpath.ContractionHierarchyAlgorithm.ContractionEdge;
import static org.jgrapht.alg.shortestpath.ContractionHierarchyAlgorithm.ContractionVertex;

public class CHManyToManyShortestPaths<V, E> extends BaseManyTwoManyShortestPaths<V, E> {
    private final Graph<V, E> graph;

    private Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph;
    private Map<V, ContractionVertex<V>> contractionMapping;


    public CHManyToManyShortestPaths(Graph<V, E> graph) {
        this.graph = graph;
        Pair<Graph<ContractionVertex<V>, ContractionEdge<E>>, Map<V, ContractionVertex<V>>> p
                = new ContractionHierarchyAlgorithm<>(graph).computeContractionHierarchy();
        this.contractionGraph = p.getFirst();
        this.contractionMapping = p.getSecond();
    }

    public CHManyToManyShortestPaths(Graph<V, E> graph,
                                     Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph,
                                     Map<V, ContractionVertex<V>> contractionMapping) {
        this.graph = graph;
        this.contractionGraph = contractionGraph;
        this.contractionMapping = contractionMapping;
    }

    @Override
    public ManyToManyShortestPaths<V, E> getManyTwoManyPaths(List<V> sources, List<V> targets) {
        Objects.requireNonNull(sources, "sources cannot be null!");
        Objects.requireNonNull(targets, "targets cannot be null!");

        Graph<ContractionVertex<V>, ContractionEdge<E>> searchGraph;
        if (sources.size() <= targets.size()) {
            searchGraph = contractionGraph;
        } else {
            searchGraph = new EdgeReversedGraph<>(contractionGraph);
            List<V> tmp = targets;
            targets = sources;
            sources = tmp;
        }

        Map<ContractionVertex<V>,
                Map<ContractionVertex<V>, Pair<Double, ContractionEdge<E>>>> forwardSearchSpaces = new HashMap<>();
        Map<ContractionVertex<V>,
                Map<ContractionVertex<V>, Pair<Double, ContractionEdge<E>>>> backwardSearchSpaces = new HashMap<>();
        Map<Pair<ContractionVertex<V>, ContractionVertex<V>>, Pair<Double, ContractionVertex<V>>>
                middleVertices = new HashMap<>();

        Set<ContractionVertex<V>> contractedSources = sources.stream()
                .map(v -> contractionMapping.get(v)).collect(Collectors.toCollection(HashSet::new));
        Set<ContractionVertex<V>> contractedTargets = targets.stream()
                .map(v -> contractionMapping.get(v)).collect(Collectors.toCollection(HashSet::new));

        Map<ContractionVertex<V>, List<Bucket>> bucketsMap = new HashMap<>();
        for (ContractionVertex<V> vertex : searchGraph.vertexSet()) {
            bucketsMap.put(vertex, new ArrayList<>());
        }


        for (ContractionVertex<V> contractedTarget : contractedTargets) {
            backwardSearch(searchGraph, contractedTarget, bucketsMap, backwardSearchSpaces, contractedSources);
        }

        for (ContractionVertex<V> contractedSource : contractedSources) {
            forwardSearch(searchGraph, contractedSource, bucketsMap, forwardSearchSpaces, middleVertices, contractedTargets);
        }

        return new CHManyToManyShortestPathsImpl(
                graph,
                searchGraph,
                contractionMapping,
                forwardSearchSpaces,
                backwardSearchSpaces,
                middleVertices
        );
    }

    private void backwardSearch(Graph<ContractionVertex<V>, ContractionEdge<E>> searchGraph,
                                ContractionVertex<V> target, Map<ContractionVertex<V>, List<Bucket>> bucketsMap,
                                Map<ContractionVertex<V>, Map<ContractionVertex<V>,
                                        Pair<Double, ContractionEdge<E>>>> backwardSearchSpaces,
                                Set<ContractionVertex<V>> contractedSources) {
        Graph<ContractionVertex<V>, ContractionEdge<E>> maskSubgraph = new MaskSubgraph<>(
                new EdgeReversedGraph<>(searchGraph), v -> false, e -> e.isUpward
        );

        Map<ContractionVertex<V>, Pair<Double, ContractionEdge<E>>> distanceAndPredecessorMap =
                getDistanceAndPredecessorMap(maskSubgraph, target, contractedSources);

        backwardSearchSpaces.put(target, distanceAndPredecessorMap);

        for (Map.Entry<ContractionVertex<V>, Pair<Double, ContractionEdge<E>>> entry :
                distanceAndPredecessorMap.entrySet()) {
            bucketsMap.get(entry.getKey()).add(new Bucket(target, entry.getValue().getFirst()));
        }
    }

    private void forwardSearch(Graph<ContractionVertex<V>, ContractionEdge<E>> searchGraph,
                               ContractionVertex<V> source, Map<ContractionVertex<V>, List<Bucket>> bucketsMap,
                               Map<ContractionVertex<V>, Map<ContractionVertex<V>,
                                       Pair<Double, ContractionEdge<E>>>> forwardSearchSpaces,
                               Map<Pair<ContractionVertex<V>, ContractionVertex<V>>,
                                       Pair<Double, ContractionVertex<V>>> middleVerticesMap,
                               Set<ContractionVertex<V>> contractedTargets) {
        Graph<ContractionVertex<V>, ContractionEdge<E>> maskSubgraph = new MaskSubgraph<>(
                searchGraph, v -> false, e -> !e.isUpward);

        Map<ContractionVertex<V>, Pair<Double, ContractionEdge<E>>> distanceAndPredecessorMap =
                getDistanceAndPredecessorMap(maskSubgraph, source, contractedTargets);

        forwardSearchSpaces.put(source, distanceAndPredecessorMap);

        for (Map.Entry<ContractionVertex<V>, Pair<Double, ContractionEdge<E>>> entry :
                distanceAndPredecessorMap.entrySet()) {
            ContractionVertex<V> middleVertex = entry.getKey();
            double forwardDistance = entry.getValue().getFirst();

            for (Bucket bucket : bucketsMap.get(middleVertex)) {
                double pathDistance = forwardDistance + bucket.distance;
                Pair<ContractionVertex<V>, ContractionVertex<V>> pair = Pair.of(source, bucket.target);
                middleVerticesMap.compute(pair, (p, distanceAndMiddleNode) -> {
                    if (distanceAndMiddleNode == null || distanceAndMiddleNode.getFirst() > pathDistance) {
                        return Pair.of(pathDistance, middleVertex);
                    }
                    return distanceAndMiddleNode;
                });
            }
        }
    }

    private Map<ContractionVertex<V>, Pair<Double, ContractionEdge<E>>> getDistanceAndPredecessorMap(
            Graph<ContractionVertex<V>, ContractionEdge<E>> graph,
            ContractionVertex<V> source,
            Set<ContractionVertex<V>> targets
    ) {
        return ((TreeSingleSourcePathsImpl<ContractionVertex<V>, ContractionEdge<E>>)
                getShortestPathsTree(graph, source, targets)).map;
    }


    private class Bucket {
        ContractionVertex<V> target;
        double distance;

        public Bucket(ContractionVertex<V> target, double distance) {
            this.target = target;
            this.distance = distance;
        }
    }


    private class CHManyToManyShortestPathsImpl implements ManyToManyShortestPaths<V, E> {
        private final Graph<V, E> graph;
        private final Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph;
        private final Map<V, ContractionVertex<V>> contractionMapping;

        private Map<ContractionVertex<V>,
                Map<ContractionVertex<V>, Pair<Double, ContractionEdge<E>>>> forwardSearchSpaces;

        private Map<ContractionVertex<V>,
                Map<ContractionVertex<V>, Pair<Double, ContractionEdge<E>>>> backwardSearchSpaces;

        private Map<Pair<ContractionVertex<V>, ContractionVertex<V>>, Pair<Double, ContractionVertex<V>>> middleVertices;


        public CHManyToManyShortestPathsImpl(Graph<V, E> graph,
                                             Graph<ContractionVertex<V>, ContractionEdge<E>> contractionGraph,
                                             Map<V, ContractionVertex<V>> contractionMapping,
                                             Map<ContractionVertex<V>, Map<ContractionVertex<V>,
                                                     Pair<Double, ContractionEdge<E>>>> forwardSearchSpaces,
                                             Map<ContractionVertex<V>, Map<ContractionVertex<V>,
                                                     Pair<Double, ContractionEdge<E>>>> backwardSearchSpaces,
                                             Map<Pair<ContractionVertex<V>, ContractionVertex<V>>,
                                                     Pair<Double, ContractionVertex<V>>> middleVertices) {
            this.graph = graph;
            this.contractionGraph = contractionGraph;
            this.contractionMapping = contractionMapping;
            this.forwardSearchSpaces = forwardSearchSpaces;
            this.backwardSearchSpaces = backwardSearchSpaces;
            this.middleVertices = middleVertices;
        }

        @Override
        public GraphPath<V, E> getPath(V source, V target) {
            LinkedList<E> edgeList = new LinkedList<>();
            LinkedList<V> vertexList = new LinkedList<>();

            ContractionVertex<V> contractedSource = contractionMapping.get(source);
            ContractionVertex<V> contractedTarget = contractionMapping.get(target);

            Map<ContractionVertex<V>, Pair<Double, ContractionEdge<E>>> forwardTree
                    = forwardSearchSpaces.get(contractedSource);
            Map<ContractionVertex<V>, Pair<Double, ContractionEdge<E>>> backwardTree
                    = backwardSearchSpaces.get(contractedTarget);

            Pair<ContractionVertex<V>, ContractionVertex<V>> contractedVertices = Pair.of(contractedSource, contractedTarget);
            Pair<Double, ContractionVertex<V>> distanceAndCommonVertex = middleVertices.get(contractedVertices);

            if (distanceAndCommonVertex == null) {
                return null;
            }

            ContractionVertex<V> commonVertex = distanceAndCommonVertex.getSecond();

            // add common vertex
            vertexList.add(commonVertex.vertex);

            // traverse forward path
            ContractionVertex<V> v = commonVertex;
            while (true) {
                ContractionEdge<E> e = forwardTree.get(v).getSecond();

                if (e == null) {
                    break;
                }

                ContractionHierarchyBidirectionalDijkstra.unpackBackward(contractionGraph, e, vertexList, edgeList);
                v = contractionGraph.getEdgeSource(e);
            }

            // traverse reverse path
            v = commonVertex;
            while (true) {
                ContractionEdge<E> e = backwardTree.get(v).getSecond();

                if (e == null) {
                    break;
                }

                ContractionHierarchyBidirectionalDijkstra.unpackForward(contractionGraph, e, vertexList, edgeList);
                v = contractionGraph.getEdgeTarget(e);
            }

            return new GraphWalk<>(graph, source, target, vertexList, edgeList, distanceAndCommonVertex.getFirst());
        }

        @Override
        public double getWeight(V source, V target) {
            Pair<ContractionVertex<V>, ContractionVertex<V>> contractedVertices
                    = Pair.of(contractionMapping.get(source), contractionMapping.get(target));

            if (middleVertices.containsKey(contractedVertices)) {
                return middleVertices.get(contractedVertices).getFirst();
            } else {
                return Double.POSITIVE_INFINITY;
            }
        }
    }
}
