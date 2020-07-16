package org.pmoi.model.vis;

import com.google.gson.Gson;
import org.pmoi.util.VisEdgeAdapter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class VisGraph {

    private HashMap<Long,VisNode> nodes;
    private List<VisEdge> edges;

    public VisGraph(){
        nodes = new HashMap<>();
        edges = new ArrayList<>();
    }

    public VisGraph(List<VisNode> nodes, List<VisEdge> edges){
        this.edges = edges;
        this.nodes = new HashMap<>();
        for(VisNode node : nodes)
            this.nodes.put(node.getId(),node);
    }

    public void addNodes(Collection<VisNode> nodes){
        for(VisNode node : nodes)
            this.nodes.put(node.getId(),node);
    }

    public void addEdges(List<VisEdge> edges){
        this.edges.addAll(edges);
    }

    public List<VisNode> nodesAsList(){
        return new ArrayList<>(nodes.values());
    }

    public String getNodesJson(){
        Gson gson = new Gson();
        return gson.toJson(nodesAsList());
    }

    public String getEdgesJson(){
        return VisEdgeAdapter.getAsJsonArray(edges).toString();
    }

    public boolean containsNode(long offset) {
        return this.nodes.containsKey(offset);
    }

    public VisNode getNode(long id){
        return this.nodes.get(id);
    }

}
