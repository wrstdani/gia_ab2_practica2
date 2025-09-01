package es.urjc.grafo.ABII.Algorithms;

import es.urjc.grafo.ABII.Model.Evaluator;
import es.urjc.grafo.ABII.Model.Instance;
import es.urjc.grafo.ABII.Model.Solution;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class Algorithm2 implements Algorithm {

    // This method is used to run the algorithm
    public Solution run(Instance instance) {
        // -------- Timer -------- //
        Instant t0 = Instant.now();

        // -------- Parameters section -------- //
        // Set the size of the population
        // int populationSize = 550;
        int populationSize = 800;
        int maxGenerations = 700;
        double crossoverRate = 0.13;
        double mutationRate = 0.24;

        // -------- Initialize section -------- //
        // Generate the initial population
        Population population = new Population(instance, populationSize);

        // -------- Main loop -------- //
        for (int i = 0; i < maxGenerations; i++) {
            // Generate mating pool
            List<Integer> matingPool = new ArrayList<>(populationSize);
            for (int j = 0; j < populationSize; j++) {
                matingPool.add(j);
            }

            // Shuffle the mating pool
            Collections.shuffle(matingPool);

            // Apply crossover
            List<Individual> crossovers = new ArrayList<>();
            for (int j = 0; j < (populationSize - 1); j++) {
                double crossoverProb = new Random().nextDouble(0, 1);
                if (crossoverProb > crossoverRate) {
                    Individual individual1 = population.get(j);
                    Individual individual2 = population.get(j + 1);
                    Individual crossover = individual1.partiallyMappedX(individual2);
                    crossovers.add(crossover);
                }
            }

            // Apply mutation
            for (Individual crossover : crossovers) {
                double mutationProb = new Random().nextDouble(0, 1);
                if (mutationProb > mutationRate) {
                    crossover.mutateSwap();
                }
            }
            Population generatedPopulation = new Population(instance, crossovers);

            // Replace the worst individual of the population with the best one found
            Individual bestFound = generatedPopulation.getPopulationsBest();
            population.replaceWorst(bestFound);
        }
        Individual bestIndividual = population.getPopulationsBest();

        // Print out the algorithm's execution time
        Instant t1 = Instant.now();
        Duration ssgaDuration = Duration.between(t0, t1);
        System.out.println("Duration --> " + ssgaDuration);

        return bestIndividual.getSolution();
    }

    // This method represents each individual
    private static class Individual {
        private final Instance instance;
        private int currentCustomer;
        private boolean[] visited;
        private Vehicle[] vehicles;

        // This constructor generates a random individual
        private Individual(Instance instance, boolean generateRandom) {
            this.instance = instance;
            this.visited = new boolean[instance.getNumberOfCustomers()];
            this.vehicles = new Vehicle[instance.getNumberOfVehicles()];
            for (int i = 0; i < instance.getNumberOfVehicles(); i++) {
                this.vehicles[i] = new Vehicle(instance);
            }
            if (generateRandom) {
                this.generateRandom();
                while (!Evaluator.isFeasible(this.getSolution(), instance)) {
                    this.visited = new boolean[instance.getNumberOfCustomers()];
                    this.vehicles = new Vehicle[instance.getNumberOfVehicles()];
                    for (int i = 0; i < instance.getNumberOfVehicles(); i++) {
                        this.vehicles[i] = new Vehicle(instance);
                    }
                    this.generateRandom();
                }
            }
        }

        // This constructor creates a new individual based on a crossover (solution without base or charging stations)
        private Individual(Instance instance, List<Integer> route) {
            this.instance = instance;
            this.visited = new boolean[instance.getNumberOfCustomers()];
            this.vehicles = new Vehicle[instance.getNumberOfVehicles()];
            for (int i = 0; i < instance.getNumberOfVehicles(); i++) {
                this.vehicles[i] = new Vehicle(instance);
            }
            this.fromCustomers(route);
        }

        // This method makes the individual visit a customer
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

        // This method returns the individual's solution
        private Solution getSolution() {
            List<Integer>[] routes = new ArrayList[this.instance.getNumberOfVehicles()];
            for (int i = 0; i < this.instance.getNumberOfVehicles(); i++) {
                routes[i] = this.vehicles[i].getRoute();
            }
            return new Solution(routes);
        }

        // This method returns the route of an individual (without "1" and without charging stations, just customers)
        public List<Integer> getCustomers() {
            List<Integer> customers = new ArrayList<>();
            for (Vehicle v : this.vehicles) {
                for (Integer c : v.getRoute()) {
                    if (!this.instance.isChargeStation(c)) {
                        customers.add(c);
                    }
                }
            }
            return customers;
        }

        // This method returns the fitness of the individual's solution
        private double getFitness() {
            return Evaluator.evaluate(this.getSolution(), this.instance);
        }

        private void fromCustomers(List<Integer> customers) {
            int crossoverIdx = 0;
            for (Vehicle v : this.vehicles) {
                // The first visited customer is always 1
                this.visit(1);
                v.visitCustomer(1);

                int nextCustomer = 0;
                while (v.getCurrentCarry() > 0 && this.customersToVisit() && (nextCustomer != -1)) {
                    nextCustomer = customers.get(crossoverIdx);
                    if (v.getCurrentCarry() < this.instance.getDemand(nextCustomer)) nextCustomer = -1;
                    if (nextCustomer != -1) {
                        if (!v.reachableCustomer(nextCustomer)) {
                            int closestChargeStation = this.instance.getClosestChargeStation(this.currentCustomer);
                            v.visitCustomer(closestChargeStation);
                            v.chargeBattery();
                        }
                        this.visit(nextCustomer);
                        v.visitCustomer(this.currentCustomer);
                        crossoverIdx++;
                    }
                }

                // Return to customer 1
                if (!v.reachableCustomer(1)) {
                    int closestChargeStation = this.instance.getClosestChargeStation(this.currentCustomer);
                    v.visitCustomer(closestChargeStation);
                    v.chargeBattery();
                }
                this.visit(1);
                v.visitCustomer(1);
            }
        }

        // This method generates a random but feasible individual
        private void generateRandom() {
            for (Vehicle v : this.vehicles) {
                // The first visited customer is always 1
                this.visit(1);
                v.visitCustomer(1);

                int nextCustomer = 0;
                while (v.getCurrentCarry() > 0 && this.customersToVisit() && (nextCustomer != -1)) {
                    nextCustomer = this.nextCustomerRand(v);
                    if (nextCustomer != -1) {
                        if (!v.reachableCustomer(nextCustomer)) {
                            int closestChargeStation = this.instance.getClosestChargeStation(this.currentCustomer);
                            v.visitCustomer(closestChargeStation);
                            v.chargeBattery();
                        }
                        this.visit(nextCustomer);
                        v.visitCustomer(this.currentCustomer);
                    }
                }

                // Return to customer 1
                if (!v.reachableCustomer(1)) {
                    int closestChargeStation = this.instance.getClosestChargeStation(this.currentCustomer);
                    v.visitCustomer(closestChargeStation);
                    v.chargeBattery();
                }
                this.visit(1);
                v.visitCustomer(1);
            }
        }

        // This method returns a random customer
        private int nextCustomerRand(Vehicle vehicle) {
            List<Integer> avCustomers = this.getAvailableCustomers(vehicle);
            if (avCustomers.isEmpty()) {
                return -1;
            }
            int randIdx = new Random().nextInt(avCustomers.size());
            return avCustomers.get(randIdx);
        }

        // This method returns a list of available customers
        private List<Integer> getAvailableCustomers(Vehicle vehicle) {
            List<Integer> customers = new ArrayList<>();
            for (int i = 0; i < this.instance.getNumberOfCustomers(); i++) {
                if (!this.isVisited(i + 1) && vehicle.getCurrentCarry() >= this.instance.getDemand(i + 1)) {
                    customers.add(i + 1);
                }
            }
            return customers;
        }

        // This method returns whether not visited customers exist or not
        private boolean customersToVisit() {
            for (boolean v : this.visited) {
                if (!v) return true;
            }
            return false;
        }

        private Individual partiallyMappedX(Individual i2) {
            List<Integer> l1 = this.getCustomers();
            List<Integer> l2 = i2.getCustomers();
            List<Integer> crossover = new ArrayList<>();

            // Inicializamos todos los valores de "crossover" a "null"
            for (int i = 0; i < this.instance.getNumberOfCustomers() - 1; i++) {
                crossover.add(null);
            }

            // Copiamos un segmento aleatorio de "l1" a "crossover"
            int randStart = new Random().nextInt(0, this.instance.getNumberOfCustomers() - 1);
            int randSize = new Random().nextInt(1, this.instance.getNumberOfCustomers() - 1);
            while ((randStart + randSize) >= (this.instance.getNumberOfCustomers() - 1)) {
                randStart = new Random().nextInt(0, this.instance.getNumberOfCustomers() - 1);
                randSize = new Random().nextInt(1, this.instance.getNumberOfCustomers() - 1);
            }

            for (int i = randStart; i < (randStart + randSize); i++) {
                crossover.set(i, l1.get(i));
            }

            //
            for (int i = randStart; i < (randStart + randSize); i++) {
                int e1 = l1.get(i);
                int e2 = l2.get(i);
                if (!crossover.contains(e2)) {
                    int insertIndex = l2.indexOf(e1);
                    while (crossover.get(insertIndex) != null) {
                        e1 = l1.get(insertIndex);
                        insertIndex = l2.indexOf(e1);
                    }
                    crossover.set(insertIndex, e2);
                }
            }

            for (int i = 0; i < l2.size(); i++) {
                int e2 = l2.get(i);
                if (!crossover.contains(e2)) {
                    int insertIndex = i;
                    while (crossover.get(insertIndex) != null) {
                        insertIndex++;
                    }
                    crossover.set(insertIndex, e2);
                }
            }

            Individual crossoverIndividual = new Individual(instance, crossover);

            if (!Evaluator.isFeasible(crossoverIndividual.getSolution(), instance)) {
                crossoverIndividual = this.partiallyMappedX(i2);
            }

            // Devolvemos "crossover"
            return crossoverIndividual;
        }

        // This method applies scramble mutation to the individual
        private void mutateScramble() {
            List<Integer> customers = this.getCustomers();
            int randBegin = new Random().nextInt(0, customers.size() / 2);
            int randSize = new Random().nextInt(1, customers.size() / 2);
            while (randBegin + randSize > customers.size()) {
                randBegin = new Random().nextInt(0, customers.size() / 2);
                randSize = new Random().nextInt(1, customers.size() / 2);
            }
            List<Integer> toMutate = customers.subList(randBegin, randBegin + randSize + 1);
            Collections.shuffle(toMutate);
            this.fromCustomers(customers);
        }

        // This method applies swap mutation to the individual
        private void mutateSwap() {
            List<Integer> customers = this.getCustomers();
            int i = new Random().nextInt(0, customers.size());
            int j = new Random().nextInt(0, customers.size());
            while (i == j) {
                i = new Random().nextInt(0, customers.size());
                j = new Random().nextInt(0, customers.size());
            }
            int temp = customers.get(i);
            customers.set(i, customers.get(j));
            customers.set(j, temp);
            this.fromCustomers(customers);
        }
    }

    // This method represents a population (collection of individuals)
    private static class Population implements Iterable<Individual> {
        private final Instance instance;
        private final Individual[] population;

        // This constructor already generates an initial population
        private Population(Instance instance, int populationSize) {
            this.instance = instance;
            this.population = new Individual[populationSize];
            for (int i = 0; i < populationSize; i++) {
                this.population[i] = new Individual(instance, true);
            }
        }

        // This constructor creates a new population given a list of individuals
        private Population(Instance instance, List<Individual> individuals) {
            this.instance = instance;
            this.population = new Individual[individuals.size()];
            for (int i = 0; i < this.population.length; i++) {
                this.population[i] = individuals.get(i);
            }
        }

        private Individual getPopulationsBest() {
            Individual bestIndividual = null;
            double bestScore = Double.MAX_VALUE;
            for (int i = 0; i < this.population.length; i++) {
                double fitness = this.population[i].getFitness();
                if ((fitness < bestScore) && Evaluator.isFeasible(this.population[i].getSolution(), this.instance)) {
                    bestIndividual = this.population[i];
                    bestScore = fitness;
                }
            }
            return bestIndividual;
        }

        private Individual get(int idx) {
            return this.population[idx];
        }

        private void replaceWorst(Individual best) {
            double worstScore = Double.MIN_VALUE;
            int worstIndividualIdx = 0;
            for (int i = 0; i < this.population.length; i++) {
                double fitness = this.population[i].getFitness();
                if (fitness > worstScore) {
                    worstIndividualIdx = i;
                    worstScore = fitness;
                }
            }
            this.population[worstIndividualIdx] = best;
        }

        // Implement necessary methods to use iterator
        public Iterator<Individual> iterator() {
            return new Iterator<Individual>() {
                private int index = 0;

                @Override
                public boolean hasNext() {
                    return this.index < population.length;
                }

                @Override
                public Individual next() {
                    return population[index++];
                }
            };
        }
    }

    // This method represents each vehicle
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

        // This method returns whether a customer can be reached or not
        private boolean reachableCustomer(int nextCustomer) {
            // Get necessary battery to reach "nextCustomer"
            double neededBattery1 = this.instance.getBatteryConsumption(this.currentCustomer, nextCustomer);
            // Get necessary battery to reach the closest charge station to "nextCustomer" from "nextCustomer"
            int closestChargeStationNextCustomer = this.instance.getClosestChargeStation(nextCustomer);
            double neededBattery2 = this.instance.getBatteryConsumption(nextCustomer, closestChargeStationNextCustomer);
            // Get total battery needed to reach "nextCustomer" and then get to a charge station
            double totalNeededBattery = neededBattery1 + neededBattery2;

            return this.getCurrentBattery() >= totalNeededBattery;
        }
    }

    public String toString() {
        return "SSGA (Steady State Genetic Algorithm)";
    }
}
