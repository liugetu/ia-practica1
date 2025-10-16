import IA.Gasolina.GasolinaBoard;
import IA.Gasolina.GasolinaGoalTest;
import IA.Gasolina.GasolinaHeuristicFunction;
import IA.Gasolina.GasolinaSuccessorFunction;
import IA.Gasolina.CentrosDistribucion;
import IA.Gasolina.Gasolineras;
import IA.Gasolina.GasolinaSuccessorFunctionSA;

import aima.search.framework.GraphSearch;
import aima.search.framework.Problem;
import aima.search.framework.Search;
import aima.search.framework.SearchAgent;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.ArrayList;
import aima.search.informed.SimulatedAnnealingSearch;
import aima.search.informed.HillClimbingSearch;

public class Main {
    public static void main(String[] args) throws Exception{
        // generar estat inicial (aleatori/greedy)
        // definir un estat final?????? (en busqueda local fa falta?)

        // inicialitzar el problema
        int ngas = 50;
        int ncen = 5, mult = 1;
        GasolinaBoard board = new GasolinaBoard(new CentrosDistribucion(ncen, mult, 3), new Gasolineras(ngas, 5));

        System.out.println("Camions: " + board.getNCamions() + ", Gasolineres: " + board.getNGasolineras());

        // Pick an initial solution for local search
        //GasolinaBoard initial = board.solIniRandom(); 
        GasolinaBoard initial = board.solIniGreedy();
        System.out.println("Hem fet l'inicialitzacio");

        // Create the Problem object using the chosen initial state
        Problem p = new Problem(initial,
                                new GasolinaSuccessorFunction(), // or new GasolinaSuccessorFunctionSA()
                                new GasolinaGoalTest(),
                                new GasolinaHeuristicFunction());
        System.out.println("Hem fet el problema");

        //Hill Climbing search (for SA, instantiate SimulatedAnnealingSearch instead)
        HillClimbingSearch alg = new HillClimbingSearch();

        /*
        2000: número máximo de iteraciones
        100: temperatura inicial
        5: valor de k (parámetro de enfriamiento)
        0.001: lambda (tasa de enfriamiento)
        */
        //SimulatedAnnealingSearch alg = new SimulatedAnnealingSearch(2000, 100, 5, 0.001);
        System.out.println("Hem fet el HC");

        // Run the SearchAgent
        SearchAgent agent = new SearchAgent(p, alg);
        System.out.println("Hem fet el agent");

        // Print the results of the search
        System.out.println();
        printActions(agent.getActions());
        printInstrumentation(agent.getInstrumentation());

        // Final state
        GasolinaBoard goal = (GasolinaBoard) alg.getGoalState();
        System.out.println("Final benefit: " + goal.getBeneficio() + ", km: " + goal.getKm());
    }

    private static void printInstrumentation(Properties properties) {
        Iterator keys = properties.keySet().iterator();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            String property = properties.getProperty(key);
            System.out.println(key + " : " + property);
        }
    }
    
    private static void printActions(List actions) {
        for (int i = 0; i < actions.size(); i++) {
            String action = (String) actions.get(i);
            System.out.println(action);
        }
    }
}