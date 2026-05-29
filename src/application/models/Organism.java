package application.models;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public abstract class Organism {

    private final String id;
    private final String species;
    private int generation;

    protected double health;
    protected double maxHealth;
    protected double energy;
    protected double maxEnergy;
    protected int age;
    protected int maxAge;
    protected boolean alive;

    protected int x;
    protected int y;

    private final Map<String, Gene> genes;

    protected Organism(String species, int x, int y, int generation) {
        this.id = UUID.randomUUID().toString().substring(0, 8);
        this.species = species;
        this.x = x;
        this.y = y;
        this.generation = generation;
        this.age = 0;
        this.alive = true;
        this.genes = new HashMap<>();

        this.maxHealth = 100.0;
        this.health = maxHealth;
        this.maxEnergy = 100.0;
        this.energy = maxEnergy;
        this.maxAge = 200;

        initGenes();
    }

    protected abstract void initGenes();
    public abstract void act(Environment env);
    public abstract Organism reproduce(MutationEngine mutationEngine);
    public abstract String getDiet();

    public void tick() {
        age++;
        energy -= getGeneValue("metabolism");
        if (energy <= 0 || age >= maxAge || health <= 0) {
            die();
        }
    }

    public void takeDamage(double amount) {
        health = Math.max(0, health - amount);
        if (health <= 0) die();
    }

    public void heal(double amount) {
        health = Math.min(maxHealth, health + amount);
    }

    public void consumeEnergy(double amount) {
        energy = Math.max(0, energy - amount);
    }

    public void gainEnergy(double amount) {
        energy = Math.min(maxEnergy, energy + amount);
    }

    public void die() {
        alive = false;
    }

    public boolean canReproduce() {
        return alive && age >= 20 && energy >= maxEnergy * 0.6 && health >= maxHealth * 0.5;
    }

    protected void addGene(Gene gene) {
        genes.put(gene.getName(), gene);
    }

    public Gene getGene(String name) {
        return genes.get(name);
    }

    public double getGeneValue(String name) {
        Gene g = genes.get(name);
        return (g != null) ? g.getValue() : 0.0;
    }

    public void setGene(String name, Gene gene) {
        genes.put(name, gene);
    }

    public Set<String> getGeneNames() {
        return genes.keySet();
    }

    public String getId() { return id; }
    public String getSpecies() { return species; }
    public int getGeneration() { return generation; }
    public double getHealth() { return health; }
    public double getEnergy() { return energy; }
    public int getAge() { return age; }
    public boolean isAlive() { return alive; }
    public int getX() { return x; }
    public int getY() { return y; }

    public void setPosition(int x, int y) { this.x = x; this.y = y; }

    @Override
    public String toString() {
        return String.format("[%s #%s] pos=(%d,%d) hp=%.1f en=%.1f age=%d gen=%d alive=%b",
                species, id, x, y, health, energy, age, generation, alive);
    }
}