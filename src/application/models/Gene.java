package application.models;

public class Gene {

    private String name;
    private double value;
    private double minValue;
    private double maxValue;

    public Gene(String name, double value, double minValue, double maxValue) {
        this.name    = name;
        this.value   = clamp(value, minValue, maxValue);
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    public Gene(Gene other) {
        this.name     = other.name;
        this.value    = other.value;
        this.minValue = other.minValue;
        this.maxValue = other.maxValue;
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    public String getName()  { return name; }
    public double getValue() { return value; }
    public double getMin()   { return minValue; }
    public double getMax()   { return maxValue; }

    public void setValue(double value) {
        this.value = clamp(value, minValue, maxValue);
    }

    public String toString() {
        return String.format("Gene[%s=%.2f (%.1f–%.1f)]", name, value, minValue, maxValue);
    }
}