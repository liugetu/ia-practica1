package IA.Gasolina;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.Collections;
import java.util.PriorityQueue;
//import javafx.util.Pair;
import java.lang.Math;

// Simple Pair class
class Pair<T, U> {
    public final T first;
    public final U second;

    public Pair(T first, U second) {
        this.first = first;
        this.second = second;
    }
}

public class GasolinaBoard {
    /* Class independent from AIMA classes
       - It has to implement the state of the problem and its operators
    */

    // Centres i gasolineres (fixes, poden ser estàtiques per estalviar memòria)
    static ArrayList<Distribucion> camions;         // coord. dels centres de distribució (si un centre te multiples camions, les seves coords. apareixen repetides)
    static ArrayList<Gasolinera> gasolineras;       // coord. i peticions de cada gasolinera

    static ArrayList<Pair<Gasolinera, ArrayList<Boolean>>> gasolineras_info;

    // Assignació de peticions a viatges
    ArrayList<Viaje> viajes;  

    // Informació de control
    double beneficioActual; // V = 1000*p(d) - 2*2*d(c,g) - 1000*p(d+1)
    double costeTotalKm;

    /* Constructor */
    public GasolinaBoard(ArrayList<Distribucion> camions, ArrayList<Gasolinera> gasolineras) {
        this.camions = camions;
        this.gasolineras = gasolineras;
        this.viajes = new ArrayList<>();
        this.beneficioActual = 0;
        this.costeTotalKm = 0;

        // initialize gasolineras_info properly using ArrayList API
        gasolineras_info = new ArrayList<>();
        if (this.gasolineras != null) {
            for (int i = 0; i < this.gasolineras.size(); i++) {
                Gasolinera g = this.gasolineras.get(i);
                int mida = g.getPeticiones().size();
                ArrayList<Boolean> flags = new ArrayList<>(Collections.nCopies(mida, false));
                gasolineras_info.add(new Pair<>(g, flags));
            }
        }
    }

    // no-arg constructor for helper/factory methods
    /*public GasolinaBoard() {
        this.viajes = new ArrayList<>();
        this.beneficioActual = 0;
        this.costeTotalKm = 0;
    }*/

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

    public double getKm() {
        return costeTotalKm;
    }

    // Some functions will be needed for creating a copy of the state

    public int [] getConfiguration() {
        //return board;
        return new int[0];
    }

    public int getDistancia(Gasolinera g, Distribucion d) {
        int x = Math.abs(g.getCoordX() - d.getCoordX());
        int y = Math.abs(g.getCoordY() - d.getCoordY());
        return x + y;
    }

    public int getDistancia(Gasolinera g1, Gasolinera g2) {
        int x = Math.abs(g1.getCoordX() - g2.getCoordX());
        int y = Math.abs(g1.getCoordY() - g2.getCoordY());
        return x + y;
    }
    
    // genera una solucio inicial random
    // Strategy: create one Viaje per camion (if any), shuffle gasolineras and assign each randomly to a camion's viaje.
    public GasolinaBoard randomInitialSolution() {
        int mida = gasolineras.size();
        GasolinaBoard b = new GasolinaBoard(camions, gasolineras);

        // inicialitzar viatges de cada camio
        for (int i = 0; i < camions.size(); i++) b.viajes.add(new Viaje());

        // recorrer todas las gasolineras
        for (int i = 0; i < mida; i++) {
            Random rnd = new Random();
            //ArrayList<Integer> pet = gasolineras[rnd%(mida-1)].getPeticiones()
            //if (pet)
            
        }

        return b;
    }

    

   // Define the Viaje class
    class Viaje {
        int kmRecorridos = 0;
        ArrayList<Integer> gasolineras; // del propi viatge

        // creadora
        public Viaje() {
            this.gasolineras = new ArrayList<>();
        }

        // afegir gasolinera
        public void addGasolinera(int g, int km, int ipeticion) {
            gasolineras.add(g);
            //(gasolineras_info[g].second)[ipeticion] = true; 
            gasolineras_info.get(g).second.set(ipeticion, true);
            kmRecorridos += km;
        }

        // retorna el nombre de km recorreguts
        public int getKmRecorridos() {
            return kmRecorridos;
        }

        // treure una gasolinera del viatge
        public void removeGasolinera(int g, int km, int ipeticion) {
            gasolineras.remove(g);
            gasolineras_info.get(g).second.set(ipeticion, false);
            kmRecorridos -= km;
        }
    }
}