package model;

import java.util.Objects;

public class Edge {
    private static int nextId = 0;
    private int id;
    private Vertex from, to;
    private int weight;

    public Edge(Vertex from, Vertex to, int weight) {
        this.id = nextId++;
        this.from = from;
        this.to = to;
        this.weight = weight;
    }

    public int getId() { return id; }
    public Vertex getFrom() { return from; }
    public Vertex getTo() { return to; }
    public int getWeight() { return weight; }
    public void setWeight(int w) { this.weight = w; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Edge)) return false;
        Edge edge = (Edge) obj;
        return id == edge.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}