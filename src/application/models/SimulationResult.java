package application.models;

import java.util.List;
import java.util.Map;

/**
 * Snapshot of simulation outcome, passed to ResultsScreen.
 */
public class SimulationResult {

    public final int    totalTicks;
    public final boolean playerSurvived;
    public final int    playerSurvivalTicks;
    public final String playerName;
    public final PlayerOrganism.Diet playerDiet;

    // Gene values at time of death / end
    public final double finalSpeed;
    public final double finalPerception;
    public final double finalMetabolism;
    public final double finalSpecial;    // camouflage or strength
    public final double finalFertility;

    public final double avgSpeedAtEnd;
    public final double avgStrengthAtEnd;
    public final int    avgGenerationAtEnd;
    
    public final int peakHerbivores;
    public final int peakCarnivores;
    public final int finalHerbivores;
    public final int finalCarnivores;

    public final List<RandomEvent> events;

    public SimulationResult(PlayerOrganism player, int totalTicks,
                            int peakH, int peakC, int finalH, int finalC,
                            List<RandomEvent> events, Environment env) {
        this.totalTicks          = totalTicks;
        this.playerSurvived      = player.isAlive();
        this.playerSurvivalTicks = player.getSurvivalTicks();
        this.playerName          = player.getPlayerName();
        this.playerDiet          = player.getDietType();

        this.finalSpeed       = player.getGeneValue("speed");
        this.finalPerception  = player.getGeneValue("perception");
        this.finalMetabolism  = player.getGeneValue("metabolism");
        this.finalSpecial     = playerDiet == PlayerOrganism.Diet.HERBIVORE
                                    ? player.getGeneValue("camouflage")
                                    : player.getGeneValue("strength");
        this.finalFertility   = player.getGeneValue("fertility");
        
        this.avgSpeedAtEnd      = env != null ? env.getOrganisms().stream()
        	    .filter(o -> o.isAlive()).mapToDouble(o -> o.getGeneValue("speed")).average().orElse(0) : 0;
        	this.avgStrengthAtEnd   = env != null ? env.getOrganisms().stream()
        	    .filter(o -> o.isAlive() && o.getGeneValue("strength") > 0)
        	    .mapToDouble(o -> o.getGeneValue("strength")).average().orElse(0) : 0;
        	this.avgGenerationAtEnd = env != null ? (int) env.getOrganisms().stream()
        	    .filter(o -> o.isAlive()).mapToDouble(Organism::getGeneration).average().orElse(0) : 0;

        this.peakHerbivores  = peakH;
        this.peakCarnivores  = peakC;
        this.finalHerbivores = finalH;
        this.finalCarnivores = finalC;
        this.events          = events;
    }
}