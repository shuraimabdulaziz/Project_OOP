package application.models;
import java.util.Random;

public class MutationEngine {

    private double mutationRate;
    private double mutationStrength;
    private final Random random;

    public MutationEngine(double mutationRate, double mutationStrength) {
        this.mutationRate = mutationRate;
        this.mutationStrength = mutationStrength;
        this.random = new Random();
    }

    public Gene mutate(Gene parentGene) {
        Gene offspring = new Gene(parentGene);

        if (random.nextDouble() < mutationRate) {
            double range = parentGene.getMax() - parentGene.getMin();
            double delta = (random.nextDouble() * 2 - 1) * mutationStrength * range;
            offspring.setValue(offspring.getValue() + delta);
        }

        return offspring;
    }

    public void applyMutation(Organism organism) {
        for (String geneName : organism.getGeneNames()) {
            Gene original = organism.getGene(geneName);
            Gene mutated = mutate(original);
            organism.setGene(geneName, mutated);
        }
    }

    public double getMutationRate() { return mutationRate; }
    public double getMutationStrength() { return mutationStrength; }

    public void setMutationRate(double rate) { this.mutationRate = rate; }
    public void setMutationStrength(double strength) { this.mutationStrength = strength; }

    @Override
    public String toString() {
        return String.format("MutationEngine[rate=%.2f, strength=%.2f]",
                mutationRate, mutationStrength);
    }
}