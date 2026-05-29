package application.models;

/**
 * All possible random events that can occur during the simulation.
 * Each event has a display name, description, and its effect type.
 */
public enum RandomEvent {

    DROUGHT      ("🌵 Drought",          "Food supply devastated — 80% of plants wither.",   EffectType.FOOD_REDUCTION),
    DISEASE      ("☣  Disease Outbreak", "A pathogen spreads — all organisms take damage.",  EffectType.DAMAGE_ALL),
    FOOD_BOOM    ("🌿 Food Boom",         "Perfect conditions — plants flourish everywhere.", EffectType.FOOD_INCREASE),
    PREDATOR_SURGE("🐺 Predator Surge",  "New carnivores migrate into the ecosystem.",       EffectType.SPAWN_CARNIVORES),
    COLD_SNAP    ("❄  Cold Snap",         "Bitter cold drains energy from all organisms.",   EffectType.ENERGY_DRAIN),
    HERBIVORE_BOOM("🐇 Herbivore Surge", "A wave of herbivores enters the ecosystem.",       EffectType.SPAWN_HERBIVORES);

    public enum EffectType {
        FOOD_REDUCTION,
        DAMAGE_ALL,
        FOOD_INCREASE,
        SPAWN_CARNIVORES,
        ENERGY_DRAIN,
        SPAWN_HERBIVORES
    }

    public final String     displayName;
    public final String     description;
    public final EffectType effectType;

    RandomEvent(String displayName, String description, EffectType effectType) {
        this.displayName = displayName;
        this.description = description;
        this.effectType  = effectType;
    }
}
