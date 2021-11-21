package main;

import main.model.Edge;
import main.model.Vertex;
import main.model.Operation;

import java.util.*;

import static main.model.Operation.CREATE;
import static main.model.Operation.REMOVE;

/**
 * Class LWWElementGraph - implements a Last-Write-Wins Element Graph
 * Supports both directed and undirected graphs (defaults to undirected)
 * Assumes that each vertex has a unique non-empty label
 * To make sure that LWWElementGraph is CRDT, each of the internal maps (vertexMap/edgeMap)
 * will serve as both Add/Remove sets for vertices/edges, respectively
 */
public class LWWElementGraph {

    // vertexMap contains <vertex label, vertex> key-value pairs
    private final Map<String, Vertex> vertexMap = new HashMap<>();

    // edgeMap contains <source vertex label, <destination vertex label, associated Edge>>
    // key-value pairs; this map will serve as a kind of `adjacency Add/Remove set`
    private final Map<String, Map<String, Edge>> edgeMap = new HashMap<>();

    // whether this graph is directed or not
    private final boolean directed;

    public LWWElementGraph(){
        directed = false;
    }

    public LWWElementGraph(boolean directed){
        this.directed = directed;
    }

    public boolean isDirected() {
        return directed;
    }

    /**
     * Helper function to create/remove a vertex
     * To create/remove a vertex:
     * If a vertex exists in the vertex map, then modify it with a LATER timestamp, if needed
     * Else, simply create one with the correct parameters
     * @param label Vertex label
     * @param timestamp Timestamp of vertex creation/removal
     * @param operation Either CREATE or REMOVE - operation to be performed
     */
    private void updateVertex(String label, long timestamp, Operation operation){
        if(timestamp < 0 || label == null || label.trim().isEmpty()){
            System.err.println("Input provided to add/remove a vertex is not valid.");
            return;
        }
        if(vertexMap.containsKey(label)) {
            Vertex v = vertexMap.get(label);
            if(CREATE.equals(operation) && v.getCreationTimestamp() < timestamp){
                v.setCreationTimestamp(timestamp);
            } else if (REMOVE.equals(operation) && v.getRemovalTimestamp() < timestamp) {
                v.setRemovalTimestamp(timestamp);
            }
        } else {
            Vertex v;
            if(CREATE.equals(operation)) {
                v = new Vertex(label, timestamp);
            } else {
                v = new Vertex(label, -1, timestamp);
            }
            vertexMap.put(label, v);
        }
    }

    public void addVertex(String label){
        addVertex(label, System.currentTimeMillis());
    }

    public void addVertex(String label, long timestamp){
        updateVertex(label, timestamp, CREATE);
    }

    public void removeVertex(String label){
        removeVertex(label, System.currentTimeMillis());
    }

    public void removeVertex(String label, long timestamp){
        updateVertex(label, timestamp, REMOVE);
    }

    public boolean checkVertexExists(String label){
        // Vertex exists if it's present in the vertex map, and it's active,
        // i.e. its creation timestamp is AFTER removal timestamp
        return vertexMap.containsKey(label) && vertexMap.get(label).isActive();
    }

    public long getVertexCreationTimestamp(String label){
        if(vertexMap.containsKey(label))
            return vertexMap.get(label).getCreationTimestamp();
        return -1;
    }

    public long getVertexRemovalTimestamp(String label){
        if(vertexMap.containsKey(label))
            return vertexMap.get(label).getRemovalTimestamp();
        return -1;
    }

    public void addEdge(String srcLabel, String dstLabel){
        addEdge(srcLabel, dstLabel, System.currentTimeMillis());
    }

    public void addEdge(String srcLabel, String dstLabel, long timestamp){
        updateEdge(srcLabel, dstLabel, timestamp, CREATE);
        // If the graph is undirected, we need to add both src->dst and dst->src edges
        if(!directed)
            updateEdge(dstLabel, srcLabel, timestamp, CREATE);
    }

    public void removeEdge(String srcLabel, String dstLabel){
        removeEdge(srcLabel, dstLabel, System.currentTimeMillis());
    }

    public void removeEdge(String srcLabel, String dstLabel, long timestamp){
        updateEdge(srcLabel, dstLabel, timestamp, REMOVE);
        // If the graph is undirected, we need to remove both src->dst and dst->src edges
        if(!directed)
            updateEdge(dstLabel, srcLabel, timestamp, REMOVE);
    }

    /**
     * Helper function to create/remove an edge
     * To create/remove an edge:
     * If an edge already exists, then update it with a LATER timestamp, if needed
     * Else, simply create an edge with correct parameters
     * @param srcLabel Source vertex label
     * @param dstLabel Destination vertex label
     * @param timestamp Timestamp of edge creation/removal
     * @param operation Either CREATE or REMOVE - operation to be performed
     */
    private void updateEdge(String srcLabel, String dstLabel,
                            long timestamp, Operation operation){
        if(timestamp < 0 || srcLabel == null || srcLabel.trim().isEmpty() ||
                dstLabel == null || dstLabel.trim().isEmpty()){
            System.err.println("Input provided to add/remove an edge is not valid.");
            return;
        }
        if(edgeMap.containsKey(srcLabel)){
            Edge edge = edgeMap.get(srcLabel).getOrDefault(dstLabel, null);
            if(edge == null){
                if(CREATE.equals(operation)) {
                    edgeMap.get(srcLabel).put(dstLabel, new Edge(srcLabel,
                            dstLabel, timestamp));
                } else {
                    edgeMap.get(srcLabel).put(dstLabel, new Edge(srcLabel,
                            dstLabel, -1, timestamp));
                }
            } else {
                if(CREATE.equals(operation) && edge.getCreationTimestamp() < timestamp){
                    edge.setCreationTimestamp(timestamp);
                } else if (REMOVE.equals(operation) && edge.getRemovalTimestamp() < timestamp){
                    edge.setRemovalTimestamp(timestamp);
                }
            }
        } else {
            edgeMap.put(srcLabel, new HashMap<>());
            if(CREATE.equals(operation)) {
                edgeMap.get(srcLabel).put(dstLabel, new Edge(srcLabel,
                        dstLabel, timestamp));
            } else {
                edgeMap.get(srcLabel).put(dstLabel, new Edge(srcLabel,
                        dstLabel, -1, timestamp));
            }
        }
    }

    public long getEdgeCreationTimestamp(String srcLabel, String dstLabel){
        if(edgeMap.containsKey(srcLabel) && edgeMap.get(srcLabel).containsKey(dstLabel)){
            return edgeMap.get(srcLabel).get(dstLabel).getCreationTimestamp();
        }
        return -1;
    }

    public long getEdgeRemovalTimestamp(String srcLabel, String dstLabel){
        if(edgeMap.containsKey(srcLabel) && edgeMap.get(srcLabel).containsKey(dstLabel)){
            return edgeMap.get(srcLabel).get(dstLabel).getRemovalTimestamp();
        }
        return -1;
    }

    /**
     * Obtain all the vertices connected to a given vertex with VALID edges
     * VALID edge is defined as follows:
     * - Edge itself should be active (i.e. its creation time is AFTER removal time)
     * - Both of its vertices should exist and be active
     * - Both of its vertices' creation time should be AT or BEFORE creation time of the
     *          edge itself.
     * @param srcLabel Given source vertex label
     * @return List of vertex labels that are connected to srcLabel
     */
    public List<String> getAdjacentVertices(String srcLabel){
        if(srcLabel == null || srcLabel.trim().isEmpty()){
            System.err.println("Input provided to get adjacent vertices is not valid.");
            return new ArrayList<>();
        }
        // If there is no such vertex, or no edges recorded for this vertex,return empty list
        if(!vertexMap.containsKey(srcLabel) || !edgeMap.containsKey(srcLabel))
            return new ArrayList<>();
        List<String> result = new ArrayList<>();

        // Go through the 'adjacency' map (edgeMap) and filter non-valid edges
        for(String dstLabel: edgeMap.get(srcLabel).keySet()){
            Edge edge = edgeMap.get(srcLabel).get(dstLabel);
            if(edge.isActive() && vertexMap.containsKey(srcLabel) &&
                    vertexMap.containsKey(dstLabel)){
                Vertex src = vertexMap.get(srcLabel);
                Vertex dst = vertexMap.get(dstLabel);
                if(src.isActive() && edge.getCreationTimestamp() >= src.getCreationTimestamp() &&
                        dst.isActive() && edge.getCreationTimestamp() >= dst.getCreationTimestamp()){
                    result.add(dstLabel);
                }
            }
        }
        return result;
    }

    /**
     * Finds a single path between given vertices
     * Based on DFS search
     * - Uses helper function: main.LWWElementGraph#connected
     * @param srcLabel Source vertex label
     * @param dstLabel Destination vertex label
     * @return A path between srcLabel and dstLabel (list of labels of vertices
     *        that will be passed through, including srcLabel and dstLabel, if a path exists)
     *        If there is no path, returns an empty list
     */
    public List<String> findPath(String srcLabel, String dstLabel){
        if(srcLabel == null || srcLabel.trim().isEmpty() ||
                dstLabel == null || dstLabel.trim().isEmpty()){
            System.err.println("Input provided to find path is not correct.");
            return new ArrayList<>();
        }
        Stack<String> path = new Stack<>();
        Map<String, Boolean> visited = new HashMap<>();
        boolean conn = connected(srcLabel, dstLabel, path, visited);
        if(conn){
            return new ArrayList<>(path);
        }
        return new ArrayList<>();
    }

    private boolean connected(String srcLabel, String dstLabel,
                              Stack<String> path, Map<String,Boolean> visited){
        visited.put(srcLabel, true);
        path.add(srcLabel);
        if(srcLabel.equals(dstLabel)){
            return true;
        }
        for(String adj: getAdjacentVertices(srcLabel)){
            if(!visited.containsKey(adj) || !visited.get(adj)){
                if(connected(adj, dstLabel, path, visited)){
                    return true;
                }
            }
        }
        path.pop();
        return false;
    }

    /**
     * Merges the current graph with another graph
     * Both internal maps will be merged based on simple rules:
     * If an element (vertex/edge) from another graph exists in THIS graph:
     *    - Update both creation and removal timestamps with the LATEST
     * Else, deep copy this element
     * @param other Other graph to be merged with
     */
    public void merge(LWWElementGraph other){
        if(other.directed != directed){
            System.err.println("Cannot merge a directed and undirected graphs.");
            return;
        }
        for(String label: other.vertexMap.keySet()){
            Vertex otherVertex = other.vertexMap.get(label);
            if(vertexMap.containsKey(label)){
                Vertex thisVertex = vertexMap.get(label);
                if(thisVertex.getCreationTimestamp() < otherVertex.getCreationTimestamp()){
                    thisVertex.setCreationTimestamp(otherVertex.getCreationTimestamp());
                }
                if(thisVertex.getRemovalTimestamp() < otherVertex.getRemovalTimestamp()){
                    thisVertex.setRemovalTimestamp(otherVertex.getRemovalTimestamp());
                }
            } else {
                // deep copy
                vertexMap.put(label, new Vertex(otherVertex.getLabel(),
                        otherVertex.getCreationTimestamp(), otherVertex.getRemovalTimestamp()));
            }
        }

        for(String label: other.edgeMap.keySet()){
            // If some edge was associated with a given vertex `label` in THIS graph
            if(edgeMap.containsKey(label)){
                // Go through each dstLabel of OTHER graph's edge map
                for(String dstLabel: other.edgeMap.get(label).keySet()){
                    Edge otherEdge = other.edgeMap.get(label).get(dstLabel);

                    // If this graph has the same edge, update timestamps
                    if(edgeMap.get(label).containsKey(dstLabel)) {
                        Edge thisEdge = edgeMap.get(label).get(dstLabel);
                        if(thisEdge.getCreationTimestamp() < otherEdge.getCreationTimestamp()){
                            thisEdge.setCreationTimestamp(otherEdge.getCreationTimestamp());
                        }
                        if(thisEdge.getRemovalTimestamp() < otherEdge.getRemovalTimestamp()){
                            thisEdge.setRemovalTimestamp(otherEdge.getRemovalTimestamp());
                        }
                    } else {
                        // This graph doesn't have such edge, deep copy this edge
                        edgeMap.get(label).put(dstLabel, new Edge(otherEdge.getSrcLabel(),
                                otherEdge.getDstLabel(),
                                otherEdge.getCreationTimestamp(), otherEdge.getRemovalTimestamp()));
                    }
                }
            } else {
                // There were no edges whatsoever in THIS graph associated with vertex `label`
                Map<String, Edge> toPut = new HashMap<>();
                // deep copy ALL edges from OTHER graph
                for(String dstLabel: other.edgeMap.get(label).keySet()){
                    Edge otherEdge = other.edgeMap.get(label).get(dstLabel);
                    toPut.put(dstLabel, new Edge(otherEdge.getSrcLabel(), otherEdge.getDstLabel(),
                            otherEdge.getCreationTimestamp(), otherEdge.getRemovalTimestamp()));
                }
                edgeMap.put(label, toPut);
            }
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LWWElementGraph that = (LWWElementGraph) o;
        return vertexMap.equals(that.vertexMap) &&
                edgeMap.equals(that.edgeMap) && that.directed == directed;
    }

    @Override
    public int hashCode() {
        return Objects.hash(vertexMap, edgeMap, directed);
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("Vertices map:\n");
        for(String s: vertexMap.keySet()){
            sb.append(s).append(":").append(vertexMap.get(s).getCreationTimestamp());
            sb.append(":").append(vertexMap.get(s).getRemovalTimestamp()).append("\n");
        }
        sb.append("***************************************\n");
        sb.append("Edges map:\n");
        for(String s: edgeMap.keySet()){
            for(String d: edgeMap.get(s).keySet()){
                Edge e = edgeMap.get(s).get(d);
                sb.append(s).append("-").append(d);
                sb.append(":").append(e.getCreationTimestamp());
                sb.append(":").append(e.getRemovalTimestamp());
                sb.append("\n");
            }
        }
        return sb.toString();
    }

}
