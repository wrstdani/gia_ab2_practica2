package es.urjc.grafo.ABII.Model;

import java.util.HashSet;
import java.util.Set;

public class Evaluator {

    public static double evaluate(Solution solution, Instance instance) {
        double score = 0;
        for (int i = 0; i < solution.routes().length; i++) {
            for (int j = 1; j < solution.routes()[i].size(); j++) {
                score += instance.getDistance(solution.routes()[i].get(j-1), solution.routes()[i].get(j));
            }
        }
        return score;
    }

    public static boolean isFeasible(Solution solution, Instance instance) {
        if (solution.routes().length != instance.getNumberOfVehicles()) {
            return false;
        }
        // Each client is visited only once
        Set<Integer> visitedCustomers = new HashSet<>(instance.getNumberOfCustomers());
        for (int i = 0; i < solution.routes().length; i++) {
            for (int j = 0; j < solution.routes()[i].size(); j++) {
                int client = solution.routes()[i].get(j);
                if (client != 1 && !instance.isChargeStation(client)) {
                    if (visitedCustomers.contains(client)) {
                        return false;
                    }
                    visitedCustomers.add(client);
                }
            }
        }


        // Each client is visited
        if (visitedCustomers.size() != instance.getNumberOfCustomers() - 1) return false;

        // Routes start and end at base
        for (int i = 0; i < solution.routes().length; i++) {
            if (solution.routes()[i].getFirst() != 1 || solution.routes()[i].getLast() != 1) {
                return false;
            }
        }

        // For each route, total demand does not exceed capacity of vehicles
        for (int i = 0; i < solution.routes().length; i++) {
            double totalDemand = 0;
            for (int j = 0; j < solution.routes()[i].size(); j++) {
                totalDemand += instance.isChargeStation(solution.routes()[i].get(j)) ? 0 : instance.getDemand(solution.routes()[i].get(j));
            }
            if (totalDemand > instance.getCarryingCapacity()) {
                return false;
            }
        }

        // Battery is never consumed completely
        for (int i = 0; i < solution.routes().length; i++) {
            double battery = instance.getBatteryCapacity();
            for (int j = 0; j < solution.routes()[i].size(); j++) {
                int client = solution.routes()[i].get(j);
                if (client == 1 || instance.isChargeStation(client)) {
                    battery = instance.getBatteryCapacity();
                }
                else {
                    battery -= instance.getH() * instance.getDistance(solution.routes()[i].get(j-1), client);
                }
                if (battery < 0) {
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean isBetter(Solution solution1, Solution solution2, Instance instance) {
        if (solution1 == null) return false;
        else if (solution2 == null) return true;
        return evaluate(solution1, instance) < evaluate(solution2, instance);
    }
}
