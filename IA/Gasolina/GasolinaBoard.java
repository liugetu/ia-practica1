package IA.Gasolina;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.Collections;
import java.util.PriorityQueue;
import javafx.util.Pair;
import java.lang.Math;

public class GasolinaBoard {
    /* Class independent from AIMA classes
       - It has to implement the state of the problem and its operators
    */

    // Centres i gasolineres (fixes, poden ser estàtiques per estalviar memòria)
    static ArrayList<Distribucion> camions;         // coord. dels centres de distribució (si un centre te multiples camions, les seves coords. apareixen repetides)
    static ArrayList<Gasolinera> gasolineras;       // coord. i peticions de cada gasolinera

    static ArrayList<pair<Gasolinera, >>

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
            ArrayList<Integer> pet = gasolineras[rnd%(mida-1)].getPeticiones()
            if (pet)
            
        }

        return b;
    }

    public GasolinaBoard GasolinaBoardGreedy(ArrayList<Gasolinera> gasolineras) {
        GasolinaBoard board = new GasolinaBoard(camions, gasolineras);

        PriorityQueue<> peticions = new PriorityQueue<Pair<Pair<Integer, Integer>, Integer>(
            new Comparator<Pair<Pair<Integer, Integer>, Integer>> {
                public int comppare(Pair<Pair<Integer, Integer>, Integer> a, Pair<Pair<Integer, Integer>, Integer> b) {
                    if(1000 * ( 100 - Math.pow(2, a.value())) - 1000(100-Math.pow(2, a.value() + 1)) < 1000(100-Math.pow(2, b.value())) - 1000*(100-Math.pow(2, b.value() + 1))) return 1;
                    else return -1;
                }
            }
        );
        for(int i = 0; i < gasolinearas.size() and peticions.size() > 0; ++i) {
            boolean b = false;
            while(peticion.size() > 0 and not b) {
                int km;
                if(viajes[i].getKmRecorridos() == 0) {
                    km = Math.abs(camions[i].getCoordX() - peticions.peek().getKey().getKey()) + Math.abs(camions[i].getCoordY() - peticions.peek().getKey().getValue());
                }
                else km = Math.abs(viajes[i].getLastGasolinera().getCoordX() - peticions.peek().getKey().getKey()) + Math.abs(viajes[i].getLastGasolinera().getCoordY() - peticions.peek().getKey().getValue());
                if(viajes[i].getKmRecorridos() and km + viajes[i].getKmRecorridos() <= 640) {
                    viajes[i].addGasolinera(peticions.peek(), km);
                    peticions.poll();
                }
                else b = true;
            }
        }
        return board;
    }


   // Define the Viaje class
    class Viaje {
        int kmRecorridos = 0;
        List<Gasolinera> gasolineras; // del propi viatge

        // creadora
        public Viaje() {
            this.gasolineras = new ArrayList<>();
        }

        // afegir gasolinera
        public void addGasolinera(Gasolinera g, int km) {
            gasolineras.add(g);
            kmRecorridos += km;
        }

        // retorna el nombre de km recorreguts
        public int getKmRecorridos() {
            return kmRecorridos;
        }

        // treure una gasolinera del viatge
        public void removeGasolinera(Gasolinera g, int km) {
            gasolineras.remove(g);
            kmRecorridos -= km;
        }
    }
}