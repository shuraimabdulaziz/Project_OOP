package application;

import application.models.*;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/**
 * Screen 1: Player designs their organism.
 * - Choose a name and diet (Herbivore / Carnivore)
 * - Allocate 15 trait points across 5 genes
 * - Cannot exceed budget — prevents overpowered organisms
 */
public class DesignerScreen {

    private static final int    TOTAL_POINTS   = 15;
    private static final int    STEPS_PER_GENE = 5;   // 0–5 steps per trait
    private static final String BG_COLOR       = "#0a0f0a";
    private static final String PANEL_COLOR    = "#0f1f0f";
    private static final String ACCENT_GREEN   = "#39ff14";
    private static final String ACCENT_RED     = "#ff4444";
    private static final String TEXT_COLOR     = "#c8e6c9";
    private static final String DIM_COLOR      = "#4a7a4a";

    // Gene step arrays — each step maps to actual gene value
    private static final double[] SPEED_VALS       = {0.5, 1.0, 1.8, 2.8, 3.8, 5.0};
    private static final double[] PERCEPTION_VALS  = {1.0, 2.0, 3.5, 5.0, 6.5, 8.0};
    private static final double[] METABOLISM_VALS  = {0.5, 0.8, 1.2, 1.7, 2.3, 3.0};  // Lower=better
    private static final double[] CAMOUFLAGE_VALS  = {0.0, 0.2, 0.4, 0.6, 0.8, 1.0};
    private static final double[] STRENGTH_VALS    = {5.0, 10.0, 17.0, 24.0, 32.0, 40.0};
    private static final double[] FERTILITY_VALS   = {0.05, 0.1, 0.2, 0.3, 0.4, 0.5};

    // Sliders
    private Slider speedSlider, perceptionSlider, metabolismSlider,
                   specialSlider, fertilitySlider;

    private Label pointsLabel;
    private Label specialLabel;
    private Label warningLabel;
    private ToggleGroup dietGroup;
    private TextField nameField;

    public Scene createScene() {
        VBox root = new VBox(18);
        root.setStyle("-fx-background-color: " + BG_COLOR + ";");
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.TOP_CENTER);

        // ── Title ──────────────────────────────────────────────────────────
        Label title = new Label("DESIGN YOUR ORGANISM");
        title.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 26px; " +
                "-fx-font-weight: bold; -fx-text-fill: " + ACCENT_GREEN + ";");

        Label subtitle = new Label("Allocate trait points wisely — survival of the fittest awaits.");
        subtitle.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; " +
                "-fx-text-fill: " + DIM_COLOR + ";");

        // ── Name row ───────────────────────────────────────────────────────
        HBox nameRow = new HBox(12);
        nameRow.setAlignment(Pos.CENTER);
        Label nameLabel = styledLabel("SPECIES NAME:");
        nameField = new TextField("MyOrganism");
        nameField.setStyle("-fx-background-color: #162816; -fx-text-fill: " + TEXT_COLOR +
                "; -fx-font-family: 'Courier New'; -fx-font-size: 13px; " +
                "-fx-border-color: " + DIM_COLOR + "; -fx-border-width: 1px;");
        nameField.setPrefWidth(220);
        nameRow.getChildren().addAll(nameLabel, nameField);

        // ── Diet toggle ────────────────────────────────────────────────────
        dietGroup = new ToggleGroup();
        RadioButton herbBtn  = dietRadio("🌿  HERBIVORE", PlayerOrganism.Diet.HERBIVORE, true);
        RadioButton carnBtn  = dietRadio("🐺  CARNIVORE", PlayerOrganism.Diet.CARNIVORE, false);

        HBox dietRow = new HBox(30);
        dietRow.setAlignment(Pos.CENTER);
        dietRow.getChildren().addAll(herbBtn, carnBtn);

        // ── Point budget display ───────────────────────────────────────────
        pointsLabel = new Label("TRAIT POINTS REMAINING:  " + TOTAL_POINTS);
        pointsLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 15px; " +
                "-fx-font-weight: bold; -fx-text-fill: " + ACCENT_GREEN + ";");

        warningLabel = new Label("");
        warningLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; " +
                "-fx-text-fill: " + ACCENT_RED + ";");

        // ── Trait sliders ──────────────────────────────────────────────────
        specialLabel = new Label("CAMOUFLAGE  (dodge chance)");

        speedSlider      = buildSlider();
        perceptionSlider = buildSlider();
        metabolismSlider = buildSlider();
        specialSlider    = buildSlider();
        fertilitySlider  = buildSlider();

        // Sync all sliders to enforce budget
        for (Slider s : new Slider[]{speedSlider, perceptionSlider,
                metabolismSlider, specialSlider, fertilitySlider}) {
            s.valueProperty().addListener((obs, oldV, newV) -> {
                // Snap to int
                s.setValue(Math.round(newV.doubleValue()));
                enforcePointBudget(s, (int) Math.round(oldV.doubleValue()));
                updatePointsLabel();
            });
        }

        // When diet changes, relabel special gene
        dietGroup.selectedToggleProperty().addListener((obs, oldT, newT) -> {
            if (newT != null) {
                boolean isHerb = newT.getUserData() == PlayerOrganism.Diet.HERBIVORE;
                specialLabel.setText(isHerb
                        ? "CAMOUFLAGE  (dodge chance)"
                        : "STRENGTH       (attack power)");
            }
        });

        VBox sliders = new VBox(10);
        sliders.setStyle("-fx-background-color: " + PANEL_COLOR +
                "; -fx-border-color: " + DIM_COLOR +
                "; -fx-border-width: 1; -fx-padding: 18;");
        sliders.setMaxWidth(580);
        sliders.setAlignment(Pos.CENTER_LEFT);

        sliders.getChildren().addAll(
                traitRow("SPEED",         "Movement per tick",   SPEED_VALS,      speedSlider),
                traitRow("PERCEPTION",    "Sight radius",        PERCEPTION_VALS, perceptionSlider),
                traitRow("METABOLISM",    "Energy cost per tick (lower steps = more efficient)",
                                                                 METABOLISM_VALS, metabolismSlider),
                traitRow(null,            null,                  null,            null),  // spacer
                specialLabel,
                buildSliderRow(specialSlider),
                traitRow("FERTILITY",     "Reproduction chance", FERTILITY_VALS,  fertilitySlider)
        );
        // Remove null spacer
        sliders.getChildren().removeIf(n -> n == null);

        // Rebuild cleanly
        sliders.getChildren().clear();
        sliders.getChildren().addAll(
                traitRow("SPEED",       "Movement per tick",       SPEED_VALS,      speedSlider),
                sep(),
                traitRow("PERCEPTION",  "Sight radius",            PERCEPTION_VALS, perceptionSlider),
                sep(),
                traitRow("METABOLISM",  "Energy efficiency (less = cheaper)", METABOLISM_VALS, metabolismSlider),
                sep(),
                buildSpecialRow(),
                sep(),
                traitRow("FERTILITY",   "Reproduction chance",     FERTILITY_VALS,  fertilitySlider)
        );

        // ── Start button ───────────────────────────────────────────────────
        Button startBtn = new Button("RELEASE INTO ECOSYSTEM  ▶");
        startBtn.setStyle("-fx-background-color: " + ACCENT_GREEN +
                "; -fx-text-fill: #000000; -fx-font-family: 'Courier New'; " +
                "-fx-font-size: 14px; -fx-font-weight: bold; " +
                "-fx-padding: 10 30 10 30; -fx-cursor: hand;");
        startBtn.setOnMouseEntered(e -> startBtn.setStyle(startBtn.getStyle()
                .replace(ACCENT_GREEN, "#7fff50")));
        startBtn.setOnMouseExited(e  -> startBtn.setStyle(startBtn.getStyle()
                .replace("#7fff50", ACCENT_GREEN)));
        startBtn.setOnAction(e -> launchSimulation());

        root.getChildren().addAll(
                title, subtitle,
                separator(),
                nameRow, dietRow,
                separator(),
                pointsLabel, warningLabel,
                sliders,
                startBtn
        );

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: " + BG_COLOR +
                "; -fx-background-color: " + BG_COLOR + ";");

        return new Scene(scroll, 700, 660);
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private VBox buildSpecialRow() {
        VBox box = new VBox(4);
        dietGroup.selectedToggleProperty().addListener((obs, o, n) -> {
            if (n != null) {
                boolean isHerb = n.getUserData() == PlayerOrganism.Diet.HERBIVORE;
                specialLabel.setText(isHerb ? "CAMOUFLAGE  (dodge chance)" : "STRENGTH  (attack power)");
            }
        });
        specialLabel.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; " +
                "-fx-text-fill: " + ACCENT_GREEN + "; -fx-font-weight: bold;");
        box.getChildren().addAll(specialLabel, buildSliderRow(specialSlider));
        return box;
    }

    private VBox traitRow(String name, String hint, double[] vals, Slider slider) {
        VBox box = new VBox(3);
        HBox header = new HBox(10);
        Label n = new Label(name != null ? name : "");
        n.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; " +
                "-fx-text-fill: " + ACCENT_GREEN + "; -fx-font-weight: bold;");
        Label h = new Label(hint != null ? "— " + hint : "");
        h.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 11px; " +
                "-fx-text-fill: " + DIM_COLOR + ";");
        header.getChildren().addAll(n, h);
        box.getChildren().add(header);
        if (slider != null) box.getChildren().add(buildSliderRow(slider));
        return box;
    }

    private HBox buildSliderRow(Slider slider) {
        Label val = new Label("0 pts");
        val.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; " +
                "-fx-text-fill: " + TEXT_COLOR + "; -fx-min-width: 45;");
        slider.valueProperty().addListener((obs, o, n) ->
                val.setText((int) Math.round(n.doubleValue()) + " pts"));
        HBox row = new HBox(10, slider, val);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Slider buildSlider() {
        Slider s = new Slider(0, STEPS_PER_GENE, 0);
        s.setMajorTickUnit(1);
        s.setMinorTickCount(0);
        s.setSnapToTicks(true);
        s.setPrefWidth(400);
        s.setStyle("-fx-control-inner-background: #162816;");
        return s;
    }

    private RadioButton dietRadio(String text, PlayerOrganism.Diet diet, boolean selected) {
        RadioButton rb = new RadioButton(text);
        rb.setToggleGroup(dietGroup);
        rb.setUserData(diet);
        rb.setSelected(selected);
        rb.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 13px; " +
                "-fx-text-fill: " + TEXT_COLOR + ";");
        return rb;
    }

    private Label styledLabel(String text) {
        Label l = new Label(text);
        l.setStyle("-fx-font-family: 'Courier New'; -fx-font-size: 12px; " +
                "-fx-text-fill: " + TEXT_COLOR + ";");
        return l;
    }

    private Region separator() {
        Region r = new Region();
        r.setPrefHeight(1);
        r.setMaxWidth(580);
        r.setStyle("-fx-background-color: " + DIM_COLOR + ";");
        return r;
    }

    private Region sep() {
        Region r = new Region();
        r.setPrefHeight(6);
        return r;
    }

    private int usedPoints() {
        return (int)(speedSlider.getValue() + perceptionSlider.getValue() +
                metabolismSlider.getValue() + specialSlider.getValue() +
                fertilitySlider.getValue());
    }

    private void enforcePointBudget(Slider changed, int oldStep) {
        if (usedPoints() > TOTAL_POINTS) {
            changed.setValue(oldStep);   // Revert
            warningLabel.setText("⚠  Point budget exceeded! Max " + TOTAL_POINTS + " points.");
        } else {
            warningLabel.setText("");
        }
    }

    private void updatePointsLabel() {
        int remaining = TOTAL_POINTS - usedPoints();
        pointsLabel.setText("TRAIT POINTS REMAINING:  " + remaining);
        String color = remaining <= 3 ? ACCENT_RED : ACCENT_GREEN;
        pointsLabel.setStyle(pointsLabel.getStyle()
                .replaceAll("-fx-text-fill: [^;]+;", "-fx-text-fill: " + color + ";"));
    }

    private void launchSimulation() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) name = "Unknown";

        Toggle dietToggle = dietGroup.getSelectedToggle();
        PlayerOrganism.Diet diet = (dietToggle != null)
                ? (PlayerOrganism.Diet) dietToggle.getUserData()
                : PlayerOrganism.Diet.HERBIVORE;

        double speed      = SPEED_VALS      [(int) speedSlider.getValue()];
        double perception = PERCEPTION_VALS [(int) perceptionSlider.getValue()];
        double metabolism = METABOLISM_VALS [(int) metabolismSlider.getValue()];
        double special    = (diet == PlayerOrganism.Diet.HERBIVORE)
                            ? CAMOUFLAGE_VALS[(int) specialSlider.getValue()]
                            : STRENGTH_VALS  [(int) specialSlider.getValue()];
        double fertility  = FERTILITY_VALS  [(int) fertilitySlider.getValue()];

        PlayerOrganism player = new PlayerOrganism(
                name, diet, speed, perception, metabolism, special, fertility, 25, 25);

        Main.startSimulation(player);
    }
}