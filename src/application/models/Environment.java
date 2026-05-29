package application.models;

import java.util.*;

/**
 * Represents the simulation world — a 2D grid of tiles.
 * Updated to support EventManager and PlayerOrganism tracking.
 */
public class Environment {

    private final int         width;
    private final int         height;
    private final boolean[][]  foodGrid;

    private final List<Organism> organisms;
    private final List<Organism> toAdd;
    private       PlayerOrganism playerOrganism;

    private final MutationEngine mutationEngine;
    private       EventManager   eventManager;
    private final Random         random;

    private final double foodRegrowRate;
    private       int    tick;

    public Environment(int width, int height, double mutationRate,
                       double mutationStrength, double foodRegrowRate) {
        this.width          = width;
        this.height         = height;
        this.foodRegrowRate = foodRegrowRate;
        this.foodGrid       = new boolean[width][height];
        this.organisms      = new ArrayList<>();
        this.toAdd          = new ArrayList<>();
        this.mutationEngine = new MutationEngine(mutationRate, mutationStrength);
        this.random         = new Random();
        this.tick           = 0;
        seedFood(0.5);
    }

    /** Advance one tick. Returns any event that fired, or null. */
    public RandomEvent step() {
        tick++;
        for (Organism o : organisms)
            if (o.isAlive()) o.act(this);
        organisms.addAll(toAdd);
        toAdd.clear();
        organisms.removeIf(o -> !o.isAlive());
        regrowFood();
        return (eventManager != null) ? eventManager.tick(this) : null;
    }

    // ── Spatial queries ────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public <T extends Organism> Organism findNearestOfType(
            Organism origin, Class<T> type, int radius) {

        Organism nearest = null;
        double   minDist = Double.MAX_VALUE;

        for (Organism o : organisms) {
            if (!o.isAlive() || o == origin) continue;
            boolean isHerbType = type == Herbivore.class &&
                    (o instanceof Herbivore ||
                     (o instanceof PlayerOrganism &&
                      ((PlayerOrganism) o).getDietType() == PlayerOrganism.Diet.HERBIVORE));
            boolean isCarnType = type == Carnivore.class &&
                    (o instanceof Carnivore ||
                     (o instanceof PlayerOrganism &&
                      ((PlayerOrganism) o).getDietType() == PlayerOrganism.Diet.CARNIVORE));
            if (!isHerbType && !isCarnType && !type.isInstance(o)) continue;
            double d = dist(origin.getX(), origin.getY(), o.getX(), o.getY());
            if (d <= radius && d < minDist) { minDist = d; nearest = o; }
        }
        return nearest;
    }

    public int[] findNearestFood(int fromX, int fromY, int radius) {
        int[]  best = null;
        double min  = Double.MAX_VALUE;
        for (int fx = Math.max(0, fromX-radius); fx < Math.min(width, fromX+radius); fx++)
            for (int fy = Math.max(0, fromY-radius); fy < Math.min(height, fromY+radius); fy++)
                if (foodGrid[fx][fy]) {
                    double d = dist(fromX, fromY, fx, fy);
                    if (d < min) { min = d; best = new int[]{fx, fy}; }
                }
        return best;
    }

    // ── Food ───────────────────────────────────────────────────────────────

    public boolean hasFoodAt(int x, int y)  { return isValidPosition(x,y) && foodGrid[x][y]; }
    public void consumeFoodAt(int x, int y) { if (isValidPosition(x,y)) foodGrid[x][y] = false; }

    private void regrowFood() {
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                if (!foodGrid[x][y] && random.nextDouble() < foodRegrowRate) foodGrid[x][y] = true;
    }

    private void seedFood(double density) {
        for (int x = 0; x < width; x++)
            for (int y = 0; y < height; y++)
                foodGrid[x][y] = random.nextDouble() < density;
    }

    // ── Organism management ────────────────────────────────────────────────

    public void addOrganism(Organism o)        { toAdd.add(o); }
    public void addPlayerOrganism(PlayerOrganism p) {
        playerOrganism = p;
        toAdd.add(p);
        // Seed 4 additional clones so the player starts with a colony of 5
        for (int i = 1; i < 50; i++) {
            int nx = Math.min(width - 1, Math.max(0, p.getX() + random.nextInt(7) - 3));
            int ny = Math.min(height - 1, Math.max(0, p.getY() + random.nextInt(7) - 3));
            PlayerOrganism clone = new PlayerOrganism(
                p.getPlayerName(), p.getDietType(),
                p.getGeneValue("speed"), p.getGeneValue("perception"),
                p.getGeneValue("metabolism"),
                p.getDietType() == PlayerOrganism.Diet.HERBIVORE
                    ? p.getGeneValue("camouflage") : p.getGeneValue("strength"),
                p.getGeneValue("fertility"), nx, ny);
            toAdd.add(clone);
        }
    }
    
    public void populate(int herbivores, int carnivores) {
        for (int i = 0; i < herbivores; i++)
            organisms.add(new Herbivore(random.nextInt(width), random.nextInt(height), 1));
        for (int i = 0; i < carnivores; i++)
            organisms.add(new Carnivore(random.nextInt(width), random.nextInt(height), 1));
    }

    // ── Utilities ──────────────────────────────────────────────────────────

    public boolean isValidPosition(int x, int y) { return x>=0 && x<width && y>=0 && y<height; }

    private double dist(int x1, int y1, int x2, int y2) {
        int dx = x1-x2, dy = y1-y2;
        return Math.sqrt(dx*dx + dy*dy);
    }

    // ── Getters ────────────────────────────────────────────────────────────

    public int            getWidth()          { return width; }
    public int            getHeight()         { return height; }
    public int            getTick()           { return tick; }
    public MutationEngine getMutationEngine() { return mutationEngine; }
    public boolean[][]    getFoodGrid()       { return foodGrid; }
    public PlayerOrganism getPlayerOrganism() { return playerOrganism; }
    public void           setEventManager(EventManager em) { this.eventManager = em; }

    public List<Organism> getOrganisms() { return Collections.unmodifiableList(organisms); }

    public long countHerbivores() {
        return organisms.stream().filter(o ->
            o instanceof Herbivore ||
            (o instanceof PlayerOrganism &&
             ((PlayerOrganism)o).getDietType() == PlayerOrganism.Diet.HERBIVORE)).count();
    }

    public long countCarnivores() {
        return organisms.stream().filter(o ->
            o instanceof Carnivore ||
            (o instanceof PlayerOrganism &&
             ((PlayerOrganism)o).getDietType() == PlayerOrganism.Diet.CARNIVORE)).count();
    }

    public long countOf(Class<? extends Organism> type) {
        return organisms.stream().filter(type::isInstance).count();
    }
}