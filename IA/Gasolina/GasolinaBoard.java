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
import IA.Gasolina.Distribucion;
import IA.Gasolina.Gasolinera;

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
    /* 
     * PRE: la peticio no ha estat atesa encara, el viatge iviaje del camio icam existeix i te menys de 2 gasolineres
     * POST: Afegir peticio ipet de la gasolinera igas al viatge iviaje del camio icam
    */
    public void addPeticio(int igas, int ipet, int icam, int iviaje) {
        Viaje v = viajesPorCamion.get(icam).get(iviaje);
        Gasolinera g = gasolineras.get(igas);
        int km;
        if (v.gasCount == 0) {
            km = 2*getDistancia(g, camions.get(icam));
        } 
        else {
            // distancia de l'ultima gasolinera del viatge a la nova
            Gasolinera lastGas = v.getLastGasolinera();
            km = getDistancia(g, camions.get(icam)) + getDistancia(g, lastGas) + getDistancia(lastGas, camions.get(icam));
        }
        v.addGasolinera(igas, km, ipet);
        kmsPorCamion[icam] += km;
        costeTotalKm += km; /////////////////////////// VARIABLE "DIFICIL" DE CALCULAR: TENIM EN COMPTE LA TORNADA O NO? //////////////////////////////
        beneficioActual += 1000 * (100 - Math.pow(2, (double)g.getPeticiones().get(ipet))) - 4*km - (1000 * (100 - Math.pow(2, (double)g.getPeticiones().get(ipet))) - 1000 * (100 - Math.pow(2, (double)g.getPeticiones().get(ipet) + 1)));
    }

    /* 
     * PRE: el viatge iviaje del camio icam existeix i te exactament 2 gasolineres
     * POST: Reordenar les gasolineres del viatge iviaje del camio icam
    */
    public void reordenarViatje(int icam, int iviaje) {
        Viaje v = viajesPorCamion.get(icam).get(iviaje);
        // Calcular distancies actuals per restar-les
        int kmActuals = v.kmRecorridos;

        // Fer el swap
        int temp = v.gasVisitadas[0];
        v.gasVisitadas[0] = v.gasVisitadas[1];
        v.gasVisitadas[1] = temp;
        
        // Recalcular distancies amb el nou ordre
        Gasolinera gas1 = gasolineras.get(v.gasVisitadas[0]);
        Gasolinera gas2 = gasolineras.get(v.gasVisitadas[1]);
        
        int kmNuevos = getDistancia(gas1, camions.get(icam)) + getDistancia(gas1, gas2) + getDistancia(gas2, camions.get(icam));
        
        // Actualizar estado
        v.kmRecorridos = kmNuevos;
        kmsPorCamion[icam] = kmsPorCamion[icam] - kmActuals + kmNuevos;
        costeTotalKm = costeTotalKm - kmActuals + kmNuevos;
    }

    // metode auxiliar per treure la gasolinera igas del viatge v del camio icam
    // NO actualitza els beneficis!
    public void removeGasolineraViaje(Viaje v, int icam, int igas, int ipet) {
        int kmOriginal1 = v.getKmRecorridos();
        if (!v.removeGasolinera(igas, ipet)) return; // error!

        int kmEliminats;
        if (v.getNGasolineras() == 0) {  
            // v no te mes gasolineres a visitar, es treu tot: km = anada + tornada d'abans
            kmEliminats = kmOriginal1; //distancia que tenia
        }
        else {  // li queda 1 gasolinera: km = (kms que tenia) - (kms d'ara amb 1 viatge)
            int kmActuals = 2*distCentroGas[icam][igas];
            kmEliminats = kmOriginal1 - kmActuals;
        }
        v.sumaKm(-kmEliminats);            // actualitzar kms que ha fet el camio 1 en aquest viatge
        kmsPorCamion[icam] -= kmEliminats; // actualitzar kms que ha fet el camio 1 en total
        costeTotalKm -= kmEliminats;       // actualitzar kms totals
    }

    // reassignar una peticio d'un viatge a un altre viatge iviaje del camio icam
    public void reasignar(int icam1, int iviaje1, int igas1, int ipet1, int icam2, int iviaje2) {
        // comprovar que la gasolinera igas1 conte la peticio ipet1
        if ((gasolineras_info.get(igas1).second).length > ipet1) return;

        Viaje v1 = viajesPorCamion.get(icam1).get(iviaje1);
        Viaje v2 = viajesPorCamion.get(icam2).get(iviaje2);

        // eliminar la peticio del viaje original
        removeGasolineraViaje(v1, icam1, igas1, ipet1);
        // OJO: aqui no fa falta modificar beneficis pq la peticio nomes es transfereix     

        // afegir la peticio al nou viatge
        
        // pnd
    }

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
                    kmNew = 2*distCentroGas[ig][c]; // anada i tornada!

                    if (kmNew + kmCurrent <= limitKmCamioDiari) {  // limit kms diari
                        addGasolineraAViaje(ig, kmNew, ip, c);
                        assignada = true;
                    }
                } 
                else {
                    // distancia entre la gasolinera de l'ultim viatge i la nova
                    Viaje lastViaje = viajesAsignadas.get(viajesAsignadas.size() - 1);
                    int lastIdx = lastViaje.getIndexLastGas();
                    kmNew = distGasGas[lastIdx][ig];

                    if (kmNew + kmCurrent <= limitKmCamioDiari && lastViaje.gasCount < 2) {
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

    public GasolinaBoard GasolinaBoardGreedy(ArrayList<Gasolinera> gasolineras) {
        GasolinaBoard board = new GasolinaBoard(camions, gasolineras);

        // inicialitzar viatges per camio (buits)
        for (int i = 0; i < camions.size(); i++) board.viajesPorCamion.get(i).clear();

        PriorityQueue<Pair<Pair<Integer, Integer>, Integer>> peticions = new PriorityQueue<Pair<Pair<Integer, Integer>, Integer>>(
            new Comparator<Pair<Pair<Integer, Integer>, Integer>>() {
                @Override
                public int compare(Pair<Pair<Integer, Integer>, Integer> a, Pair<Pair<Integer, Integer>, Integer> b) {
                    if((1000 * ( 100 - Math.pow(2, a.second)) - 1000 * (100 - Math.pow(2, a.second + 1))) < (1000 * (100 - Math.pow(2, b.second)) - 1000 * (100 - Math.pow(2, b.second + 1)))) return 1;
                    else return -1;
                }
            }
        );

        for (int ig = 0; ig < gasolineras.size(); ig++) {
            ArrayList<Integer> pets = gasolineras.get(ig).getPeticiones();
            for (int ip = 0; ip < pets.size(); ip++) peticions.add(new Pair<>(new Pair<>(ig, ip), pets.get(ip)));
        }

        // Inicializar km de cada viaje de los camiones a 0
        board.kmsPorCamion = new int[camions.size()];
        for (int i = 0; i < camions.size(); i++) board.kmsPorCamion[i] = 0;

        for(int i = 0; i < camions.size() && peticions.size() > 0; ++i) {
            boolean b = false;
            while(peticions.size() > 0 && ! b) {
                int km;
                if(kmsPorCamion[i] == 0) {
                    km = Math.abs(camions.get(i).getCoordX() - gasolineras.get(peticions.peek().first.first).getCoordX()) + Math.abs(camions.get(i).getCoordY() - gasolineras.get(peticions.peek().first.first).getCoordY());
                    if(km <= 640) {
                        addGasolineraAViaje(peticions.peek().first.first, km, peticions.peek().first.second, i);
                        kmsPorCamion[i] += km;
                        peticions.poll();
                    }
                    else b = true;
                }
                else {
                    if(viajesPorCamion.get(i).get(viajesPorCamion.get(i).size() - 1).exit()) {
                        km = getDistancia(viajesPorCamion.get(i).get(viajesPorCamion.get(i).size() - 1).getLastGasolinera(), gasolineras.get(peticions.peek().first.first));
                        int km_back = getDistancia(gasolineras.get(peticions.peek().first.first), camions.get(i));
                        if(km + km_back + kmsPorCamion[i] <= 640) {
                            viajesPorCamion.get(i).get(viajesPorCamion.get(i).size() - 1).addGasolinera(peticions.peek().first.first, km + km_back, peticions.peek().first.second)
                            kmsPorCamion[i] += km + km_back;
                            peticions.poll();
                        }
                        else b = true;
                    }
                    else {
                        int igIdx = peticions.peek().first.first;
                        int ipIdx = peticions.peek().first.second;
                        km = getDistancia(gasolineras.get(igIdx), camions.get(i));
                        if(km + kmsPorCamion[i] <= 640) {
                            addGasolineraAViaje(igIdx, km, ipIdx, i);
                            kmsPorCamion[i] += km;
                            peticions.poll();
                        }
                        else b = true;
                    }
                }
            }
        }
        return board;
    }

    // helpers per viajesPorCamion

    public int countViajesCamion(int idCamio) {
        return viajesPorCamion.get(idCamio).size();
    }

    public int kmsCamio(int idCamio) {
        int suma = 0;
        for (Viaje v : viajesPorCamion.get(idCamio)) suma += v.getKmRecorridos();
        return suma;
    }

    public boolean swap_first_first(Viaje v1, Viaje v2, int c1, int c2) {
        if(v1.canSwap_first(v2.getGas1(), c1) && v2.canSwap_first(v1.getGas1(), c2)) {
            int g1 = v1.getGas1();
            int g2 = v2.getGas1();
            v1.swap_first(g2, c1);
            v2.swap_first(g1, c2);
            return true;
        }
        else return false;
    }

    public boolean swap_first_last(Viaje v1, Viaje v2, int c1, int c2) {
        if(v2.getGas2() > 0 && v1.canSwap_first(v2.getGas2(), c1) && v2.canSwap_last(v1.getGas1(), c2)) {
            int g1 = v1.getGas1();
            int g2 = v2.getGas2();
            v1.swap_first(g2, c1);
            v2.swap_last(g1, c2);
            return true;
        }
        else return false;
    }

    public boolean swap_last_last(Viaje v1, Viaje v2, int c1, int c2) {
        if(v2.getGas2() > 0 && v1.getGas2() > 0 && v1.canSwap_last(v2.getGas2(), c1) && v2.canSwap_last(v1.getGas2(), c2)) {
            int g1 = v1.getGas2();
            int g2 = v2.getGas2();
            v1.swap_last(g2, c1);
            v2.swap_last(g1, c2);
            return true;
        }
        else return false;
    }

    public boolean swap_last_first(Viaje v1, Viaje v2, int c1, int c2) {
        if(v1.getGas2() > 0 && v1.canSwap_last(v2.getGas1(), c1) && v2.canSwap_first(v1.getGas2(), c2)) {
            int g1 = v1.getGas2();
            int g2 = v2.getGas1();
            v1.swap_last(g2, c1);
            v2.swap_first(g1, c2);
            return true;
        }
        else return false;
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

        public boolean exit() {
            return gasCount < 2;
        }

        public int getGas1() {
            if (gasCount >= 1) return gasVisitadas[0];
            else return -1;
        }

        public int getGas2() {
            if (gasCount == 2) return gasVisitadas[1];
            else return -1;
        }

        public boolean canSwap_first(int g, int c) {
            if(gasCount == 0) return false;
            if(gasCount == 1) {
                int km_old = getDistancia(gasolineras.get(gasVisitadas[0]), camions.get(c));
                int km_new = getDistancia(gasolineras.get(g), camions.get(c));
                if(kmRecorridos - km_old + km_new <= 640) return true;
                else return false;
            }
            else {
                int km_come_old = getDistancia(gasolineras.get(gasVisitadas[0]), camions.get(c));
                int km_come_new = getDistancia(gasolineras.get(g), camions.get(c));
                int km_next_old = getDistancia(gasolineras.get(gasVisitadas[0]), gasolineras.get(gasVisitadas[1]));
                int km_next_new = getDistancia(gasolineras.get(g), gasolineras.get(gasVisitadas[1]));
                if(kmRecorridos - km_come_old - km_next_old + km_come_new + km_next_new <= 640) return true;
                else return false;
            }
        }

        public boolean canSwap_last(int g, int c) {
            if(gasCount < 2) return false;
            else {
                int km_back_old = getDistancia(gasolineras.get(gasVisitadas[1]), camions.get(c));
                int km_back_new = getDistancia(gasolineras.get(g), camions.get(c));
                int km_prev_old = getDistancia(gasolineras.get(gasVisitadas[0]), gasolineras.get(gasVisitadas[1]));
                int km_prev_new = getDistancia(gasolineras.get(gasVisitadas[0]), gasolineras.get(g));
                if(kmRecorridos - km_back_old - km_prev_old + km_back_new + km_prev_new <= 640) return true;
                else return false;
            }
        }

        public void swap_first(int g, int c) {
            if(gasCount == 1) {
                int km_old = getDistancia(gasolineras.get(gasVisitadas[0]), camions.get(c));
                int km_new = getDistancia(gasolineras.get(g), camions.get(c));
                gasVisitadas[0] = g;
                kmRecorridos = kmRecorridos - km_old + km_new;
            }
            else {
                int km_come_old = getDistancia(gasolineras.get(gasVisitadas[0]), camions.get(c));
                int km_come_new = getDistancia(gasolineras.get(g), camions.get(c));
                int km_next_old = getDistancia(gasolineras.get(gasVisitadas[0]), gasolineras.get(gasVisitadas[1]));
                int km_next_new = getDistancia(gasolineras.get(g), gasolineras.get(gasVisitadas[1]));
                gasVisitadas[0] = g;
                kmRecorridos = kmRecorridos - km_come_old - km_next_old + km_come_new + km_next_new;
            }
        }

        public boolean swap_last(int g, int c) {
            int km_back_old = getDistancia(gasolineras.get(gasVisitadas[1]), camions.get(c));
            int km_back_new = getDistancia(gasolineras.get(g), camions.get(c));
            int km_prev_old = getDistancia(gasolineras.get(gasVisitadas[0]), gasolineras.get(gasVisitadas[1]));
            int km_prev_new = getDistancia(gasolineras.get(gasVisitadas[0]), gasolineras.get(g));
            gasVisitadas[1] = g;
            kmRecorridos = kmRecorridos - km_back_old - km_prev_old + km_back_new + km_prev_new;
        }
    }
}