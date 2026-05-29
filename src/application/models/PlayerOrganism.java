package application.models;

import java.util.Random;

/**
 * The player's custom organism.
 * Diet (Herbivore / Carnivore) and genes are set by the user in DesignerScreen.
 * Inherits all Organism logic; overrides initGenes() to use player-chosen values.
 */
public class PlayerOrganism extends Organism {

    public enum Diet { HERBIVORE, CARNIVORE }

    private final Diet   diet;
    private final String playerName;
    private       int    survivalTicks;   // How long the player organism stayed alive

    private static final Random random = new Random();
    private              int    attackCooldown = 0;

    /**
     * @param name        Player-chosen name for the organism
     * @param diet        HERBIVORE or CARNIVORE
     * @param speed       Gene value (0.5–5.0)
     * @param perception  Gene value (1.0–8.0)
     * @param metabolism  Gene value (0.5–3.0)
     * @param special     Camouflage if herbivore (0–1), Strength if carnivore (5–40)
     * @param fertility   Gene value (0.05–0.5)
     */
    public PlayerOrganism(String name, Diet diet,
                          double speed, double perception, double metabolism,
                          double special, double fertility,
                          int startX, int startY) {

        super("Player (" + name + ")", startX, startY, 1);
        this.diet         = diet;
        this.playerName   = name;
        this.survivalTicks = 0;

        // Override genes with player-chosen values
        setGene("speed",       new Gene("speed",       speed,       0.5, 5.0));
        setGene("perception",  new Gene("perception",  perception,  1.0, 8.0));
        setGene("metabolism",  new Gene("metabolism",  metabolism,  0.5, 3.0));
        setGene("fertility",   new Gene("fertility",   fertility,   0.05, 0.5));

        if (diet == Diet.HERBIVORE) {
            setGene("camouflage", new Gene("camouflage", special, 0.0, 1.0));
            maxHealth  = 80 + special * 20;
            maxEnergy  = 120.0;
            maxAge     = 200;
        } else {
            setGene("strength", new Gene("strength", special, 5.0, 40.0));
            maxHealth  = 120 + special;
            maxEnergy  = 150.0;
            maxAge     = 160;
        }
        health = maxHealth;
        energy = maxEnergy;
    }

    // ── Abstract implementations ───────────────────────────────────────────

    @Override
    protected void initGenes() {
        // Defaults — overwritten by constructor immediately after super()
        addGene(new Gene("speed",       1.5, 0.5, 5.0));
        addGene(new Gene("perception",  3.0, 1.0, 8.0));
        addGene(new Gene("metabolism",  1.0, 0.5, 3.0));
        addGene(new Gene("camouflage",  0.3, 0.0, 1.0));
        addGene(new Gene("strength",    15.0, 5.0, 40.0));
        addGene(new Gene("fertility",   0.2, 0.05, 0.5));
    }

    @Override
    public void act(Environment env) {
        if (!isAlive()) return;
        tick();
        if (!isAlive()) return;

        survivalTicks++;
        if (attackCooldown > 0) attackCooldown--;

        if (diet == Diet.HERBIVORE) actAsHerbivore(env);
        else                        actAsCarnivore(env);

        if (canReproduce() && random.nextDouble() < getGeneValue("fertility") && env.countOf(PlayerOrganism.class) < 400) {
            Organism child = reproduce(env.getMutationEngine());
            env.addOrganism(child);
            consumeEnergy(30.0);
        }
    }

    @Override
    public Organism reproduce(MutationEngine mutationEngine) {
        PlayerOrganism child = new PlayerOrganism(
            playerName, diet,
            getGeneValue("speed"), getGeneValue("perception"),
            getGeneValue("metabolism"),
            diet == Diet.HERBIVORE ? getGeneValue("camouflage") : getGeneValue("strength"),
            getGeneValue("fertility"),
            x, y
        );
        mutationEngine.applyMutation(child);
        return child;
    }

    @Override
    public String getDiet() {
        return diet == Diet.HERBIVORE ? "plants" : "herbivores";
    }

    // ── Behaviour ──────────────────────────────────────────────────────────

    private void actAsHerbivore(Environment env) {
        Organism threat = env.findNearestOfType(this, Carnivore.class,
                (int) getGeneValue("perception"));
        if (threat != null) { fleeFrom(threat, env); return; }

        if (env.hasFoodAt(x, y)) {
            env.consumeFoodAt(x, y);
            gainEnergy(30.0);
            heal(5.0);
        } else {
            int[] food = env.findNearestFood(x, y, (int) getGeneValue("perception"));
            if (food != null) moveToward(food[0], food[1], env);
            else              moveRandom(env);
        }
    }

    private void actAsCarnivore(Environment env) {
        Organism prey = env.findNearestOfType(this, Herbivore.class,
                (int) getGeneValue("perception"));
        if (prey != null) {
            if (distance(prey) <= 1.5 && attackCooldown == 0) attack(prey);
            else                                               moveToward(prey.getX(), prey.getY(), env);
        } else {
            moveRandom(env);
        }
    }

    private void attack(Organism prey) {
        double dodge = prey.getGeneValue("camouflage");
        if (random.nextDouble() < dodge) return;
        double dmg = getGeneValue("strength") * (0.8 + random.nextDouble() * 0.4);
        prey.takeDamage(dmg);
        if (!prey.isAlive()) { gainEnergy(50); heal(10); }
        attackCooldown = 3;
    }

    private void fleeFrom(Organism threat, Environment env) {
        int dx = Integer.signum(x - threat.getX());
        int dy = Integer.signum(y - threat.getY());
        for (int i = 0; i < (int) getGeneValue("speed"); i++) {
            if (env.isValidPosition(x + dx, y + dy)) { x += dx; y += dy; }
        }
        consumeEnergy(getGeneValue("speed") * 0.5);
    }

    private void moveToward(int tx, int ty, Environment env) {
        int dx = Integer.signum(tx - x), dy = Integer.signum(ty - y);
        if (env.isValidPosition(x + dx, y + dy)) { x += dx; y += dy; }
        consumeEnergy(getGeneValue("metabolism") * 0.3);
    }

    private void moveRandom(Environment env) {
        int[] d = {-1, 0, 1};
        int nx = x + d[random.nextInt(3)], ny = y + d[random.nextInt(3)];
        if (env.isValidPosition(nx, ny)) { x = nx; y = ny; }
        consumeEnergy(getGeneValue("metabolism") * 0.2);
    }

    private double distance(Organism o) {
        int dx = x - o.getX(), dy = y - o.getY();
        return Math.sqrt(dx * dx + dy * dy);
    }

    // ── Getters ────────────────────────────────────────────────────────────

    public Diet   getDietType()      { return diet; }
    public String getPlayerName()    { return playerName; }
    public int    getSurvivalTicks() { return survivalTicks; }
}