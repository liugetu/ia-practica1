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

    /* Constants */
    public static final int limitKmCamioDiari = 640;
    public static final int limitViatgesCamio = 5;
    public static final double valorDeposito = 1000.0;
    public static final double costePorKm = 2.0;

    /* Atributs */

    // Centres i gasolineres (fixes, poden ser estàtiques per estalviar memòria)
    static ArrayList<Distribucion> camions;         // coord. dels centres de distribució (si un centre te multiples camions, les seves coords. apareixen repetides)
    static ArrayList<Gasolinera> gasolineras;       // coord. i peticions de cada gasolinera

    // informacio de cada gasolinera: array de que si cada peticio ha estat atesa
    static ArrayList<Pair<Gasolinera, boolean[]>> gasolineras_info;

    // distancies precalculades de centre a gasolinera
    static int[][] distCentroGas;
    // distancies precalculades entre gasolineres (simetrica)
    static int[][] distGasGas;

    // Assignació de peticions a viatges per camió
    ArrayList<ArrayList<Viaje>> viajesPorCamion;  // index exterior = idCamio
    int[] kmsPorCamion;  // els kms que ha fet cada camio (total dels seus viatges)

    // Informació de control
    double beneficioActual; // V = beneficis per pet ateses - costos kms camions - perdues pet no ateses
    double costeTotalKm; // total km de tots els viatges

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
        distGasGas = new int[nGas][nGas];
        for (int i = 0; i < nCentres; i++) {
            for (int g = 0; g < nGas; g++) {
                distCentroGas[i][g] = getDistancia(this.gasolineras.get(g), this.camions.get(i));
            }
        }
        // precomputar diestancies entre gasolineres
        for (int a = 0; a < nGas; a++) {
            for (int b = a; b < nGas; b++) {
                int d = getDistancia(this.gasolineras.get(a), this.gasolineras.get(b));
                distGasGas[a][b] = d;
                distGasGas[b][a] = d;
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

    // retorna el preu d'un diposit d'una peticio que porta diesPnd dies pendent
    // pre: diesPnd >= 0
    public double getPreuDiposit(int diesPnd) {
        if (diesPnd == 0) return 1.02 * valorDeposito;
        else {
            double percentatge = (100.0 - Math.pow(2, diesPnd))/100.0;
            return percentatge * valorDeposito;
        }
    }

    // calcul de la perdua de beneficis per no atendre la peticio el dia actual, sino el dia seguent
    // pre: diesPnd >= 0
    public double calcPerdida(int diasPnd) {
        double beneficiAvui = getPreuDiposit(diasPnd);
        double beneficiDema = getPreuDiposit(diasPnd + 1);
        return (beneficiAvui - beneficiDema);
    }

    // afegir una gasolinera a un nou viatge d'un camio
    public void addGasolineraAViaje(int igas, int km, int ipet, int icam) {
        Viaje nv = new Viaje();
        nv.addGasolinera(igas, km, ipet);
        ArrayList<Viaje> viajesCamion = viajesPorCamion.get(icam);
        viajesCamion.add(nv);
        kmsPorCamion[icam] += km;
    }

    // actualitzar beneficis i kms total quan s'aten una peticio
    // pre: igas i ipet son indexs valids
    public void registrarPeticioAtesa(int igas, int ipet, int kmAfegits) {
        ArrayList<Integer> pets = gasolineras.get(igas).getPeticiones();
        int diesPend = pets.get(ipet);
        double ingres = getPreuDiposit(diesPend);
        double costeViaje = kmAfegits * costePorKm;
        this.beneficioActual += ingres - costeViaje;
        this.costeTotalKm += kmAfegits;
    }

    // penalitzar les peticions no ateses avui (despres d'haver assignat els viatges)
    public void penalitzarPeticionsNoAteses() {
        for (int gi = 0; gi < gasolineras.size(); gi++) {
            boolean[] flags = gasolineras_info.get(gi).second;
            ArrayList<Integer> pets = gasolineras.get(gi).getPeticiones();
            for (int ip = 0; ip < pets.size(); ip++) {
                if (!flags[ip]) {  // no atesa
                    int diesPend = pets.get(ip);
                    this.beneficioActual -= calcPerdida(diesPend);
                }
            }
        }
    }

    /* Solucions inicials */
    
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

                    if (kmNew + kmCurrent <= limitKmCamioDiari) {  // limit kms diari
                        addGasolineraAViaje(ig, kmNew, ip, c);
                        b.registrarPeticioAtesa(ig, ip, kmNew);
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
                        b.registrarPeticioAtesa(ig, ip, kmNew);
                        assignada = true;
                    } 
                    else if (viajesAsignadas.size() < limitViatgesCamio && kmNew + kmCurrent <= limitKmCamioDiari) {
                        // crear un nou viatge per aquest camio
                        addGasolineraAViaje(ig, kmNew, ip, c);
                        b.registrarPeticioAtesa(ig, ip, kmNew);
                        assignada = true;
                    }
                }
            }
            // si no assignada, no s'aten aquesta peticio
        }

        // penalitzar les peticions que han quedat sense atendre avui (perdues d'un dia)
        b.penalitzarPeticionsNoAteses();

        return b;
    }

    // genera una solucio inicial greedy
    public GasolinaBoard solIniGreedy(ArrayList<Gasolinera> gasolineras) {
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
                    if(km <= limitKmCamioDiari) {
                        addGasolineraAViaje(peticions.peek().first.first, km, peticions.peek().first.second, i);
                        kmsPorCamion[i] += km;
                        peticions.poll();
                    }
                    else b = true;
                }
                else {
                    if(viajesPorCamion.get(i).get(viajesPorCamion.get(i).size() - 1).exit()) {
                        int coord_x = viajesPorCamion.get(i).get(viajesPorCamion.get(i).size() - 1).getLastGasolinera().getCoordX();
                        int coord_y = viajesPorCamion.get(i).get(viajesPorCamion.get(i).size() - 1).getLastGasolinera().getCoordY();
                        km = Math.abs(coord_x - gasolineras.get(peticions.peek().first.first).getCoordX()) + Math.abs(coord_y - gasolineras.get(peticions.peek().first.first).getCoordY());
                        if(viajesPorCamion.get(i).get(viajesPorCamion.get(i).size() - 1).addGasolinera(peticions.peek().first.first, km, peticions.peek().first.second)) {
                            kmsPorCamion[i] += km;
                            peticions.poll();
                        }
                        else b = true;
                    }
                    else {
                        int igIdx = peticions.peek().first.first;
                        int ipIdx = peticions.peek().first.second;
                        km = Math.abs(camions.get(i).getCoordX() - gasolineras.get(igIdx).getCoordX()) + Math.abs(camions.get(i).getCoordY() - gasolineras.get(igIdx).getCoordY());
                        int km_back = Math.abs(camions.get(i).getCoordX() - viajesPorCamion.get(i).get(viajesPorCamion.get(i).size() - 1).getLastGasolinera().getCoordX()) + Math.abs(camions.get(i).getCoordY() - viajesPorCamion.get(i).get(viajesPorCamion.get(i).size() - 1).getLastGasolinera().getCoordY());
                        if(km + km_back + kmsPorCamion[i] <= limitKmCamioDiari) {
                            addGasolineraAViaje(igIdx, km, ipIdx, i);
                            kmsPorCamion[i] += km + km_back;
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

        // retorna l'ultima gasolinera (null si no n'hi ha)
        public Gasolinera getLastGasolinera() {
            if (gasCount == 0) return null;
            int idx = gasVisitadas[gasCount - 1];
            return GasolinaBoard.gasolineras.get(idx);
        }

        // retorna l'index de l'ultima gasolinera (-1 si no n'hi ha)
        public int getIndexLastGas() {
            if (gasCount == 0) return -1;
            return gasVisitadas[gasCount - 1];
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
    }
}