package model;

import algorithm.UnionFind;
import java.util.*;

public class Graph {
    private List<Vertex> vertices = new ArrayList<>();
    private List<Edge> edges = new ArrayList<>();
    private List<Edge> originalEdges = new ArrayList<>();
    private boolean isDirected = false;
    private Map<Integer, Vertex> idToVertex = new HashMap<>();
    private int nextId = 1;

    private String lastInput = "";

    public void setDirected(boolean d) {
        if (this.isDirected != d) {
            this.isDirected = d;
            rebuildEdgesFromOriginal();
        }
    }
    public boolean isDirected() { return isDirected; }

    private void rebuildEdgesFromOriginal() {
        edges.clear();

        for (Edge original : originalEdges) {
            Edge e = new Edge(original.getFrom(), original.getTo(), original.getWeight());
            edges.add(e);

            if (!isDirected && original.getFrom() != original.getTo()) {
                Edge reverse = new Edge(original.getTo(), original.getFrom(), original.getWeight());
                edges.add(reverse);
            }
        }
    }

    public void clear() {
        vertices.clear();
        edges.clear();
        originalEdges.clear();
        idToVertex.clear();
        nextId = 1;
        lastInput = "";
    }

    public Vertex addVertex(double x, double y) {
        int id = nextId++;
        Vertex v = new Vertex(id, x, y);
        vertices.add(v);
        idToVertex.put(id, v);
        return v;
    }

    public void addVertex(int id, double x, double y) {
        if (idToVertex.containsKey(id)) return;
        Vertex v = new Vertex(id, x, y);
        vertices.add(v);
        idToVertex.put(id, v);
        if (id >= nextId) nextId = id + 1;
    }

    public void addOriginalEdge(int fromId, int toId, int weight) {
        Vertex f = idToVertex.get(fromId);
        Vertex t = idToVertex.get(toId);
        if (f == null || t == null) return;

        Edge original = new Edge(f, t, weight);
        originalEdges.add(original);
        System.out.println("  addOriginalEdge: " + fromId + "-" + toId + " w=" + weight);

        // Thêm vào edges hiện tại
        Edge e = new Edge(f, t, weight);
        edges.add(e);
    }

    public List<Vertex> getVertices() { return vertices; }
    public List<Edge> getEdges() { return edges; }
    public List<Edge> getOriginalEdges() { return originalEdges; }

    public void updateVertexPosition(int id, double x, double y) {
        Vertex v = idToVertex.get(id);
        if (v != null) {
            v.setX(x);
            v.setY(y);
        }
    }

    // ===================== INPUT PARSING =====================
    public void parseInput(String text) {
        // Lưu đồ thị cũ để giữ vị trí nếu có
        Map<Integer, Vertex> oldPositions = new HashMap<>(idToVertex);

        clear();

        String[] lines = text.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] parts = line.split("\\s+");
            try {
                if (parts.length == 1) {
                    int id = Integer.parseInt(parts[0]);
                    if (oldPositions.containsKey(id)) {
                        Vertex old = oldPositions.get(id);
                        addVertex(id, old.getX(), old.getY());
                    } else {
                        double x = 100 + Math.random() * 700;
                        double y = 100 + Math.random() * 400;
                        addVertex(id, x, y);
                    }
                    System.out.println("Them dinh: " + id);
                } else if (parts.length >= 2) {
                    int from = Integer.parseInt(parts[0]);
                    int to = Integer.parseInt(parts[1]);
                    int weight = (parts.length >= 3) ? Integer.parseInt(parts[2]) : 0;

                    System.out.println("Them cung: " + from + " -> " + to + " w=" + weight);

                    // Thêm đỉnh nếu chưa có
                    if (!idToVertex.containsKey(from)) {
                        if (oldPositions.containsKey(from)) {
                            addVertex(from, oldPositions.get(from).getX(), oldPositions.get(from).getY());
                        } else {
                            addVertex(from, 100 + Math.random() * 700, 100 + Math.random() * 400);
                        }
                    }
                    if (!idToVertex.containsKey(to)) {
                        if (oldPositions.containsKey(to)) {
                            addVertex(to, oldPositions.get(to).getX(), oldPositions.get(to).getY());
                        } else {
                            addVertex(to, 100 + Math.random() * 700, 100 + Math.random() * 400);
                        }
                    }

                    addOriginalEdge(from, to, weight);
                }
            } catch (NumberFormatException ignored) {}
        }

        System.out.println("Tong so dinh: " + vertices.size());
        System.out.println("Tong so cung goc: " + originalEdges.size());
        for (Edge e : originalEdges) {
            System.out.println("  Edge: " + e.getFrom().getId() + "-" + e.getTo().getId() + " w=" + e.getWeight());
        }
    }

    // ===================== KIỂM TRA MIỀN LIÊN THÔNG =====================
    public int getNumberOfConnectedComponents() {
        if (vertices.isEmpty()) return 0;

        Set<Vertex> visited = new HashSet<>();
        Map<Vertex, List<Vertex>> adj = buildAdjListForUndirected();
        int count = 0;

        for (Vertex v : vertices) {
            if (!visited.contains(v)) {
                dfs(v, visited, adj);
                count++;
            }
        }
        return count;
    }

    public Map<Vertex, Integer> getComponentColors() {
        Map<Vertex, Integer> componentMap = new HashMap<>();
        if (vertices.isEmpty()) return componentMap;

        Set<Vertex> visited = new HashSet<>();
        Map<Vertex, List<Vertex>> adj = buildAdjListForUndirected();
        int color = 0;

        for (Vertex v : vertices) {
            if (!visited.contains(v)) {
                dfs(v, visited, adj);
                // Gán màu cho tất cả các đỉnh trong component này
                for (Vertex vertex : vertices) {
                    if (visited.contains(vertex) && !componentMap.containsKey(vertex)) {
                        componentMap.put(vertex, color);
                    }
                }
                color++;
            }
        }
        return componentMap;
    }

    private void dfs(Vertex v, Set<Vertex> visited, Map<Vertex, List<Vertex>> adj) {
        visited.add(v);
        for (Vertex nei : adj.getOrDefault(v, new ArrayList<>())) {
            if (!visited.contains(nei)) {
                dfs(nei, visited, adj);
            }
        }
    }

    private Map<Vertex, List<Vertex>> buildAdjListForUndirected() {
        Map<Vertex, List<Vertex>> adj = new HashMap<>();
        for (Vertex v : vertices) adj.put(v, new ArrayList<>());

        Set<String> seen = new HashSet<>();
        for (Edge e : originalEdges) {
            int u = e.getFrom().getId();
            int v = e.getTo().getId();
            String key = (u < v) ? u + "-" + v : v + "-" + u;

            if (!seen.contains(key)) {
                seen.add(key);
                adj.get(e.getFrom()).add(e.getTo());
                adj.get(e.getTo()).add(e.getFrom());
            }
        }
        return adj;
    }

    // ===================== MST =====================
    public static class MSTResult {
        public final List<Edge> edges;
        public final int totalWeight;
        public MSTResult(List<Edge> e, int w) { edges = e; totalWeight = w; }
    }

    public MSTResult findMST(String algo) {
        if (isDirected) {
            throw new IllegalArgumentException("Chỉ hỗ trợ đồ thị vô hướng");
        }
        return algo.equals("Kruskal") ? kruskal() : prim();
    }

    private MSTResult kruskal() {
        List<Edge> mstEdges = new ArrayList<>();
        int totalWeight = 0;

        // Tạo bản sao và sắp xếp
        List<Edge> sortedEdges = new ArrayList<>(originalEdges);
        sortedEdges.sort(Comparator.comparingInt(Edge::getWeight));

        System.out.println("Các cung sau khi sắp xếp");
        for (Edge e : sortedEdges) {
            System.out.println("  Edge: " + e.getFrom().getId() + "-" + e.getTo().getId() + " w=" + e.getWeight());
        }

        // Tạo map id -> index
        Map<Integer, Integer> idToIndex = new HashMap<>();
        for (int i = 0; i < vertices.size(); i++) {
            idToIndex.put(vertices.get(i).getId(), i);
        }

        UnionFind uf = new UnionFind(vertices.size());

        for (Edge e : sortedEdges) {
            int uIndex = idToIndex.get(e.getFrom().getId());
            int vIndex = idToIndex.get(e.getTo().getId());

            int rootU = uf.find(uIndex);
            int rootV = uf.find(vIndex);

            if (rootU != rootV) {
                uf.union(uIndex, vIndex);
                mstEdges.add(e);
                totalWeight += e.getWeight();
            }
        }

        return new MSTResult(mstEdges, totalWeight);
    }

    private MSTResult prim() {
        if (vertices.isEmpty()) return new MSTResult(new ArrayList<>(), 0);

        List<Edge> mstEdges = new ArrayList<>();
        int totalWeight = 0;

        Set<Integer> inMST = new HashSet<>();
        PriorityQueue<Edge> pq = new PriorityQueue<>(Comparator.comparingInt(Edge::getWeight));

        Vertex start = vertices.get(0);
        int startId = start.getId();
        inMST.add(startId);

        // Thêm các cung từ đỉnh start
        for (Edge e : originalEdges) {
            if (e.getFrom().getId() == startId || e.getTo().getId() == startId) {
                pq.add(e);
            }
        }

        while (!pq.isEmpty() && inMST.size() < vertices.size()) {
            Edge minEdge = pq.poll();

            int fromId = minEdge.getFrom().getId();
            int toId = minEdge.getTo().getId();
            int nextId = -1;

            if (!inMST.contains(fromId)) {
                nextId = fromId;
            } else if (!inMST.contains(toId)) {
                nextId = toId;
            }

            if (nextId != -1 && !inMST.contains(nextId)) {
                inMST.add(nextId);
                mstEdges.add(minEdge);
                totalWeight += minEdge.getWeight();

                // Thêm các cung mới
                for (Edge e : originalEdges) {
                    int eFrom = e.getFrom().getId();
                    int eTo = e.getTo().getId();
                    if ((eFrom == nextId && !inMST.contains(eTo)) ||
                            (eTo == nextId && !inMST.contains(eFrom))) {
                        pq.add(e);
                    }
                }
            }
        }
        return new MSTResult(mstEdges, totalWeight);
    }

    public MSTResult findMSTWithStart(String algo, Vertex startVertex) {
        if (isDirected) {
            throw new IllegalArgumentException("Chỉ hỗ trợ đồ thị vô hướng");
        }
        if (algo.equals("Prim")) {
            return primWithStart(startVertex);
        }
        return findMST(algo);
    }

    private MSTResult primWithStart(Vertex startVertex) {
        if (vertices.isEmpty()) return new MSTResult(new ArrayList<>(), 0);

        // Kiểm tra đỉnh bắt đầu có trong đồ thị không
        if (!vertices.contains(startVertex)) {
            throw new IllegalArgumentException("Đỉnh bắt đầu không tồn tại trong đồ thị!");
        }

        List<Edge> mstEdges = new ArrayList<>();
        int totalWeight = 0;

        Set<Vertex> inMST = new HashSet<>();
        PriorityQueue<Edge> pq = new PriorityQueue<>(Comparator.comparingInt(Edge::getWeight));

        // Bắt đầu từ đỉnh đã chọn
        Vertex start = startVertex;
        inMST.add(start);

        // Thêm các cung từ đỉnh start
        for (Edge e : originalEdges) {
            if (e.getFrom().equals(start) || e.getTo().equals(start)) {
                pq.add(e);
            }
        }

        while (!pq.isEmpty() && inMST.size() < vertices.size()) {
            Edge minEdge = pq.poll();
            Vertex next = null;

            if (!inMST.contains(minEdge.getFrom())) {
                next = minEdge.getFrom();
            } else if (!inMST.contains(minEdge.getTo())) {
                next = minEdge.getTo();
            }

            if (next != null && !inMST.contains(next)) {
                inMST.add(next);
                mstEdges.add(minEdge);
                totalWeight += minEdge.getWeight();

                // Thêm các cung mới từ đỉnh vừa thêm
                for (Edge e : originalEdges) {
                    if ((e.getFrom().equals(next) && !inMST.contains(e.getTo())) ||
                            (e.getTo().equals(next) && !inMST.contains(e.getFrom()))) {
                        pq.add(e);
                    }
                }
            }
        }
        System.out.println("Các cạnh của cây khung bắt đầu từ đỉnh: " + start.getId());
        for (Edge e : mstEdges) {
            System.out.println("Edge: " + e.getFrom().getId() + "-" + e.getTo().getId() + " w=" + e.getWeight());
        }

        // Kiểm tra xem có tìm được cây khung trên toàn bộ đồ thị không
        if (inMST.size() < vertices.size()) {
            // Nếu không, chỉ trả về cây khung trên miền liên thông chứa đỉnh bắt đầu
            System.out.println("Chỉ tìm được cây khung trên miền liên thông chứa đỉnh " + startVertex.getId());
        }

        return new MSTResult(mstEdges, totalWeight);
    }

    public MSTResult findMSTInComponent(String algo, Vertex startVertex) {
        if (isDirected) {
            throw new IllegalArgumentException("Chỉ hỗ trợ đồ thị vô hướng");
        }

        Set<Vertex> componentVertices = getComponentVertices(startVertex);
        if (componentVertices.size() <= 1) return new MSTResult(new ArrayList<>(), 0);

        List<Edge> componentEdges = new ArrayList<>();
        for (Edge e : originalEdges) {
            if (componentVertices.contains(e.getFrom()) && componentVertices.contains(e.getTo())) {
                componentEdges.add(e);
            }
        }

        // Sắp xếp và chạy Kruskal trên componentEdges
        List<Edge> sorted = new ArrayList<>(componentEdges);
        sorted.sort(Comparator.comparingInt(Edge::getWeight));
        System.out.println("Các cung trong bộ phận liên thông sau khi sắp xếp");
        for (Edge e : sorted) {
            System.out.println("  Edge: " + e.getFrom().getId() + "-" + e.getTo().getId() + " w=" + e.getWeight());
        }

        Map<Integer, Integer> idToIndex = new HashMap<>();
        for (int i = 0; i < vertices.size(); i++) {
            idToIndex.put(vertices.get(i).getId(), i);
        }

        UnionFind uf = new UnionFind(vertices.size());
        List<Edge> mstEdges = new ArrayList<>();
        int totalWeight = 0;
        for (Edge e : sorted) {
            int u = idToIndex.get(e.getFrom().getId());
            int v = idToIndex.get(e.getTo().getId());
            if (uf.find(u) != uf.find(v)) {
                uf.union(u, v);
                mstEdges.add(e);
                totalWeight += e.getWeight();
            }
        }
        return new MSTResult(mstEdges, totalWeight);
    }

    private Set<Vertex> getComponentVertices(Vertex startVertex) {
        Set<Vertex> component = new HashSet<>();
        Set<Vertex> visited = new HashSet<>();
        Map<Vertex, List<Vertex>> adj = buildAdjListForUndirected();

        Queue<Vertex> queue = new LinkedList<>();
        queue.add(startVertex);
        visited.add(startVertex);

        while (!queue.isEmpty()) {
            Vertex current = queue.poll();
            component.add(current);

            for (Vertex neighbor : adj.getOrDefault(current, new ArrayList<>())) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return component;
    }

}