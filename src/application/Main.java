package application;

import application.models.*;
import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) {
        primaryStage = stage;
        stage.setTitle("EcoSim — Evolution Ecosystem Simulator");
        stage.setResizable(true);
        showDesigner();
        stage.show();
    }

    public static void showDesigner() {
        DesignerScreen screen = new DesignerScreen();
        primaryStage.setScene(screen.createScene());
        primaryStage.setWidth(700);
        primaryStage.setHeight(620);
        primaryStage.centerOnScreen();
    }

    public static void startSimulation(PlayerOrganism player) {
        SimulationScreen screen = new SimulationScreen(player);
        primaryStage.setScene(screen.createScene());
        primaryStage.setWidth(1100);
        primaryStage.setHeight(720);
        primaryStage.centerOnScreen();
        screen.start();
    }

    public static void showResults(SimulationResult result) {
        ResultsScreen screen = new ResultsScreen(result);
        primaryStage.setScene(screen.createScene());
        primaryStage.setWidth(750);
        primaryStage.setHeight(680);
        primaryStage.centerOnScreen();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
