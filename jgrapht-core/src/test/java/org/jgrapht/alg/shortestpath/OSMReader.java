package org.jgrapht.alg.shortestpath;

import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.alg.interfaces.AStarAdmissibleHeuristic;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

class OSMReader {
    private static final String SPACE = " ";

    void readGraph(
            Graph<ShortestPathPerformance.Node, DefaultWeightedEdge> graph,
            String path,
            BiFunction<ShortestPathPerformance.Node,ShortestPathPerformance.Node,Double> distanceFunction
    ) {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            String line = reader.readLine();
            int numOfNodes = Integer.valueOf(line);

            Map<Long, ShortestPathPerformance.Node> idToNodeMap = new HashMap<>(numOfNodes);

            for (int i = 0; i < numOfNodes; i++) {
                line = reader.readLine();
                String[] nodeData = line.split(SPACE);

                if (nodeData.length != 3) {
                    throw new IllegalArgumentException("" + nodeData.length);
                }

                long id = Long.valueOf(nodeData[0]);
                double lon = Double.valueOf(nodeData[1]);
                double lat = Double.valueOf(nodeData[2]);
                ShortestPathPerformance.Node node = new ShortestPathPerformance.Node(id, lon, lat);

                graph.addVertex(node);
                idToNodeMap.put(id, node);
            }
//            System.out.println("nodes read");

//            long numOfArcs = 0;
            while ((line = reader.readLine()) != null) {
                String[] edgesData = line.split(SPACE);
                ShortestPathPerformance.Node source = idToNodeMap.get(Long.valueOf(edgesData[0]));
                for (int i = 1; i < edgesData.length; i++) {
                    ShortestPathPerformance.Node target = idToNodeMap.get(Long.valueOf(edgesData[i]));
                    if (distanceFunction == null) {
                        Graphs.addEdge(graph, source, target, 1.0);
                    } else {
                        Graphs.addEdge(graph, source, target, distanceFunction.apply(source, target));
                    }
                }
//                numOfArcs += edgesData.length - 1;
            }

//            System.out.println("num of arcs read: " + numOfArcs + " " + numOfArcs / 2);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
