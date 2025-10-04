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
import java.util.PriorityQueue;
import java.util.Comparator;

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

    // informacio de cada gasolinera: array de que si cada peticio ha estat atesa
    static ArrayList<Pair<Gasolinera, boolean[]>> gasolineras_info;

    // Assignació de peticions a viatges per camió
    ArrayList<ArrayList<Viaje>> viajesPorCamion;  // index exterior = idCamio
    int[] kmsPorCamion;  // els kms que ha fet cada camio (total dels seus viatges)

    // Informació de control
    double beneficioActual; // V = 1000*p(d) - 2*2*d(c,g) - 1000*p(d+1)
    double costeTotalKm;
    
    // distancies precalculades de centre a gasolinera
    int[][] distCentroGas;

    /* Constructor */
    public GasolinaBoard(ArrayList<Distribucion> camions, ArrayList<Gasolinera> gasolineras) {
        this.camions = camions;
        this.gasolineras = gasolineras;

        this.viajesPorCamion = new ArrayList<>();
        for (int i = 0; i < camions.size(); i++) {
            // per cada camio inicialitzar amb zero viatges
            this.viajesPorCamion.add(new ArrayList<Viaje>());
        }

        this.beneficioActual = 0;
        this.costeTotalKm = 0;

        kmsPorCamion = new int[camions.size()];

        gasolineras_info = new ArrayList<>();
        if (this.gasolineras != null) {
            for (int i = 0; i < this.gasolineras.size(); i++) {
                Gasolinera g = this.gasolineras.get(i);
                int mida = g.getPeticiones().size();
                boolean[] flags = new boolean[mida];
                gasolineras_info.add(new Pair<>(g, flags));
            }
        }

        // precomputar distancies de cada centre de distribucio a cada gasolinera
        int nCentres = camions.size();
        int nGas = gasolineras.size();
        distCentroGas = new int[nCentres][nGas];
        for (int i = 0; i < nCentres; i++) {
            for (int g = 0; g < nGas; g++) {
                distCentroGas[i][g] = getDistancia(this.gasolineras.get(g), this.camions.get(i));
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

    // afegir una gasolinera a un nou viatge d'un camio
    public void addGasolineraAViaje(int igas, int km, int ipet, int icam) {
        Viaje nv = new Viaje();
        nv.addGasolinera(igas, km, ipet);
        ArrayList<Viaje> viajesCamion = viajesPorCamion.get(icam);
        viajesCamion.add(nv);
        kmsPorCamion[icam] += km;
    }
    
    // genera una solucio inicial random
    public GasolinaBoard solIniRandom() {
        int ngas = gasolineras.size();
        GasolinaBoard b = new GasolinaBoard(camions, gasolineras);

        // inicialitzar viatges per camio (buits)
        for (int i = 0; i < camions.size(); i++) b.viajesPorCamion.get(i).clear();

        // crear la llista de peticions pendents (parella de index gasolinera, index peticio)
        ArrayList<Pair<Integer, Integer>> petPnd = new ArrayList<>();
        for (int ig = 0; ig < gasolineras.size(); ig++) {
            ArrayList<Integer> pets = gasolineras.get(ig).getPeticiones();
            for (int ip = 0; ip < pets.size(); ip++) petPnd.add(new Pair<>(ig, ip));
        }

        // barrejar l'ordre de les peticions
        Random rnd = new Random();
        Collections.shuffle(petPnd, rnd);

        // intentar assignar cada peticio a un camio aleatoriament
        for (Pair<Integer, Integer> pet : petPnd) {
            int ig = pet.first;
            int ip = pet.second;
            boolean assignada = false;

            int camioIni = rnd.nextInt(Math.max(1, camions.size())); // comencar per un camio random

            // mirar tots els camions circularment a partir de camioIni mentre la peticio no estigui assignada
            for (int i = 0; i < camions.size() && !assignada; i++) {
                int c = (camioIni + i) % camions.size();
                ArrayList<Viaje> viajesAsignadas = viajesPorCamion.get(c);
                int kmCurrent = kmsPorCamion[c];
                int kmNew;

                if (viajesAsignadas.isEmpty()) {
                    kmNew = distCentroGas[ig][c];

                    if (kmNew + kmCurrent <= 640) {  // limit kms diari
                        addGasolineraAViaje(ig, kmNew, ip, c);
                        assignada = true;
                    }
                } 
                else {
                    // distancia entre la gasolinera de l'ultim viatge i la nova
                    Viaje lastViaje = viajesAsignadas.get(viajesAsignadas.size() - 1);
                    Gasolinera lastGas = lastViaje.getLastGasolinera();
                    kmNew = getDistancia(lastGas, gasolineras.get(ig));

                    if (kmNew + kmCurrent <= 640 && lastViaje.gasCount < 2) {
                        // afegir la parada al mateix viatge
                        lastViaje.addGasolinera(ig, kmNew, ip);
                        kmsPorCamion[c] += kmNew;
                        assignada = true;
                    } 
                    else if (viajesAsignadas.size() < 5 && kmNew + kmCurrent <= 640) {
                        // crear un nou viatge per aquest camio
                        addGasolineraAViaje(ig, kmNew, ip, c);
                        assignada = true;
                    }
                }
            }
            // si no assignada, no s'aten aquesta peticio
        }

        return b;
    }

    /* la que proposa el copilot:
    public GasolinaBoard solIniGreedy(ArrayList<Gasolinera> gasolineras) {
        GasolinaBoard board = new GasolinaBoard(camions, gasolineras);

        PriorityQueue<Pair<Pair<Integer, Integer>, Integer>> peticions = new PriorityQueue<Pair<Pair<Integer, Integer>, Integer>>(
            new Comparator<Pair<Pair<Integer, Integer>, Integer>>() {
                @Override
                public int compare(Pair<Pair<Integer, Integer>, Integer> a, Pair<Pair<Integer, Integer>, Integer> b) {
                    if((1000 * ( 100 - Math.pow(2, a.second)) - 1000 * (100 - Math.pow(2, a.second + 1))) < (1000 * (100 - Math.pow(2, b.second)) - 1000 * (100 - Math.pow(2, b.second + 1)))) return 1;
                    else return -1;
                }
            }
        );

        // initialize queue with gasolineras that have petitions
        for (int gi = 0; gi < gasolineras.size(); gi++) {
            if (gasolineras.get(gi).getPeticiones() != null && gasolineras.get(gi).getPeticiones().size() > 0) {
                Gasolinera g = gasolineras.get(gi);
                peticions.add(new Pair<>(new Pair<>(g.getCoordX(), g.getCoordY()), gi));
            }
        }

        // For each camio try to serve petitions while possible
        for (int i = 0; i < camions.size() && peticions.size() > 0; ++i) {
            boolean stop = false;
            ArrayList<Viaje> trips = viajesPorCamion.get(i);
            while (peticions.size() > 0 && !stop) {
                Pair<Pair<Integer, Integer>, Integer> top = peticions.peek();
                int idxGasTop = top.second;

                int kmCurrent = kmsPorCamion[i];
                int km;

                if (trips.isEmpty()) {
                    // distance center->gas
                    km = (distCentroGas != null && idxGasTop >= 0 && idxGasTop < distCentroGas[i].length)
                        ? distCentroGas[i][idxGasTop]
                        : getDistancia(gasolineras.get(idxGasTop), camions.get(i));
                    if (km + kmCurrent <= 640) {
                        Viaje nv = new Viaje();
                        // use petition index 0 (first) for minimal change
                        nv.addGasolinera(idxGasTop, km, 0);
                        trips.add(nv);
                        kmsPorCamion[i] += km;
                        peticions.poll();
                    } else stop = true;
                } else {
                    Viaje last = trips.get(trips.size() - 1);
                    Gasolinera lastG = last.getLastGasolinera();
                    if (lastG == null) {
                        km = (distCentroGas != null && idxGasTop >= 0 && idxGasTop < distCentroGas[i].length)
                            ? distCentroGas[i][idxGasTop]
                            : getDistancia(gasolineras.get(idxGasTop), camions.get(i));
                    } else {
                        km = getDistancia(lastG, gasolineras.get(idxGasTop));
                    }

                    if (km + kmCurrent <= 640 && last.gasCount < 2) {
                        last.addGasolinera(idxGasTop, km, 0);
                        kmsPorCamion[i] += km;
                        peticions.poll();
                    } else {
                        if (trips.size() < 5 && kmCurrent + km <= 640) {
                            Viaje nv = new Viaje();
                            nv.addGasolinera(idxGasTop, km, 0);
                            trips.add(nv);
                            kmsPorCamion[i] += km;
                            peticions.poll();
                        } else stop = true;
                    }
                }
            }
        }
        return board;
    }*/

    // helpers per viajesPorCamion

    public int countViajesCamion(int idCamio) {
        return viajesPorCamion.get(idCamio).size();
    }

    public int kmsCamio(int idCamio) {
        int suma = 0;
        for (Viaje v : viajesPorCamion.get(idCamio)) suma += v.getKmRecorridos();
        return suma;
    }

    class Viaje {
        int kmRecorridos;
        int[] gasVisitadas = new int[2];
        int gasCount; // nombre de gasolines visitades

        // creadora
        public Viaje() {
            kmRecorridos = 0;
            gasVisitadas[0] = gasVisitadas[1] = -1; // -1 = buit
            gasCount = 0;
        }

        // afegir gasolinera
        // no oblidar fer kmsPorCamion[idCamio] += km; !!
        public boolean addGasolinera(int g, int km, int ipeticion) {
            if (gasCount >= 2) return false;
            gasVisitadas[gasCount++] = g;
            gasolineras_info.get(g).second[ipeticion] = true;
            kmRecorridos += km;
            return true;
        }

        // retorna el nombre de km recorreguts
        public int getKmRecorridos() {
            return kmRecorridos;
        }

        // retornar l'ultima gasolinera (null si no n'hi ha)
        public Gasolinera getLastGasolinera() {
            if (gasCount == 0) return null;
            int idx = gasVisitadas[gasCount - 1];
            return GasolinaBoard.gasolineras.get(idx);
        }

        // treure una gasolinera del viatge
        // no oblidar fer kmsPorCamion[idCamio] -= km; !!
        public void removeGasolinera(int g, int km, int ipeticion) {
            int pos = -1;
            for (int i = 0; i < gasCount; i++) {
                if (gasVisitadas[i] == g) { 
                    pos = i; 
                    break; 
                }
            }
            if (pos >= 0) {  // trobat
                for (int j = pos; j < gasCount - 1; j++) gasVisitadas[j] = gasVisitadas[j+1];
                gasVisitadas[gasCount - 1] = -1;
                gasCount--;
                gasolineras_info.get(g).second[ipeticion] = false;
                kmRecorridos -= km;
                if (kmRecorridos < 0) kmRecorridos = 0;
            }
        }
    }
}