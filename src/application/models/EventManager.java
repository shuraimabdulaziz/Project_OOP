package application.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

/**
 * Manages random event scheduling and application.
 * Fires an event every eventIntervalTicks ticks (±randomness).
 * Notifies a listener (the GUI) when an event fires.
 */
public class EventManager {

    private final int    eventIntervalTicks; // Average ticks between events
    private final double eventChancePerTick; // Probability per tick
    private final Random random;

    private Consumer<RandomEvent> onEventFired; // GUI callback
    private final List<RandomEvent> eventHistory;

    private int ticksSinceLastEvent;
    private int minTicksBetweenEvents; // Prevents two events back-to-back

    public EventManager(int eventIntervalTicks) {
        this.eventIntervalTicks   = eventIntervalTicks;
        this.eventChancePerTick   = 1.0 / eventIntervalTicks;
        this.minTicksBetweenEvents = eventIntervalTicks / 2;
        this.random               = new Random();
        this.eventHistory         = new ArrayList<>();
        this.ticksSinceLastEvent  = 0;
    }

    /**
     * Called every tick. May trigger an event on the environment.
     * @return The triggered event, or null if nothing happened this tick.
     */
    public RandomEvent tick(Environment env) {
        ticksSinceLastEvent++;

        if (ticksSinceLastEvent < minTicksBetweenEvents) return null;
        if (random.nextDouble() > eventChancePerTick)    return null;

        RandomEvent event = pickEvent();
        applyEvent(event, env);
        eventHistory.add(event);
        ticksSinceLastEvent = 0;

        if (onEventFired != null) onEventFired.accept(event);
        return event;
    }

    private RandomEvent pickEvent() {
        RandomEvent[] values = RandomEvent.values();
        return values[random.nextInt(values.length)];
    }

    private void applyEvent(RandomEvent event, Environment env) {
        boolean[][] food = env.getFoodGrid();
        int w = env.getWidth(), h = env.getHeight();

        switch (event.effectType) {

            case FOOD_REDUCTION:
                for (int x = 0; x < w; x++)
                    for (int y = 0; y < h; y++)
                        if (food[x][y] && random.nextDouble() < 0.80) food[x][y] = false;
                break;

            case FOOD_INCREASE:
                for (int x = 0; x < w; x++)
                    for (int y = 0; y < h; y++)
                        if (!food[x][y] && random.nextDouble() < 0.60) food[x][y] = true;
                break;

            case DAMAGE_ALL:
                for (Organism o : env.getOrganisms())
                    if (o.isAlive()) o.takeDamage(15 + random.nextDouble() * 20);
                break;

            case ENERGY_DRAIN:
                for (Organism o : env.getOrganisms())
                    if (o.isAlive()) o.consumeEnergy(20 + random.nextDouble() * 15);
                break;

            case SPAWN_CARNIVORES:
                for (int i = 0; i < 5; i++)
                    env.addOrganism(new Carnivore(random.nextInt(w), random.nextInt(h), 1));
                break;

            case SPAWN_HERBIVORES:
                for (int i = 0; i < 8; i++)
                    env.addOrganism(new Herbivore(random.nextInt(w), random.nextInt(h), 1));
                break;
        }
    }

    public void setOnEventFired(Consumer<RandomEvent> listener) {
        this.onEventFired = listener;
    }

    public List<RandomEvent> getEventHistory() {
        return eventHistory;
    }
}