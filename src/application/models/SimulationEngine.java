package application.models;

public class SimulationEngine {

    private final Environment env;
    private final int maxTicks;
    private final int reportInterval;
    private boolean running;

    private int peakHerbivores;
    private int peakCarnivores;

    public SimulationEngine(int gridWidth, int gridHeight,
                            int initHerbivores, int initCarnivores,
                            double mutationRate, double mutationStr,
                            double foodRegrowRate, int maxTicks, int reportInterval) {

        this.env = new Environment(gridWidth, gridHeight,
                                   mutationRate, mutationStr, foodRegrowRate);
        this.maxTicks = maxTicks;
        this.reportInterval = reportInterval;
        this.running = false;

        env.populate(initHerbivores, initCarnivores);
    }

    public void run() {
        running = true;
        System.out.println("=== Evolution Simulation Start ===");
        System.out.println(env);
        System.out.println();

        while (running) {
            env.step();

            int h = (int) env.countOf(Herbivore.class);
            int c = (int) env.countOf(Carnivore.class);
            if (h > peakHerbivores) peakHerbivores = h;
            if (c > peakCarnivores) peakCarnivores = c;

            if (env.getTick() % reportInterval == 0) {
                printStats();
            }

            if (h == 0 && c == 0) {
                System.out.println("\n⚠  Total extinction at tick " + env.getTick());
                running = false;
            } else if (h == 0) {
                System.out.println("\n⚠  Herbivores extinct at tick " + env.getTick()
                        + " — carnivores will follow.");
            }

            if (maxTicks > 0 && env.getTick() >= maxTicks) {
                running = false;
            }
        }

        printFinalSummary();
    }

    public void stop() { running = false; }

    private void printStats() {
        int h = (int) env.countOf(Herbivore.class);
        int c = (int) env.countOf(Carnivore.class);

        System.out.printf("Tick %4d │ Herbivores: %4d │ Carnivores: %3d%n",
                env.getTick(), h, c);
    }

    private void printFinalSummary() {
        System.out.println();
        System.out.println("=== Simulation Complete ===");
        System.out.printf("  Total ticks     : %d%n", env.getTick());
        System.out.printf("  Peak herbivores : %d%n", peakHerbivores);
        System.out.printf("  Peak carnivores : %d%n", peakCarnivores);
        System.out.printf("  Final herbivores: %d%n", (int) env.countOf(Herbivore.class));
        System.out.printf("  Final carnivores: %d%n", (int) env.countOf(Carnivore.class));
    }

    public static void main(String[] args) {
        SimulationEngine sim = new SimulationEngine(
                50, 50, 80, 10, 0.10, 0.05, 0.002, 1000, 50
        );

        sim.run();
    }
}