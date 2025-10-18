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
import java.util.Random;
import aima.search.informed.SimulatedAnnealingSearch;
import aima.search.informed.HillClimbingSearch;

public class Main {
    
    public static void main(String[] args) throws Exception{
        // verificar arguments
        if (args.length != 3) {
            System.out.println("Error: es requereixen 3 arguments (0 o 1).");
            System.out.println("Exemple d'ús: java -cp .:AIMA.jar:Gasolina.jar Main 1 0 10");
            return;
        }
        int a = Integer.parseInt(args[0]);
        int b = Integer.parseInt(args[1]);
        int NUMERO_EJECUCIONES = Integer.parseInt(args[2]);
        
        // inicialitzar el problema
        int ngas = 500;
        int ncen = 50, mult = 1;
        //System.out.println("Camions: " + board.getNCamions() + ", Gasolineres: " + board.getNGasolineras());
        //System.out.println("Executant " + NUMERO_EJECUCIONES + " vegades per trobar el millor resultat...\n");

        String inicialitzacio;
        if (a == 0) { 
            inicialitzacio = "random";
        }
        else if (a == 1) { 
            inicialitzacio = "greedy1";
        }
        else if (a == 2){ 
            inicialitzacio = "greedy2";
        }
        else {  
            inicialitzacio = "buida";
        }

        // Variables para almacenar el mejor resultado
        GasolinaBoard mejorResultado = null;
        double mejorBeneficio = Double.NEGATIVE_INFINITY;
        int mejorEjecucion = -1;
        List<Object> mejoresAcciones = null;
        Properties mejorInstrumentacion = null;

        // Ejecutar el algoritmo múltiples veces
        for (int ejecucion = 1; ejecucion <= NUMERO_EJECUCIONES; ejecucion++) {
            Random myRandom = new Random();
            int seed1 = myRandom.nextInt(1234);
            int seed2 = myRandom.nextInt(1234);
            GasolinaBoard board = new GasolinaBoard(new CentrosDistribucion(ncen, mult, seed1), new Gasolineras(ngas, seed2));

            System.out.println("=== Execució " + ejecucion + " ===");
            
            // Generar estado inicial para esta ejecución
            GasolinaBoard initial;
            if (a == 0) { // generar estat inicial aleatori
                initial = board.solIniRandom(); 
            }
            else if (a == 1) { // generar estat inicial greedy 1
                initial = board.solIniGreedy();
            }
            else if (a == 2){ // greedy 2
                initial = board.solIniGreedy2();
            }
            else {  // sol ini "buida"
                initial = board;
            }

            GasolinaBoard resultado = null;
            List<Object> acciones = null;
            Properties instrumentacion = null;

            if (b == 0) {     // hill climbing
                Problem p = new Problem(initial,
                                        new GasolinaSuccessorFunction(),
                                        new GasolinaGoalTest(),
                                        new GasolinaHeuristicFunction());

                HillClimbingSearch alg = new HillClimbingSearch();
                SearchAgent agent = new SearchAgent(p, alg);
                
                resultado = (GasolinaBoard) alg.getGoalState();
                acciones = agent.getActions();
                instrumentacion = agent.getInstrumentation();
            }
            else { // simulated annealing
                Problem p = new Problem(initial,
                                        new GasolinaSuccessorFunctionSA(),
                                        new GasolinaGoalTest(),
                                        new GasolinaHeuristicFunction());

                // SA: param: nº max d'iteracions, temp ini, k, lambda
                SimulatedAnnealingSearch alg = new SimulatedAnnealingSearch(100000, 1000, 5, 0.01);
                SearchAgent agent = new SearchAgent(p, alg);
                
                resultado = (GasolinaBoard) alg.getGoalState();
                acciones = agent.getActions();
                instrumentacion = agent.getInstrumentation();
            }

            // Evaluar si este resultado es mejor que el anterior
            double beneficioActual = resultado.getBeneficiAvui();
            System.out.println("Benefici: " + beneficioActual + ", km: " + resultado.getKm());
            
            if (beneficioActual > mejorBeneficio) {
                mejorBeneficio = beneficioActual;
                mejorResultado = resultado;
                mejorEjecucion = ejecucion;
                mejoresAcciones = acciones;
                mejorInstrumentacion = instrumentacion;
                System.out.println("*** NOU MILLOR RESULTAT! ***");
            }
            
            System.out.println();
        }
        
        String algoritmo = (b == 0) ? "HC" : "SA";
        System.out.println("Fet amb " + algoritmo + " i inicialitzacio " + inicialitzacio);
        System.out.println();
        
        System.out.println("Accions del millor resultat:");
        printActions(mejoresAcciones);
        System.out.println();
        
        System.out.println("Instrumentació del millor resultat:");
        printInstrumentation(mejorInstrumentacion);

        // Mostrar el mejor resultado encontrado
        System.out.println("===============================================");
        System.out.println("MILLOR RESULTAT TROBAT:");
        System.out.println("Execució: " + mejorEjecucion + " de " + NUMERO_EJECUCIONES);
        System.out.println("Final benefit: " + mejorResultado.getBeneficiAvui() + ", km: " + mejorResultado.getKm());
        
        //mejorResultado.printEstatComplet();
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