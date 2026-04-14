package ui;

import javafx.scene.input.KeyCode;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import model.Graph;
import model.Vertex;
import model.Edge;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.animation.PauseTransition;
import javafx.util.Duration;
import javafx.scene.image.WritableImage;
import javafx.scene.image.Image;
import javafx.scene.SnapshotParameters;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.paint.Color;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.function.Consumer;
import java.util.Stack;
import java.util.function.BiConsumer;

public class Main extends Application {
    private Graph graph = new Graph();
    private GraphCanvas canvas;
    private TextArea inputArea;
    private boolean isDark = false;
    private PauseTransition pause = new PauseTransition(Duration.millis(500));
    private GridPane virtualKeyboard;
    private boolean keyboardVisible = false;

    private Label statusLabel;
    private HBox statusBar;

    private Label errorLabel;
    private VBox errorPanel;

    private BorderPane root;
    private VBox left;
    private HBox bottom;
    private Label titleLabel;
    private Label inputLabel;
    private ToggleButton guideToggle;
    private ToggleButton undir;
    private ToggleButton dir;
    private Button clearBtn;
    private Button keyboardBtn;
    private Button ccBtn;
    private Button mstBtn;
    private Button darkBtn;
    private Button saveImageBtn;
    private Button saveGraphBtn;
    private Button loadGraphBtn;

    // Nút kép cho Kruskal
    private VBox kruskalContainer;
    private ToggleButton kruskalMainBtn;
    private Button kruskalSelectComponentBtn;
    private boolean kruskalModeAll = true; // true: toàn bộ, false: chọn miền
    private Vertex selectedVertexForKruskal = null;

    // Nút kép cho Prim
    private VBox primContainer;
    private ToggleButton primMainBtn;
    private Button primSelectStartBtn;
    private Vertex selectedStartVertex = null;

    // ToggleGroup cho thuật toán
    private ToggleGroup algoToggleGroup;

    // Danh sách lỗi
    private List<Integer> errorLines = new ArrayList<>();

    // Panel hướng dẫn
    private VBox guidePanel;
    private boolean guideVisible = false;

    // Thêm các biến ở đầu class
    private Stack<String> undoStack = new Stack<>();
    private Stack<String> redoStack = new Stack<>();
    private String currentText = "";

    @Override
    public void start(Stage stage) {
        loadApplicationIcon(stage);

        root = new BorderPane();
        root.setPadding(new Insets(10));

        // ==================== TOP ====================
        HBox top = new HBox(10);
        top.setPadding(new Insets(5));
        top.setAlignment(Pos.CENTER_LEFT);

        ToggleGroup directionGroup = new ToggleGroup();
        undir = new ToggleButton("Vô hướng");
        dir = new ToggleButton("Có hướng");
        undir.setToggleGroup(directionGroup);
        dir.setToggleGroup(directionGroup);
        undir.setSelected(true);

        undir.setOnAction(e -> {
            if (undir.isSelected()) {
                graph.setDirected(false);
                canvas.setGraph(graph);
                enableAlgorithmButtons(true);
                resetSelection();
                showStatus("Đã chuyển sang chế độ vô hướng", "info");
            }
        });

        dir.setOnAction(e -> {
            if (dir.isSelected()) {
                graph.setDirected(true);
                canvas.setGraph(graph);
                enableAlgorithmButtons(false);
                resetSelection();
                showStatus("Đã chuyển sang chế độ có hướng", "info");
            }
        });

        titleLabel = new Label("ỨNG DỤNG ĐỒ THỊ");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        HBox directionContainer = new HBox(5);
        directionContainer.setAlignment(Pos.CENTER);
        directionContainer.getChildren().addAll(undir, dir);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        top.getChildren().addAll(titleLabel, spacer, directionContainer);
        root.setTop(top);

        // ==================== LEFT ====================
        left = new VBox(10);
        left.setPadding(new Insets(10));
        left.setPrefWidth(350);
        left.setStyle("-fx-border-color: lightgray; -fx-border-width: 0 1 0 0; -fx-background-color: white;");

        inputLabel = new Label("Nhập đồ thị:");
        inputLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: black;");

        inputArea = new TextArea();
        currentText = inputArea.getText();
        inputArea.setPrefHeight(150);
        inputArea.setPromptText("Nhập theo hướng dẫn\nVí dụ:\n1 2 3\n1 2\n4");
        inputArea.setStyle("-fx-text-fill: black; -fx-control-inner-background: white; -fx-prompt-text-fill: #999999;");

        errorPanel = new VBox(5);
        errorPanel.setPadding(new Insets(5));
        errorPanel.setStyle("-fx-background-color: #ffeeee; -fx-border-color: #ff8888; -fx-border-radius: 3;");
        errorPanel.setVisible(false);

        Label errorTitle = new Label("⚠ DÒNG LỖI (bị bỏ qua):");
        errorTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #ff4444;");

        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #ff4444; -fx-wrap-text: true;");
        errorLabel.setFont(javafx.scene.text.Font.font(12));

        errorPanel.getChildren().addAll(errorTitle, errorLabel);

        inputArea.textProperty().addListener((obs, old, newText) -> {
            if (isUpdatingFromGraph) return; // Bỏ qua nếu đang cập nhật từ graph

            if (!newText.equals(currentText)) {
                saveToUndo();
            }

            validateInput(newText);

            pause.setOnFinished(e -> {
                String filteredText = getValidLinesOnly(newText);
                graph.parseInput(filteredText);
                canvas.setGraph(graph);
                canvas.clearHighlights();
                selectedStartVertex = null;
                selectedVertexForKruskal = null;
                if (!filteredText.trim().isEmpty() && errorLines.isEmpty()) {
                    showStatus("Đã cập nhật đồ thị", "success");
                }
            });
            pause.playFromStart();
        });

        HBox buttonRow = new HBox(10);
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        clearBtn = new Button("Xóa tất cả");
        clearBtn.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white; -fx-background-radius: 3; -fx-cursor: hand;");
        clearBtn.setOnAction(e -> {
            graph.clear();
            inputArea.clear();
            canvas.setGraph(graph);
            canvas.clearHighlights();
            errorLines.clear();
            errorPanel.setVisible(false);
            resetSelection();
            showStatus("Đã xóa toàn bộ đồ thị", "warning");
        });

        keyboardBtn = new Button("⌨️ Bàn phím");
        keyboardBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 3; -fx-cursor: hand;");
        keyboardBtn.setOnAction(e -> {
            keyboardVisible = !keyboardVisible;
            virtualKeyboard.setVisible(keyboardVisible);
            showStatus(keyboardVisible ? "Đã mở bàn phím ảo" : "Đã đóng bàn phím ảo", "info");
        });

        guideToggle = new ToggleButton("📘 Hướng dẫn");
        guideToggle.setStyle("-fx-background-color: #607D8B; -fx-text-fill: white; -fx-background-radius: 3; -fx-cursor: hand;");
        guideToggle.setOnAction(e -> toggleGuidePanel());

        buttonRow.getChildren().addAll(clearBtn, keyboardBtn, guideToggle);

        virtualKeyboard = createVirtualKeyboard();
        virtualKeyboard.setVisible(false);
        virtualKeyboard.setPadding(new Insets(10));
        virtualKeyboard.setStyle("-fx-background-color: #f0f0f0; -fx-border-radius: 5;");

        HBox fileButtonRow = new HBox(10);
        fileButtonRow.setAlignment(Pos.CENTER_LEFT);

        saveGraphBtn = new Button("💾 Lưu đồ thị");
        saveGraphBtn.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white; -fx-background-radius: 3; -fx-cursor: hand;");
        saveGraphBtn.setOnAction(e -> saveGraphToFile(stage));

        loadGraphBtn = new Button("📂 Đọc đồ thị");
        loadGraphBtn.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white; -fx-background-radius: 3; -fx-cursor: hand;");
        loadGraphBtn.setOnAction(e -> loadGraphFromFile(stage));

        fileButtonRow.getChildren().addAll(saveGraphBtn, loadGraphBtn);

        left.getChildren().addAll(
                inputLabel,
                inputArea,
                errorPanel,
                buttonRow,
                virtualKeyboard,
                fileButtonRow
        );
        root.setLeft(left);

        // ==================== CENTER ====================
        canvas = new GraphCanvas(graph);
        canvas.setStatusCallback(this::showStatus);


        // Thiết lập callback
        canvas.setOnVertexSelected(this::onVertexSelected);
        canvas.setOnVertexSelectedForKruskal(this::onVertexSelectedForKruskal);

        // ==================== RIGHT (Hướng dẫn) ====================
        createGuidePanel();

        HBox centerBox = new HBox();
        centerBox.setSpacing(10);
        centerBox.getChildren().addAll(canvas, guidePanel);
        HBox.setHgrow(canvas, Priority.ALWAYS);

        root.setCenter(centerBox);

        // ==================== BOTTOM ====================
        bottom = new HBox(15);
        bottom.setPadding(new Insets(10));
        bottom.setAlignment(Pos.CENTER);
        bottom.setStyle("-fx-border-color: lightgray; -fx-border-width: 1 0 0 0; -fx-background-color: white;");

        ccBtn = new Button("📊 Số miền liên thông");
        ccBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 3; -fx-cursor: hand;");
        ccBtn.setOnAction(e -> {
            int num = graph.getNumberOfConnectedComponents();
            canvas.showConnectedComponents(graph.getComponentColors());
            showStatus("Đồ thị có " + num + " miền liên thông", "success");
        });

        // Tạo ToggleGroup cho thuật toán
        algoToggleGroup = new ToggleGroup();

        // Tạo nút Kruskal và Prim
        createKruskalButton();
        createPrimButton();

        mstBtn = new Button("🌲 Cây khung tối tiểu");
        mstBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-background-radius: 3; -fx-cursor: hand;");
        mstBtn.setOnAction(e -> calculateMST());

        saveImageBtn = new Button("🖼️ Lưu hình ảnh");
        saveImageBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-background-radius: 3; -fx-cursor: hand;");
        saveImageBtn.setOnAction(e -> saveGraphAsImage(stage));

        darkBtn = new Button("🌙 Chế độ tối");
        darkBtn.setStyle("-fx-background-color: #666666; -fx-text-fill: white; -fx-background-radius: 3; -fx-cursor: hand;");
        darkBtn.setOnAction(e -> {
            isDark = !isDark;
            updateTheme();
            updateGuideTheme();
            updateAlgorithmButtonsStyle();
            showStatus(isDark ? "Đã chuyển sang chế độ tối" : "Đã chuyển sang chế độ sáng", "info");
        });


        bottom.getChildren().addAll(ccBtn, kruskalContainer, primContainer, mstBtn, saveImageBtn, darkBtn);

        statusBar = new HBox();
        statusBar.setPadding(new Insets(5, 10, 5, 10));
        statusBar.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: lightgray; -fx-border-width: 1 0 0 0;");
        statusBar.setAlignment(Pos.CENTER_LEFT);

        statusLabel = new Label("Sẵn sàng");
        statusLabel.setStyle("-fx-text-fill: #666666;");

        statusBar.getChildren().add(statusLabel);

        VBox bottomContainer = new VBox();
        bottomContainer.getChildren().addAll(bottom, statusBar);
        root.setBottom(bottomContainer);


        Scene scene = new Scene(root, 1400, 800);

        scene.setOnKeyPressed(e -> {
            if (e.isControlDown() && e.getCode() == KeyCode.Z) {
                if (e.isShiftDown()) {
                    // Ctrl + Shift + Z: Redo
                    redo();
                } else {
                    // Ctrl + Z: Undo
                    undo();
                }
                e.consume();
            } else if (e.isControlDown() && e.getCode() == KeyCode.Y) {
                // Ctrl + Y: Redo
                redo();
                e.consume();
            }
        });

        stage.setScene(scene);
        stage.setTitle("Ứng dụng Đồ thị – Niên luận");
        stage.show();

        inputArea.setText("1 2 6\n1 3 15\n1 4 1\n1 6 4\n2 4 15\n2 5 13\n2 6 13\n3 4 12\n3 5 4\n4 6 1");

        enableAlgorithmButtons(!graph.isDirected());
        showStatus("Ứng dụng đã sẵn sàng", "success");
    }

    /**
     * Tạo nút kép cho Kruskal
     */
    private void createKruskalButton() {
        kruskalContainer = new VBox();
        kruskalContainer.setSpacing(2);
        kruskalContainer.setStyle("-fx-background-color: transparent;");

        kruskalMainBtn = new ToggleButton("Kruskal");
        kruskalMainBtn.setPrefSize(100, 35);
        kruskalMainBtn.setToggleGroup(algoToggleGroup);
        kruskalMainBtn.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-font-weight: bold; -fx-background-radius: 5 5 0 0; -fx-cursor: hand; -fx-border-color: #ccc; -fx-border-width: 1;");

        kruskalMainBtn.setOnAction(e -> {
            if (graph.isDirected()) {
                showStatus("Cây khung chỉ hỗ trợ đồ thị vô hướng!", "error");
                kruskalMainBtn.setSelected(false);
                return;
            }
            updateAlgorithmButtonsStyle();
            showStatus("Đã chọn thuật toán Kruskal - Chọn chế độ ở nút bên dưới", "info");
        });

        // Nút chọn miền liên thông
        kruskalSelectComponentBtn = new Button("🌐 Chọn miền");
        kruskalSelectComponentBtn.setPrefSize(100, 30);
        kruskalSelectComponentBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 11; -fx-background-radius: 0 0 5 5; -fx-cursor: hand;");
        kruskalSelectComponentBtn.setOnAction(e -> {
            // Tự động chọn Kruskal
            kruskalMainBtn.setSelected(true);
            updateAlgorithmButtonsStyle();
            showKruskalModePopup();
        });

        kruskalContainer.getChildren().addAll(kruskalMainBtn, kruskalSelectComponentBtn);
    }

    /**
     * Tạo nút kép cho Prim
     */
    private void createPrimButton() {
        primContainer = new VBox();
        primContainer.setSpacing(2);
        primContainer.setStyle("-fx-background-color: transparent;");

        primMainBtn = new ToggleButton("Prim");
        primMainBtn.setPrefSize(100, 35);
        primMainBtn.setToggleGroup(algoToggleGroup);
        primMainBtn.setStyle("-fx-background-color: white; -fx-text-fill: black; -fx-font-weight: bold; -fx-background-radius: 5 5 0 0; -fx-cursor: hand; -fx-border-color: #ccc; -fx-border-width: 1;");

        primMainBtn.setOnAction(e -> {
            if (graph.isDirected()) {
                showStatus("Cây khung chỉ hỗ trợ đồ thị vô hướng!", "error");
                primMainBtn.setSelected(false);
                return;
            }
            updateAlgorithmButtonsStyle();
            showStatus("Đã chọn thuật toán Prim - Hãy nhấn nút Chọn đỉnh để chọn đỉnh bắt đầu", "info");
        });

        // Nút chọn đỉnh bắt đầu
        primSelectStartBtn = new Button("🎯 Chọn đỉnh");
        primSelectStartBtn.setPrefSize(100, 30);
        primSelectStartBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-size: 11; -fx-background-radius: 0 0 5 5; -fx-cursor: hand;");
        primSelectStartBtn.setOnAction(e -> {
            // Tự động chọn Prim
            primMainBtn.setSelected(true);
            updateAlgorithmButtonsStyle();

            if (graph.isDirected()) {
                showStatus("Cây khung chỉ hỗ trợ đồ thị vô hướng!", "error");
                return;
            }
            if (graph.getVertices().isEmpty()) {
                showStatus("Không có đồ thị để chọn đỉnh!", "error");
                return;
            }
            showStatus("Vui lòng click vào đỉnh trên đồ thị để chọn đỉnh bắt đầu", "info");
            canvas.enableVertexSelectionMode(true);
            canvas.setSelectionModeForPrim(true);
        });

        primContainer.getChildren().addAll(primMainBtn, primSelectStartBtn);
    }

    private Popup kruskalPopupWindow = null;
    private VBox kruskalModePopup;
    private boolean kruskalPopupVisible = false;

    private void showKruskalModePopup() {

        if (kruskalPopupWindow != null && kruskalPopupWindow.isShowing()) {
            hideKruskalModePopup();
            return;
        }

        if (kruskalModePopup == null) {
            createKruskalModePopupContent();
        }

        kruskalModePopup.setVisible(true);

        if (kruskalPopupWindow == null) {
            kruskalPopupWindow = new Popup();
            kruskalPopupWindow.setAutoHide(true);
            kruskalPopupWindow.getContent().add(kruskalModePopup);
            kruskalPopupWindow.setOnHidden((WindowEvent we) -> {
                kruskalPopupVisible = false;
                kruskalModePopup.setVisible(false);
            });
        } else {
            if (!kruskalPopupWindow.getContent().contains(kruskalModePopup)) {
                kruskalPopupWindow.getContent().clear();
                kruskalPopupWindow.getContent().add(kruskalModePopup);
            }
        }

        kruskalModePopup.applyCss();
        kruskalModePopup.layout();

        Window window = root.getScene().getWindow();

        double popupWidth = kruskalModePopup.getWidth() > 0 ? kruskalModePopup.getWidth() : kruskalModePopup.prefWidth(-1);
        double popupHeight = kruskalModePopup.getHeight() > 0 ? kruskalModePopup.getHeight() : kruskalModePopup.prefHeight(-1);

        double x = window.getX() + (window.getWidth() - popupWidth) / 2.0;
        double y = window.getY() + (window.getHeight() - popupHeight) / 2.0;

        kruskalPopupWindow.show(window, x, y);
        kruskalPopupVisible = true;
    }

    private void hideKruskalModePopup() {
        if (kruskalPopupWindow != null && kruskalPopupWindow.isShowing()) {
            kruskalPopupWindow.hide();
        }
        if (kruskalModePopup != null) {
            kruskalModePopup.setVisible(false);
        }
        kruskalPopupVisible = false;
    }

    private void createKruskalModePopupContent() {
        kruskalModePopup = new VBox();
        kruskalModePopup.setPadding(new Insets(20));
        kruskalModePopup.setSpacing(15);
        kruskalModePopup.setMaxWidth(350);
        kruskalModePopup.setPrefWidth(350);
        kruskalModePopup.setFillWidth(true);
        kruskalModePopup.setStyle(
                "-fx-background-color: #2d2d2d; " +
                        "-fx-background-radius: 15; " +
                        "-fx-border-radius: 15; " +
                        "-fx-border-color: #4CAF50; " +
                        "-fx-border-width: 2;"
        );

        Label title = new Label("CHỌN CHẾ ĐỘ");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 18));
        title.setTextFill(Color.WHITE);
        title.setAlignment(Pos.CENTER);
        title.setMaxWidth(Double.MAX_VALUE);

        Label desc = new Label("Bạn muốn tìm cây khung tối tiểu trên:");
        desc.setFont(Font.font("Arial", 13));
        desc.setTextFill(Color.LIGHTGRAY);
        desc.setWrapText(true);
        desc.setAlignment(Pos.CENTER);

        Button allBtn = new Button("TOÀN BỘ");
        allBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-size: 14; -fx-font-weight: bold;");
        allBtn.setMaxWidth(Double.MAX_VALUE);
        allBtn.setPrefHeight(45);
        allBtn.setOnAction(e -> {
            kruskalModeAll = true;
            selectedVertexForKruskal = null;
            canvas.clearHighlights();
            showStatus("Đã chọn chế độ: Tìm cây khung tối tiểu trên TOÀN BỘ đồ thị", "success");
            hideKruskalModePopup();
        });

        Button componentBtn = new Button("MỘT MIỀN");
        componentBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-size: 14; -fx-font-weight: bold;");
        componentBtn.setMaxWidth(Double.MAX_VALUE);
        componentBtn.setPrefHeight(45);
        componentBtn.setOnAction(e -> {
            kruskalModeAll = false;
            selectedVertexForKruskal = null;
            canvas.clearHighlights();
            showStatus("Vui lòng chọn 1 đỉnh để xác định miền liên thông", "info");
            canvas.enableVertexSelectionMode(true);
            canvas.setSelectionModeForKruskal(true);
            hideKruskalModePopup();
        });

        Button cancelBtn = new Button("HUỶ");
        cancelBtn.setStyle("-fx-background-color: #666666; -fx-text-fill: white; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-size: 14;");
        cancelBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.setPrefHeight(40);
        cancelBtn.setOnAction(e -> hideKruskalModePopup());

        allBtn.setMaxWidth(Double.MAX_VALUE);
        componentBtn.setMaxWidth(Double.MAX_VALUE);
        cancelBtn.setMaxWidth(Double.MAX_VALUE);

        allBtn.setMinWidth(300);
        componentBtn.setMinWidth(300);
        cancelBtn.setMinWidth(300);

        kruskalModePopup.getChildren().addAll(title, desc, allBtn, componentBtn, cancelBtn);
    }

    /**
     * Tính cây khung tối tiểu dựa trên thuật toán và lựa chọn
     */
    private void calculateMST() {
        if (graph.isDirected()) {
            showStatus("Cây khung chỉ hỗ trợ đồ thị vô hướng!", "error");
            return;
        }

        if (graph.getVertices().isEmpty()) {
            showStatus("Không có đồ thị để tính!", "error");
            return;
        }

        // Kiểm tra đã chọn thuật toán
        if (!kruskalMainBtn.isSelected() && !primMainBtn.isSelected()) {
            showStatus("Vui lòng chọn thuật toán (Kruskal hoặc Prim)!", "warning");
            return;
        }

        try {
            Graph.MSTResult result;

            if (kruskalMainBtn.isSelected()) {
                // Xử lý Kruskal
                if (kruskalModeAll) {
                    result = graph.findMST("Kruskal");
                    showStatus("Kruskal (toàn bộ đồ thị) - Tổng trọng số: " + result.totalWeight, "success");
                    System.out.println("MST edges count: " + result.edges.size());
                    for (Edge e : result.edges) {
                        System.out.println("Edge: " + e.getFrom().getId() + "-" + e.getTo().getId() + " w=" + e.getWeight());
                    }
                } else {
                    if (selectedVertexForKruskal == null) {
                        showStatus("Vui lòng chọn đỉnh để xác định miền liên thông (nhấn nút '📍 Chọn miền')!", "warning");
                        return;
                    }
                    result = graph.findMSTInComponent("Kruskal", selectedVertexForKruskal);
                    showStatus("Kruskal (trên miền chứa đỉnh " + selectedVertexForKruskal.getId() +
                            ") - Tổng trọng số: " + result.totalWeight, "success");
                    System.out.println("Các cạnh của cây khung");
                    for (Edge e : result.edges) {
                        System.out.println("Edge: " + e.getFrom().getId() + "-" + e.getTo().getId() + " w=" + e.getWeight());
                    }
                }
            } else {
                // Xử lý Prim
                if (selectedStartVertex == null) {
                    showStatus("Vui lòng chọn đỉnh bắt đầu (nhấn nút '🎯 Chọn đỉnh')!", "warning");
                    return;
                }
                result = graph.findMSTWithStart("Prim", selectedStartVertex);
                showStatus("Prim (bắt đầu từ đỉnh " + selectedStartVertex.getId() +
                        ") - Tổng trọng số: " + result.totalWeight, "success");
            }

            // Highlight cây khung
            canvas.highlightMST(result.edges);

        } catch (Exception ex) {
            showStatus("Lỗi: " + ex.getMessage(), "error");
            ex.printStackTrace();
        }
    }

    /**
     * Xử lý khi đỉnh được chọn cho Prim
     */
    private void onVertexSelected(Vertex vertex) {
        if (primMainBtn.isSelected() && !graph.isDirected()) {
            selectedStartVertex = vertex;
            showStatus("Đã chọn đỉnh " + vertex.getId() + " làm đỉnh bắt đầu cho thuật toán Prim", "success");
            canvas.enableVertexSelectionMode(false);
            canvas.setSelectionModeForPrim(false);
            canvas.highlightVertex(vertex);
        }
    }

    /**
     * Xử lý khi đỉnh được chọn cho Kruskal (chọn miền)
     */
    private void onVertexSelectedForKruskal(Vertex vertex) {
        if (kruskalMainBtn.isSelected() && !graph.isDirected() && !kruskalModeAll) {
            selectedVertexForKruskal = vertex;
            showStatus("Đã chọn đỉnh " + vertex.getId() + " để xác định miền liên thông cho thuật toán Kruskal", "success");
            canvas.enableVertexSelectionMode(false);
            canvas.setSelectionModeForKruskal(false);
            canvas.highlightVertex(vertex);
        }
    }

    /**
     * Reset tất cả lựa chọn
     */
    private void resetSelection() {
        selectedStartVertex = null;
        selectedVertexForKruskal = null;
        kruskalModeAll = true;
        canvas.clearHighlights();
        canvas.enableVertexSelectionMode(false);
        canvas.setSelectionModeForPrim(false);
        canvas.setSelectionModeForKruskal(false);

        if (algoToggleGroup != null) {
            algoToggleGroup.selectToggle(null);
        }
        updateAlgorithmButtonsStyle();
    }

    /**
     * Cập nhật style cho các nút thuật toán
     * Chưa chọn: nền trắng chữ đen
     * Đã chọn: nền xanh lá chữ trắng
     */
    private void updateAlgorithmButtonsStyle() {
        String normalStyle = "-fx-background-color: white; -fx-text-fill: black; -fx-font-weight: bold; -fx-background-radius: 5 5 0 0; -fx-cursor: hand; -fx-border-color: #ccc; -fx-border-width: 1;";
        String selectedStyle = "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 5 5 0 0; -fx-cursor: hand; -fx-border-color: #45a049; -fx-border-width: 1;";

        if (kruskalMainBtn.isSelected()) {
            kruskalMainBtn.setStyle(selectedStyle);
            primMainBtn.setStyle(normalStyle);
        } else if (primMainBtn.isSelected()) {
            primMainBtn.setStyle(selectedStyle);
            kruskalMainBtn.setStyle(normalStyle);
        } else {
            kruskalMainBtn.setStyle(normalStyle);
            primMainBtn.setStyle(normalStyle);
        }
    }

    /**
     * Bật/tắt các nút thuật toán
     */
    private void enableAlgorithmButtons(boolean enable) {
        kruskalMainBtn.setDisable(!enable);
        kruskalSelectComponentBtn.setDisable(!enable);
        primMainBtn.setDisable(!enable);
        primSelectStartBtn.setDisable(!enable);

        if (!enable) {
            if (algoToggleGroup != null) {
                algoToggleGroup.selectToggle(null);
            }
            updateAlgorithmButtonsStyle();
        }

        updateTheme();
    }

    /**
     * Tạo panel hướng dẫn bên phải
     */
    private void createGuidePanel() {
        guidePanel = new VBox();
        guidePanel.setPadding(new Insets(15));
        guidePanel.setSpacing(10);
        guidePanel.setPrefWidth(300);
        guidePanel.setMaxWidth(300);
        guidePanel.setStyle(
                "-fx-background-color: #3a3a3a; " +
                        "-fx-background-radius: 10; " +
                        "-fx-border-radius: 10; " +
                        "-fx-border-color: #4CAF50; " +
                        "-fx-border-width: 2;"
        );
        guidePanel.setVisible(false);

        Label title = new Label("📘 HƯỚNG DẪN SỬ DỤNG");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        title.setTextFill(Color.WHITE);
        title.setAlignment(Pos.CENTER);
        title.setMaxWidth(Double.MAX_VALUE);

        TextArea guideContent = new TextArea();
        guideContent.setEditable(false);
        guideContent.setWrapText(true);
        guideContent.setFont(Font.font("Arial", 11));
        guideContent.setPrefHeight(700);
        guideContent.setStyle(
                "-fx-control-inner-background: #2a2a2a; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 8;"
        );

        String guideText =
                "📌 CÁCH NHẬP ĐỒ THỊ\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "• '1 2' → cung 1-2 (w=0)\n" +
                        "• '1 2 5' → cung 1-2 (w=5)\n" +
                        "• '3' → thêm đỉnh 3\n\n" +

                        "📌 QUY TẮC NHẬP\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "• Mỗi dòng: 1-3 số\n" +
                        "• Dòng 4+ số → bị bỏ qua\n\n" +

                        "📌 THUẬT TOÁN KRUSKAL\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "1. Chọn '🌐 Chọn miền' (tự động chọn Kruskal)\n" +
                        "2. Chọn 'Toàn bộ' hoặc 'Chọn miền'\n" +
                        "3. Nếu chọn miền, click vào đỉnh\n" +
                        "4. Nhấn '🌲 Cây khung tối tiểu'\n\n" +

                        "📌 THUẬT TOÁN PRIM\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "1. Chọn '🎯 Chọn đỉnh' (tự động chọn Prim)\n" +
                        "2. Click vào đỉnh bắt đầu\n" +
                        "3. Nhấn '🌲 Cây khung tối tiểu'\n\n" +

                        "📌 CHỨC NĂNG KHÁC\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "📊 Số miền liên thông\n" +
                        "🖼️ Lưu hình ảnh\n" +
                        "💾 Lưu đồ thị (file .txt)\n" +
                        "📂 Đọc đồ thị (file .txt)\n" +
                        "⌨️ Bàn phím ảo\n\n" +

                        "📌 TƯƠNG TÁC\n" +
                        "━━━━━━━━━━━━━━━━━━━━━━\n" +
                        "• Kéo thả đỉnh\n" +
                        "• Kéo thả trọng số\n" +
                        "• Click phải → Thêm đỉnh\n" +
                        "• Chế độ tối → Đổi màu giao diện";

        guideContent.setText(guideText);

        Button closeBtn = new Button("✕ ẨN HƯỚNG DẪN");
        closeBtn.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        closeBtn.setStyle(
                "-fx-background-color: #ff4444; " +
                        "-fx-text-fill: white; " +
                        "-fx-background-radius: 20; " +
                        "-fx-cursor: hand;"
        );
        closeBtn.setMaxWidth(Double.MAX_VALUE);
        closeBtn.setOnAction(e -> toggleGuidePanel());

        guidePanel.getChildren().addAll(title, guideContent, closeBtn);
    }

    private void toggleGuidePanel() {
        guideVisible = !guideVisible;
        guidePanel.setVisible(guideVisible);
        guideToggle.setSelected(guideVisible);
        showStatus(guideVisible ? "Đã mở hướng dẫn" : "Đã đóng hướng dẫn", "info");
    }

    private void updateGuideTheme() {
        if (guidePanel != null) {
            String bgColor = isDark ? "#2d2d2d" : "#3a3a3a";
            String contentBg = isDark ? "#3d3d3d" : "#4a4a4a";

            guidePanel.setStyle(
                    "-fx-background-color: " + bgColor + "; " +
                            "-fx-background-radius: 10; " +
                            "-fx-border-radius: 10; " +
                            "-fx-border-color: #4CAF50; " +
                            "-fx-border-width: 2;"
            );

            for (javafx.scene.Node node : guidePanel.getChildren()) {
                if (node instanceof TextArea) {
                    ((TextArea) node).setStyle(
                            "-fx-control-inner-background: " + contentBg + "; " +
                                    "-fx-text-fill: white; " +
                                    "-fx-background-radius: 8;"
                    );
                }
            }
        }
    }

    private void validateInput(String text) {
        errorLines.clear();
        String[] lines = text.split("\n");
        StringBuilder errorDetails = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;

            String[] parts = line.split("\\s+");
            if (parts.length >= 4) {
                errorLines.add(i + 1);
                errorDetails.append("Dòng ").append(i + 1).append(": \"").append(line).append("\" → ").append(parts.length).append(" số\n");
            }

            for (String part : parts) {
                if (!part.matches("-?\\d+")) {
                    errorLines.add(i + 1);
                    errorDetails.append("Dòng ").append(i + 1).append(": \"").append(line).append("\" → chứa ký tự không hợp lệ\n");
                    break;
                }
            }

        }

        if (!errorLines.isEmpty()) {
            errorLabel.setText(errorDetails.toString());
            errorPanel.setVisible(true);
            showStatus("Có " + errorLines.size() + " dòng lỗi (4+ số), đã bỏ qua khỏi đồ thị", "error");
        } else {
            errorPanel.setVisible(false);
        }
    }

    private String getValidLinesOnly(String text) {
        String[] lines = text.split("\n");
        StringBuilder validLines = new StringBuilder();

        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) continue;

            String[] parts = trimmedLine.split("\\s+");
            if (parts.length >= 1 && parts.length <= 3) {
                if (validLines.length() > 0) {
                    validLines.append("\n");
                }
                validLines.append(trimmedLine);
            }
        }

        return validLines.toString();
    }

    private void loadApplicationIcon(Stage stage) {
        try {
            File iconFile = new File("BackEnd/src/img/app-icon.png");
            if (iconFile.exists()) {
                Image icon = new Image(new FileInputStream(iconFile));
                if (!icon.isError()) {
                    stage.getIcons().add(icon);
                    System.out.println("✓ Đã load icon thành công!");
                    return;
                }
            }

            javafx.scene.canvas.Canvas canvas = new javafx.scene.canvas.Canvas(64, 64);
            javafx.scene.canvas.GraphicsContext gc = canvas.getGraphicsContext2D();
            gc.setFill(javafx.scene.paint.Color.web("#2196F3"));
            gc.fillRoundRect(0, 0, 64, 64, 10, 10);
            gc.setFill(javafx.scene.paint.Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font("Arial", 36));
            gc.fillText("G", 20, 48);
            stage.getIcons().add(canvas.snapshot(null, null));
            System.out.println("✓ Đã tạo icon fallback!");

        } catch (Exception e) {
            System.out.println("Lỗi load icon: " + e.getMessage());
        }
    }

    private void saveGraphToFile(Stage stage) {
        if (graph.getVertices().isEmpty()) {
            showStatus("Không có đồ thị để lưu! Hãy nhập đồ thị trước.", "error");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lưu đồ thị");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        fileChooser.setInitialFileName("graph_" + System.currentTimeMillis() + ".txt");

        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                // Chỉ lưu các cung
                for (Edge e : graph.getOriginalEdges()) {
                    StringBuilder line = new StringBuilder();
                    line.append(e.getFrom().getId()).append(" ").append(e.getTo().getId());
                    if (e.getWeight() > 0) {
                        line.append(" ").append(e.getWeight());
                    }
                    writer.write(line.toString());
                    writer.newLine();
                }

                showStatus("Đã lưu đồ thị thành công: " + file.getName(), "success");
            } catch (IOException e) {
                showStatus("Lỗi khi lưu file: " + e.getMessage(), "error");
                e.printStackTrace();
            }
        }
    }

    private void loadGraphFromFile(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Đọc đồ thị");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Text Files", "*.txt"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );

        File file = fileChooser.showOpenDialog(stage);
        if (file != null) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                StringBuilder content = new StringBuilder();
                String line;
                boolean readingVertices = false;
                boolean readingEdges = false;

                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;

                    if (readingVertices) {
                        String[] parts = line.split("\\s+");
                        if (parts.length == 3) {
                            int id = Integer.parseInt(parts[0]);
                            double x = Double.parseDouble(parts[1]);
                            double y = Double.parseDouble(parts[2]);
                            // Cập nhật vị trí sau khi parse
                            graph.addVertex(id, x, y);
                        }
                    } else if (readingEdges) {
                        if (content.length() > 0) content.append("\n");
                        content.append(line);
                    } else {
                        // Fallback cho file cũ không có header
                        if (content.length() > 0) content.append("\n");
                        content.append(line);
                    }
                }

                inputArea.setText(content.toString());
                showStatus("Đã đọc đồ thị thành công từ: " + file.getName(), "success");
            } catch (IOException e) {
                showStatus("Lỗi khi đọc file: " + e.getMessage(), "error");
                e.printStackTrace();
            }
        }
    }

    private void saveGraphAsImage(Stage stage) {
        if (graph.getVertices().isEmpty()) {
            showStatus("Không có đồ thị để lưu! Hãy nhập đồ thị trước.", "error");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Lưu hình ảnh đồ thị");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG Image", "*.png"),
                new FileChooser.ExtensionFilter("JPG Image", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        fileChooser.setInitialFileName("graph_image_" + System.currentTimeMillis() + ".png");

        File file = fileChooser.showSaveDialog(stage);
        if (file != null) {
            try {
                WritableImage writableImage = canvas.snapshot(new SnapshotParameters(), null);

                BufferedImage bufferedImage = new BufferedImage(
                        (int) writableImage.getWidth(),
                        (int) writableImage.getHeight(),
                        BufferedImage.TYPE_INT_ARGB
                );

                javafx.scene.image.PixelReader pixelReader = writableImage.getPixelReader();
                for (int y = 0; y < writableImage.getHeight(); y++) {
                    for (int x = 0; x < writableImage.getWidth(); x++) {
                        javafx.scene.paint.Color color = pixelReader.getColor(x, y);
                        int argb = (int) (color.getOpacity() * 255) << 24 |
                                (int) (color.getRed() * 255) << 16 |
                                (int) (color.getGreen() * 255) << 8 |
                                (int) (color.getBlue() * 255);
                        bufferedImage.setRGB(x, y, argb);
                    }
                }

                String fileName = file.getName().toLowerCase();
                String format = "png";
                if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                    format = "jpg";
                }

                ImageIO.write(bufferedImage, format, file);

                showStatus("Đã lưu hình ảnh thành công: " + file.getName(), "success");
            } catch (Exception ex) {
                showStatus("Lỗi khi lưu ảnh: " + ex.getMessage(), "error");
                ex.printStackTrace();
            }
        }
    }

    private void showStatus(String message, String type) {
        statusLabel.setText(message);
        switch (type) {
            case "success":
                statusLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                break;
            case "error":
                statusLabel.setStyle("-fx-text-fill: #ff4444; -fx-font-weight: bold;");
                break;
            case "warning":
                statusLabel.setStyle("-fx-text-fill: #ff8800; -fx-font-weight: bold;");
                break;
            case "info":
                statusLabel.setStyle("-fx-text-fill: #2196F3; -fx-font-weight: bold;");
                break;
            default:
                statusLabel.setStyle("-fx-text-fill: #666666;");
        }
    }

    private void updateTheme() {
        String bg = isDark ? "#1e1e1e" : "white";
        String fg = isDark ? "white" : "black";
        String borderColor = isDark ? "#444" : "lightgray";
        String inputBg = isDark ? "#3d3d3d" : "white";
        String inputPromptFg = isDark ? "#888888" : "#999999";
        String buttonBg = isDark ? "#3d3d3d" : "white";
        String buttonFg = isDark ? "white" : "black";
        String disabledBg = isDark ? "#2d2d2d" : "#f0f0f0";
        String disabledFg = isDark ? "#666666" : "#999999";
        String statusBg = isDark ? "#2d2d2d" : "#f0f0f0";
        String errorBg = isDark ? "#442222" : "#ffeeee";
        String errorBorder = isDark ? "#884444" : "#ff8888";

        root.setStyle("-fx-background-color: " + bg + ";");
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + fg + ";");
        left.setStyle("-fx-border-color: " + borderColor + "; -fx-border-width: 0 1 0 0; -fx-background-color: " + bg + ";");
        inputLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + fg + ";");
        inputArea.setStyle("-fx-text-fill: " + fg + "; -fx-control-inner-background: " + inputBg + "; -fx-prompt-text-fill: " + inputPromptFg + ";");
        errorPanel.setStyle("-fx-background-color: " + errorBg + "; -fx-border-color: " + errorBorder + "; -fx-border-radius: 3;");
        statusBar.setStyle("-fx-background-color: " + statusBg + "; -fx-border-color: " + borderColor + "; -fx-border-width: 1 0 0 0;");

        // Style cho nút toggle (Vô hướng/Có hướng)
        String toggleNormalStyle = "-fx-background-color: " + buttonBg + "; -fx-text-fill: " + buttonFg + "; -fx-border-color: " + (isDark ? "#666" : "#ccc") + "; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 5 10 5 10; -fx-cursor: hand;";
        String toggleSelectedStyle = "-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-border-color: #45a049; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5; -fx-padding: 5 10 5 10; -fx-cursor: hand;";

        if (undir.isSelected()) undir.setStyle(toggleSelectedStyle);
        else undir.setStyle(toggleNormalStyle);

        if (dir.isSelected()) dir.setStyle(toggleSelectedStyle);
        else dir.setStyle(toggleNormalStyle);

        saveImageBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-background-radius: 3; -fx-cursor: hand;");
        saveGraphBtn.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white; -fx-background-radius: 3; -fx-cursor: hand;");
        loadGraphBtn.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white; -fx-background-radius: 3; -fx-cursor: hand;");

        virtualKeyboard.setStyle("-fx-background-color: " + (isDark ? "#3d3d3d" : "#f0f0f0") + "; -fx-border-radius: 5;");
        bottom.setStyle("-fx-border-color: " + borderColor + "; -fx-border-width: 0 0 0 0; -fx-background-color: " + bg + ";");
        canvas.setDarkMode(isDark);
        darkBtn.setText(isDark ? "☀️ Chế độ sáng" : "🌙 Chế độ tối");

        for (javafx.scene.Node node : virtualKeyboard.getChildren()) {
            if (node instanceof Button) {
                Button btn = (Button) node;
                String currentStyle = btn.getStyle();
                if (!currentStyle.contains("#ff4444") && !currentStyle.contains("#4CAF50") && !currentStyle.contains("#9C27B0")) {
                    btn.setStyle("-fx-background-color: " + (isDark ? "#4d4d4d" : "#f0f0f0") +
                            "; -fx-text-fill: " + fg + "; " +
                            "-fx-border-color: " + (isDark ? "#666" : "#ccc") + "; " +
                            "-fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5; " +
                            "-fx-cursor: hand;");
                }
            }
        }

        updateAlgorithmButtonsStyle();
        updateGuideTheme();
    }

    private GridPane createVirtualKeyboard() {
        GridPane gp = new GridPane();
        gp.setHgap(5);
        gp.setVgap(5);
        gp.setAlignment(Pos.CENTER);

        String[] keys = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "0", "Space", "Delete", "Enter"};
        int col = 0, row = 0;

        for (String k : keys) {
            Button b = new Button(k.equals("Space") ? "␣" : k);
            b.setPrefSize(60, 40);

            if (k.equals("Delete")) {
                b.setStyle("-fx-background-color: #ff4444; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
            } else if (k.equals("Enter")) {
                b.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-background-radius: 5; -fx-cursor: hand;");
            } else {
                b.setStyle("-fx-background-color: #f0f0f0; -fx-text-fill: black; -fx-border-color: #ccc; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5; -fx-cursor: hand;");
            }

            b.setOnAction(ev -> {
                int caretPos = inputArea.getCaretPosition();
                String currentText = inputArea.getText();
                if (k.equals("Delete")) {
                    if (caretPos > 0) {
                        inputArea.setText(currentText.substring(0, caretPos - 1) + currentText.substring(caretPos));
                        inputArea.positionCaret(caretPos - 1);
                    }
                } else if (k.equals("Enter")) {
                    inputArea.setText(currentText.substring(0, caretPos) + "\n" + currentText.substring(caretPos));
                    inputArea.positionCaret(caretPos + 1);
                } else if (k.equals("Space")) {
                    inputArea.setText(currentText.substring(0, caretPos) + " " + currentText.substring(caretPos));
                    inputArea.positionCaret(caretPos + 1);
                } else {
                    inputArea.setText(currentText.substring(0, caretPos) + k + currentText.substring(caretPos));
                    inputArea.positionCaret(caretPos + 1);
                }
                inputArea.requestFocus();
            });
            gp.add(b, col, row);
            col++;
            if (col > 4) { col = 0; row++; }
        }
        return gp;
    }

    private void saveToUndo() {
        if (!currentText.equals(inputArea.getText())) {
            if (!currentText.isEmpty()) {
                undoStack.push(currentText);
            }
            currentText = inputArea.getText();
            redoStack.clear();
            // Giới hạn số lượng undo
            while (undoStack.size() > 50) {
                undoStack.remove(0);
            }
        }
    }

    private void undo() {
        if (!undoStack.isEmpty()) {
            redoStack.push(currentText);
            currentText = undoStack.pop();
            inputArea.setText(currentText);
        } else {
            showStatus("Không còn thao tác để hoàn tác", "warning");
        }
    }

    private void redo() {
        if (!redoStack.isEmpty()) {
            undoStack.push(currentText);
            currentText = redoStack.pop();
            inputArea.setText(currentText);
        } else {
            showStatus("Không còn thao tác để làm lại", "warning");
        }
    }

    private boolean isUpdatingFromGraph = false;

    public static void main(String[] args) {
        launch(args);
    }
}