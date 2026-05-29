package application.models;

import java.util.List;
import java.util.Random;

public class Herbivore extends Organism {

    private static final Random random = new Random();

    public Herbivore(int x, int y, int generation) {
        super("Herbivore", x, y, generation);
    }

    @Override
    protected void initGenes() {
        addGene(new Gene("speed", 1.5, 0.5, 5.0));
        addGene(new Gene("perception", 3.0, 1.0, 8.0));
        addGene(new Gene("metabolism", 1.0, 0.5, 3.0));
        addGene(new Gene("camouflage", 0.3, 0.0, 1.0));
        addGene(new Gene("fertility", 0.2, 0.05, 0.5));

        maxHealth = 80 + getGeneValue("camouflage") * 20;
        maxEnergy = 120.0;
        maxAge = 180;
        health = maxHealth;
        energy = maxEnergy;
    }

    @Override
    public void act(Environment env) {
        if (!isAlive()) return;

        tick();
        if (!isAlive()) return;

        Organism nearestPredator = env.findNearestOfType(this, Carnivore.class, (int) getGeneValue("perception"));

        if (nearestPredator != null) {
            fleeFrom(nearestPredator, env);
            return;
        }

        if (env.hasFoodAt(x, y)) {
            env.consumeFoodAt(x, y);
            gainEnergy(30.0);
            heal(5.0);
        } else {
            int[] foodTile = env.findNearestFood(x, y, (int) getGeneValue("perception"));
            if (foodTile != null) {
                moveToward(foodTile[0], foodTile[1], env);
            } else {
                moveRandom(env);
            }
        }

        if (canReproduce() && random.nextDouble() < getGeneValue("fertility") && env.countHerbivores() < 700) {
            Organism offspring = reproduce(env.getMutationEngine());
            env.addOrganism(offspring);
            consumeEnergy(30.0);
        }
    }

    @Override
    public Organism reproduce(MutationEngine mutationEngine) {
        Herbivore child = new Herbivore(x, y, getGeneration() + 1);
        for (String name : getGeneNames()) {
            child.setGene(name, new Gene(getGene(name)));
        }
        mutationEngine.applyMutation(child);
        child.maxHealth = 80 + child.getGeneValue("camouflage") * 20;
        child.health = child.maxHealth;
        return child;
    }

    @Override
    public String getDiet() {
        return "plants";
    }

    private void fleeFrom(Organism threat, Environment env) {
        int dx = Integer.signum(x - threat.getX());
        int dy = Integer.signum(y - threat.getY());
        int steps = (int) getGeneValue("speed");

        for (int i = 0; i < steps; i++) {
            int nx = x + dx;
            int ny = y + dy;
            if (env.isValidPosition(nx, ny)) {
                x = nx;
                y = ny;
            }
        }
        consumeEnergy(getGeneValue("speed") * 0.5);
    }

    private void moveToward(int tx, int ty, Environment env) {
        int dx = Integer.signum(tx - x);
        int dy = Integer.signum(ty - y);
        int nx = x + dx;
        int ny = y + dy;
        if (env.isValidPosition(nx, ny)) {
            x = nx;
            y = ny;
        }
        consumeEnergy(getGeneValue("metabolism") * 0.3);
    }

    private void moveRandom(Environment env) {
        int[] dirs = {-1, 0, 1};
        int nx = x + dirs[random.nextInt(3)];
        int ny = y + dirs[random.nextInt(3)];
        if (env.isValidPosition(nx, ny)) {
            x = nx;
            y = ny;
        }
        consumeEnergy(getGeneValue("metabolism") * 0.2);
    }
}
