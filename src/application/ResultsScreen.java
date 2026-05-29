package application;

import application.models.*;

import javafx.scene.control.ScrollPane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.HashMap;
import java.util.Map;

/**
 * Screen 3: Results screen after simulation ends.
 *
 * Shows:
 *  - Survival outcome (survived / extinct)
 *  - Survival duration vs total ticks
 *  - Bar chart of player's final gene values
 *  - Peak populations
 *  - Events that occurred
 *  - Play Again button
 */
public class ResultsScreen {

    private static final String BG     = "#0a0f0a";
    private static final String PANEL  = "#0f1f0f";
    private static final String GREEN  = "#39ff14";
    private static final String RED    = "#ff4444";
    private static final String CYAN   = "#00d4ff";
    private static final String GOLD   = "#ffd700";
    private static final String DIM    = "#2a4a2a";
    private static final String TEXT   = "#c8e6c9";

    private final SimulationResult result;

    public ResultsScreen(SimulationResult result) {
        this.result = result;
    }

    public Scene createScene() {
        VBox root = new VBox(16);
        root.setStyle("-fx-background-color: " + BG + ";");
        root.setPadding(new Insets(28, 36, 28, 36));
        root.setAlignment(Pos.TOP_CENTER);

        // ── Title ──────────────────────────────────────────────────────────
        boolean survived = result.playerSurvived;
        String outcomeText = survived ? "✦  SURVIVED  ✦" : "☠  EXTINCT  ☠";
        String outcomeColor = survived ? GOLD : RED;

        Label outcomeLabel = new Label(outcomeText);
        outcomeLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 28px; " +
                "-fx-font-weight: bold; -fx-text-fill: " + outcomeColor + ";");

        Label nameLabel = new Label(result.playerName.toUpperCase() + "  ·  " +
                (result.playerDiet == PlayerOrganism.Diet.HERBIVORE ? "HERBIVORE" : "CARNIVORE"));
        nameLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 13px; " +
                "-fx-text-fill: " + CYAN + ";");

        // ── Survival bar ───────────────────────────────────────────────────
        VBox survivalBox = new VBox(6);
        survivalBox.setMaxWidth(560);
        survivalBox.setAlignment(Pos.CENTER_LEFT);

        double survivalPct = result.totalTicks > 0
                ? (double) result.playerSurvivalTicks / result.totalTicks : 0;

        Label survivalTxt = new Label(String.format(
                "SURVIVAL:  %d / %d ticks  (%.0f%%)",
                result.playerSurvivalTicks, result.totalTicks, survivalPct * 100));
        survivalTxt.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; " +
                "-fx-text-fill: " + TEXT + ";");

        StackPane barBg = new StackPane();
        barBg.setMaxWidth(560);
        barBg.setPrefHeight(14);
        barBg.setStyle("-fx-background-color: #162816; -fx-border-color: " + DIM +
                "; -fx-border-width: 1;");

        Rectangle fill = new Rectangle();
        fill.setHeight(14);
        fill.setWidth(Math.max(2, 560 * survivalPct));
        fill.setFill(Color.web(survived ? GOLD : RED));
        fill.setArcWidth(4);
        fill.setArcHeight(4);
        StackPane.setAlignment(fill, Pos.CENTER_LEFT);

        barBg.getChildren().add(fill);
        survivalBox.getChildren().addAll(survivalTxt, barBg);

        // ── Two-column stats ───────────────────────────────────────────────
        HBox statsRow = new HBox(30);
        statsRow.setMaxWidth(620);

        VBox ecoStats = styledPanel("ECOSYSTEM STATS");
        ecoStats.getChildren().addAll(
                statLine("Total ticks",       String.valueOf(result.totalTicks)),
                statLine("Peak herbivores",   String.valueOf(result.peakHerbivores)),
                statLine("Peak carnivores",   String.valueOf(result.peakCarnivores)),
                statLine("Final herbivores",  String.valueOf(result.finalHerbivores)),
                statLine("Final carnivores",  String.valueOf(result.finalCarnivores)),
                statLine("Events triggered",  String.valueOf(result.events.size()))
        );

        VBox traitStats = styledPanel("YOUR FINAL TRAITS");
        String specialLabel = result.playerDiet == PlayerOrganism.Diet.HERBIVORE
                ? "Camouflage" : "Strength";
        traitStats.getChildren().addAll(
                statLine("Speed",       format(result.finalSpeed)),
                statLine("Perception",  format(result.finalPerception)),
                statLine("Metabolism",  format(result.finalMetabolism)),
                statLine(specialLabel,  format(result.finalSpecial)),
                statLine("Fertility",   format(result.finalFertility))
        );

        statsRow.getChildren().addAll(ecoStats, traitStats);

        // ── Trait bar chart ────────────────────────────────────────────────
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis(0, 1, 0.25);
        xAxis.setStyle("-fx-tick-label-fill: " + TEXT + ";");
        yAxis.setLabel("Normalised Value");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle("Trait Profile at End of Simulation");
        barChart.setStyle("-fx-background-color: " + PANEL + "; -fx-plot-background-color: #0d1a0d;");
        barChart.setLegendVisible(false);
        barChart.setAnimated(false);
        barChart.setPrefHeight(180);
        barChart.setMaxWidth(560);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        // Normalise each gene to 0–1 range
        series.getData().add(new XYChart.Data<>("Speed",      norm(result.finalSpeed,       0.5, 5.0)));
        series.getData().add(new XYChart.Data<>("Perception", norm(result.finalPerception,  1.0, 8.0)));
        series.getData().add(new XYChart.Data<>("Metabolism", 1.0 - norm(result.finalMetabolism, 0.5, 3.0))); // Invert
        series.getData().add(new XYChart.Data<>(specialLabel, result.playerDiet == PlayerOrganism.Diet.HERBIVORE
                ? norm(result.finalSpecial, 0.0, 1.0)
                : norm(result.finalSpecial, 5.0, 40.0)));
        series.getData().add(new XYChart.Data<>("Fertility",  norm(result.finalFertility,  0.05, 0.5)));
        barChart.getData().add(series);
        
     // ── Evolution summary ──────────────────────────────────────────────
        VBox evoBox = styledPanel("EVOLUTION SUMMARY");
        String evoMsg = String.format(
            "By tick %d, the average generation reached %d.\n" +
            "Average speed across all survivors: %.2f  |  Average strength: %.2f\n" +
            "Compared to starting values, %s",
            result.totalTicks,
            result.avgGenerationAtEnd,
            result.avgSpeedAtEnd,
            result.avgStrengthAtEnd,
            result.avgGenerationAtEnd >= 5
                ? "significant evolution occurred — traits drifted under selection pressure."
                : "evolution was still early — more ticks would show stronger divergence."
        );
        Label evoLabel = new Label(evoMsg);
        evoLabel.setWrapText(true);
        evoLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; -fx-text-fill: " + TEXT + ";");
        evoBox.getChildren().add(evoLabel);

        // ── Events summary ─────────────────────────────────────────────────
        VBox eventsBox = new VBox(4);
        eventsBox.setMaxWidth(560);
        if (!result.events.isEmpty()) {
            Label evTitle = sectionLabel("EVENTS THAT OCCURRED");
            eventsBox.getChildren().add(evTitle);
            Map<String, Integer> counts = new HashMap<>();
            for (RandomEvent e : result.events)
                counts.merge(e.displayName, 1, Integer::sum);
            counts.forEach((name, count) -> {
                Label l = new Label("  " + name + "  ×" + count);
                l.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; " +
                        "-fx-text-fill: " + TEXT + ";");
                eventsBox.getChildren().add(l);
            });
        }

        // ── Buttons ────────────────────────────────────────────────────────
        Button playAgain = new Button("▶  DESIGN NEW ORGANISM");
        playAgain.setStyle("-fx-background-color: " + GREEN +
                "; -fx-text-fill: #000; -fx-font-family: 'Courier New'; " +
                "-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 9 26; -fx-cursor: hand;");
        playAgain.setOnAction(e -> Main.showDesigner());
        playAgain.setOnMouseEntered(e -> playAgain.setStyle(playAgain.getStyle()
                .replace(GREEN, "#7fff50")));
        playAgain.setOnMouseExited(e  -> playAgain.setStyle(playAgain.getStyle()
                .replace("#7fff50", GREEN)));

        root.getChildren().addAll(
                outcomeLabel, nameLabel,
                sep(),
                survivalBox,
                sep(),
                statsRow,
                barChart,
                eventsBox,
                evoBox,
                sep()
        );
        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);	
        scroll.setStyle("-fx-background-color: " + BG + "; -fx-background: " + BG + ";");

        BorderPane outer = new BorderPane();
        outer.setStyle("-fx-background-color: " + BG + ";");
        outer.setCenter(scroll);

        HBox btnBar = new HBox(playAgain);
        btnBar.setAlignment(Pos.CENTER);
        btnBar.setPadding(new Insets(12, 0, 16, 0));
        btnBar.setStyle("-fx-background-color: " + BG + "; -fx-border-color: " + DIM + "; -fx-border-width: 1 0 0 0;");
        outer.setBottom(btnBar);

        return new Scene(outer, 750, 680);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private VBox styledPanel(String title) {
        VBox box = new VBox(6);
        box.setStyle("-fx-background-color: " + PANEL +
                "; -fx-border-color: " + DIM + "; -fx-border-width: 1; -fx-padding: 12;");
        box.setPrefWidth(260);
        Label t = new Label(title);
        t.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; " +
                "-fx-font-weight: bold; -fx-text-fill: " + GREEN + ";");
        box.getChildren().add(t);
        return box;
    }

    private HBox statLine(String key, String val) {
        Label k = new Label(key);
        k.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; " +
                "-fx-text-fill: " + DIM.replace("2a4a2a", "7ab07a") + ";");
        Label v = new Label(val);
        v.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; " +
                "-fx-text-fill: " + TEXT + "; -fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return new HBox(k, spacer, v);
    }

    private Label sectionLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; " +
                "-fx-font-weight: bold; -fx-text-fill: " + GREEN + ";");
        return l;
    }

    private Region sep() {
        Region r = new Region();
        r.setPrefHeight(4);
        return r;
    }

    private String format(double v) { return String.format("%.2f", v); }

    private double norm(double v, double min, double max) {
        return Math.max(0, Math.min(1, (v - min) / (max - min)));
    }
}