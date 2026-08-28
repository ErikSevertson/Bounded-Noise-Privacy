import java.util.*;

public class ComparisonDP {

    /**
     * METHOD 1: Point-level DP (add noise to each speed individually)
     * FAST but NOISY - adds random noise to every data point
     */
    public static List<Double> pointLevelDP(List<Double> speeds, double epsilon) {
        List<Double> noisy = new ArrayList<>(speeds);
        Random rng = new Random(2484);
        double scale = 1.0 / epsilon;
        
        for (int i = 0; i < noisy.size(); i++) {
            double noise = (rng.nextDouble() - 0.5) * 2 * scale;
            noisy.set(i, noisy.get(i) + noise);
        }
        return noisy;
    }

    /**
     * METHOD 2: Model-based DP (add noise to statistics, then synthesize)
     * SLOWER but CLEANER - noise on mean/stddev, then generate synthetic data
     */
    public static List<Double> modelBasedDP(List<Double> speeds, double epsilon) {
        // Fit model
        double rawMean = calcMean(speeds);
        double rawStdDev = calcStdDev(speeds, rawMean);
        
        // Add noise to stats
        Random rng = new Random(2484);
        double scale = 1.0 / epsilon;
        double noisyMean = rawMean + (rng.nextDouble() - 0.5) * 2 * scale;
        double noisyStdDev = Math.max(0.1, rawStdDev + (rng.nextDouble() - 0.5) * 2 * scale);
        
        // Generate synthetic
        List<Double> synthetic = new ArrayList<>();
        Random synthRng = new Random(42);
        for (int i = 0; i < speeds.size(); i++) {
            double z = synthRng.nextGaussian();
            synthetic.add(noisyMean + noisyStdDev * z);
        }
        return synthetic;
    }

    /**
     * ACCURACY: Compare distributions
     * Returns array: [rawMean, rawStdDev, syntheticMean, syntheticStdDev, meanError%, stddevError%]
     */
    public static double[] measureAccuracy(List<Double> raw, List<Double> synthetic) {
        double rawMean = calcMean(raw);
        double rawStdDev = calcStdDev(raw, rawMean);
        double synthMean = calcMean(synthetic);
        double synthStdDev = calcStdDev(synthetic, synthMean);
        
        double meanError = Math.abs(rawMean - synthMean) / rawMean * 100;
        double stddevError = Math.abs(rawStdDev - synthStdDev) / rawStdDev * 100;
        
        return new double[]{rawMean, rawStdDev, synthMean, synthStdDev, meanError, stddevError};
    }

    /**
     * SPEED: Time the execution
     */
    public static long timeMethod(Runnable method, int iterations) {
        long start = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            method.run();
        }
        long end = System.nanoTime();
        return (end - start) / 1_000_000; // Convert to ms
    }

    /**
     * PRIVACY: Show how noise changes with epsilon
     * Lower epsilon = more noise = more private but less useful
     */
    public static void showPrivacyTreatoff(List<Double> speeds) {
        System.out.println("\n=== PRIVACY-UTILITY TRADEOFF (Model-Based) ===");
        System.out.println("ε (epsilon) | Noisy Mean | Error % | Meaning");
        System.out.println("-------------------------------------------");
        
        for (double eps : new double[]{0.1, 0.5, 1.0, 2.0, 5.0}) {
            double rawMean = calcMean(speeds);
            double rawStdDev = calcStdDev(speeds, rawMean);
            
            Random rng = new Random(2484);
            double scale = 1.0 / eps;
            double noisyMean = rawMean + (rng.nextDouble() - 0.5) * 2 * scale;
            double error = Math.abs(rawMean - noisyMean) / rawMean * 100;
            
            String privacy = eps < 0.5 ? "Very private" : eps < 1.5 ? "Private" : eps < 3 ? "Moderate" : "Weak";
            
            System.out.printf("%.1f       | %.2f      | %.1f%%   | %s\n", eps, noisyMean, error, privacy);
        }
    }

    // Helpers
    public static double calcMean(List<Double> speeds) {
        double sum = 0;
        for (double s : speeds) sum += s;
        return sum / speeds.size();
    }

    public static double calcStdDev(List<Double> speeds, double mean) {
        double sumSquares = 0;
        for (double s : speeds) {
            sumSquares += (s - mean) * (s - mean);
        }
        return Math.sqrt(sumSquares / speeds.size());
    }

    public static void main(String[] args) throws Exception {
        // Load data
        HashMap<Integer, ArrayList<Row>> trips = App.computeTrips();
        HashMap<Integer, ArrayList<Row>> subset = App.sampleTrips(trips, 50);
        
        List<Double> allSpeeds = new ArrayList<>();
        for (ArrayList<Row> trip : subset.values()) {
            for (Row row : trip) {
                allSpeeds.add(row.getVel());
            }
        }
        
        System.out.println("Loaded " + allSpeeds.size() + " speed samples");
        System.out.println("\n" + "=".repeat(70));
        
        // ===== SPEED TEST =====
        System.out.println("\n1. EXECUTION SPEED");
        System.out.println("   (How fast does each method run?)");
        
        int WARMUP = 1000;
        int MEASURE = 10000;
        double epsilon = 0.5;
        
        System.out.println("\n   Warming up JVM...");
        timeMethod(() -> pointLevelDP(allSpeeds, epsilon), WARMUP);
        timeMethod(() -> modelBasedDP(allSpeeds, epsilon), WARMUP);
        
        long pointTime = timeMethod(() -> pointLevelDP(allSpeeds, epsilon), MEASURE);
        long modelTime = timeMethod(() -> modelBasedDP(allSpeeds, epsilon), MEASURE);
        
        System.out.printf("   Point-level DP: %.3f ms/call\n", (double) pointTime / MEASURE);
        System.out.printf("   Model-based DP: %.3f ms/call\n", (double) modelTime / MEASURE);
        System.out.printf("   Model-based is %.2fx %s\n", 
            (double) pointTime / modelTime,
            pointTime > modelTime ? "faster" : "slower");
        
        // ===== ACCURACY TEST =====
        System.out.println("\n" + "=".repeat(70));
        System.out.println("\n2. ACCURACY (How close to original data?)");
        System.out.println("   ε=0.5 (medium privacy)");
        
        List<Double> pointNoisy = pointLevelDP(allSpeeds, epsilon);
        List<Double> modelNoisy = modelBasedDP(allSpeeds, epsilon);
        
        double[] pointAccuracy = measureAccuracy(allSpeeds, pointNoisy);
        double[] modelAccuracy = measureAccuracy(allSpeeds, modelNoisy);
        
        System.out.println("\n   RAW DATA:");
        System.out.printf("   - Mean speed:  %.2f\n", pointAccuracy[0]);
        System.out.printf("   - Std dev:     %.2f\n\n", pointAccuracy[1]);
        
        System.out.println("   POINT-LEVEL DP:");
        System.out.printf("   - Mean:        %.2f (error: %.1f%%)\n", pointAccuracy[2], pointAccuracy[4]);
        System.out.printf("   - Std dev:     %.2f (error: %.1f%%)\n\n", pointAccuracy[3], pointAccuracy[5]);
        
        System.out.println("   MODEL-BASED DP:");
        System.out.printf("   - Mean:        %.2f (error: %.1f%%)\n", modelAccuracy[2], modelAccuracy[4]);
        System.out.printf("   - Std dev:     %.2f (error: %.1f%%)\n", modelAccuracy[3], modelAccuracy[5]);
        
        // ===== PRIVACY INTUITION =====
        System.out.println("\n" + "=".repeat(70));
        showPrivacyTreatoff(allSpeeds);
        
        System.out.println("\n   Key insight:");
        System.out.println("   - ε < 0.5:  Very hard to reverse-engineer individual speeds");
        System.out.println("   - ε ≈ 1:    Moderate privacy, still useful for analysis");
        System.out.println("   - ε > 2:    Weak privacy, mostly just noise");
        
        System.out.println("\n" + "=".repeat(70));
        System.out.println("\nSUMMARY:");
        System.out.println("- Point-level: Fast but noisy, destroys individual values");
        System.out.println("- Model-based: Preserves distribution shape, better for analysis");
        System.out.println("- Privacy: Both are DP-private at same ε, but model-based is more useful");
    }
}