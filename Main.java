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
        // verificar arguments
        if (args.length != 2) {
            System.out.println("Error: es requereixen 2 arguments (0 o 1).");
            System.out.println("Exemple d'ús: java -cp .:AIMA.jar:Gasolina.jar Main 1 0");
            return;
        }
        int a = Integer.parseInt(args[0]);
        int b = Integer.parseInt(args[1]);

        // inicialitzar el problema
        int ngas = 50;
        int ncen = 5, mult = 1;
        GasolinaBoard board = new GasolinaBoard(new CentrosDistribucion(ncen, mult, 3), new Gasolineras(ngas, 5));

        System.out.println("Camions: " + board.getNCamions() + ", Gasolineres: " + board.getNGasolineras());

        String inicialitzacio;
        GasolinaBoard initial;
        if (a == 0) { // generar estat inicial aleatori
            initial = board.solIniRandom(); 
            inicialitzacio = "random";
        }
        else if (a == 1) { // generar estat inicial greedy 1
            initial = board.solIniGreedy();
            inicialitzacio = "greedy1";
        }
        else if (a == 2){ // greedy 2
            initial = board.solIniGreedy2();
            inicialitzacio = "greedy2";
        }
        else {  // sol ini "buida"
            initial = board;
            inicialitzacio = "buida";
        }
        System.out.println("Hem fet la inicialitzacio "+inicialitzacio);

        if (b == 0) {     // hill climbing
            Problem p = new Problem(initial,
                                    new GasolinaSuccessorFunction(),
                                    new GasolinaGoalTest(),
                                    new GasolinaHeuristicFunction());

            HillClimbingSearch alg = new HillClimbingSearch();
            System.out.println("Hem fet el HC");

            SearchAgent agent = new SearchAgent(p, alg);

            System.out.println();
            printActions(agent.getActions());
            printInstrumentation(agent.getInstrumentation());

            // estat final
            GasolinaBoard goal = (GasolinaBoard) alg.getGoalState();
            goal.printEstatComplet();
            System.out.println("Final benefit: " + goal.getBeneficio() + ", km: " + goal.getKm());
            System.out.println("Final benefit avui: " + goal.getBeneficiAvui());
            System.out.println("Fet amb HC i inicialitzacio "+inicialitzacio);
        }
        else { // simulated annealing
            Problem p = new Problem(initial,
                                    new GasolinaSuccessorFunctionSA(),
                                    new GasolinaGoalTest(),
                                    new GasolinaHeuristicFunction());

            // SA: param: nº max d'iteracions, temp ini, k, lambda
            SimulatedAnnealingSearch alg = new SimulatedAnnealingSearch(100000, 1000, 5, 0.01);
            System.out.println("Hem fet el SA");

            SearchAgent agent = new SearchAgent(p, alg);

            System.out.println();
            printActions(agent.getActions());
            printInstrumentation(agent.getInstrumentation());

            // estat final
            GasolinaBoard goal = (GasolinaBoard) alg.getGoalState();
            goal.printEstatComplet();
            System.out.println("Final benefit: " + goal.getBeneficio() + ", km: " + goal.getKm());
            System.out.println("Final benefit avui: " + goal.getBeneficiAvui());
            System.out.println("Fet amb SA i inicialitzacio "+inicialitzacio);
        }
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
        if (actions == null || actions.isEmpty()) {
            System.out.println("No actions to print (local search doesn't track action sequence)");
            return;
        }
        for (int i = 0; i < actions.size(); i++) {
            Object action = actions.get(i);
            if (action instanceof String) {
                System.out.println(action);
            } else {
                System.out.println("Action " + i + ": " + action.getClass().getSimpleName());
            }
        }
    }
}