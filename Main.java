import IA.Gasolina.GasolinaBoard;
//import IA.ProbIA5.ProbIA5GoalTest;
//import IA.ProbIA5.ProbIA5HeuristicFunction;
//import IA.ProbIA5.ProbIA5SuccesorFunction;
import aima.search.framework.GraphSearch;
import aima.search.framework.Problem;
import aima.search.framework.Search;
import aima.search.framework.SearchAgent;
import aima.search.informed.AStarSearch;
import aima.search.informed.IterativeDeepeningAStarSearch;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.ArrayList;
import IA.Gasolina.Gasolinera;

public class Main {
    public static void main(String[] args) throws Exception{
        // generar estat inicial (aleatori/greedy)
        // definir un estat final?????? (en busqueda local fa falta?)

        // inicialitzar el problema.
        // ProbIA5Board board = new ProbIA5Board(prob, sol );

        // Create the Problem object (canviar noms)
        /*Problem p = new  Problem(board,
                                new ProbIA5SuccesorFunction(),
                                new ProbIA5GoalTest(),
                                new ProbIA5HeuristicFunction());  */

        // Instantiate the search algorithm
        // hill climbing / simulated annealing
	    // AStarSearch(new GraphSearch()) or IterativeDeepeningAStarSearch()
        //Search alg = new AStarSearch(new GraphSearch());

        // Instantiate the SearchAgent object
        //SearchAgent agent = new SearchAgent(p, alg);

	    // We print the results of the search
        //System.out.println();
        //printActions(agent.getActions());
        //printInstrumentation(agent.getInstrumentation());

        // You can access also to the goal state using the method getGoalState of class Search
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

    // Define the Viaje class
    static class Viaje {
        int idCamio;
        ArrayList<Gasolinera> gasolineras; // del propi viatge

        // creadora
        public Viaje(int idCamio) {
            this.idCamio = idCamio;
            this.gasolineras = new ArrayList<>();
        }

        // afegir gasolinera
        public void addGasolinera(Gasolinera g) {
            gasolineras.add(g);
        }
    }
}