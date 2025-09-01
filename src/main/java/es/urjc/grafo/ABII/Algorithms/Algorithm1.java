package es.urjc.grafo.ABII.Algorithms;

import es.urjc.grafo.ABII.Model.Evaluator;
import es.urjc.grafo.ABII.Model.Instance;
import es.urjc.grafo.ABII.Model.Solution;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class Algorithm1 implements Algorithm {
    // Matrix of doubles that stores the values of the pheromones
    private double[][] pheromoneMatrix;

    // This method is used to run the algorithm
    public Solution run(Instance instance) {
        // -------- Timer -------- //
        Instant t0 = Instant.now();

        // -------- Parameters section -------- //
        // Set the maximum number of iterations for the algorithm
        int maxIter = 100;
        // Set the number of ants
        int numAnts = 40;
        // Set the value for alpha
        double alpha = 0.2;
        // Set the value for beta
        double beta = 1.0;
        // Set the value for rho
        double rho = 0.9;
        // Set the value for Q
        double q = 1.0;

        // -------- Initialize section -------- //
        // Generate a colony of "numAnts" ants
        AntColony colony = new AntColony(instance, numAnts);
        // Initialize the best solution to null
        Solution bestSolution = null;
        // Initialize the pheromone matrix
        this.initializePheromones(instance.getNumberOfCustomers());

        // -------- Main loop -------- //
        for (int i = 0; i < maxIter; i++) {
            // For each ant in "colony"...
            for (Ant ant : colony) {
                // Build a route
                ant.buildAntRoute(this.pheromoneMatrix, alpha, beta);
            }
            // Update pheromone matrix
            this.updatePheromones(rho, q, colony, instance);

            // Update best solution
            bestSolution = this.updateBestSolution(bestSolution, colony, instance);

            // Reset ant colony
            colony.resetAnts();
        }

        // Print out the algorithm's execution time
        Instant t1 = Instant.now();
        Duration acoDuration = Duration.between(t0, t1);
        System.out.println("Duration --> " + acoDuration);

        return bestSolution;
    }

    // This method initializes the pheromone matrix
    private void initializePheromones(int numNodes) {
        double initValue = 1.0;
        this.pheromoneMatrix = new double[numNodes][numNodes];
        for (int i = 0; i < numNodes; i++) {
            for (int j = 0; j < numNodes; j++) {
                // Initialize every position to 1.0
                this.pheromoneMatrix[i][j] = initValue;
            }
        }
    }

    // This method updates the values in the pheromone matrix
    private void updatePheromones(double rho, double q, AntColony colony, Instance instance) {
        // First, we multiply by the evaporation factor "rho"
        for (int i = 0; i < this.pheromoneMatrix.length; i++) {
            for (int j = 0; j < this.pheromoneMatrix.length; j++) {
                this.pheromoneMatrix[i][j] *= rho;
            }
        }

        // Then, we add the pheromone quantity
        for (Ant ant : colony) {
            double quality = Evaluator.evaluate(ant.getAntSolution(), instance);
            Solution antSolution = ant.getAntSolution();
            for (List<Integer> route : antSolution.routes()) {
                for (int i = 0; i < route.size() - 1; i++) {
                    int customerI = route.get(i);
                    int customerJ = route.get(i + 1);
                    if ((customerI == 1 || !instance.isChargeStation(customerI)) && (customerJ == 1 || !instance.isChargeStation(customerJ))) {
                        this.pheromoneMatrix[customerI - 1][customerJ - 1] += (q / quality);
                    }
                }
            }
        }
    }

    // This method updates the best solution found by the algorithm
    private Solution updateBestSolution(Solution bestSolution, AntColony colony, Instance instance) {
        for (Ant ant : colony) {
            Solution antSolution = ant.getAntSolution();
            if (Evaluator.isBetter(antSolution, bestSolution, instance)) {
                bestSolution = antSolution;
            }
        }
        return bestSolution;
    }

    // This class represents each ant
    private static class Ant {
        private final Instance instance;
        private int currentCustomer;
        private boolean[] visited;
        private Vehicle[] vehicles;

        private Ant(Instance instance) {
            this.instance = instance;
            this.visited = new boolean[instance.getNumberOfCustomers()];

            // Initialize the array of vehicles
            int numVehicles = instance.getNumberOfVehicles();
            this.vehicles = new Vehicle[numVehicles];
            for (int v = 0; v < numVehicles; v++) {
                this.vehicles[v] = new Vehicle(instance);
            }
            this.currentCustomer = this.vehicles[0].getCurrentCustomer(); // The first "currentCustomer" will be 1
            this.visit(1);
        }

        private void buildAntRoute(double[][] pheromoneMatrix, double alpha, double beta) {
            for (Vehicle v : this.vehicles) {
                // The first visited customer is always 1
                this.visit(1);
                v.visitCustomer(1);

                // The second visited customer is selected randomly
                int nextCustomer = this.nextCustomerRand();
                if (nextCustomer != -1) {
                    this.visit(nextCustomer);
                    v.visitCustomer(this.currentCustomer);
                }

                // Main loop
                while (v.getCurrentCarry() > 0 && this.customersToVisit() && (nextCustomer != -1)) {
                    nextCustomer = this.nextCustomerProb(pheromoneMatrix, alpha, beta, v);
                    if (nextCustomer != -1) {
                        if (!this.reachableCustomer(nextCustomer, v)) {
                            int closestChargeStation = this.instance.getClosestChargeStation(this.currentCustomer);
                            v.visitCustomer(closestChargeStation);
                            v.chargeBattery();
                        }
                        this.visit(nextCustomer);
                        v.visitCustomer(this.currentCustomer);
                    }
                }

                // Return to customer 1
                if (!this.reachableCustomer(1, v)) {
                    int closestChargeStation = this.instance.getClosestChargeStation(this.currentCustomer);
                    v.visitCustomer(closestChargeStation);
                    v.chargeBattery();
                }
                this.visit(1);
                v.visitCustomer(1);
            }
        }

        private void resetAnt() {
            // Sets all customers to not visited (false)
            this.visited = new boolean[instance.getNumberOfCustomers()];

            // Initialize the array of vehicles
            int numVehicles = instance.getNumberOfVehicles();
            this.vehicles = new Vehicle[numVehicles];
            for (int v = 0; v < numVehicles; v++) {
                this.vehicles[v] = new Vehicle(instance);
            }
            this.currentCustomer = this.vehicles[0].getCurrentCustomer(); // The first "currentCustomer" will be 1
        }

        private boolean reachableCustomer(int nextCustomer, Vehicle vehicle) {
            // Get necessary battery to reach "nextCustomer"
            double neededBattery1 = this.instance.getBatteryConsumption(this.currentCustomer, nextCustomer);
            // Get necessary battery to reach the closest charge station to "nextCustomer" from "nextCustomer"
            int closestChargeStationNextCustomer = this.instance.getClosestChargeStation(nextCustomer);
            double neededBattery2 = this.instance.getBatteryConsumption(nextCustomer, closestChargeStationNextCustomer);
            // Get total battery needed to reach "nextCustomer" and then get to a charge station
            double totalNeededBattery = neededBattery1 + neededBattery2;

            return vehicle.getCurrentBattery() >= totalNeededBattery;
        }

        private boolean customersToVisit() {
            for (boolean v : this.visited) {
                if (!v) return true;
            }
            return false;
        }

        private double getAddedPheromones(double q) {
            return q / Evaluator.evaluate(this.getAntSolution(), this.instance);
        }

        private Solution getAntSolution() {
            List<Integer>[] routes = new ArrayList[this.instance.getNumberOfVehicles()];
            for (int i = 0; i < this.instance.getNumberOfVehicles(); i++) {
                routes[i] = this.vehicles[i].getRoute();
            }
            return new Solution(routes);
        }

        // This method returns a random customer
        private int nextCustomerRand() {
            int nextCustomer = new Random().nextInt(2, this.instance.getNumberOfCustomers() + 1);
            if (!this.customersToVisit()) return -1;
            while (this.isVisited(nextCustomer)) {
                nextCustomer = new Random().nextInt(2, this.instance.getNumberOfCustomers() + 1);
            }
            return nextCustomer;
        }

        // This method returns the next customer to visit according to the provided formula
        private int nextCustomerProb(double[][] pheromoneMatrix, double alpha, double beta, Vehicle vehicle) {
            // Compute probabilities
            List<Double> probabilities = this.computeProb(pheromoneMatrix, alpha, beta, vehicle);

            // Return a customer selected randomly from the "nHighestProbs" ones with the highest probabilities
            if (!probabilities.isEmpty()) {
                return probabilities.indexOf(Collections.max(probabilities)) + 1;
            } else {
                return -1;
            }
        }

        private List<Double> computeProb(double[][] pheromoneMatrix, double alpha, double beta, Vehicle vehicle) {
            List<Double> probabilities = new ArrayList<>(this.instance.getNumberOfCustomers());
            List<Integer> availableCustomers = this.getAvailableCustomers(vehicle);
            if (!availableCustomers.isEmpty()) {
                double d = 0;
                for (Integer customer : availableCustomers) {
                    double distance = this.instance.getDistance(this.currentCustomer, customer);
                    if ((customer - 1) != this.currentCustomer) {
                        d += (Math.pow(pheromoneMatrix[this.currentCustomer - 1][customer - 1], alpha) * Math.pow((1 / distance), beta));
                    }
                }
                for (int j = 0; j < this.instance.getNumberOfCustomers(); j++) {
                    if (this.currentCustomer == j || this.visited[j] || vehicle.getCurrentCarry() < this.instance.getDemand(j + 1)) {
                        probabilities.add(-1.0);
                    } else {
                        double distance = this.instance.getDistance(this.currentCustomer, (j + 1));
                        double n = Math.pow(pheromoneMatrix[this.currentCustomer - 1][j], alpha) * Math.pow((1 / distance), beta);
                        probabilities.add(j, (n / d));
                    }
                }
            }
            return probabilities;
        }

        // This method sets a customer as visited
        private void visit(int customer) {
            this.currentCustomer = customer;
            if ((customer == 1) || (!this.instance.isChargeStation(customer))) { // If "customer" is not a charge station
                this.visited[customer - 1] = true;
            }
        }

        // This method returns whether a customer has been visited or not
        private boolean isVisited(int customer) {
            return this.visited[customer - 1];
        }

        // This method returns the available customers from "customer"
        private List<Integer> getAvailableCustomers(Vehicle vehicle) {
            List<Integer> avCustomers = new ArrayList<>();
            for (int i = 0; i < this.visited.length; i++) {
                if (!this.isVisited(i + 1) && (this.instance.getDemand(i + 1) <= vehicle.getCurrentCarry())) {
                    avCustomers.add(i + 1);
                }
            }
            return avCustomers;
        }
    }

    // This class represents an ant colony (collection of "Ant" objects)
    private static class AntColony implements Iterable<Ant> {
        private final Ant[] colony;

        // Constructor, creates a new colony with "numAnts" ants
        private AntColony(Instance instance, int numAnts) {
            this.colony = new Ant[numAnts];
            for (int i = 0; i < numAnts; i++) {
                this.colony[i] = new Ant(instance);
            }
        }

        // Implement necessary methods to use iterator
        @Override
        public Iterator<Ant> iterator() {
            return new Iterator<Ant>() {
                private int index = 0;

                @Override
                public boolean hasNext() {
                    return this.index < colony.length;
                }

                @Override
                public Ant next() {
                    return colony[index++];
                }
            };
        }

        // This method returns the ant at position "position" of the colony
        private Ant getAnt(int position) {
            return this.colony[position];
        }

        // This method resets the ant colony
        private void resetAnts() {
            for (Ant ant : this.colony) {
                ant.resetAnt();
            }
        }

        // This method returns de sum of the pheromones added by each ant
        private double getAddedPheromonesSum(double q) {
            double sum = 0.0;
            for (Ant ant : this.colony) {
                sum += ant.getAddedPheromones(q);
            }
            return sum;
        }
    }

    // This class represents each electric vehicle
    private static class Vehicle {
        private final Instance instance;
        private final List<Integer> vehicleRoute;
        private int currentCustomer;
        private final double carryingCapacity;
        private double currentCarry;
        private final double batteryCapacity;
        private double currentBattery;

        private Vehicle(Instance instance) {
            this.instance = instance;
            this.vehicleRoute = new ArrayList<>();
            this.currentCustomer = 1;
            this.carryingCapacity = instance.getCarryingCapacity();
            this.currentCarry = instance.getCarryingCapacity();
            this.batteryCapacity = instance.getBatteryCapacity();
            this.currentBattery = instance.getBatteryCapacity();
        }

        private int getCurrentCustomer() {
            return this.currentCustomer;
        }

        private double getCurrentBattery() {
            return this.currentBattery;
        }

        private double getCurrentCarry() {
            return this.currentCarry;
        }

        private void visitCustomer(int customer) {
            double distance = this.instance.getDistance(this.currentCustomer, customer);
            // Update the vehicle's battery
            this.currentBattery -= this.instance.getBatteryConsumption(this.currentCustomer, customer);
            // Update the vehicle's carry (if "customer" is not a charge station)
            if (!this.instance.isChargeStation(customer)) {
                this.currentCarry -= this.instance.getDemand(customer);
            }
            // Update the current customer
            this.currentCustomer = customer;
            // Add the customer to the vehicle's route
            this.vehicleRoute.add(customer);
        }

        private List<Integer> getRoute() {
            return this.vehicleRoute;
        }

        private void chargeBattery() {
            this.currentBattery = this.batteryCapacity;
        }
    }

    // This method is used to get the name of the algorithm
    public String toString() {
        return "ACO (Ant Colony Optimization)";
    }
}
