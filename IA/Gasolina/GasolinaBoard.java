package IA.Gasolina;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.Collections;
import java.util.PriorityQueue;
import java.lang.Math;
import java.util.Comparator;

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

    // centres i gasolineres
    static ArrayList<Distribucion> camions; // coord. dels centres de distribucio (si un centre te multiples camions, les seves coords. apareixen repetides)
    static ArrayList<Gasolinera> gasolineras; // coord. i peticions de cada gasolinera

    // informacio de cada gasolinera: array de que si cada peticio ha estat atesa
    static ArrayList<boolean[]> gasolineras_info;

    // distancies precalculades de centre a gasolinera
    static int[][] distCentroGas;
    // distancies precalculades entre gasolineres (matriu simetrica)
    static int[][] distGasGas;

    // assignacio de peticions a viatges per camio
    ArrayList<ArrayList<Viaje>> viajesPorCamion;  // index exterior = idCamio
    int[] kmsPorCamion;  // els kms que ha fet cada camio (total dels seus viatges)

    double beneficioActual; // V = beneficis per pet ateses - costos kms camions - perdues pet no ateses (per l'heuristica)
    double beneficiAvui; // V = beneficis per pet ateses avui - costos kms camions 
    int costeTotalKm; // total km de tots els viatges de tots els camions

    // retorna la distancia entre una gasolinera i un centre
    public int getDistancia(Gasolinera g, Distribucion d) {
        int x = Math.abs(g.getCoordX() - d.getCoordX());
        int y = Math.abs(g.getCoordY() - d.getCoordY());
        return x + y;
    }

    // retorna la distancia entre dues gasolineres
    public int getDistancia(Gasolinera g1, Gasolinera g2) {
        int x = Math.abs(g1.getCoordX() - g2.getCoordX());
        int y = Math.abs(g1.getCoordY() - g2.getCoordY());
        return x + y;
    }

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
        this.beneficiAvui = 0;
        this.costeTotalKm = 0;

        kmsPorCamion = new int[camions.size()];

        gasolineras_info = new ArrayList<>();
        if (this.gasolineras != null) {
            for (int i = 0; i < this.gasolineras.size(); i++) {
                Gasolinera g = this.gasolineras.get(i);
                int mida = g.getPeticiones().size();
                boolean[] flags = new boolean[mida];
                gasolineras_info.add(flags);
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


    // constructora copia: crea una copia total del estado actual
    public GasolinaBoard(GasolinaBoard other) { // las variables estaticas no necesitan ser copiadas
        this.beneficioActual = other.beneficioActual;
        this.beneficiAvui = other.beneficiAvui;
        this.costeTotalKm = other.costeTotalKm;

        this.kmsPorCamion = new int[other.kmsPorCamion.length];
        System.arraycopy(other.kmsPorCamion, 0, this.kmsPorCamion, 0, other.kmsPorCamion.length);

        // copia de viajesPorCamion
        this.viajesPorCamion = new ArrayList<>();
        for (int i = 0; i < other.viajesPorCamion.size(); i++) {
            ArrayList<Viaje> viajesOriginales = other.viajesPorCamion.get(i);
            ArrayList<Viaje> nuevosViajes = new ArrayList<>();

            for (Viaje viajeOriginal : viajesOriginales) {
                // copia del viaje
                Viaje nuevoViaje = new Viaje();
                nuevoViaje.copyFrom(viajeOriginal);
                nuevosViajes.add(nuevoViaje);
            }

            this.viajesPorCamion.add(nuevosViajes);
        }
    }

    // retorna una copia completa del board
    public GasolinaBoard copy() {
        return new GasolinaBoard(this);
    }

    /* Operadors */

    /* 
     * PRE: la peticio no ha estat atesa encara, el viatge iviaje del camio icam existeix i te menys de 2 gasolineres
     * POST: Afegir peticio ipet de la gasolinera igas al viatge iviaje del camio icam
    */
    public boolean addPeticio(int igas, int ipet, int icam, int iviaje) {
        Viaje v = viajesPorCamion.get(icam).get(iviaje);
        int kmAfegits;
        
        if (v.gasCount == 0) {  // cas d'haver creat un viatge nou (buit)
            kmAfegits = 2 * distCentroGas[icam][igas];
            if (kmsPorCamion[icam] + kmAfegits > limitKmCamioDiari) return false;
        } 
        else { // te 1 parada
            int igas1 = v.getIndexLastGas();
            // delta respecte a 1 parada: -retorn al centre des de igas1 + desplaçament igas1->igas + retorn des de igas
            kmAfegits = -distCentroGas[icam][igas1] + distGasGas[igas1][igas] + distCentroGas[icam][igas];
            if (kmsPorCamion[icam] + kmAfegits > limitKmCamioDiari) return false;
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
        else {  // li queda exactament 1 gasolinera: km = (kms que tenia) - (kms d'ara amb 1 viatge)
            int remainingGas = v.getIndexLastGas();
            int kmActuals = 2 * distCentroGas[icam][remainingGas];
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
        if (ipet < 0 || igas < 0 || igas >= gasolineras_info.size() || ipet >= (gasolineras_info.get(igas)).length) return false; // error

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

    // operador d'intercanviar una petició atesa per una que no ho està
    public boolean intercanvi(int igas1, int ipet1, int icam1, int iviatje1, int igas2, int ipet2) {
        Viaje v = viajesPorCamion.get(icam1).get(iviatje1);

        // determinar si la petició es primera o segona al seu viatje
        boolean isPrimera = (v.getGasVisitadas()[0] == igas1 && v.getPetVisitadas()[0] == ipet1);
        if (!isPrimera && v.gasCount < 2) return false; // La petició no és la segona si no hi ha segona parada
        if (!isPrimera && !(v.getGasVisitadas()[1] == igas1 && v.getPetVisitadas()[1] == ipet1)) return false;

        // calc els nous kilòmetres si es fa l'intercanvi
        int kmOld = v.getKmRecorridos();
        int kmNew;
        
        if (v.gasCount == 1) {
            // només hi ha una parada, es canvia per la nova gasolinera
            kmNew = 2 * distCentroGas[icam1][igas2];
        } else { // Hi ha dues parades
            if (isPrimera) {
                // Canviar la primera parada
                int kmCentreOld = distCentroGas[icam1][igas1];
                int kmCentreNew = distCentroGas[icam1][igas2];
                int kmEntreOld = distGasGas[igas1][v.getGasVisitadas()[1]];
                int kmEntreNew = distGasGas[igas2][v.getGasVisitadas()[1]];
                kmNew = kmOld - kmCentreOld - kmEntreOld + kmCentreNew + kmEntreNew;
            } else {
                // Canviar la segona parada
                int kmTornadaOld = distCentroGas[icam1][igas1];
                int kmTornadaNew = distCentroGas[icam1][igas2];
                int kmEntreOld = distGasGas[v.getGasVisitadas()[0]][igas1];
                int kmEntreNew = distGasGas[v.getGasVisitadas()[0]][igas2];
                kmNew = kmOld - kmTornadaOld - kmEntreOld + kmTornadaNew + kmEntreNew;
            }
        }

        // Verificar que els nous kilòmetres no superen el límit diari
        int newTotalKmsCamio = kmsPorCamion[icam1] - kmOld + kmNew;
        if (newTotalKmsCamio > limitKmCamioDiari) return false;

        // Calcular el canvi en beneficis
        ArrayList<Integer> pets1 = gasolineras.get(igas1).getPeticiones();
        ArrayList<Integer> pets2 = gasolineras.get(igas2).getPeticiones();
        
        double beneficiOld = getPreuDiposit(pets1.get(ipet1));
        double costOld = kmOld * costePorKm;
        double costPerduaOld = calcPerdida(pets1.get(ipet1));
        
        double beneficiNew = getPreuDiposit(pets2.get(ipet2));
        double costNew = kmNew * costePorKm;
        double costPerduaNew = calcPerdida(pets2.get(ipet2));
        
        // realitzar l'intercanvi
        // actualitzar les gasolineres visitades i peticions
        if (isPrimera) {
            v.gasVisitadas[0] = igas2;
            v.petVisitadas[0] = ipet2;
        } else {
            v.gasVisitadas[1] = igas2;
            v.petVisitadas[1] = ipet2;
        }
        
        // actualitzar l'estat de les peticions
        gasolineras_info.get(igas1)[ipet1] = false; // ja no està atesa
        gasolineras_info.get(igas2)[ipet2] = true;  // ara està atesa
        
        // actualitzar kilòmetres
        v.kmRecorridos = kmNew;
        int deltaKm = kmNew - kmOld;
        kmsPorCamion[icam1] += deltaKm;
        costeTotalKm += deltaKm;
        
        // actualitzar beneficis (intercanvi atesa <-> no atesa)
        beneficioActual = beneficioActual - beneficiOld + costOld + beneficiNew - costNew - costPerduaOld + costPerduaNew;
        beneficiAvui = beneficiAvui - beneficiOld + costOld + beneficiNew - costNew;
        
        return true;
    }

    /* Getters dels atributs */

    public int getNCamions() {
        return camions.size();
    }

    public int getNGasolineras() {
        return gasolineras.size();
    }

    public int getNPeticionsGasolinera(int igas) {
        return (gasolineras_info.get(igas)).length;
    }

    public int getNViajesCamion(int icam) {
        return viajesPorCamion.get(icam).size();
    }

    // getter del nombre de parades (gasolineres) que fa un camio en un viatge 
    // pre: camio icam te viatge iv
    public int getNParadasViaje(int icam, int iv) {
        return viajesPorCamion.get(icam).get(iv).getNGasolineras();
    }

    // post: retorna el num de la peticio atesa de la gasolinera amb index idx
    public int getPeticioViaje(int icam, int iv, int idx) {
        return viajesPorCamion.get(icam).get(iv).getPeticio(idx);
    }

    // post: retorna el num de la gasolinera atesa de la gasolinera amb index idx
    public int getGasolineraViaje(int icam, int iv, int idx) {
        return viajesPorCamion.get(icam).get(iv).getGasolinera(idx);
    }

    public double getBeneficio() {
        return beneficioActual;
    }

    public double getBeneficiAvui() {
        return beneficiAvui;
    }

    public int getKm() {
        return costeTotalKm;
    }

    /* helpers per viajesPorCamion */

    public int countViajesCamion(int idCamio) {
        return viajesPorCamion.get(idCamio).size();
    }

    public int kmsCamio(int idCamio) {
        int suma = 0;
        for (Viaje v : viajesPorCamion.get(idCamio)) suma += v.getKmRecorridos();
        return suma;
    }

    public int getCamionMinGasolineras(int gasolinera) {
       int minCamion = 0;
       int min_dist = Integer.MAX_VALUE;

        for (int i = 0; i < camions.size(); i++) {
            if(distCentroGas[i][gasolinera] < min_dist) {
               min_dist = distCentroGas[i][gasolinera];
               minCamion = i;
            }
        }

       return minCamion;
    }

    /* Càlculs */

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
        this.beneficiAvui += ingres - costeViaje;
        this.costeTotalKm += kmAfegits;
    }

    // penalitzar les peticions no ateses avui (despres d'haver assignat els viatges)
    public void penalitzarPeticionsNoAteses() {
        for (int gi = 0; gi < gasolineras.size(); gi++) {
            boolean[] flags = gasolineras_info.get(gi);
            ArrayList<Integer> pets = gasolineras.get(gi).getPeticiones();
            for (int ip = 0; ip < pets.size(); ip++) {
                if (!flags[ip]) {  // no atesa
                    int diesPend = pets.get(ip);
                    this.beneficioActual -= calcPerdida(diesPend);
                }
            }
        }
    }

    // mira si la peticio ipet de igas esta atesa
    public boolean isPeticioAtesa(int igas, int ipet) {
        if (igas < 0 || igas >= gasolineras_info.size()) return false;
        boolean[] flags = gasolineras_info.get(igas);
        if (ipet < 0 || ipet >= flags.length) return false;
        return flags[ipet];
    }

    /* Solucions inicials */
    
    // genera una solucio inicial random
    public GasolinaBoard solIniRandom() {
        int ngas = gasolineras.size();
        GasolinaBoard b = this;

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
    public GasolinaBoard solIniGreedy() {
        GasolinaBoard board = this;

        // inicialitzar viatges per camio (buits)
        for (int i = 0; i < camions.size(); i++) board.viajesPorCamion.get(i).clear();

        PriorityQueue<Pair<Pair<Integer, Integer>, Integer>> peticions = new PriorityQueue<Pair<Pair<Integer, Integer>, Integer>>(
            new Comparator<Pair<Pair<Integer, Integer>, Integer>>() {
                @Override
                public int compare(Pair<Pair<Integer, Integer>, Integer> a, Pair<Pair<Integer, Integer>, Integer> b) {
                    double x, y;
                    x = getPreuDiposit(a.second) - getPreuDiposit(a.second + 1);
                    y = getPreuDiposit(b.second) - getPreuDiposit(b.second + 1);
                    if (x < y) return 1;
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
                    km = 2 * distCentroGas[i][peticions.peek().first.first];
                    if(kmsPorCamion[i] + km <= limitKmCamioDiari) {
                        addGasolineraAViaje(peticions.peek().first.first, km, peticions.peek().first.second, i);
                        kmsPorCamion[i] += km;
                        registrarPeticioAtesa(peticions.peek().first.first, peticions.peek().first.second, km);
                        peticions.poll();
                    }
                    else b = true;
                }
                else {
                    if(viajesPorCamion.get(i).get(viajesPorCamion.get(i).size() - 1).exit()) {
                        int lastIdx = viajesPorCamion.get(i).get(viajesPorCamion.get(i).size() - 1).getIndexLastGas();
                        int igIdx = peticions.peek().first.first;
                        int ipIdx = peticions.peek().first.second;
                        int kmAfegits = -distCentroGas[i][lastIdx] + distGasGas[lastIdx][igIdx] + distCentroGas[i][igIdx];
                        if(kmsPorCamion[i] + kmAfegits <= limitKmCamioDiari) {
                            viajesPorCamion.get(i).get(viajesPorCamion.get(i).size() - 1).addGasolinera(igIdx, kmAfegits, ipIdx);
                            kmsPorCamion[i] += kmAfegits;
                            registrarPeticioAtesa(igIdx, ipIdx, kmAfegits);
                            peticions.poll();
                        }
                        else b = true;
                    }
                    else {
                        int igIdx = peticions.peek().first.first;
                        int ipIdx = peticions.peek().first.second;
                        km = 2 * distCentroGas[i][igIdx];
                        if(kmsPorCamion[i] + km <= limitKmCamioDiari) {
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

    // manera 2 de generar una solucio inicial greedy
    public GasolinaBoard solIniGreedy2() {
        GasolinaBoard board = this;

        // inicialitzar viatges per camio (buits)
        for (int i = 0; i < camions.size(); i++) board.viajesPorCamion.get(i).clear();

        PriorityQueue<Pair<Pair<Integer, Integer>, Integer>> peticions = new PriorityQueue<Pair<Pair<Integer, Integer>, Integer>>(
            new Comparator<Pair<Pair<Integer, Integer>, Integer>>() {
                @Override
                public int compare(Pair<Pair<Integer, Integer>, Integer> a, Pair<Pair<Integer, Integer>, Integer> b) {
                    double x, y;
                    x = getPreuDiposit(a.second) - getPreuDiposit(a.second + 1);
                    y = getPreuDiposit(b.second) - getPreuDiposit(b.second + 1);
                    if (x < y) return 1;
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

        

        while (peticions.size() > 0) {
            Pair<Pair<Integer, Integer>, Integer> top = peticions.peek();
            int igIdx = top.first.first;
            int ipIdx = top.first.second;

            // es prefereix el camio mes aprop a aquesta gasolinera
            int preferredCamion = getCamionMinGasolineras(igIdx);
            boolean assigned = false;

            // provar 1r el camio preferit, i despres els altres amb round-robin
            for (int offset = 0; offset < camions.size() && !assigned; offset++) {
                int i = (preferredCamion + offset) % camions.size();
                int km;
                if (kmsPorCamion[i] == 0) {
                    km = 2 * distCentroGas[i][igIdx];
                    if (kmsPorCamion[i] + km <= limitKmCamioDiari) {
                        addGasolineraAViaje(igIdx, km, ipIdx, i);
                        kmsPorCamion[i] += km;
                        registrarPeticioAtesa(igIdx, ipIdx, km);
                        peticions.poll();
                        assigned = true;
                    }
                } else {
                    Viaje lastViaje = viajesPorCamion.get(i).get(viajesPorCamion.get(i).size() - 1);
                    if (lastViaje.exit()) {
                        int lastIdx = lastViaje.getIndexLastGas();
                        int kmAfegits = -distCentroGas[i][lastIdx] + distGasGas[lastIdx][igIdx] + distCentroGas[i][igIdx];
                        if (kmsPorCamion[i] + kmAfegits <= limitKmCamioDiari) {
                            lastViaje.addGasolinera(igIdx, kmAfegits, ipIdx);
                            kmsPorCamion[i] += kmAfegits;
                            registrarPeticioAtesa(igIdx, ipIdx, kmAfegits);
                            peticions.poll();
                            assigned = true;
                        }
                    } else {
                        km = 2 * distCentroGas[i][igIdx];
                        if (kmsPorCamion[i] + km <= limitKmCamioDiari) {
                            addGasolineraAViaje(igIdx, km, ipIdx, i);
                            kmsPorCamion[i] += km;
                            registrarPeticioAtesa(igIdx, ipIdx, km);
                            peticions.poll();
                            assigned = true;
                        }
                    }
                }
            }

            // si no s'ha assignat a cap camio, eliminar-lo
            if (!assigned) {
                peticions.poll();
            }
        }
        board.penalitzarPeticionsNoAteses();
        return board;
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
                if(swap_first_first(v1, v2, icam1, icam2)) return true;
                else return false;
            }
            else {
                // peticio 2 es la segona del viatge 2
                if(petV2.length > 1 && gasV2[1] == igas2 && petV2[1] == ipet2) {
                    if(swap_first_last(v1, v2, icam1, icam2)) return true;
                    else return false;
                }
                else return false; // error
            }
        }
        else {
            // peticio 1 es la segona del viatge 1
            if(gasV2[0] == igas2 && petV2[0] == ipet2) {
                // peticio 2 es la primera del viatge 2
                if(petV1.length > 1 && gasV1[1] == igas1 && petV1[1] == ipet1) {
                    if(swap_last_first(v1, v2, icam1, icam2)) return true;
                    else return false;
                }
                else return false; // error
            }
            else {
                // peticio 2 es la segona del viatge 2
                if(petV1.length > 1 && petV2.length > 1 && gasV1[1] == igas1 && petV1[1] == ipet1 && gasV2[1] == igas2 && petV2[1] == ipet2) {
                    if(swap_last_last(v1, v2, icam1, icam2)) return true;
                    else return false;
                }
                else return false; // error
            }
        }
    }

    /* Helpers pel swap */

    public boolean swap_first_first(Viaje v1, Viaje v2, int c1, int c2) {
        if(v1.canSwap_first(v2.getGas1(), c1) && v2.canSwap_first(v1.getGas1(), c2)) {
            int g1 = v1.getGas1();
            int g2 = v2.getGas1();
            int pet1 = v1.getPeticio(0);
            int pet2 = v2.getPeticio(0);
            v1.swap_first(g2, c1, pet2);
            v2.swap_first(g1, c2, pet1);
            return true;
        }
        else return false;
    }

    public boolean swap_first_last(Viaje v1, Viaje v2, int c1, int c2) {
        if(v2.getGas2() > 0 && v1.canSwap_first(v2.getGas2(), c1) && v2.canSwap_last(v1.getGas1(), c2)) {
            int g1 = v1.getGas1();
            int g2 = v2.getGas2();
            int pet1 = v1.getPeticio(0);
            int pet2 = v2.getPeticio(1);
            v1.swap_first(g2, c1, pet2);
            v2.swap_last(g1, c2, pet1);
            return true;
        }
        else return false;
    }

    public boolean swap_last_last(Viaje v1, Viaje v2, int c1, int c2) {
        if(v2.getGas2() > 0 && v1.getGas2() > 0 && v1.canSwap_last(v2.getGas2(), c1) && v2.canSwap_last(v1.getGas2(), c2)) {
            int g1 = v1.getGas2();
            int g2 = v2.getGas2();
            int pet1 = v1.getPeticio(1);
            int pet2 = v2.getPeticio(1);
            v1.swap_last(g2, c1, pet2);
            v2.swap_last(g1, c2, pet1);
            return true;
        }
        else return false;
    }

    public boolean swap_last_first(Viaje v1, Viaje v2, int c1, int c2) {
        if(v1.getGas2() > 0 && v1.canSwap_last(v2.getGas1(), c1) && v2.canSwap_first(v1.getGas2(), c2)) {
            int g1 = v1.getGas2();
            int g2 = v2.getGas1();
            int pet1 = v1.getPeticio(1);
            int pet2 = v2.getPeticio(0);
            v1.swap_last(g2, c1, pet2);
            v2.swap_first(g1, c2, pet1);
            return true;
        }
        else return false;
    }

    // imprimir benefici actual i kms
    public void printBeneKm() {
        System.out.println("Benefici actual (avui): "+beneficiAvui+", km: "+costeTotalKm);
    }

    // imprimeix tot l'estat del board de manera detallada
    // per debuguejar
    public void printEstatComplet() {
        System.out.println("\n" + "=".repeat(80));
        System.out.println("ESTAT COMPLET DEL BOARD");
        System.out.println("=".repeat(80));
        System.out.println("\n--- RESUM GLOBAL ---");
        System.out.println("Benefici actual: " + String.format("%.2f", beneficioActual));
        System.out.println("Benefici avui: " + String.format("%.2f", beneficiAvui));
        System.out.println("Kilòmetres totals: " + costeTotalKm);
        System.out.println("Nombre de camions: " + camions.size());
        System.out.println("Nombre de gasolineres: " + gasolineras.size());
        
        // comptar peticions ateses i no ateses
        int petAteses = 0, petNoAteses = 0;
        for (int i = 0; i < gasolineras_info.size(); i++) {
            boolean[] flags = gasolineras_info.get(i);
            for (boolean atesa : flags) {
                if (atesa) petAteses++;
                else petNoAteses++;
            }
        }
        System.out.println("Peticions ateses: " + petAteses);
        System.out.println("Peticions no ateses: " + petNoAteses);
        System.out.println("Total peticions: " + (petAteses + petNoAteses));
        
        // informació per cada camió
        System.out.println("\n" + "=".repeat(80));
        System.out.println("INFORMACIÓ DELS CAMIONS I ELS SEUS VIATGES");
        System.out.println("=".repeat(80));
        
        for (int ic = 0; ic < camions.size(); ic++) {
            Distribucion centre = camions.get(ic);
            ArrayList<Viaje> viatges = viajesPorCamion.get(ic);
            
            System.out.println("\n--- CAMIÓ " + ic + " ---");
            System.out.println("Centre de distribució: (" + centre.getCoordX() + ", " + centre.getCoordY() + ")");
            System.out.println("Km totals del camió: " + kmsPorCamion[ic] + " / " + limitKmCamioDiari);
            System.out.println("Nombre de viatges: " + viatges.size() + " / " + limitViatgesCamio);
            
            if (viatges.isEmpty()) {
                System.out.println("  (No té cap viatge assignat)");
            } else {
                for (int iv = 0; iv < viatges.size(); iv++) {
                    Viaje v = viatges.get(iv);
                    System.out.println("\n  Viatge " + iv + ":");
                    System.out.println("    Km del viatge: " + v.getKmRecorridos());
                    System.out.println("    Nombre de parades: " + v.getNGasolineras());
                    
                    for (int ip = 0; ip < v.getNGasolineras(); ip++) {
                        int igas = v.getGasolinera(ip);
                        int ipet = v.getPeticio(ip);
                        Gasolinera gas = gasolineras.get(igas);
                        int diesPend = gas.getPeticiones().get(ipet);
                        
                        System.out.println("    Parada " + (ip + 1) + ":");
                        System.out.println("      Gasolinera " + igas + ": (" + gas.getCoordX() + ", " + gas.getCoordY() + ")");
                        System.out.println("      Petició " + ipet + ": " + diesPend + " dies pendent");
                        System.out.println("      Benefici: " + String.format("%.2f", getPreuDiposit(diesPend)));
                    }
                }
            }
        }
        
        // informacio de les gasolineres i les seves peticions
        System.out.println("\n" + "=".repeat(80));
        System.out.println("ESTAT DE LES GASOLINERES I LES SEVES PETICIONS");
        System.out.println("=".repeat(80));
        
        for (int ig = 0; ig < gasolineras.size(); ig++) {
            Gasolinera gas = gasolineras.get(ig);
            boolean[] ateses = gasolineras_info.get(ig);
            ArrayList<Integer> pets = gas.getPeticiones();
            
            System.out.println("\n--- GASOLINERA " + ig + " ---");
            System.out.println("Coordenades: (" + gas.getCoordX() + ", " + gas.getCoordY() + ")");
            System.out.println("Nombre de peticions: " + pets.size());
            
            for (int ip = 0; ip < pets.size(); ip++) {
                int diesPend = pets.get(ip);
                boolean atesa = ateses[ip];
                String estatStr = atesa ? "ATESA" : "NO ATESA";
                double preu = getPreuDiposit(diesPend);
                
                System.out.print("  Petició " + ip + ": " + diesPend + " dies pendent");
                System.out.print(" | Benefici: " + String.format("%.2f", preu));
                System.out.print(" | " + estatStr);
                
                if (!atesa) {
                    double perdua = calcPerdida(diesPend);
                    System.out.print(" | Pèrdua si no s'atén avui: " + String.format("%.2f", perdua));
                }
                
                System.out.println();
            }
        }
        
        System.out.println("\n" + "=".repeat(80) + "\n");
    }

    /* Classe Viaje */

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
            gasolineras_info.get(g)[ipeticion] = true;
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

        // pre: idx es 0 o 1
        // post: retorna el num de la peticio atesa de la gasolinera amb index idx
        public int getPeticio(int idx) {
            if (gasCount < idx + 1) return -1;
            return petVisitadas[idx];
        }

        // pre: idx es 0 o 1
        // post: retorna el num de la gasolinera atesa de la gasolinera amb index idx
        public int getGasolinera(int idx) {
            if (gasCount < idx + 1) return -1;
            return gasVisitadas[idx];
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
                gasolineras_info.get(g)[ipeticion] = false;
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

        public void swap_first(int g, int c, int pet) {
            if(gasCount == 1) {
                int km_old = distCentroGas[c][gasVisitadas[0]];
                int km_new = distCentroGas[c][g];
                petVisitadas[0] = pet;
                gasVisitadas[0] = g;
                kmRecorridos = kmRecorridos - km_old + km_new;
                kmsPorCamion[c] = kmsPorCamion[c] - km_old + km_new;
                beneficioActual += (km_old - km_new) * costePorKm;
                beneficiAvui += (km_old - km_new) * costePorKm;
                costeTotalKm += (km_new - km_old);
            }
            else {
                int km_come_old = distCentroGas[c][gasVisitadas[0]];
                int km_come_new = distCentroGas[c][g];
                int km_next_old = distGasGas[gasVisitadas[0]][gasVisitadas[1]];
                int km_next_new = distGasGas[g][gasVisitadas[1]];
                petVisitadas[0] = pet;
                gasVisitadas[0] = g;
                kmRecorridos = kmRecorridos - km_come_old - km_next_old + km_come_new + km_next_new;
                kmsPorCamion[c] = kmsPorCamion[c] - km_come_old - km_next_old + km_come_new + km_next_new;
                beneficioActual += (km_come_old + km_next_old - km_come_new - km_next_new) * costePorKm;
                beneficiAvui += (km_come_old + km_next_old - km_come_new - km_next_new) * costePorKm;
                costeTotalKm += (km_come_new + km_next_new - km_come_old - km_next_old);
            }
        }

        public void swap_last(int g, int c, int pet) {
            int km_back_old = distCentroGas[c][gasVisitadas[1]];
            int km_back_new = distCentroGas[c][g];
            int km_prev_old = distGasGas[gasVisitadas[0]][gasVisitadas[1]];
            int km_prev_new = distGasGas[gasVisitadas[0]][g];
            petVisitadas[1] = pet;
            gasVisitadas[1] = g;
            kmRecorridos = kmRecorridos - km_back_old - km_prev_old + km_back_new + km_prev_new;
            kmsPorCamion[c] = kmsPorCamion[c] - km_back_old - km_prev_old + km_back_new + km_prev_new;
            beneficioActual += (km_back_old + km_prev_old - km_back_new - km_prev_new) * costePorKm;
            beneficiAvui += (km_back_old + km_prev_old - km_back_new - km_prev_new) * costePorKm;
            costeTotalKm += (km_back_new + km_prev_new - km_back_old - km_prev_old);
        }

        // metodo para copiar el estado de otro viaje
        public void copyFrom(Viaje other) {
            this.kmRecorridos = other.kmRecorridos;
            this.gasCount = other.gasCount;
            System.arraycopy(other.gasVisitadas, 0, this.gasVisitadas, 0, this.gasVisitadas.length);
            System.arraycopy(other.petVisitadas, 0, this.petVisitadas, 0, this.petVisitadas.length);
        }
    }
}