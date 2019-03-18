package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.graph.GraphWalk;
import org.jgrapht.graph.MaskSubgraph;
import org.jheaps.AddressableHeap;
import org.jheaps.tree.PairingHeap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

public class YenOldShortestPathIterator<V, E> implements Iterator<GraphPath<V, E>> {
    /**
     * Underlying graph.
     */
    private final Graph<V, E> graph;
    /**
     * Source vertex.
     */
    private final V source;
    /**
     * Sink vertex.
     */
    private final V sink;

    private List<GraphPath<V, E>> resultList;
    private AddressableHeap<Double, GraphPath<V, E>> candidatePaths;

    private Map<GraphPath<V, E>, V> deviations;

    private Map<Double, Integer> weightsFrequencies;
    private Integer amountOfPathsWithMinimumWeight;

    Integer getAmountOfPathsWithMinimumWeight() {
        return amountOfPathsWithMinimumWeight;
    }

    public YenOldShortestPathIterator(Graph<V, E> graph, V source, V sink) {
        this(graph, source, sink, PairingHeap::new);
    }

    public YenOldShortestPathIterator(Graph<V, E> graph, V source, V sink,
                                      Supplier<AddressableHeap<Double, GraphPath<V, E>>> heapSupplier) {
        this.graph = Objects.requireNonNull(graph, "Graph cannot be null!");
        if (!graph.containsVertex(source)) {
            throw new IllegalArgumentException("Graph should contain source vertex!");
        }
        this.source = source;
        if (!graph.containsVertex(source)) {
            throw new IllegalArgumentException("Graph should contain sink vertex!");
        }
        this.sink = sink;
        Objects.requireNonNull(heapSupplier, "Heap supplier cannot be null");
        this.resultList = new ArrayList<>();
        this.candidatePaths = heapSupplier.get();
        this.deviations = new HashMap<>();
        this.weightsFrequencies = new HashMap<>();

        GraphPath<V, E> shortestPath = DijkstraShortestPath.findPathBetween(graph, source, sink);
        if (shortestPath != null) {
            candidatePaths.insert(shortestPath.getWeight(), shortestPath);
            deviations.put(shortestPath, source);
            weightsFrequencies.put(shortestPath.getWeight(), 1);
        }
    }

    @Override
    public boolean hasNext() {
        return !candidatePaths.isEmpty();
    }

    @Override
    public GraphPath<V, E> next() {
        if (candidatePaths.isEmpty()) {
            throw new NoSuchElementException();
        }

        GraphPath<V, E> path = candidatePaths.deleteMin().getValue();
        resultList.add(path);
        double pathWeight = path.getWeight();
        int minWeightFrequency = weightsFrequencies.get(pathWeight);
        if (minWeightFrequency == 1) {
            weightsFrequencies.remove(pathWeight);
            if (candidatePaths.isEmpty()) {
                amountOfPathsWithMinimumWeight = 0;
            } else {
                amountOfPathsWithMinimumWeight = weightsFrequencies.get(candidatePaths.findMin().getKey());
            }
        } else {
            weightsFrequencies.put(pathWeight, minWeightFrequency - 1);
        }

        V pathDeviation = deviations.get(path);
        List<V> pathVertices = path.getVertexList();
        int pathDeviationIndex = pathVertices.indexOf(pathDeviation);

        Set<V> maskedVertices = new HashSet<>();
        Set<E> maskedEdges = new HashSet<>();

        int firstUnmaskedVertex = pathDeviationIndex;
        for (int i = 0; i < firstUnmaskedVertex; i++) { // mask vertices till the deviation
            maskedVertices.add(pathVertices.get(i));
        }

        int pathLength = pathVertices.size();
        for (int i = pathDeviationIndex; i < pathLength - 1; ++i) {
            V currentDeviation = pathVertices.get(i);
            if (firstUnmaskedVertex != i) { // mask one more vertex if needed
                maskedVertices.add(pathVertices.get(firstUnmaskedVertex));
                firstUnmaskedVertex++;
            }
//            if (!unproductiveVertices.contains(currentDeviation)) {
            maskedEdges.clear(); // remove edges masked in previous iteration

            for (GraphPath<V, E> resultPath : resultList) {
                List<V> resultPathVertices = resultPath.getVertexList();
                int deviationIndex = resultPathVertices.indexOf(currentDeviation);

                if (deviationIndex < 0 || deviationIndex != i ||
                        !equalLists(pathVertices, resultPathVertices, deviationIndex)) {
                    continue;
                }

                V successor = resultPathVertices.get(deviationIndex + 1);
                maskedEdges.add(graph.getEdge(currentDeviation, successor));
            }

            // find
            Graph<V, E> maskSubgraph = new MaskSubgraph<>(graph, maskedVertices::contains, maskedEdges::contains);
            GraphPath<V, E> spurPath = DijkstraShortestPath.findPathBetween(maskSubgraph, currentDeviation, sink);

            if (spurPath != null) { // the vertex became unproductive
                double rootPathWeight = 0.0;
                List<V> candidatePathVertices = new ArrayList<>();
                List<E> candidatePathEdges = new ArrayList<>();
                for (int j = 0; j < i; j++) {
                    E edge = graph.getEdge(pathVertices.get(j), pathVertices.get(j + 1));
                    rootPathWeight += graph.getEdgeWeight(edge);
                    candidatePathEdges.add(edge);
                    candidatePathVertices.add(pathVertices.get(j));
                }
                candidatePathEdges.addAll(spurPath.getEdgeList());
                candidatePathVertices.addAll(spurPath.getVertexList());

                double candidateWeight = rootPathWeight + spurPath.getWeight();
                GraphPath<V, E> candidate = new GraphWalk<>(graph, source, sink,
                        candidatePathVertices, candidatePathEdges, candidateWeight);

                candidatePaths.insert(candidate.getWeight(), candidate);
                deviations.put(candidate, currentDeviation);

                if (weightsFrequencies.containsKey(candidateWeight)) {
                    weightsFrequencies.computeIfPresent(candidateWeight, (aDouble, integer) -> integer + 1);
                } else {
                    weightsFrequencies.put(candidateWeight, 1);
                }
            }
        }

        return path;
    }

    private boolean equalLists(List<V> first, List<V> second, int index) {
        for (int i = 0; i <= index; i++) {
            if (!first.get(i).equals(second.get(i))) {
                return false;
            }
        }
        return true;
    }
}
