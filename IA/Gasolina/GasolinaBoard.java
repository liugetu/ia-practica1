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
    int costeTotalKm; // total km de tots els viatges

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
        for (int c = 0; c < nCentres; c++) {
            for (int g = 0; g < nGas; g++) {
                distCentroGas[c][g] = getDistancia(this.gasolineras.get(g), this.camions.get(c));
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
    public boolean addPeticio(int igas, int ipet, int icam, int iviaje) {
        Viaje v = viajesPorCamion.get(icam).get(iviaje);
        int kmAfegits;

        // comprovar que la gasolinera igas conte la peticio ipet
        //if ((gasolineras_info.get(igas).second).length > ipet) return false; // error
        // la peticio no ha estat atesa encara
        //if (!gasolineras_info.get(igas).second.get(ipet)) return false; // error
        
        if (v.gasCount == 0) {  // cas d'haver creat un viatge nou (buit)
            kmAfegits = 2*distCentroGas[icam][igas];
        } 
        else { // te 1 parada
            int igas1 = v.getIndexLastGas();
            int kmOld = 2*distCentroGas[icam][igas];
            int kmNew = -distCentroGas[icam][igas] + distGasGas[igas][igas1] + distGasGas[icam][igas1];
            kmAfegits = kmNew - kmOld;
            // pnd!!
            if (kmAfegits + kmOld > limitKmCamioDiari) return false;
        }
        v.addGasolinera(igas, kmAfegits, ipet);

        // Actualitzar estat
        viajesPorCamion.get(icam).set(iviaje, v);
        kmsPorCamion[icam] += kmAfegits;
        registrarPeticioAtesa(igas, ipet, kmAfegits);
        return true;
    }



    // metode auxiliar per treure la gasolinera igas del viatge v del camio icam
    // NO actualitza els beneficis!
    public void removeGasolineraViaje(int iviaje, int icam, int igas, int ipet) {
        Viaje v = viajesPorCamion.get(icam).get(iviaje);
        int kmOriginal1 = v.getKmRecorridos();
        if (!v.removeGasolinera(igas, ipet)) return; // error!

        int kmEliminats;
        if (v.getNGasolineras() == 0) {  
            // v no te mes gasolineres a visitar, es treu tot: km = anada + tornada d'abans
            kmEliminats = kmOriginal1; //distancia que tenia
            viajesPorCamion.get(icam).remove(iviaje); // eliminar viatge
        }
        else {  // li queda 1 gasolinera: km = (kms que tenia) - (kms d'ara amb 1 viatge)
            int kmActuals = 2*distCentroGas[icam][igas];
            kmEliminats = kmOriginal1 - kmActuals;
        }
        v.sumaKm(-kmEliminats);            // actualitzar kms que ha fet el camio 1 en aquest viatge
        kmsPorCamion[icam] -= kmEliminats; // actualitzar kms que ha fet el camio 1 en total
        costeTotalKm -= kmEliminats;       // actualitzar kms totals
    }

    // reassignar una peticio d'un viatge a un altre viatge del camio icam
    // post: retorna true si s'ha pogut reassignar la peticio, false altrament
    public boolean reasignar(int icam1, int iviaje1, int igas, int ipet, int icam2) {
        // comprovar que la gasolinera igas conte la peticio ipet
        if ((gasolineras_info.get(igas).second).length > ipet) return false; // error

        // provar d'afegir la peticio ipet de la gasolina igas al camio icam2
        ArrayList<Viaje> viajesAsignadas = viajesPorCamion.get(icam2);
        int kmCurrentCam = kmsPorCamion[icam2];
        int kmNew = 2*distCentroGas[icam2][igas]; // anada + tornada (cas unica parada)
        boolean assignada = false;

        if (viajesAsignadas.isEmpty()) { // el camio no te encara cap viatge
            if (kmNew <= limitKmCamioDiari) {  // limit kms diari
                addGasolineraAViaje(igas, kmNew, ipet, icam2);
                costeTotalKm += kmNew;
                assignada = true;
            }
        } 
        else {
            // recorrem pels seus viatges existents per veure si es pot afegir a algun
            int nViajes= viajesAsignadas.size();
            for (int iv = 0; iv < nViajes; iv++) {
                Viaje v = viajesAsignadas.get(iv);
                int lastGas = v.getIndexLastGas();
                if (v.getNGasolineras() == 1) {     // s'assumeix que si no hi ha cap gasolinera, no existiria el viatge
                    // distancia recorreguda si s'afageix la nova parada
                    int kmOld = v.getKmRecorridos(); // anada + tornada a 1 gasolinera
                    int kmNew2 = kmOld / 2 + distGasGas[v.getIndexLastGas()][igas] + distCentroGas[icam2][igas];
                    int kmAfegits = kmNew2 - kmOld;
                    if (kmCurrentCam + kmAfegits <= limitKmCamioDiari) {
                        // afegir la parada al mateix viatge
                        v.addGasolinera(igas, kmAfegits, ipet);
                        kmsPorCamion[icam2] += kmAfegits;
                        costeTotalKm += kmAfegits;
                        assignada = true;
                    }
                }
            }
            if (!assignada && nViajes < limitViatgesCamio) {
                // a veure si es pot atendre en un nou viatge del camio
                if (kmNew + kmCurrentCam <= limitKmCamioDiari) {  // limit kms diari
                    addGasolineraAViaje(igas, kmNew, ipet, icam2);
                    costeTotalKm += kmNew;
                    assignada = true;
                }
            }
        }

        if (assignada) { // eliminar la peticio del viaje original
            removeGasolineraViaje(iviaje1, icam1, igas, ipet);
            // OJO: aqui no fa falta modificar beneficis pq la peticio nomes es transfereix     
            return true;
        }
        else return false;
    }

    /* Heuristic function */
    public double heuristic(){
        return 0;
    }

    /* Goal test */
    public boolean is_goal(){
        return false;
    }

    public double getBeneficio() {
        return beneficioActual;
    }

    public int getKm() {
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
        viajesPorCamion.get(icam).add(nv);
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
                int ic = (camioIni + i) % camions.size();
                ArrayList<Viaje> viajesAsignadas = viajesPorCamion.get(ic);
                int kmCurrent = kmsPorCamion[ic];
                int kmNew = 2*distCentroGas[ic][ig]; // anada i tornada!

                if (viajesAsignadas.isEmpty()) { // el camio no te encara cap viatge
                    if (kmNew <= limitKmCamioDiari) {  // limit kms diari
                        addGasolineraAViaje(ig, kmNew, ip, ic);
                        registrarPeticioAtesa(ig, ip, kmNew);
                        assignada = true;
                    }
                } 
                else {
                    // afegim les gasolineres als viatges per ordre
                    Viaje lastViaje = viajesAsignadas.get(viajesAsignadas.size() - 1);
                    if (lastViaje.getNGasolineras() == 1) {
                        int kmOld = lastViaje.getKmRecorridos(); // anada + tornada a 1 gasolinera
                        int lastIdx = lastViaje.getIndexLastGas();
                        int kmNew2 = kmOld / 2 + distGasGas[lastIdx][ig] + distCentroGas[ic][ig]; // ******
                        int kmAfegits = kmNew2 - kmOld;
                        if (kmAfegits + kmCurrent <= limitKmCamioDiari) { 
                            // es pot afegir la parada a l'ultim viatge
                            lastViaje.addGasolinera(ig, kmAfegits, ip);
                            kmsPorCamion[ic] += kmAfegits;
                            registrarPeticioAtesa(ig, ip, kmAfegits);
                            assignada = true;
                        } 
                    }
                    else if (viajesAsignadas.size() < limitViatgesCamio && 
                        kmNew + kmCurrent <= limitKmCamioDiari) { // mirar si es pot afegirla a un nou viatge
                        // crear un nou viatge per aquest camio
                        addGasolineraAViaje(ig, kmNew, ip, ic);
                        registrarPeticioAtesa(ig, ip, kmNew); // actualitzar beneficis i kms
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
                    km = distCentroGas[i][peticions.peek().first.first];
                    if(km <= limitKmCamioDiari) {
                        addGasolineraAViaje(peticions.peek().first.first, km, peticions.peek().first.second, i);
                        kmsPorCamion[i] += km;
                        registrarPeticioAtesa(peticions.peek().first.first, peticions.peek().first.second, km);
                        peticions.poll();
                    }
                    else b = true;
                }
                else {
                    if(viajesPorCamion.get(i).get(viajesPorCamion.get(i).size() - 1).exit()) {
                        km = distGasGas[viajesPorCamion.get(i).get(viajesPorCamion.get(i).size() - 1).getIndexLastGas()][peticions.peek().first.first];
                        int km_back = distCentroGas[i][peticions.peek().first.first];
                        if(km + km_back + kmsPorCamion[i] <= limitKmCamioDiari) {
                            viajesPorCamion.get(i).get(viajesPorCamion.get(i).size() - 1).addGasolinera(peticions.peek().first.first, km + km_back, peticions.peek().first.second);
                            kmsPorCamion[i] += km + km_back;
                            registrarPeticioAtesa(peticions.peek().first.first, peticions.peek().first.second, km);
                            peticions.poll();
                        }
                        else b = true;
                    }
                    else {
                        int igIdx = peticions.peek().first.first;
                        int ipIdx = peticions.peek().first.second;
                        km = distCentroGas[i][igIdx];
                        if(km + kmsPorCamion[i] <= limitKmCamioDiari) {
                            addGasolineraAViaje(igIdx, km, ipIdx, i);
                            kmsPorCamion[i] += km;
                            registrarPeticioAtesa(igIdx, ipIdx, km);
                            peticions.poll();
                        }
                        else b = true;
                    }
                }
            }
        }
        board.penalitzarPeticionsNoAteses();
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

    // swap de peticions entre viatges de camions
    public boolean swap(int igas1, int ipet1, int icam1, int iviatje1, int igas2, int ipet2, int icam2, int iviatje2) {
        Viaje v1 = viajesPorCamion.get(icam1).get(iviatje1);
        Viaje v2 = viajesPorCamion.get(icam2).get(iviatje2);

        int[] gasV1 = v1.getGasVisitadas();
        int[] gasV2 = v2.getGasVisitadas();
        int[] petV1 = v1.getPetVisitadas();
        int[] petV2 = v2.getPetVisitadas();

        if(gasV1[0] == igas1 && petV1[0] == ipet1) {
            // peticio 1 es la primera del viatge 1
            if(gasV2[0] == igas2 && petV2[0] == ipet2) {
                // peticio 2 es la primera del viatge 2
                if(swap_first_first(v1, v2, igas1, igas2)) return true;
                else return fasle;
            }
            else {
                // peticio 2 es la segona del viatge 2
                if(swap_first_last(v1, v2, igas1, igas2)) return true;
                else return false;
            }
        }
        else {
            // peticio 1 es la segona del viatge 1
            if(gasV2[0] == igas2 && petV2[0] == ipet2) {
                // peticio 2 es la primera del viatge 2
                if(swap_last_first(v1, v2, igas1, igas2)) return true;
                else return false;
            }
            else {
                // peticio 2 es la segona del viatge 2
                if(swap_last_last(v1, v2, igas1, igas2)) return true;
                else return false;
            }
        }
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
        int[] petVisitadas = new int[2];  // quina peticio s'ha ates de cada gasolinera atesa
        int gasCount; // nombre de gasolines visitades

        // creadora
        public Viaje() {
            kmRecorridos = 0;
            gasVisitadas[0] = gasVisitadas[1] = -1; // -1 = buit
            petVisitadas[0] = petVisitadas[1] = -1;
            gasCount = 0;
        }

        public int[] getGasVisitadas() {
            return gasVisitadas;
        }

        public int[] getPetVisitadas() {
            return petVisitadas;
        }

        // afegir gasolinera
        // no oblidar fer kmsPorCamion[idCamio] += km; !!
        public boolean addGasolinera(int g, int km, int ipeticion) {
            if (gasCount >= 2) return false;
            gasVisitadas[gasCount] = g;
            petVisitadas[gasCount] = ipeticion;
            gasCount++;
            gasolineras_info.get(g).second[ipeticion] = true;
            kmRecorridos += km;
            return true;
        }

        // retorna el nombre de km recorreguts
        public int getKmRecorridos() {
            return kmRecorridos;
        }

        // retorna el nombre de parades
        public int getNGasolineras() {
            return gasCount;
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

        // retorna l'index de la 1a gasolinera (-1 si no n'hi ha)
        public int getIndexFirstGas() {
            if (gasCount == 0) return -1;
            return gasVisitadas[0];
        }

        // retorna l'index de la peticio atesa de l'ultima gasolinera (-1 si no n'hi ha)
        public int getLastPeticio() {
            if (gasCount == 0) return -1;
            return petVisitadas[gasCount - 1];
        }

        // retorna l'index de la peticio atesa de la 1a gasolinera (-1 si no n'hi ha)
        public int getFirstPeticio() {
            if (gasCount == 0) return -1;
            return petVisitadas[0];
        }

        // pre: km pot ser negatiu
        public void sumaKm(int km) {
            kmRecorridos += km;
            if (kmRecorridos < 0) kmRecorridos = 0; // control
        }

        // treure una peticio d'una gasolinera del viatge
        // no oblidar fer kmsPorCamion[idCamio] -= km; !!
        public boolean removeGasolinera(int g, int ipeticion) {
            int pos = -1;  // posicio de la gas i pet (o 0 o 1)
            for (int i = 0; i < gasCount; i++) {
                if (gasVisitadas[i] == g) { 
                    pos = i; 
                    break; 
                }
            }
            if (pos >= 0) {  // trobat
                // comprovar que petVisitadas[pos] coincideix amb ipeticion
                if (petVisitadas[pos] != ipeticion) return false;
                for (int j = pos; j < gasCount - 1; j++) {  // shift left arrays de gas i pet
                    gasVisitadas[j] = gasVisitadas[j+1];
                    petVisitadas[j] = petVisitadas[j+1];
                }
                gasVisitadas[gasCount - 1] = -1;
                petVisitadas[gasCount - 1] = -1;
                gasCount--;
                gasolineras_info.get(g).second[ipeticion] = false;
                return true;
            }
            else return false;
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
                int km_old = distCentroGas[c][gasVisitadas[0]];
                int km_new = distCentroGas[c][g];
                if(kmRecorridos - km_old + km_new <= limitKmCamioDiari) return true;
                else return false;
            }
            else {
                int km_come_old = distCentroGas[c][gasVisitadas[0]];
                int km_come_new = distCentroGas[c][g];
                int km_next_old = distGasGas[gasVisitadas[0]][gasVisitadas[1]];
                int km_next_new = distGasGas[g][gasVisitadas[1]];
                if(kmRecorridos - km_come_old - km_next_old + km_come_new + km_next_new <= limitKmCamioDiari) return true;
                else return false;
            }
        }

        public boolean canSwap_last(int g, int c) {
            if(gasCount < 2) return false;
            else {
                int km_back_old = distCentroGas[c][gasVisitadas[1]];
                int km_back_new = distCentroGas[c][g];
                int km_prev_old = distGasGas[gasVisitadas[0]][gasVisitadas[1]];
                int km_prev_new = distGasGas[gasVisitadas[0]][g];
                if(kmRecorridos - km_back_old - km_prev_old + km_back_new + km_prev_new <= limitKmCamioDiari) return true;
                else return false;
            }
        }

        public void swap_first(int g, int c) {
            if(gasCount == 1) {
                int km_old = distCentroGas[c][gasVisitadas[0]];
                int km_new = distCentroGas[c][g];
                gasVisitadas[0] = g;
                kmRecorridos = kmRecorridos - km_old + km_new;
            }
            else {
                int km_come_old = distCentroGas[c][gasVisitadas[0]];
                int km_come_new = distCentroGas[c][g];
                int km_next_old = distGasGas[gasVisitadas[0]][gasVisitadas[1]];
                int km_next_new = distGasGas[g][gasVisitadas[1]];
                gasVisitadas[0] = g;
                kmRecorridos = kmRecorridos - km_come_old - km_next_old + km_come_new + km_next_new;
            }
        }

        public void swap_last(int g, int c) {
            int km_back_old = distCentroGas[c][gasVisitadas[1]];
            int km_back_new = distCentroGas[c][g];
            int km_prev_old = distGasGas[gasVisitadas[0]][gasVisitadas[1]];
            int km_prev_new = distGasGas[gasVisitadas[0]][g];
            gasVisitadas[1] = g;
            kmRecorridos = kmRecorridos - km_back_old - km_prev_old + km_back_new + km_prev_new;
        }
    }
}