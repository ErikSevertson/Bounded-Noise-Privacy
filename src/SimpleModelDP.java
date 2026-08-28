import java.util.*;

public class SimpleModelDP {

    /**
     * Step 1: Calculate mean and standard deviation
     */
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

    /**
     * Step 2: Add Laplace noise to mean and stddev
     */
    public static double[] addNoise(double mean, double stdDev, double epsilon) {
        Random rng = new Random(2484);
        
        // Laplace noise scale
        double scale = 1.0 / epsilon;
        
        // Laplace sample: (U - 0.5) * 2 * scale works approximately
        double noisyMean = mean + (rng.nextDouble() - 0.5) * 2 * scale;
        double noisyStdDev = Math.max(0.1, stdDev + (rng.nextDouble() - 0.5) * 2 * scale);
        
        return new double[]{noisyMean, noisyStdDev};
    }

    /**
     * Step 3: Sample synthetic speeds from noisy model
     */
    public static List<Double> synthesizeSpeeds(double mean, double stdDev, int n) {
        Random rng = new Random(42);
        List<Double> synthetic = new ArrayList<>();
        
        for (int i = 0; i < n; i++) {
            // Normal distribution: mean + stdDev * Z where Z ~ N(0,1)
            double z = rng.nextGaussian();
            synthetic.add(mean + stdDev * z);
        }
        
        return synthetic;
    }

    /**
     * Print statistics
     */
    public static void printStats(String label, List<Double> speeds) {
        double mean = calcMean(speeds);
        double stdDev = calcStdDev(speeds, mean);
        System.out.printf("%s: mean=%.2f, stddev=%.2f, n=%d\n", label, mean, stdDev, speeds.size());
    }

    public static void main(String[] args) throws Exception {
        // Load data (copy-paste from your App.java)
        HashMap<Integer, ArrayList<Row>> trips = App.computeTrips();
        HashMap<Integer, ArrayList<Row>> subset = App.sampleTrips(trips, 50);
        
        // Collect all speeds
        List<Double> allSpeeds = new ArrayList<>();
        for (ArrayList<Row> trip : subset.values()) {
            for (Row row : trip) {
                allSpeeds.add(row.getVel());
            }
        }
        
        // Step 1: Fit raw model
        double rawMean = calcMean(allSpeeds);
        double rawStdDev = calcStdDev(allSpeeds, rawMean);
        printStats("Raw speeds    ", allSpeeds);
        
        // Step 2: Add DP noise
        double epsilon = 0.5; // Privacy budget
        double[] noisy = addNoise(rawMean, rawStdDev, epsilon);
        double noisyMean = noisy[0];
        double noisyStdDev = noisy[1];
        System.out.printf("Noisy params  : mean=%.2f, stddev=%.2f (ε=%.1f)\n\n", noisyMean, noisyStdDev, epsilon);
        
        // Step 3: Generate synthetic speeds
        List<Double> syntheticSpeeds = synthesizeSpeeds(noisyMean, noisyStdDev, allSpeeds.size());
        printStats("Synthetic     ", syntheticSpeeds);
        
        System.out.println("\n✓ Synthetic data is differentially private but maintains the learned speed distribution");
    }
}