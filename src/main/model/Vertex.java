package main.model;

import java.util.Objects;

public class Vertex {
    private final String label;
    private long creationTimestamp;
    private long removalTimestamp;

    public Vertex(String label, long creationTimestamp){
        this.label = label;
        this.creationTimestamp = creationTimestamp;
        this.removalTimestamp = -1;
    }

    public Vertex(String label, long creationTimestamp, long removalTimestamp){
        this.label = label;
        this.creationTimestamp = creationTimestamp;
        this.removalTimestamp = removalTimestamp;
    }

    public long getCreationTimestamp(){
        return creationTimestamp;
    }

    public long getRemovalTimestamp(){
        return removalTimestamp;
    }

    public String getLabel(){
        return label;
    }

    public void setCreationTimestamp(long creationTimestamp){
        this.creationTimestamp = creationTimestamp;
    }

    public void setRemovalTimestamp(long removalTimestamp){
        this.removalTimestamp = removalTimestamp;
    }

    public boolean isActive(){
        return creationTimestamp > removalTimestamp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Vertex vertex = (Vertex) o;
        return creationTimestamp == vertex.creationTimestamp &&
                removalTimestamp == vertex.removalTimestamp && label.equals(vertex.label);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, creationTimestamp, removalTimestamp);
    }
}
