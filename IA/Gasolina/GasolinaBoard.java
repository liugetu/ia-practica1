package IA.Gasolina;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

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
    static Matrix<Integer> distancias;         // Per cada centro, distancia a totes les gasolineres (nms calcular si menos a màxim km d’un viaje)
    static Matrix<Map.Entry<Gasolinera, Integer>> distancia;       // Per cada centro, distancia a totes les gasolineres (nms calcular si menos a màxim km d’un viaje) ordenat de menor a major.
    
    // Assignació de peticions a viatges
    List<Viaje> viajes;  
    
    // Informació de control
    double beneficioActual; // V = 1000*p(d) - 2*2*d(c,g) - 1000*p(d+1)
    double costeTotalKm;
    

    /* Constructor */
    public GasolinaBoard(int []init) {
        
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

    // Define the Viaje class
    static class Viaje {
        int idCamio;
        List<Gasolinera> gasolineras; // del propi viatge

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

    // Initialize static variables
    static {
        camions = new ArrayList<>();
        gasolineras = new ArrayList<>();
        distancias = new Matrix<>(0, 0); // Placeholder dimensions
        distancia = new Matrix<>(0, 0); // Placeholder dimensions
    }
}