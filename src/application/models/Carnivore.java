package application.models;

import java.util.Random;

public class Carnivore extends Organism {

    private static final Random random = new Random();
    private int attackCooldown = 0;

    public Carnivore(int x, int y, int generation) {
        super("Carnivore", x, y, generation);
    }

    @Override
    protected void initGenes() {
        addGene(new Gene("speed", 2.0, 0.5, 6.0));
        addGene(new Gene("perception", 5.0, 2.0, 10.0));
        addGene(new Gene("metabolism", 1.8, 0.8, 4.0));
        addGene(new Gene("strength", 15.0, 5.0, 40.0));
        addGene(new Gene("stealth", 0.2, 0.0, 1.0));
        addGene(new Gene("fertility", 0.1, 0.02, 0.3));

        maxHealth = 120 + getGeneValue("strength");
        maxEnergy = 150.0;
        maxAge = 150;
        health = maxHealth;
        energy = maxEnergy;
    }

    @Override
    public void act(Environment env) {
        if (!isAlive()) return;

        tick();
        if (!isAlive()) return;

        if (attackCooldown > 0) attackCooldown--;

        Organism prey = env.findNearestOfType(this, Herbivore.class, (int) getGeneValue("perception"));

        if (prey != null) {
            double dist = distance(prey);

            if (dist <= 1.5 && attackCooldown == 0) {
                attack(prey, env);
            } else {
                moveToward(prey.getX(), prey.getY(), env);
            }
        } else {
            moveRandom(env);
        }

        if (canReproduce() && random.nextDouble() < getGeneValue("fertility") && env.countCarnivores() < 500) {
            Organism offspring = reproduce(env.getMutationEngine());
            env.addOrganism(offspring);
            consumeEnergy(40.0);
        }
    }

    @Override
    public Organism reproduce(MutationEngine mutationEngine) {
        Carnivore child = new Carnivore(x, y, getGeneration() + 1);
        for (String name : getGeneNames()) {
            child.setGene(name, new Gene(getGene(name)));
        }
        mutationEngine.applyMutation(child);
        child.maxHealth = 120 + child.getGeneValue("strength");
        child.health = child.maxHealth;
        return child;
    }

    @Override
    public String getDiet() {
        return "herbivores";
    }

    private void attack(Organism prey, Environment env) {
        double dodgeChance = prey.getGeneValue("camouflage");
        if (random.nextDouble() < dodgeChance) {
            return;
        }

        double damage = getGeneValue("strength") * (0.8 + random.nextDouble() * 0.4);
        prey.takeDamage(damage);

        if (!prey.isAlive()) {
            gainEnergy(50 + getGeneValue("strength") * 0.5);
            heal(10.0);
        }

        attackCooldown = 3;
    }

    private void moveToward(int tx, int ty, Environment env) {
        int dx = Integer.signum(tx - x);
        int dy = Integer.signum(ty - y);
        int steps = (int) getGeneValue("speed");

        for (int i = 0; i < steps; i++) {
            int nx = x + dx;
            int ny = y + dy;
            if (env.isValidPosition(nx, ny)) {
                x = nx;
                y = ny;
            }
        }
        consumeEnergy(getGeneValue("metabolism") * 0.4);
    }

    private void moveRandom(Environment env) {
        int[] dirs = {-1, 0, 1};
        int nx = x + dirs[random.nextInt(3)];
        int ny = y + dirs[random.nextInt(3)];
        if (env.isValidPosition(nx, ny)) {
            x = nx;
            y = ny;
        }
        consumeEnergy(getGeneValue("metabolism") * 0.25);
    }

    private double distance(Organism other) {
        int dx = x - other.getX();
        int dy = y - other.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }
}