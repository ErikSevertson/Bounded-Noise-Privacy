import org.apache.commons.math3.distribution.LaplaceDistribution;
import org.apache.commons.math3.random.RandomGenerator;

import com.opencsv.CSVWriter;

import org.apache.commons.math3.random.JDKRandomGenerator;

import java.util.*;
import com.opencsv.CSVReader;
import java.io.FileReader;

public class App {

    private static final double EPSILON = 0.1; // epsilon is a value chosen to balance privacy vs. utility
    private static double maxVel = 0;

    public static void main(String[] args) throws Exception {

        HashMap<Integer, ArrayList<Row>> trips = computeTrips(); // raw data organized into trips
        HashMap<Integer, ArrayList<Row>> subset = sampleTrips(trips, 50);

        double targetB = 5.0;

        // Get raw means for the subset
        HashMap<Integer, Double> rawMeans = meanSpeedPerVehicle(subset);

        // Apply DP to a copy of the subset means
        HashMap<Integer, Double> dpMeans = new HashMap<>(rawMeans);
        double sens = maxVel / trips.size(); // Sensitivity uses TOTAL fleet size
        System.out.println("number of trips:" + trips.size());
        System.out.println("Max velocity: " + maxVel);
        System.out.println("Sensitivity: " + sens);
        double epsilon = sens/targetB;
        addLaplaceNoise(dpMeans, sens, epsilon);

        // Apply RDP to a copy of the subset means
        HashMap<Integer, Double> rdpMeans = new HashMap<>(rawMeans);
        // double delta = sens / (2 * targetB);
        addGuaranteedNoise(rdpMeans, targetB);

        // Apply BNP to a copy of the subset means
        HashMap<Integer, Double> gpMeans = new HashMap<>(rawMeans);
        // double B = 2 * sens / EPSILON; 
        addGuaranteedNoise(gpMeans, targetB);

        benchmarkNoiseGeneration(rawMeans, sens, epsilon, targetB);

        // Export to CSV for plotting
        exportToCSV(rawMeans, dpMeans, rdpMeans, gpMeans, "privacy_comparison2.csv");

        // HashMap<Integer, Double> means = meanSpeedPerVehicle(trips);
        // double runningMax = 0.0;
        // for (Double value : means.values()) {
        //     runningMax += value;    
        // }
        // double rawMeanSpeed = runningMax / means.size();

        // System.out.println("Raw fleet mean speed: " + rawMeanSpeed);

        // applyPerVehicleDP(trips, EPSILON);

        // trips = computeTrips(); // resets to raw data before calling GP

        // double sens = maxVel / trips.size();
        // double B = 2 * sens / EPSILON;

        // applyPerVehicleGP(trips, B);

        // trips = computeTrips(); // resets to raw data before calling RDP

        // double chosenB = 5;

        // applyPerVehicleRDP(trips, chosenB);

        // double max = fleetMax(maxSpeedPerVehicle(trips));
        // double min = fleetMin(minSpeedPerVehicle(trips));

        // System.out.println("Raw max speed: " + max);
        // System.out.println("Raw min speed: " + min);


        // //max/min calls
        // applyDPMaxMin(trips, EPSILON);

        // trips = computeTrips();
        // applyGPMaxMin(trips, B);

        // trips = computeTrips();
        // applyRDPMaxMin(trips, chosenB);

    }

    public static void exportToCSV(HashMap<Integer, Double> raw, HashMap<Integer, Double> dp, HashMap<Integer, Double> rdp, HashMap<Integer, Double> gp, String fileName) {
        try (CSVWriter writer = new CSVWriter(new java.io.FileWriter(fileName))) {
            writer.writeNext(new String[]{"VehicleID", "RawSpeed", "DPSpeed", "RDPSpeed", "GPSpeed"});
            for (Integer id : raw.keySet()) {
                writer.writeNext(new String[]{
                    id.toString(),
                    raw.get(id).toString(),
                    dp.get(id).toString(),
                    rdp.get(id).toString(),
                    gp.get(id).toString()
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void benchmarkNoiseGeneration(HashMap<Integer, Double> rawMeans, double sens, double epsilon, double targetB) {
        int WARMUP_ITER = 10000;
        int MEASURE_ITER = 100000;

        System.out.println("Starting JVM Warmup...");
        // Dummy maps so we don't accidentally ruin the real data during benchmarks
        HashMap<Integer, Double> dummyLaplace = new HashMap<>(rawMeans);
        HashMap<Integer, Double> dummyGuaranteed = new HashMap<>(rawMeans);

        // 1. WARMUP PHASE (Let Java optimize the code)
        for (int i = 0; i < WARMUP_ITER; i++) {
            addLaplaceNoise(dummyLaplace, sens, epsilon);
            addGuaranteedNoise(dummyGuaranteed, targetB);
        }

        System.out.println("Warmup complete. Measuring execution time...");

        // 2. MEASURE LAPLACE (Standard DP)
        long startLaplace = System.nanoTime();
        for (int i = 0; i < MEASURE_ITER; i++) {
            addLaplaceNoise(dummyLaplace, sens, epsilon);
        }
        long endLaplace = System.nanoTime();
        double laplaceTotalMs = (endLaplace - startLaplace) / 1_000_000.0;

        // 3. MEASURE GUARANTEED UNIFORM (BNP)
        long startGuaranteed = System.nanoTime();
        for (int i = 0; i < MEASURE_ITER; i++) {
            addGuaranteedNoise(dummyGuaranteed, targetB);
        }
        long endGuaranteed = System.nanoTime();
        double guaranteedTotalMs = (endGuaranteed - startGuaranteed) / 1_000_000.0;

        // 4. PRINT RESULTS FOR YOUR PAPER
        System.out.println("\n--- BENCHMARKING RESULTS (" + MEASURE_ITER + " iterations) ---");
        System.out.println("Laplace (DP) Total Time: " + laplaceTotalMs + " ms");
        System.out.println("Laplace (DP) Average Time per call: " + (laplaceTotalMs / MEASURE_ITER) + " ms");
        System.out.println("--------------------------------------------------");
        System.out.println("Uniform BNP (GP) Total Time: " + guaranteedTotalMs + " ms");
        System.out.println("Uniform BNP (GP) Average Time per call: " + (guaranteedTotalMs / MEASURE_ITER) + " ms");
        
        // Calculate how much faster BNP is
        double speedup = laplaceTotalMs / guaranteedTotalMs;
        System.out.printf("Conclusion: Uniform BNP sampling is %.2fx faster than Laplace sampling.\n", speedup);
    }

    public static HashMap<Integer, ArrayList<Row>> computeTrips() {
        String fileName = "ngsim.csv";

        HashMap<Integer, ArrayList<Row>> trips = new HashMap<>();

        try (CSVReader reader = new CSVReader(new FileReader(fileName))) {
            String[] line;
            reader.readNext(); // skip header

            while ((line = reader.readNext()) != null) {

                // Parse values from CSV
                int vehicleId = Integer.parseInt(line[0].replace(",", ""));
                int frameId = Integer.parseInt(line[1].replace(",", ""));
                double globalX = Double.parseDouble(line[3].replace(",", ""));
                double globalY = Double.parseDouble(line[4].replace(",", ""));
                double vel = Double.parseDouble(line[5].replace(",", ""));
                double acc = Double.parseDouble(line[6].replace(",", ""));
                int laneId = Integer.parseInt(line[7].replace(",", ""));

                maxVel = Math.max(maxVel, vel);

                Row row = new Row(
                        frameId,
                        vehicleId,
                        globalX,
                        globalY,
                        vel,
                        acc,
                        laneId);

                trips.computeIfAbsent(vehicleId, k -> new ArrayList<>()).add(row);

            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return trips;
    }

    public static double calcAvgLaneChanges(HashMap<Integer, ArrayList<Row>> trips) {
        int totalLaneChanges = 0;
        final int PERSIST_FRAMES = 5;

        for (ArrayList<Row> trip : trips.values()) {
            int laneChanges = 0;

            int currentLane = trip.get(0).getLaneId();
            int candidateLane = currentLane;
            int candidateCount = 0;

            for (int i = 1; i < trip.size(); i++) {
                int lane = trip.get(i).getLaneId();

                if (lane == currentLane) {
                    candidateCount = 0;
                } else if (lane == candidateLane) {
                    candidateCount++;
                    if (candidateCount >= PERSIST_FRAMES) {
                        laneChanges++;
                        currentLane = candidateLane;
                        candidateCount = 0;
                    }
                } else {
                    candidateLane = lane;
                    candidateCount = 1;
                }
            }

            totalLaneChanges += laneChanges;
        }

        return trips.size() == 0 ? 0.0 : (double) totalLaneChanges / trips.size();
    }

    public static void applyPerVehicleDP(HashMap<Integer, ArrayList<Row>> trips, double epsilon) {

        HashMap<Integer, Double> meanPerVehicle = meanSpeedPerVehicle(trips);

        double sens = maxVel / trips.size(); // largest change is from 0 to maxVel, and since mean is recorded divide by
                                             // # cars

        addLaplaceNoise(meanPerVehicle, sens, epsilon);
        double sum = 0.0;
        for (double v : meanPerVehicle.values()) {
            sum += v;
        }
        double noisyFleetMean = sum / meanPerVehicle.size();

        System.out.println("DP noisy fleet mean speed: " + noisyFleetMean);
    }

    public static void applyPerVehicleGP(HashMap<Integer, ArrayList<Row>> trips, double B) {

        HashMap<Integer, Double> meanPerVehicle = meanSpeedPerVehicle(trips);

        addGuaranteedNoise(meanPerVehicle, B);

        double sum = 0.0;
        for (double v : meanPerVehicle.values()) {
            sum += v;
        }

        double noisyFleetMean = sum / meanPerVehicle.size();

        System.out.println("Guaranteed noisy fleet mean speed: " + noisyFleetMean);
    }

    public static void applyPerVehicleRDP(HashMap<Integer, ArrayList<Row>> trips, double B) {

        HashMap<Integer, Double> meanPerVehicle = meanSpeedPerVehicle(trips);

        Random rng = new Random(2484);
        for (Map.Entry<Integer, Double> e : meanPerVehicle.entrySet()) {
            double noise = (rng.nextDouble() * 2 - 1) * B;
            e.setValue(e.getValue() + noise);
        }

        double sum = 0.0;
        for (double v : meanPerVehicle.values()) {
            sum += v;
        }
        double fleetMean = sum / meanPerVehicle.size();

        // Step 4: print result
        System.out.println("RDP fleet mean speed: " + fleetMean);
    }

    public static void addLaplaceNoise(HashMap<Integer, Double> values, double sensitivity, double epsilon) {

        RandomGenerator rng = new JDKRandomGenerator();
        rng.setSeed(2484);

        LaplaceDistribution laplace = new LaplaceDistribution(rng, 0.0, sensitivity / epsilon);

        for (Map.Entry<Integer, Double> e : values.entrySet()) {
            e.setValue(e.getValue() + laplace.sample());
        }
    }

    public static void addGuaranteedNoise(HashMap<Integer, Double> values, double B) {

        Random rng = new Random(2484);

        for (Map.Entry<Integer, Double> e : values.entrySet()) {
            double noise = (rng.nextDouble() * 2 - 1) * B;
            e.setValue(e.getValue() + noise);
        }
    }

    public static HashMap<Integer, Double> meanSpeedPerVehicle(HashMap<Integer, ArrayList<Row>> trips) {

        HashMap<Integer, Double> means = new HashMap<>();

        for (Map.Entry<Integer, ArrayList<Row>> entry : trips.entrySet()) {
            double sum = 0.0;
            for (Row r : entry.getValue()) {
                sum += r.getVel();
            }
            means.put(entry.getKey(), sum / entry.getValue().size());
        }

        return means;
    }

    public static HashMap<Integer, Double> maxSpeedPerVehicle(HashMap<Integer, ArrayList<Row>> trips) {

        HashMap<Integer, Double> maxes = new HashMap<>();

        for (Map.Entry<Integer, ArrayList<Row>> entry : trips.entrySet()) {
            double max = Double.NEGATIVE_INFINITY;
            for (Row r : entry.getValue()) {
                max = Math.max(max, r.getVel());
            }
            maxes.put(entry.getKey(), max);
        }

        return maxes;
    }

    public static HashMap<Integer, Double> minSpeedPerVehicle(HashMap<Integer, ArrayList<Row>> trips) {

        HashMap<Integer, Double> mins = new HashMap<>();

        for (Map.Entry<Integer, ArrayList<Row>> entry : trips.entrySet()) {
            double min = Double.POSITIVE_INFINITY;
            for (Row r : entry.getValue()) {
                min = Math.min(min, r.getVel());
            }
            mins.put(entry.getKey(), min);
        }

        return mins;
    }

    public static double fleetMax(HashMap<Integer, Double> values) {
        double max = Double.NEGATIVE_INFINITY;
        for (double v : values.values()) {
            max = Math.max(max, v);
        }
        return max;
    }

    public static double fleetMin(HashMap<Integer, Double> values) {
        double min = Double.POSITIVE_INFINITY;
        for (double v : values.values()) {
            min = Math.min(min, v);
        }
        return min;
    }

    public static void applyDPMaxMin(HashMap<Integer, ArrayList<Row>> trips, double epsilon) {

        HashMap<Integer, Double> maxes = maxSpeedPerVehicle(trips);
        HashMap<Integer, Double> mins = minSpeedPerVehicle(trips);

        double sens = maxVel / trips.size();

        addLaplaceNoise(maxes, sens, epsilon);
        addLaplaceNoise(mins, sens, epsilon);

        System.out.println("DP fleet max speed: " + fleetMax(maxes));
        System.out.println("DP fleet min speed: " + fleetMin(mins));
    }

    public static void applyGPMaxMin(HashMap<Integer, ArrayList<Row>> trips, double B) {

        HashMap<Integer, Double> maxes = maxSpeedPerVehicle(trips);
        HashMap<Integer, Double> mins = minSpeedPerVehicle(trips);

        addGuaranteedNoise(maxes, B);
        addGuaranteedNoise(mins, B);

        double fleetMax = fleetMax(maxes);
        double fleetMin = fleetMin(mins);

        System.out.println("GP fleet speed interval: [" + fleetMin + ", " + fleetMax + "]");
    }

    public static void applyRDPMaxMin(HashMap<Integer, ArrayList<Row>> trips, double B) {

        HashMap<Integer, Double> maxes = maxSpeedPerVehicle(trips);
        HashMap<Integer, Double> mins = minSpeedPerVehicle(trips);

        Random rng = new Random(2484);

        for (int id : maxes.keySet()) {
            maxes.put(id, maxes.get(id) + (rng.nextDouble() * 2 - 1) * B);
            mins.put(id, mins.get(id) + (rng.nextDouble() * 2 - 1) * B);
        }

        System.out.println("RDP fleet max speed: " + fleetMax(maxes));
        System.out.println("RDP fleet min speed: " + fleetMin(mins));
    }

    public static HashMap<Integer, ArrayList<Row>> sampleTrips(HashMap<Integer, ArrayList<Row>> trips, int n) {
    List<Integer> keys = new ArrayList<>(trips.keySet());
    Collections.shuffle(keys, new Random(2484)); // Consistent seed for reproducibility
    
    HashMap<Integer, ArrayList<Row>> subset = new HashMap<>();
    for (int i = 0; i < n; i++) {
        int key = keys.get(i);
        subset.put(key, trips.get(key));
    }
    return subset;
}

}
