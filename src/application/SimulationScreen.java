package application;

import application.models.*;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.stream.Collectors;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen 2: Live simulation view.
 *
 * Left  — Animated 50×50 grid canvas
 * Right — Live population LineChart + event log
 * Top   — Stats bar (tick, populations, player status)
 * Bottom — Pause / Stop buttons
 */
public class SimulationScreen {

    private static final int GRID_W    = 50;
    private static final int GRID_H    = 50;
    private static final int TILE_SIZE = 11;        // px per tile
    private static final int MAX_TICKS = 800;

    private static final String BG       = "#0a0f0a";
    private static final String PANEL    = "#0f1f0f";
    private static final String GREEN    = "#39ff14";
    private static final String RED      = "#ff4444";
    private static final String CYAN     = "#00d4ff";
    private static final String DIM      = "#2a4a2a";
    private static final String TEXT     = "#c8e6c9";

    private final PlayerOrganism   player;
    private       Environment      env;
    private       EventManager     eventManager;
    private       Timeline         timeline; 
    private       boolean          paused = false;

    // Chart series
    private XYChart.Series<Number,Number> herbSeries;
    private XYChart.Series<Number,Number> carnSeries;
    private XYChart.Series<Number,Number> playerSeries;
    private XYChart.Series<Number, Number> avgSpeedSeries, avgStrengthSeries;
    private LineChart<Number, Number> evoChart;
    
    // Stats labels
    private Label tickLabel, herbLabel, carnLabel, playerLabel;
    private TextArea eventLog;
    private Canvas   canvas;

    // For results
    private int peakH = 0, peakC = 0;
    private int playerCount = 0;

    public SimulationScreen(PlayerOrganism player) {
        this.player = player;
    }

    public Scene createScene() {
        // ── Environment setup ──────────────────────────────────────────────
        env = new Environment(GRID_W, GRID_H, 0.10, 0.05, 0.015);
        env.populate(60, 8);
        env.addPlayerOrganism(player);

        eventManager = new EventManager(80);
        eventManager.setOnEventFired(this::onEvent);
        env.setEventManager(eventManager);

        // ── Root layout ────────────────────────────────────────────────────
        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: " + BG + ";");
        root.setPadding(new Insets(10));

        root.setTop(buildStatsBar());
        root.setLeft(buildCanvas());
        root.setCenter(buildRightPanel());
        root.setBottom(buildButtonBar());

        return new Scene(root, 1100, 720);
    }

    // ── UI Builders ────────────────────────────────────────────────────────

    private HBox buildStatsBar() {
        tickLabel   = statLabel("TICK: 0");
        herbLabel   = statLabel("🌿 HERBIVORES: --");
        carnLabel   = statLabel("🐺 CARNIVORES: --");
        playerLabel = statLabel("👤 " + player.getPlayerName().toUpperCase() + ": ALIVE");
        playerLabel.setStyle(playerLabel.getStyle().replace(TEXT, CYAN));

        HBox bar = new HBox(30, tickLabel, herbLabel, carnLabel, playerLabel);
        bar.setStyle("-fx-background-color: " + PANEL +
                "; -fx-border-color: " + DIM + "; -fx-border-width: 0 0 1 0;");
        bar.setPadding(new Insets(8, 14, 8, 14));
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private StackPane buildCanvas() {
        canvas = new Canvas(GRID_W * TILE_SIZE, GRID_H * TILE_SIZE);
        StackPane wrap = new StackPane(canvas);
        wrap.setStyle("-fx-background-color: #050d05; -fx-border-color: " + DIM +
                "; -fx-border-width: 1;");
        wrap.setPadding(new Insets(4));
        wrap.setMargin(canvas, new Insets(0));
        return wrap;
    }

    private VBox buildRightPanel() {
        // Population chart
        NumberAxis xAxis = new NumberAxis(0, MAX_TICKS, 100);
        NumberAxis yAxis = new NumberAxis();
        yAxis.setAutoRanging(true);
        xAxis.setLabel("Tick");
        yAxis.setLabel("Population");
        xAxis.setStyle("-fx-tick-label-fill: " + TEXT + ";");
        yAxis.setStyle("-fx-tick-label-fill: " + TEXT + ";");

        LineChart<Number,Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Population Over Time");
        chart.setStyle("-fx-background-color: " + PANEL +
                "; -fx-plot-background-color: #0d1a0d;");
        chart.setCreateSymbols(false);
        chart.setAnimated(false);
        chart.setPrefHeight(320);
        chart.setPrefWidth(440);

        herbSeries   = new XYChart.Series<>(); herbSeries.setName("Herbivores");
        carnSeries   = new XYChart.Series<>(); carnSeries.setName("Carnivores");
        playerSeries = new XYChart.Series<>(); playerSeries.setName(player.getPlayerName() +" .Pop");
        chart.getData().addAll(herbSeries, carnSeries, playerSeries);

        // Style series colors via CSS workaround
        chart.getStylesheets().add("data:text/css," +
                ".default-color0.chart-series-line{-fx-stroke:#39ff14;}" +
                ".default-color0.chart-legend-symbol{-fx-background-color:#39ff14,white;}" +
                ".default-color1.chart-series-line{-fx-stroke:#ff4444;}" +
                ".default-color1.chart-legend-symbol{-fx-background-color:#ff4444,white;}" +
                ".default-color2.chart-series-line{-fx-stroke:#00d4ff;}" +
                ".default-color2.chart-legend-symbol{-fx-background-color:#00d4ff,white;}");

        // Event log
        Label logTitle = new Label("EVENT LOG");
        logTitle.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; " +
                "-fx-text-fill: " + DIM + "; -fx-font-weight: bold;");

        eventLog = new TextArea();
        eventLog.setEditable(false);
        eventLog.setStyle("-fx-control-inner-background: #0d1a0d; " +
                "-fx-text-fill: " + TEXT + "; -fx-font-family: 'Courier New'; " +
                "-fx-font-size: 11px; -fx-border-color: " + DIM + ";");
        eventLog.setPrefHeight(200);
        eventLog.setWrapText(true);

     // Evolution chart
        NumberAxis evoX = new NumberAxis(0, MAX_TICKS, 100);
        NumberAxis evoY = new NumberAxis();
        evoY.setAutoRanging(true);
        evoX.setLabel("Tick");
        evoY.setLabel("Gene Value");
        evoX.setStyle("-fx-tick-label-fill: " + TEXT + ";");
        evoY.setStyle("-fx-tick-label-fill: " + TEXT + ";");

        evoChart = new LineChart<>(evoX, evoY);
        evoChart.setTitle("Gene Evolution Over Time");
        evoChart.setStyle("-fx-background-color: " + PANEL + "; -fx-plot-background-color: #0d1a0d;");
        evoChart.setCreateSymbols(false);
        evoChart.setAnimated(false);
        evoChart.setPrefHeight(180);
        evoChart.setPrefWidth(440);

        avgSpeedSeries    = new XYChart.Series<>(); avgSpeedSeries.setName("Avg Speed");
        avgStrengthSeries = new XYChart.Series<>(); avgStrengthSeries.setName("Avg Strength");
        evoChart.getData().addAll(avgSpeedSeries, avgStrengthSeries);

        evoChart.getStylesheets().add("data:text/css," +
            ".default-color0.chart-series-line{-fx-stroke:#ffaa00;}" +
            ".default-color0.chart-legend-symbol{-fx-background-color:#ffaa00,white;}" +
            ".default-color1.chart-series-line{-fx-stroke:#ff66ff;}" +
            ".default-color1.chart-legend-symbol{-fx-background-color:#ff66ff,white;}");

        VBox panel = new VBox(10, chart, evoChart, logTitle, eventLog);
        panel.setPadding(new Insets(6, 6, 6, 12));
        return panel;
    }

    private HBox buildButtonBar() {
        Button pauseBtn = new Button("⏸  PAUSE");
        Button stopBtn  = new Button("⏹  END SIMULATION");

        String btnStyle = "-fx-background-color: " + PANEL +
                "; -fx-text-fill: " + TEXT +
                "; -fx-font-family: 'Courier New'; -fx-font-size: 12px;" +
                "-fx-border-color: " + DIM + "; -fx-border-width: 1; -fx-cursor: hand; -fx-padding: 7 18;";
        pauseBtn.setStyle(btnStyle);
        stopBtn.setStyle(btnStyle);

        pauseBtn.setOnAction(e -> {
            paused = !paused;
            if (paused) { timeline.pause(); pauseBtn.setText("▶  RESUME"); }
            else        { timeline.play();  pauseBtn.setText("⏸  PAUSE"); }
        });
        stopBtn.setOnAction(e -> endSimulation());

        Label legend = new Label("🟩 Herbivore   🟥 Carnivore   🔵 MyOrganism Pop   ·  Food");
        legend.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 10px; " +
                "-fx-text-fill: " + DIM + ";");

        HBox bar = new HBox(14, legend, new Region(), pauseBtn, stopBtn);
        HBox.setHgrow(bar.getChildren().get(1), Priority.ALWAYS);
        bar.setPadding(new Insets(8, 6, 4, 6));
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    // ── Simulation loop ────────────────────────────────────────────────────

    public void start() {
        timeline = new Timeline(new KeyFrame(Duration.millis(60), e -> tick()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void tick() {
        env.step();  // Returns event but we handle via callback

        int tick = env.getTick();
        int h    = (int) env.countHerbivores();
        int c    = (int) env.countCarnivores();
        playerCount = (int) env.countOf(PlayerOrganism.class);
        if (h > peakH) peakH = h;
        if (c > peakC) peakC = c;

        // Update chart every 5 ticks for performance
        if (tick % 5 == 0) {
            herbSeries.getData().add(new XYChart.Data<>(tick, h));
            carnSeries.getData().add(new XYChart.Data<>(tick, c));
            playerSeries.getData().add(new XYChart.Data<>(tick, playerCount));
            double avgSpd = env.getOrganisms().stream()
                    .filter(o -> o.isAlive())
                    .mapToDouble(o -> o.getGeneValue("speed"))
                    .average().orElse(0);
                double avgStr = env.getOrganisms().stream()
                    .filter(o -> o.isAlive())
                    .mapToDouble(o -> o.getGeneValue("strength"))
                    .average().orElse(0);
                avgSpeedSeries.getData().add(new XYChart.Data<>(tick, avgSpd));
                avgStrengthSeries.getData().add(new XYChart.Data<>(tick, avgStr));
        }

        // Update stats bar
        tickLabel.setText("TICK: " + tick);
        herbLabel.setText("🌿 HERBIVORES: " + h);
        carnLabel.setText("🐺 CARNIVORES: " + c);
        
        if (playerCount > 0) {
            playerLabel.setText("👤 " + player.getPlayerName().toUpperCase() +
                    ": " + playerCount + " ALIVE");
            playerLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; " +
                    "-fx-font-weight: bold; -fx-text-fill: " + CYAN + ";");
        } else {
            playerLabel.setText("☠  " + player.getPlayerName().toUpperCase() + ": EXTINCT");
            playerLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; " +
                    "-fx-font-weight: bold; -fx-text-fill: " + RED + ";");
        }
        // Draw grid
        drawGrid();

        // Check end conditions
        boolean totalExtinct = (h == 0 && c == 0);
        boolean maxReached   = (tick >= MAX_TICKS);

        if (totalExtinct || maxReached) {
            timeline.stop();
            Platform.runLater(this::endSimulation);
        }
    }

    // ── Grid rendering ─────────────────────────────────────────────────────

    private void drawGrid() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        boolean[][] food = env.getFoodGrid();

        // Background tiles
        for (int x = 0; x < GRID_W; x++) {
            for (int y = 0; y < GRID_H; y++) {
                gc.setFill(food[x][y]
                        ? Color.web("#122012")
                        : Color.web("#080f08"));
                gc.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }

        // Organisms
        for (Organism o : env.getOrganisms()) {
            if (!o.isAlive()) continue;
            double px = o.getX() * TILE_SIZE + 1;
            double py = o.getY() * TILE_SIZE + 1;
            double sz = TILE_SIZE - 2;

            if (o instanceof PlayerOrganism) {
                gc.setFill(Color.web(CYAN));
                gc.fillOval(px, py, sz, sz);
                // Bright glow outline
                gc.setStroke(Color.web("#ffffff"));
                gc.setLineWidth(1);
                gc.strokeOval(px, py, sz, sz);
            } else if (o instanceof Carnivore) {
                gc.setFill(Color.web(RED));
                gc.fillRect(px, py, sz, sz);
            } else {
                gc.setFill(Color.web(GREEN));
                gc.fillOval(px, py, sz * 0.85, sz * 0.85);
            }
        }
    }

    // ── Events ─────────────────────────────────────────────────────────────

    private void onEvent(RandomEvent event) {
        Platform.runLater(() -> {
            String line = "[T" + env.getTick() + "]  " +
                    event.displayName + " — " + event.description + "\n";
            eventLog.appendText(line);
        });
    }

    // ── End ────────────────────────────────────────────────────────────────

    private void endSimulation() {
        if (timeline != null) timeline.stop();
        SimulationResult result = new SimulationResult(
                player, env.getTick(),
                peakH, peakC,
                (int) env.countHerbivores(),
                (int) env.countCarnivores(),
                eventManager.getEventHistory(),
                env
        );
        Main.showResults(result);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private Label statLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; " +
                "-fx-font-weight: bold; -fx-text-fill: " + TEXT + ";");
        return l;
    }
}