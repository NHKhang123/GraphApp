package ui;

import model.*;
import javafx.scene.layout.Pane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.Cursor;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class GraphCanvas extends Pane {
    private Graph graph;
    private Set<Integer> highlightedMSTIds = new HashSet<>();
    private Map<Vertex, Integer> componentColors = new HashMap<>();
    private boolean showComponents = false;
    private final Map<Vertex, Circle> nodeMap = new HashMap<>();
    private final Map<String, List<javafx.scene.Node>> edgeNodesMap = new HashMap<>();
    private final Map<Edge, WeightLabel> weightLabels = new HashMap<>();
    private boolean isDarkMode = false;
    private static final double VERTEX_RADIUS = 18;
    private static final double ARROW_SIZE = 12;

    // Chế độ chọn đỉnh
    private boolean selectionModeForPrim = false;
    private boolean selectionModeForKruskal = false;
    private Consumer<Vertex> onVertexSelectedCallback = null;
    private Consumer<Vertex> onVertexSelectedForKruskalCallback = null;
    private BiConsumer<String, String> statusCallback = null;

    private Runnable onGraphChangedCallback = null;

    private final Color[] COMPONENT_COLORS = {
            Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE,
            Color.PURPLE, Color.CYAN, Color.PINK, Color.YELLOW,
            Color.BROWN, Color.MAGENTA, Color.LIME, Color.INDIGO
    };

    private class WeightLabel {
        Text text;
        double offsetX;
        double offsetY;
        Edge edge;
        boolean isDragging;

        WeightLabel(Text text, Edge edge, double defaultX, double defaultY) {
            this.text = text;
            this.edge = edge;
            this.offsetX = 0;
            this.offsetY = 0;
            this.isDragging = false;
            text.setUserData(new double[]{defaultX, defaultY});
        }

        void updatePosition(double x, double y) {
            text.setX(x + offsetX);
            text.setY(y + offsetY);
        }

        void setOffset(double x, double y) {
            this.offsetX = x;
            this.offsetY = y;
        }

        double getBaseX() {
            double[] pos = (double[]) text.getUserData();
            return pos[0];
        }

        double getBaseY() {
            double[] pos = (double[]) text.getUserData();
            return pos[1];
        }
    }

    public GraphCanvas(Graph g) {
        this.graph = g;
        setPrefWidth(900);
        setPrefHeight(600);
        setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-border-width: 2; -fx-border-style: solid;");
        redraw();
    }

    private void showStatus(String message, String type) {
        if (statusCallback != null) {
            statusCallback.accept(message, type);
        }
    }

    public void setStatusCallback(BiConsumer<String, String> callback) {
        this.statusCallback = callback;
    }

    // Các getter/setter cho selection mode
    public void setSelectionModeForPrim(boolean enable) {
        this.selectionModeForPrim = enable;
        if (enable) {
            setCursor(Cursor.CROSSHAIR);
        } else if (!selectionModeForKruskal) {
            setCursor(Cursor.DEFAULT);
        }
    }

    public void setSelectionModeForKruskal(boolean enable) {
        this.selectionModeForKruskal = enable;
        if (enable) {
            setCursor(Cursor.CROSSHAIR);
        } else if (!selectionModeForPrim) {
            setCursor(Cursor.DEFAULT);
        }
    }

    public void enableVertexSelectionMode(boolean enable) {
        if (enable) {
            setCursor(Cursor.CROSSHAIR);
        } else {
            selectionModeForPrim = false;
            selectionModeForKruskal = false;
            setCursor(Cursor.DEFAULT);
        }
    }

    public void setOnVertexSelected(Consumer<Vertex> callback) {
        this.onVertexSelectedCallback = callback;
    }

    public void setOnVertexSelectedForKruskal(Consumer<Vertex> callback) {
        this.onVertexSelectedForKruskalCallback = callback;
    }

    public void highlightVertex(Vertex vertex) {
        for (Circle c : nodeMap.values()) {
            c.setStroke(isDarkMode ? Color.WHITE : Color.GRAY);
            c.setStrokeWidth(2);
        }
        Circle selectedCircle = nodeMap.get(vertex);
        if (selectedCircle != null) {
            selectedCircle.setStroke(Color.GREEN);
            selectedCircle.setStrokeWidth(4);
        }
    }

    public void setGraph(Graph g) {
        this.graph = g;
        highlightedMSTIds.clear();
        componentColors.clear();
        showComponents = false;
        weightLabels.clear();
        redraw();
    }

    public void showConnectedComponents(Map<Vertex, Integer> colors) {
        this.componentColors = colors;
        this.showComponents = true;
        this.highlightedMSTIds.clear();
        weightLabels.clear();
        redraw();
    }

    public void clearHighlights() {
        highlightedMSTIds.clear();
        this.componentColors.clear();
        this.showComponents = false;
        weightLabels.clear();
        redraw();
    }

    public void setDarkMode(boolean dark) {
        this.isDarkMode = dark;
        String bg = dark ? "#1e1e1e" : "white";
        setStyle("-fx-background-color: " + bg + ";");
        redraw();
    }

    public void highlightMST(List<Edge> mst) {
        highlightedMSTIds.clear();
        if (mst != null) {
            for (Edge e : mst) {
                highlightedMSTIds.add(e.getId());
            }
        }
        this.showComponents = false;
        this.componentColors.clear();
        weightLabels.clear();
        redraw();
    }

    public void redraw() {
        getChildren().clear();
        nodeMap.clear();
        edgeNodesMap.clear();
        weightLabels.clear();

        if (graph.isDirected()) {
            drawDirectedGraph();
        } else {
            drawUndirectedGraph();
        }

        for (Vertex v : graph.getVertices()) {
            drawVertex(v);
        }
    }

    private void drawUndirectedGraph() {
        Map<String, List<Edge>> edgePairs = new HashMap<>();

        for (Edge e : graph.getOriginalEdges()) {
            int u = e.getFrom().getId();
            int v = e.getTo().getId();
            String pairKey = (u < v) ? u + "-" + v : v + "-" + u;

            if (!edgePairs.containsKey(pairKey)) {
                edgePairs.put(pairKey, new ArrayList<>());
            }
            edgePairs.get(pairKey).add(e);
        }

        for (Map.Entry<String, List<Edge>> entry : edgePairs.entrySet()) {
            List<Edge> edges = entry.getValue();
            drawUndirectedEdgePair(edges);
        }
    }

    private void drawUndirectedEdgePair(List<Edge> edges) {
        if (edges.isEmpty()) return;

        Edge first = edges.get(0);
        double x1 = first.getFrom().getX(), y1 = first.getFrom().getY();
        double x2 = first.getTo().getX(), y2 = first.getTo().getY();

        if (first.getFrom().equals(first.getTo())) {
            for (int i = 0; i < edges.size(); i++) {
                drawLoop(edges.get(i), i, edges.size());
            }
            return;
        }

        int count = edges.size();
        String pairKey = (first.getFrom().getId() < first.getTo().getId()) ?
                first.getFrom().getId() + "-" + first.getTo().getId() :
                first.getTo().getId() + "-" + first.getFrom().getId();

        List<javafx.scene.Node> nodes = new ArrayList<>();

        double distance = Math.hypot(x2 - x1, y2 - y1);

        for (int i = 0; i < count; i++) {
            Edge e = edges.get(i);

            double curve = 0;
            if (count > 1) {
                double maxCurve = Math.min(40, distance * 0.3);
                curve = maxCurve * (i - (count - 1) / 2.0);
            }

            double dx = x2 - x1;
            double dy = y2 - y1;
            double len = Math.sqrt(dx * dx + dy * dy);

            if (len < 1) continue;

            double midX = (x1 + x2) / 2;
            double midY = (y1 + y2) / 2;

            double perpX = -dy / len * curve;
            double perpY = dx / len * curve;

            midX += perpX;
            midY += perpY;

            CubicCurve curveLine = new CubicCurve();
            curveLine.setStartX(x1);
            curveLine.setStartY(y1);
            curveLine.setControlX1(midX - dx * 0.2);
            curveLine.setControlY1(midY - dy * 0.2);
            curveLine.setControlX2(midX + dx * 0.2);
            curveLine.setControlY2(midY + dy * 0.2);
            curveLine.setEndX(x2);
            curveLine.setEndY(y2);

            // Xác định màu sắc cho cung
            Color edgeColor;
            double strokeWidth = 2;

            if (showComponents) {
                Integer fromColor = componentColors.get(e.getFrom());
                Integer toColor = componentColors.get(e.getTo());
                if (fromColor != null && toColor != null && fromColor.equals(toColor)) {
                    edgeColor = COMPONENT_COLORS[fromColor % COMPONENT_COLORS.length];
                } else {
                    edgeColor = isDarkMode ? Color.LIGHTGRAY : Color.BLACK;
                }
            } else if (highlightedMSTIds.contains(e.getId())) {
                edgeColor = Color.RED;
                strokeWidth = 4;
            } else {
                edgeColor = isDarkMode ? Color.LIGHTGRAY : Color.BLACK;
            }

            curveLine.setStroke(edgeColor);
            curveLine.setStrokeWidth(strokeWidth);
            curveLine.setFill(null);

            getChildren().add(curveLine);
            nodes.add(curveLine);

            // Vẽ trọng số
            if (e.getWeight() != 0) {
                // Kiểm tra xem trọng số đã có offset chưa
                WeightLabel existing = weightLabels.get(e);
                double weightX, weightY;

                if (existing != null && existing.offsetX != 0 && existing.offsetY != 0) {
                    weightX = midX + existing.offsetX;
                    weightY = midY + existing.offsetY;
                } else {
                    double weightOffsetX = (i - (count - 1) / 2.0) * 20;
                    double weightOffsetY = -20 + Math.abs(i - (count - 1) / 2.0) * 8;
                    weightX = midX + weightOffsetX;
                    weightY = midY + weightOffsetY;
                }

                Text weight = new Text(weightX, weightY, String.valueOf(e.getWeight()));
                weight.setFont(Font.font(12));
                weight.setFill(isDarkMode ? Color.WHITE : Color.BLACK);
                weight.setCursor(Cursor.HAND);

                WeightLabel wl = new WeightLabel(weight, e, weightX, weightY);
                makeWeightDraggable(wl);
                weightLabels.put(e, wl);

                getChildren().add(weight);
                nodes.add(weight);
            }
        }

        edgeNodesMap.put(pairKey, nodes);
    }

    private void drawDirectedGraph() {
        Map<String, List<Edge>> edgeGroups = new HashMap<>();
        for (Edge e : graph.getEdges()) {
            String key = e.getFrom().getId() + "->" + e.getTo().getId();
            if (!edgeGroups.containsKey(key)) {
                edgeGroups.put(key, new ArrayList<>());
            }
            edgeGroups.get(key).add(e);
        }

        for (Map.Entry<String, List<Edge>> entry : edgeGroups.entrySet()) {
            List<Edge> edges = entry.getValue();
            drawMultipleDirectedEdges(edges);
        }
    }

    private void drawMultipleDirectedEdges(List<Edge> edges) {
        if (edges.isEmpty()) return;

        Edge first = edges.get(0);
        double x1 = first.getFrom().getX(), y1 = first.getFrom().getY();
        double x2 = first.getTo().getX(), y2 = first.getTo().getY();

        if (first.getFrom().equals(first.getTo())) {
            for (int i = 0; i < edges.size(); i++) {
                drawLoop(edges.get(i), i, edges.size());
            }
            return;
        }

        int count = edges.size();
        String dirKey = first.getFrom().getId() + "->" + first.getTo().getId();
        List<javafx.scene.Node> nodes = new ArrayList<>();

        double distance = Math.hypot(x2 - x1, y2 - y1);

        for (int i = 0; i < count; i++) {
            Edge e = edges.get(i);

            double curve = 0;
            if (count > 1) {
                double maxCurve = Math.min(40, distance * 0.3);
                curve = maxCurve * (i - (count - 1) / 2.0);
            }

            double dx = x2 - x1;
            double dy = y2 - y1;
            double len = Math.sqrt(dx * dx + dy * dy);

            if (len < 1) continue;

            double midX = (x1 + x2) / 2;
            double midY = (y1 + y2) / 2;

            double perpX = -dy / len * curve;
            double perpY = dx / len * curve;

            midX += perpX;
            midY += perpY;

            CubicCurve curveLine = new CubicCurve();
            curveLine.setStartX(x1);
            curveLine.setStartY(y1);
            curveLine.setControlX1(midX - dx * 0.2);
            curveLine.setControlY1(midY - dy * 0.2);
            curveLine.setControlX2(midX + dx * 0.2);
            curveLine.setControlY2(midY + dy * 0.2);
            curveLine.setEndX(x2);
            curveLine.setEndY(y2);

            Color edgeColor;
            if (showComponents) {
                Integer fromColor = componentColors.get(e.getFrom());
                Integer toColor = componentColors.get(e.getTo());
                if (fromColor != null && toColor != null && fromColor.equals(toColor)) {
                    edgeColor = COMPONENT_COLORS[fromColor % COMPONENT_COLORS.length];
                } else {
                    edgeColor = isDarkMode ? Color.LIGHTGRAY : Color.BLACK;
                }
            } else if (highlightedMSTIds.contains(e.getId())) {
                edgeColor = Color.RED;
                curveLine.setStrokeWidth(4);
            } else {
                edgeColor = isDarkMode ? Color.LIGHTGRAY : Color.BLACK;
                curveLine.setStrokeWidth(2);
            }

            curveLine.setStroke(edgeColor);
            curveLine.setStrokeWidth(highlightedMSTIds.contains(e) ? 4 : 2);
            curveLine.setFill(null);

            getChildren().add(curveLine);
            nodes.add(curveLine);

            if (e.getWeight() != 0) {
                double weightOffsetX = (i - (count - 1) / 2.0) * 15;
                double weightOffsetY = -15 + Math.abs(i - (count - 1) / 2.0) * 5;

                Text weight = new Text(midX + weightOffsetX, midY + weightOffsetY, String.valueOf(e.getWeight()));
                weight.setFont(Font.font(12));
                weight.setFill(isDarkMode ? Color.WHITE : Color.BLACK);
                weight.setCursor(Cursor.HAND);

                WeightLabel wl = new WeightLabel(weight, e, midX + weightOffsetX, midY + weightOffsetY);
                makeWeightDraggable(wl);
                weightLabels.put(e, wl);

                getChildren().add(weight);
                nodes.add(weight);
            }

            drawArrowAtTarget(e, curveLine, x1, y1, x2, y2, midX, midY, dx, dy, len, edgeColor, nodes);
        }

        edgeNodesMap.put(dirKey, nodes);
    }

    private void drawArrowAtTarget(Edge e, CubicCurve curve, double x1, double y1, double x2, double y2,
                                   double midX, double midY, double dx, double dy, double len,
                                   Color edgeColor, List<javafx.scene.Node> nodes) {

        double t = 1.0 - (VERTEX_RADIUS + 5) / len;
        t = Math.max(0.7, Math.min(0.95, t));

        double arrowX = bezierPoint(t, x1, midX - dx * 0.2, midX + dx * 0.2, x2);
        double arrowY = bezierPoint(t, y1, midY - dy * 0.2, midY + dy * 0.2, y2);

        double derivativeX = bezierDerivative(t, x1, midX - dx * 0.2, midX + dx * 0.2, x2);
        double derivativeY = bezierDerivative(t, y1, midY - dy * 0.2, midY + dy * 0.2, y2);

        double angle = Math.atan2(derivativeY, derivativeX);

        Polygon arrow = new Polygon(
                arrowX, arrowY,
                arrowX - ARROW_SIZE * Math.cos(angle - 0.3), arrowY - ARROW_SIZE * Math.sin(angle - 0.3),
                arrowX - ARROW_SIZE * Math.cos(angle + 0.3), arrowY - ARROW_SIZE * Math.sin(angle + 0.3)
        );
        arrow.setFill(edgeColor);
        getChildren().add(arrow);
        nodes.add(arrow);
    }

    private double bezierPoint(double t, double p0, double p1, double p2, double p3) {
        return Math.pow(1-t, 3) * p0 +
                3 * Math.pow(1-t, 2) * t * p1 +
                3 * (1-t) * Math.pow(t, 2) * p2 +
                Math.pow(t, 3) * p3;
    }

    private double bezierDerivative(double t, double p0, double p1, double p2, double p3) {
        return 3 * Math.pow(1-t, 2) * (p1 - p0) +
                6 * (1-t) * t * (p2 - p1) +
                3 * Math.pow(t, 2) * (p3 - p2);
    }

    private void drawLoop(Edge e, int index, int total) {
        List<javafx.scene.Node> nodes = new ArrayList<>();

        double x = e.getFrom().getX();
        double y = e.getFrom().getY();

        double offset = 25 + index * 20;

        Circle loop = new Circle(x, y - offset, 15);

        Color edgeColor;
        if (showComponents) {
            Integer color = componentColors.get(e.getFrom());
            if (color != null) {
                edgeColor = COMPONENT_COLORS[color % COMPONENT_COLORS.length];
            } else {
                edgeColor = isDarkMode ? Color.LIGHTGRAY : Color.BLACK;
            }
        } else if (highlightedMSTIds.contains(e.getId())) {
            edgeColor = Color.RED;
        } else {
            edgeColor = isDarkMode ? Color.LIGHTGRAY : Color.BLACK;
        }

        loop.setStroke(edgeColor);
        loop.setStrokeWidth(2);
        loop.setFill(null);

        getChildren().add(loop);
        nodes.add(loop);

        if (e.getWeight() != 0) {
            double weightOffsetX = (index - (total - 1) / 2.0) * 10;
            Text weight = new Text(x + 20 + weightOffsetX, y - offset + 5, String.valueOf(e.getWeight()));
            weight.setFont(Font.font(12));
            weight.setFill(isDarkMode ? Color.WHITE : Color.BLACK);
            weight.setCursor(Cursor.HAND);

            WeightLabel wl = new WeightLabel(weight, e, x + 20 + weightOffsetX, y - offset + 5);
            makeWeightDraggable(wl);
            weightLabels.put(e, wl);

            getChildren().add(weight);
            nodes.add(weight);
        }

        if (graph.isDirected()) {
            double angle = Math.PI / 2;
            double arrowX = x + 15 * Math.cos(angle);
            double arrowY = y - offset + 15 * Math.sin(angle);

            Polygon arrow = new Polygon(
                    arrowX, arrowY,
                    arrowX - 8, arrowY - 5,
                    arrowX - 8, arrowY + 5
            );
            arrow.setFill(edgeColor);
            getChildren().add(arrow);
            nodes.add(arrow);
        }

        String key = e.getFrom().getId() + "-" + e.getTo().getId() + "-loop-" + index;
        edgeNodesMap.put(key, nodes);
    }

    private void drawVertex(Vertex v) {
        Circle c = new Circle(v.getX(), v.getY(), VERTEX_RADIUS);

        if (showComponents) {
            Integer colorIndex = componentColors.get(v);
            if (colorIndex != null) {
                c.setFill(COMPONENT_COLORS[colorIndex % COMPONENT_COLORS.length]);
            } else {
                c.setFill(isDarkMode ? Color.DARKGRAY : Color.BLACK);
            }
        } else {
            c.setFill(isDarkMode ? Color.DARKGRAY : Color.BLACK);
        }

        c.setStroke(isDarkMode ? Color.WHITE : Color.GRAY);
        c.setStrokeWidth(2);
        c.setCursor(Cursor.HAND);

        Text label = new Text(v.getX() - 6, v.getY() + 6, String.valueOf(v.getId()));
        label.setFill(Color.WHITE);
        label.setFont(Font.font(14));

        Group group = new Group(c, label);
        getChildren().add(group);
        nodeMap.put(v, c);

        makeDraggable(c, v, group);
    }

    private void makeDraggable(Circle c, Vertex v, Group group) {
        final double[] offset = new double[2];
        final boolean[] dragging = new boolean[1];

        c.setOnMousePressed(e -> {
            if (selectionModeForPrim && !graph.isDirected()) {
                if (onVertexSelectedCallback != null) {
                    onVertexSelectedCallback.accept(v);
                }
                selectionModeForPrim = false;
                setCursor(Cursor.DEFAULT);
                e.consume();
                return;
            }

            if (selectionModeForKruskal && !graph.isDirected()) {
                if (onVertexSelectedForKruskalCallback != null) {
                    onVertexSelectedForKruskalCallback.accept(v);
                }
                selectionModeForKruskal = false;
                setCursor(Cursor.DEFAULT);
                e.consume();
                return;
            }

            // Kéo thả đỉnh bình thường
            offset[0] = e.getX() - v.getX();
            offset[1] = e.getY() - v.getY();
            c.setCursor(Cursor.CLOSED_HAND);
            dragging[0] = true;
            e.consume();
        });

        c.setOnMouseDragged(e -> {
            if (!dragging[0]) return;

            double newX = e.getX() - offset[0];
            double newY = e.getY() - offset[1];

            newX = Math.max(20, Math.min(getWidth() - 20, newX));
            newY = Math.max(20, Math.min(getHeight() - 20, newY));

            v.setX(newX);
            v.setY(newY);

            c.setCenterX(newX);
            c.setCenterY(newY);

            if (group.getChildren().size() > 1 && group.getChildren().get(1) instanceof Text) {
                Text label = (Text) group.getChildren().get(1);
                label.setX(newX - 6);
                label.setY(newY + 6);
            }

            updateEdgesAfterVertexMove();
            e.consume();
        });

        c.setOnMouseReleased(e -> {
            c.setCursor(Cursor.HAND);
            dragging[0] = false;
            e.consume();
        });

        c.setOnMouseExited(e -> {
            if (!dragging[0]) {
                c.setCursor(Cursor.HAND);
            }
        });
    }

    private void makeWeightDraggable(WeightLabel wl) {
        Text weight = wl.text;
        final double[] dragOffset = new double[2];

        weight.setOnMousePressed(e -> {
            dragOffset[0] = e.getX() - weight.getX();
            dragOffset[1] = e.getY() - weight.getY();
            weight.setCursor(Cursor.CLOSED_HAND);
            wl.isDragging = true;
            e.consume();
        });

        weight.setOnMouseDragged(e -> {
            if (!wl.isDragging) return;

            double newX = e.getX() - dragOffset[0];
            double newY = e.getY() - dragOffset[1];

            newX = Math.max(10, Math.min(getWidth() - 10, newX));
            newY = Math.max(10, Math.min(getHeight() - 10, newY));

            wl.setOffset(newX - wl.getBaseX(), newY - wl.getBaseY());

            weight.setX(newX);
            weight.setY(newY);

            e.consume();
        });

        weight.setOnMouseReleased(e -> {
            weight.setCursor(Cursor.HAND);
            wl.isDragging = false;
            e.consume();
        });

        weight.setOnMouseEntered(e -> {
            if (!wl.isDragging) {
                weight.setCursor(Cursor.HAND);
            }
        });
    }

    private void updateEdgesAfterVertexMove() {
        for (List<javafx.scene.Node> nodes : edgeNodesMap.values()) {
            getChildren().removeAll(nodes);
        }
        edgeNodesMap.clear();
        weightLabels.clear();

        if (graph.isDirected()) {
            drawDirectedGraph();
        } else {
            drawUndirectedGraph();
        }

        for (Vertex v : graph.getVertices()) {
            Circle c = nodeMap.get(v);
            if (c != null && c.getParent() != null) {
                Group group = (Group) c.getParent();
                group.toFront();
            }
        }
    }

    public void setOnGraphChanged(Runnable callback) {
        this.onGraphChangedCallback = callback;
    }

    private void notifyGraphChanged() {
        if (onGraphChangedCallback != null) {
            onGraphChangedCallback.run();
        }
    }

}