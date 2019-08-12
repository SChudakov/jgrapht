package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.event.ConnectedComponentTraversalEvent;
import org.jgrapht.event.EdgeTraversalEvent;
import org.jgrapht.event.TraversalListener;
import org.jgrapht.event.VertexTraversalEvent;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedPseudograph;
import org.jgrapht.traverse.DepthFirstIterator;

import java.io.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static java.lang.Math.max;

public class OSMFilter {
    private static String xmlFilePath = "/home/D074374/drive/osm/graph/sweden.osm";
    private static String finalFilePath = "/home/D074374/drive/osm/final/sweden.txt";
    private static final String SPACE = " ";

    public static void main(String[] args) {
        Graph<ShortestPathPerformance.Node, DefaultWeightedEdge> graph =
                new DirectedWeightedPseudograph<>(DefaultWeightedEdge.class);
        OSMReader reader = new OSMReader();
        reader.readGraph(graph, xmlFilePath, null);
        System.out.println("graph read");

        DepthFirstIterator<ShortestPathPerformance.Node, DefaultWeightedEdge> iterator = new DepthFirstIterator<>(graph);
        ConnectedComponentsListener listener = new ConnectedComponentsListener();
        iterator.addTraversalListener(listener);

        int numOfNodes = 0;
        int printSize = 1000000;
        while (iterator.hasNext()) {
            iterator.next();
            if(++numOfNodes % printSize == 0){
                System.out.println(numOfNodes);
            }
        }

        System.out.println("num of connected components: " + listener.getNumOfComponents());
        System.out.println("max connected component sizes: " + listener.getMaxComponentSize());
        for (Long l : listener.getSizes()) {
            System.out.println(l);
        }

        List<List<ShortestPathPerformance.Node>> components = listener.getComponents();
        removeMaxSizeComponent(components);

        Set<Long> nodesToRemove = components.stream().flatMap(List::stream)
                .mapToLong(node -> node.id).boxed().collect(Collectors.toCollection(HashSet::new));
        filteredLines(xmlFilePath, finalFilePath,
                nodesToRemove, graph.vertexSet().size() - nodesToRemove.size());
    }

    private static void filteredLines(String source, String target, Set<Long> ids, int numOfVertices) {
        try (BufferedReader reader = new BufferedReader(new FileReader(source));
             BufferedWriter writer = new BufferedWriter(new FileWriter(target))) {
            writer.write(String.valueOf(numOfVertices));
            writer.write('\n');

            String line = reader.readLine();
            int numOfNodes = Integer.valueOf(line);

            for (int i = 0; i < numOfNodes; i++) {
                line = reader.readLine();
                String[] nodeData = line.split(SPACE);
                if (!ids.contains(Long.valueOf(nodeData[0]))) {
                    writer.write(line);
                    writer.write('\n');
                }
            }

            while ((line = reader.readLine()) != null) {
                String[] edgesData = line.split(SPACE);
                boolean needAdd = true;
                for (String vertexId : edgesData) {
                    if (ids.contains(Long.valueOf(vertexId))) {
                        needAdd = false;
                        break;
                    }
                }
                if (needAdd) {
                    writer.write(line);
                    writer.write('\n');
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void removeMaxSizeComponent(List<List<ShortestPathPerformance.Node>> components) {
        int maxSize = components.stream().mapToInt(List::size).max().getAsInt();
        Iterator<List<ShortestPathPerformance.Node>> iterator = components.iterator();
        while (iterator.hasNext()) {
            List<ShortestPathPerformance.Node> c = iterator.next();
            if (c.size() == maxSize) {
                System.out.println("removed");
                iterator.remove();
            }
        }
    }


    private static void writeLines(String path, List<String> lines, int numOfVertices) {
        try (Writer writer = new FileWriter(path)) {
            writer.write(String.valueOf(numOfVertices));
            writer.write('\n');
            for (String line : lines) {
                writer.write(line);
                writer.write('\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    static class ConnectedComponentsListener
            implements TraversalListener<ShortestPathPerformance.Node, DefaultWeightedEdge> {
        private long numOfComponents;
        private long maxComponentSize;

        private long currentComponentSize;
        private Set<Long> sizes;
        private List<List<ShortestPathPerformance.Node>> components;
        private List<ShortestPathPerformance.Node> currentComponent;

        ConnectedComponentsListener() {
            sizes = new TreeSet<>();
            components = new LinkedList<>();
        }

        long getNumOfComponents() {
            return numOfComponents;
        }

        long getMaxComponentSize() {
            return maxComponentSize;
        }

        Set<Long> getSizes() {
            return sizes;
        }

        List<List<ShortestPathPerformance.Node>> getComponents() {
            return components;
        }

        @Override
        public void connectedComponentStarted(ConnectedComponentTraversalEvent e) {
            currentComponentSize = 0;
            currentComponent = new LinkedList<>();
        }

        @Override
        public void connectedComponentFinished(ConnectedComponentTraversalEvent e) {
            maxComponentSize = max(maxComponentSize, currentComponentSize);

            sizes.add(currentComponentSize);
            components.add(currentComponent);

            numOfComponents++;
        }

        @Override
        public void edgeTraversed(EdgeTraversalEvent<DefaultWeightedEdge> e) {

        }

        @Override
        public void vertexTraversed(VertexTraversalEvent<ShortestPathPerformance.Node> e) {
            currentComponent.add(e.getVertex());
        }

        @Override
        public void vertexFinished(VertexTraversalEvent<ShortestPathPerformance.Node> e) {
            currentComponentSize++;
        }
    }
}
