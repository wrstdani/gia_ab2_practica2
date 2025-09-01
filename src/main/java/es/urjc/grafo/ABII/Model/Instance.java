package es.urjc.grafo.ABII.Model;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class Instance {

    private Map<Integer, Double[]> coordinates;
    private Set<Integer> chargeStations;
    private int numberOfVehicles;
    private Map<Integer, Double> customersDemand;
    private double batteryCapacity;
    private double carryingCapacity;
    private double h;
    private double optimumValue;

    /**
     * Reads the file line by line
     * @param filePath
     */
    public Instance(String filePath) {
        File instance = new File(filePath);
        try {
            Scanner scanner = new Scanner(instance);
            // Optimum value
            if (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] splitLine = line.split(" ");
                optimumValue = Double.parseDouble(splitLine[1]);
            }
            // Number of vehicles
            if (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] splitLine = line.split(" ");
                numberOfVehicles = Integer.parseInt(splitLine[1]);
            }
            // Dimension
            if (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] splitLine = line.split(" ");
                customersDemand = new HashMap<>(Integer.parseInt(splitLine[1]));
            }
            // Charge stations
            if (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] splitLine = line.split(" ");
                chargeStations = new HashSet<>(Integer.parseInt(splitLine[1]));
                coordinates = new HashMap<>(customersDemand.size() + Integer.parseInt(splitLine[1]));
            }
            // Vehicle capacity
            if (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] splitLine = line.split(" ");
                carryingCapacity = Double.parseDouble(splitLine[1]);
            }
            // Vehicle battery
            if (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] splitLine = line.split(" ");
                batteryCapacity = Double.parseDouble(splitLine[1]);
            }
            // h
            if (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] splitLine = line.split(" ");
                h = Double.parseDouble(splitLine[1]);
            }
            // Coordinates start
            if (scanner.hasNextLine()) {
                scanner.nextLine();
            }
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.equals("SECCION_DEMANDA")) {
                    break;
                }
                String[] splitLine = line.split(" ");
                int id = Integer.parseInt(splitLine[0]);
                double firstCoordinate = Integer.parseInt(splitLine[1]);
                double secondCoordinate = Double.parseDouble(splitLine[2]);
                this.coordinates.put(id, new Double[]{firstCoordinate, secondCoordinate});
            }
            // Demand
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.equals("ID_NODOS_ESTACIONES_CARGA")) {
                    break;
                }
                String[] splitLine = line.split(" ");
                int id = Integer.parseInt(splitLine[0]);
                double demand = Integer.parseInt(splitLine[1]);
                this.customersDemand.put(id, demand);
            }
            // Charge stations
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] splitLine = line.split(" ");
                int id = Integer.parseInt(splitLine[0]);
                this.chargeStations.add(id);
            }
            chargeStations.add(1);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public double getOptimumValue() {
        return optimumValue;
    }

    public double getH() {
        return h;
    }

    public double getCarryingCapacity() {
        return carryingCapacity;
    }

    public double getBatteryCapacity() {
        return batteryCapacity;
    }

    public int getNumberOfVehicles() {
        return numberOfVehicles;
    }

    public Set<Integer> getChargeStations() {
        return chargeStations;
    }

    public Double[] getCoordinates(int id) {
        return coordinates.get(id);
    }

    public double getDemand(int id) {
        return customersDemand.get(id);
    }

    public int getNumberOfCustomers() {
        return customersDemand.size();
    }

    public boolean isChargeStation(Integer id){
        return chargeStations.contains(id);
    }

    public double getDistance(int id1, int id2){
        Double[] coordinatesFirstPoint = getCoordinates(id1);
        Double[] coordinatesSecondPoint = getCoordinates(id2);
        return Math.sqrt(Math.pow(coordinatesFirstPoint[0] - coordinatesSecondPoint[0], 2) + Math.pow(coordinatesFirstPoint[1] - coordinatesSecondPoint[1], 2));
    }

    public double[] getDistancesFrom(int id) {
        double[] distances = new double[this.getNumberOfCustomers()];
        for (int i = 0; i < distances.length; i++) {
            distances[i] = this.getDistance(id, i);
        }
        return distances;
    }

    public double getBatteryConsumption(int id1, int id2) {
        return this.h * this.getDistance(id1, id2);
    }

    public int getNumberOfChargeStations() {
        return chargeStations.size();
    }

    public int getClosestChargeStation(int customer) {
        Integer closestChargeStation = -1;
        double bestDistance = Double.POSITIVE_INFINITY;
        for (Integer cs : this.chargeStations) {
            if (closestChargeStation == -1) {
                closestChargeStation = cs;
            }
            else {
                double distance = this.getDistance(customer, cs);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    closestChargeStation = cs;
                }
            }
        }
        return closestChargeStation;
    }

}
