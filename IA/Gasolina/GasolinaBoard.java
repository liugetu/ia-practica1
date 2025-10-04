package IA.Gasolina;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.Collections;

public class GasolinaBoard {
    /* Class independent from AIMA classes
       - It has to implement the state of the problem and its operators
     *

    /* State data structure
        vector with the parity of the coins (we can assume 0 = heads, 1 = tails
     */

    // declarar un board
    //private int [] board;
    //private static int [] solution;


    // Centres i gasolineres (fixes, poden ser estàtiques per estalviar memòria)
    static List<Distribucion> camions;         // coord. dels centres de distribució (si un centre te multiples camions, les seves coords. apareixen repetides)
    static List<Gasolinera> gasolineras;       // coord. i peticions de cada gasolinera
    
    // Assignació de peticions a viatges
    List<Viaje> viajes;  
    
    // Informació de control
    double beneficioActual; // V = 1000*p(d) - 2*2*d(c,g) - 1000*p(d+1)
    double costeTotalKm;
    

    /* Constructor */
    public GasolinaBoard(int []init) {
        this(); // delegate to no-arg constructor
        // optionally use init to set up board/configuration later
    }

    // no-arg constructor to initialize collections and control variables
    public GasolinaBoard() {
        viajes = new ArrayList<>();
        beneficioActual = 0;
        costeTotalKm = 0;
    }

    /* Operadors */
    /*public void flip_it(int i){
        // flip the coins i and i + 1
        board[i] = board[i]^1;  // xor
        if (i <= board.length - 2) board[i+1] = board[i+1]^1;
        else board[0] = board[0]^1;  // start from the beginning
    }*/

    /* Heuristic function */
    public double heuristic(){
        return 0;
    }

    /* Goal test */
    public boolean is_goal(){
        return false;
    }

    // Some functions will be needed for creating a copy of the state

    public int [] getConfiguration() {
        //return board;
        return new int[0];
    }

    // Define the Distribucion class
    static class Distribucion {
        // Example attributes for a distribution center
        int id;
        double x, y;

        public Distribucion(int id, double x, double y) {
            this.id = id;
            this.x = x;
            this.y = y;
        }
    }

    // Define the Gasolinera class
    static class Gasolinera {
        int id;
        double x, y;
        int peticions;

        public Gasolinera(int id, double x, double y, int peticions) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.peticions = peticions;
        }
    }

    // Define the Matrix class
    static class Matrix<T> {
        private List<List<T>> data;

        public Matrix(int rows, int cols) {
            data = new ArrayList<>();
            for (int i = 0; i < rows; i++) {
                List<T> row = new ArrayList<>();
                for (int j = 0; j < cols; j++) {
                    row.add(null);
                }
                data.add(row);
            }
        }

        public void set(int row, int col, T value) {
            data.get(row).set(col, value);
        }

        public T get(int row, int col) {
            return data.get(row).get(col);
        }
    }

    // Initialize static variables
    static {
        camions = new ArrayList<>();
        gasolineras = new ArrayList<>();
    }

    // Generates a random initial solution for hill-climbing.
    // Strategy: create one Viaje per camion (if any), shuffle gasolineras and assign each randomly to a camion's viaje.
    public static GasolinaBoard randomInitialSolution() {
        Random rnd = new Random();
        GasolinaBoard b = new GasolinaBoard();

        if (camions == null || camions.isEmpty()) {
            // If there are no camions defined, create a single viaje and assign all gasolineras to it.
            b.viajes.add(new Viaje(0));
            if (gasolineras != null && !gasolineras.isEmpty()) {
                List<Gasolinera> pool = new ArrayList<>(gasolineras);
                Collections.shuffle(pool, rnd);
                for (Gasolinera g : pool) b.viajes.get(0).addGasolinera(g);
            }
            return b;
        }

        int numCamions = camions.size();
        for (int i = 0; i < numCamions; i++) b.viajes.add(new Viaje(i));

        if (gasolineras != null && !gasolineras.isEmpty()) {
            List<Gasolinera> pool = new ArrayList<>(gasolineras);
            Collections.shuffle(pool, rnd);
            for (Gasolinera g : pool) {
                int r = rnd.nextInt(numCamions);
                b.viajes.get(r).addGasolinera(g);
            }
        }

        return b;
    }
}