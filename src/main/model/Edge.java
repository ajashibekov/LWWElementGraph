package main.model;

import java.util.Objects;

public class Edge {
    private final String srcLabel;
    private final String dstLabel;
    private long creationTimestamp;
    private long removalTimestamp;

    public Edge(String srcLabel, String dstLabel, long creationTimestamp, long removalTimestamp){
        this.srcLabel = srcLabel;
        this.dstLabel = dstLabel;
        this.creationTimestamp = creationTimestamp;
        this.removalTimestamp = removalTimestamp;
    }

    public Edge(String srcLabel, String dstLabel, long creationTimestamp){
        this.srcLabel = srcLabel;
        this.dstLabel = dstLabel;
        this.creationTimestamp = creationTimestamp;
        this.removalTimestamp = -1;
    }

    public String getSrcLabel(){
        return srcLabel;
    }

    public String getDstLabel(){
        return dstLabel;
    }

    public long getCreationTimestamp(){
        return creationTimestamp;
    }

    public long getRemovalTimestamp(){
        return removalTimestamp;
    }

    public void setCreationTimestamp(long timestamp){
        this.creationTimestamp = timestamp;
    }

    public void setRemovalTimestamp(long timestamp){
        this.removalTimestamp = timestamp;
    }

    public boolean isActive(){
        return creationTimestamp > removalTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Edge edge = (Edge) o;
        return creationTimestamp == edge.creationTimestamp && removalTimestamp == edge.removalTimestamp &&
                srcLabel.equals(edge.srcLabel) && dstLabel.equals(edge.dstLabel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(srcLabel, dstLabel, creationTimestamp, removalTimestamp);
    }
}
