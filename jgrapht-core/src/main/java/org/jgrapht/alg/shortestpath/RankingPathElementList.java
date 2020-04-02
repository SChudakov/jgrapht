/*
 * (C) Copyright 2007-2020, by France Telecom and Contributors.
 *
 * JGraphT : a free Java graph-theory library
 *
 * See the CONTRIBUTORS.md file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0, or the
 * GNU Lesser General Public License v2.1 or later
 * which is available at
 * http://www.gnu.org/licenses/old-licenses/lgpl-2.1-standalone.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR LGPL-2.1-or-later
 */
package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.Graphs;
import org.jgrapht.graph.GraphWalk;
import org.jgrapht.graph.MaskSubgraph;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * List of simple paths in increasing order of weight.
 *
 */
final class RankingPathElementList<V, E>
    extends
    AbstractPathElementList<V, E, RankingPathElement<V, E>>
{
    /**
     * Vertex that paths of the list must not disconnect.
     */
    private V guardVertexToNotDisconnect = null;

    private Map<RankingPathElement<V, E>, Double> elementToSpurPathDistance = new HashMap<>();

    /**
     * To be used on top of general path validations. May invalidate the path though they pass the
     * basic validations done internally (path is from source to target and w/o loops).
     */
    private PathValidator<V, E> externalPathValidator = null;

    /**
     * Creates a list with an empty path. The list size is 1.
     *
     * @param maxSize max number of paths the list is able to store.
     */
    RankingPathElementList(Graph<V, E> graph, int maxSize, RankingPathElement<V, E> pathElement)
    {
        this(graph, maxSize, pathElement, null);
    }

    /**
     * Creates a list with an empty path. The list size is 1.
     *
     * @param maxSize max number of paths the list is able to store.
     * @param pathValidator path validator to be used in addition to basic validations (path is from
     *        source to target w/o loops)
     * 
     */
    RankingPathElementList(
        Graph<V, E> graph, int maxSize, RankingPathElement<V, E> pathElement,
        PathValidator<V, E> pathValidator)
    {
        super(graph, maxSize, pathElement);
        this.externalPathValidator = pathValidator;
    }

    /**
     * Creates paths obtained by concatenating the specified edge to the specified paths.
     *
     * @param elementList paths, list of <code>
     * RankingPathElement</code>.
     * @param edge edge reaching the end vertex of the created paths.
     * @param maxSize maximum number of paths the list is able to store.
     */
    RankingPathElementList(
        Graph<V, E> graph, int maxSize, RankingPathElementList<V, E> elementList, E edge)
    {
        this(graph, maxSize, elementList, edge, null);

        assert (!this.pathElements.isEmpty());
    }

    /**
     * Creates paths obtained by concatenating the specified edge to the specified paths.
     *
     * @param elementList paths, list of <code>
     * RankingPathElement</code>.
     * @param edge edge reaching the end vertex of the created paths.
     * @param maxSize maximum number of paths the list is able to store.
     */
    RankingPathElementList(
        Graph<V, E> graph, int maxSize, RankingPathElementList<V, E> elementList, E edge,
        V guardVertexToNotDisconnect)
    {
        this(graph, maxSize, elementList, edge, guardVertexToNotDisconnect, null);
    }

    /**
     * Creates paths obtained by concatenating the specified edge to the specified paths.
     *
     * @param elementList paths, list of <code>
     * RankingPathElement</code>.
     * @param edge edge reaching the end vertex of the created paths.
     * @param maxSize maximum number of paths the list is able to store.
     * @param pathValidator path validator to be used in addition to basic validations (path is from
     *        source to target w/o loops)
     */
    RankingPathElementList(
        Graph<V, E> graph, int maxSize, RankingPathElementList<V, E> elementList, E edge,
        V guardVertexToNotDisconnect, PathValidator<V, E> pathValidator)
    {
        super(graph, maxSize, elementList, edge);
        this.guardVertexToNotDisconnect = guardVertexToNotDisconnect;
        this.externalPathValidator = pathValidator;

        // loop over the path elements in increasing order of weight.
        for (int i = 0; (i < elementList.size()) && (size() < maxSize); i++) {
            RankingPathElement<V, E> prevPathElement = elementList.get(i);

            if (isNotValidPath(prevPathElement, edge)) {
                // go to the next path element in the loop
                continue;
            }

            double weight = calculatePathWeight(prevPathElement, edge);
            double spurPathDistance = getSpurPathDistance(prevPathElement);
            if(spurPathDistance == Double.POSITIVE_INFINITY){
                continue;
            }
            double newPathPriority = weight + spurPathDistance;
            RankingPathElement<V, E> newPathElement =
                new RankingPathElement<>(this.graph, prevPathElement, edge, weight, newPathPriority);

            // the new path is inserted at the end of the list.
            this.pathElements.add(newPathElement);
        }

        for (int i = 1; i < pathElements.size(); ++i) {
            if (pathElements.get(i).getPriority() < pathElements.get(i - 1).getPriority()) {
                throw new RuntimeException("not sorted");
            }
        }
//        pathElements.sort(Comparator.comparingDouble(RankingPathElement::getWeight));
    }

    /**
     * Creates an empty list. The list size is 0.
     *
     * @param maxSize max number of paths the list is able to store.
     */
    RankingPathElementList(Graph<V, E> graph, int maxSize, V vertex)
    {
        this(graph, maxSize, vertex, null);
    }

    /**
     * Creates an empty list. The list size is 0.
     *
     * @param maxSize max number of paths the list is able to store.
     * @param pathValidator path validator to be used in addition to basic validations (path is from
     *        source to target w/o loops)
     */
    RankingPathElementList(
        Graph<V, E> graph, int maxSize, V vertex, PathValidator<V, E> pathValidator)
    {
        super(graph, maxSize, vertex);
        this.externalPathValidator = pathValidator;
    }

    /**
     * <p>
     * Adds paths in the list at vertex $y$. Candidate paths are obtained by concatenating the
     * specified edge $(v, y)$ to the paths <code>
     * elementList</code> at vertex $v$.
     * </p>
     *
     * Complexity =
     *
     * <ul>
     * <li>w/o guard-vertex: $O(knp)$ where $k$ is the max size limit of the list and $np$ is the
     * maximum number of vertices in the paths stored in the list</li>
     * <li>with guard-vertex: $O(k(m+n)$</code>) where $k$ is the max size limit of the list, $m$ is
     * the number of edges of the graph and $n$ is the number of vertices of the graph, $O(m + n)$
     * being the complexity of the <code>
     * ConnectivityInspector</code> to check whether a path exists towards the guard-vertex</li>
     * </ul>
     *
     * @param elementList list of paths at vertex $v$.
     * @param edge edge $(v, y)$.
     *
     * @return <code>true</code> if at least one path has been added in the list, <code>false</code>
     *         otherwise.
     */
    public boolean addPathElements(RankingPathElementList<V, E> elementList, E edge)
    {
        assert (this.vertex
            .equals(Graphs.getOppositeVertex(this.graph, edge, elementList.getVertex())));

        boolean pathAdded = false;

        // loop over the paths elements of the list at vertex v.
        for (int vIndex = 0, yIndex = 0; vIndex < elementList.size(); vIndex++) {
            RankingPathElement<V, E> prevPathElement = elementList.get(vIndex);

            if (isNotValidPath(prevPathElement, edge)) {
                // checks if path is simple and if guard-vertex is not
                // disconnected.
                continue;
            }
            double newPathWeight = calculatePathWeight(prevPathElement, edge);
            double spurPathDistance = getSpurPathDistance(prevPathElement);
            if(spurPathDistance == Double.POSITIVE_INFINITY){
                continue;
            }
            double newPathPriority = newPathWeight + spurPathDistance;
            RankingPathElement<V, E> newPathElement =
                new RankingPathElement<>(this.graph, prevPathElement, edge, newPathWeight, newPathPriority);

            // loop over the paths of the list at vertex y from yIndex to the
            // end.
            RankingPathElement<V, E> yPathElement = null;
            double yPathPriority = Double.NEGATIVE_INFINITY;
            for (; yIndex < size(); yIndex++) {
                yPathElement = get(yIndex);
                yPathPriority = yPathElement.getPriority();

                // case when the new path is shorter than the path Py stored at
                // index y
                if (newPathPriority < yPathPriority) {
                    this.pathElements.add(yIndex, newPathElement);
                    pathAdded = true;

                    // ensures max size limit is not exceeded.
                    if (size() > this.maxSize) {
                        this.pathElements.remove(this.maxSize);
                    }
                    break;
                }

                // case when the new path is of the same length as the path Py
                // stored at index y
                if (newPathPriority == yPathPriority) {
                    this.pathElements.add(yIndex + 1, newPathElement);
                    pathAdded = true;

                    // ensures max size limit is not exceeded.
                    if (size() > this.maxSize) {
                        this.pathElements.remove(this.maxSize);
                    }
                    break;
                }
            }

            // case when the new path is longer than the longest path in the
            // list (Py stored at the last index y)
            if (newPathPriority > yPathPriority) {
                // ensures max size limit is not exceeded.
                if (size() < this.maxSize) {
                    // the new path is inserted at the end of the list.
                    this.pathElements.add(newPathElement);
                    pathAdded = true;
                } else {
                    // max size limit is reached -> end of the loop over the
                    // paths elements of the list at vertex v.
                    break;
                }
            }
        }
//        pathElements.sort(Comparator.comparingDouble(RankingPathElement::getWeight));
        for (int i = 1; i < pathElements.size(); ++i) {
            if (pathElements.get(i).getPriority() < pathElements.get(i - 1).getPriority()) {
                System.out.println(elementList.get(0).getVertex() + " " + edge + " " + getVertex());
                throw new RuntimeException("not sorted");
            }
        }
        return pathAdded;
    }

    /**
     * @return list of <code>RankingPathElement</code>.
     */
    List<RankingPathElement<V, E>> getPathElements()
    {
        return this.pathElements;
    }

    /**
     * Costs taken into account are the weights stored in <code>Edge</code> objects.
     *
     * @param pathElement
     * @param edge the edge via which the vertex was encountered.
     *
     * @return the cost obtained by concatenation.
     *
     * @see Graph#getEdgeWeight(E)
     */
    private double calculatePathWeight(RankingPathElement<V, E> pathElement, E edge)
    {
        double pathWeight = this.graph.getEdgeWeight(edge);

        // otherwise it's the start vertex.
        if ((pathElement.getPrevEdge() != null)) {
            pathWeight += pathElement.getWeight();
        }

        return pathWeight;
    }


    private double getSpurPathDistance(RankingPathElement<V, E> prevPathElement)
    {
        if (this.guardVertexToNotDisconnect == null) {
            throw new RuntimeException();
        }

        Double cachedDistance = this.elementToSpurPathDistance.get(prevPathElement);
        if(cachedDistance != null){
            return cachedDistance;
        }

        PathMask<V, E> pathMask = new PathMask<>(prevPathElement);
        MaskSubgraph<V, E> maskSubgraph = new MaskSubgraph<>(
            this.graph, pathMask::isVertexMasked, pathMask::isEdgeMasked);

        double result;
        if (pathMask.isVertexMasked(this.guardVertexToNotDisconnect)) {
            // the guard-vertex was already in the path element -> invalid path
            result = Double.POSITIVE_INFINITY;
        } else {
            GraphPath<V, E> graphPath = DijkstraShortestPath.findPathBetween(maskSubgraph, this.vertex, this.guardVertexToNotDisconnect);
            if (graphPath == null) {
                // there is no path to guard-vertex
                result = Double.POSITIVE_INFINITY;
            } else {
                result = graphPath.getWeight();
            }
        }
        this.elementToSpurPathDistance.put(prevPathElement, result);
        return result;
    }

    private boolean isNotValidPath(RankingPathElement<V, E> prevPathElement, E edge)
    {
        if (!isSimplePath(prevPathElement, edge)) {
            return true;
        }

        if (externalPathValidator != null) {
            GraphPath<V, E> prevPath;
            if (prevPathElement.getPrevEdge() == null) {
                prevPath = new GraphWalk<>(
                    graph, Collections.singletonList(prevPathElement.getVertex()),
                    prevPathElement.getWeight());
            } else {
                List<E> prevEdges = prevPathElement.createEdgeListPath();
                prevPath = new GraphWalk<V, E>(
                    graph, graph.getEdgeSource(prevEdges.get(0)), prevPathElement.getVertex(),
                    prevEdges, prevPathElement.getWeight());
            }

            if (!externalPathValidator.isValidPath(prevPath, edge)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Ensures that paths of the list are simple (check that the vertex was not already in the path
     * element).
     *
     * @param prevPathElement
     * @param edge
     *
     * @return <code>true</code> if the resulting path (obtained by concatenating the specified edge
     *         to the specified path) is simple, <code>
     * false</code> otherwise.
     */
    private boolean isSimplePath(RankingPathElement<V, E> prevPathElement, E edge)
    {
        V endVertex = Graphs.getOppositeVertex(this.graph, edge, prevPathElement.getVertex());
        assert (endVertex.equals(this.vertex));

        RankingPathElement<V, E> pathElementToTest = prevPathElement;
        do {
            if (pathElementToTest.getVertex().equals(endVertex)) {
                return false;
            } else {
                pathElementToTest = pathElementToTest.getPrevPathElement();
            }
        } while (pathElementToTest != null);

        return true;
    }

    private static class PathMask<V, E>
    {
        private Set<E> maskedEdges;

        private Set<V> maskedVertices;

        /**
         * Creates a mask for all the edges and the vertices of the path (including the 2 extremity
         * vertices).
         *
         * @param pathElement
         */
        PathMask(RankingPathElement<V, E> pathElement)
        {
            this.maskedEdges = new HashSet<>();
            this.maskedVertices = new HashSet<>();

            while (pathElement.getPrevEdge() != null) {
                this.maskedEdges.add(pathElement.getPrevEdge());
                this.maskedVertices.add(pathElement.getVertex());
                pathElement = pathElement.getPrevPathElement();
            }
            this.maskedVertices.add(pathElement.getVertex());
        }

        public boolean isEdgeMasked(E edge)
        {
            return this.maskedEdges.contains(edge);
        }

        public boolean isVertexMasked(V vertex)
        {
            return this.maskedVertices.contains(vertex);
        }
    }
}
