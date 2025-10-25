package IA.Gasolina;

import aima.search.framework.Successor;
import aima.search.framework.SuccessorFunction;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GasolinaSuccessorFunctionSA implements SuccessorFunction {

    public static final int limitViatgesCamio = 5;

    public List getSuccessors(Object aState) {
        ArrayList                retVal = new ArrayList();
        GasolinaBoard             board  = (GasolinaBoard) aState;
        GasolinaHeuristicFunction gasolinaHF  = new GasolinaHeuristicFunction();
        Random myRandom = new Random();
        //board.printBeneKm();
        
        // Precalcular listas para evitar código repetido
        ArrayList<int[]> viajesConPeticiones = buscarViajesConPeticiones(board);
        ArrayList<int[]> peticionesNoAtendidas = buscarPeticionesNoAtendidas(board);
               
        boolean condicions = false;
        while (!condicions) {
            condicions = true;
            int k = myRandom.nextInt(3);
            // Operador d'afegir
            if(k == 0) {
                int icam, iviatje, igas, ipet;
                GasolinaBoard newBoard = board.copy();
                
                if (peticionesNoAtendidas.size() > 0) {
                    // Seleccionar una petición no atendida aleatoria directamente
                    int idx = myRandom.nextInt(peticionesNoAtendidas.size());
                    int[] peticion = peticionesNoAtendidas.get(idx);
                    igas = peticion[0];
                    ipet = peticion[1];
                    
                    // Seleccionar camión aleatorio
                    icam = myRandom.nextInt(board.viajesPorCamion.size());
                    iviatje = myRandom.nextInt(limitViatgesCamio);  // Valor random entre 0 i 4
                    
                    // Si el camión no tiene el viaje iviatje, crearlo (vacío)
                    while (newBoard.viajesPorCamion.get(icam).size() <= iviatje) {
                        newBoard.viajesPorCamion.get(icam).add(newBoard.new Viaje());
                    }
                    
                    // Verificar que el viaje tiene menos de 2 gasolineras asignadas
                    if (newBoard.viajesPorCamion.get(icam).get(iviatje).getNGasolineras() < 2) {
                        if (newBoard.addPeticio(igas, ipet, icam, iviatje)) {
                            double v = gasolinaHF.getHeuristicValue(newBoard);
                            String S = "El camió " + icam + " afegeix la petició " + ipet + " de la gasolinera " + igas + " al viatge " + iviatje + ". Coste(" + v + ")";
                            retVal.add(new Successor(S, newBoard));
                        }
                        else condicions = false;
                    }
                    else condicions = false;
                }
                else condicions = false; // No hay peticiones disponibles para añadir
            }
            // Operador de swap
            else if(k == 1) {
                int icam1, iviatje1, ipos1, icam2, iviatje2, ipos2;
                GasolinaBoard newBoard = board.copy();
                
                if (viajesConPeticiones.size() >= 2) {
                    // Seleccionar dos posiciones aleatorias diferentes
                    int idx1 = myRandom.nextInt(viajesConPeticiones.size());
                    int idx2;
                    do {
                        idx2 = myRandom.nextInt(viajesConPeticiones.size());
                    } while (idx2 == idx1);
                    
                    int[] pos1 = viajesConPeticiones.get(idx1);
                    int[] pos2 = viajesConPeticiones.get(idx2);
                    
                    icam1 = pos1[0]; iviatje1 = pos1[1]; ipos1 = pos1[2];
                    icam2 = pos2[0]; iviatje2 = pos2[1]; ipos2 = pos2[2];
                    
                    if (newBoard.swapByPosition(icam1, iviatje1, ipos1, icam2, iviatje2, ipos2)) {
                        double v = gasolinaHF.getHeuristicValue(newBoard);
                        // Obtener información para el mensaje
                        GasolinaBoard.Viaje v1 = board.viajesPorCamion.get(icam1).get(iviatje1);
                        GasolinaBoard.Viaje v2 = board.viajesPorCamion.get(icam2).get(iviatje2);
                        int igas1 = v1.gasVisitadas[ipos1], ipet1 = v1.petVisitadas[ipos1];
                        int igas2 = v2.gasVisitadas[ipos2], ipet2 = v2.petVisitadas[ipos2];
                        String S = "El camió " + icam1 + " i el camió "+icam2+" s'intercanvien les peticions "+ipet1+" i "+ipet2+" de les gasolineres " +igas1+" i "+igas2+", respectivament. Coste(" + v + ")";
                        retVal.add(new Successor(S, newBoard));
                    }
                    else condicions = false;
                }
                else condicions = false;
            }
            // Operador d'intercanvi
            else {
                int icam1, iviatje1, ipos1, igas2, ipet2;
                GasolinaBoard newBoard = board.copy();
                
                if (viajesConPeticiones.size() > 0 && peticionesNoAtendidas.size() > 0) {
                    // Seleccionar una petición atendida aleatoria
                    int idx1 = myRandom.nextInt(viajesConPeticiones.size());
                    int[] pos1 = viajesConPeticiones.get(idx1);
                    icam1 = pos1[0]; iviatje1 = pos1[1]; ipos1 = pos1[2];
                    
                    // Seleccionar una petición no atendida aleatoria
                    int idx2 = myRandom.nextInt(peticionesNoAtendidas.size());
                    int[] pos2 = peticionesNoAtendidas.get(idx2);
                    igas2 = pos2[0]; ipet2 = pos2[1];
                    
                    if (newBoard.intercanviByPosition(icam1, iviatje1, ipos1, igas2, ipet2)) {
                        double v = gasolinaHF.getHeuristicValue(newBoard);
                        // Obtener información para el mensaje
                        GasolinaBoard.Viaje v1 = board.viajesPorCamion.get(icam1).get(iviatje1);
                        int igas1 = v1.gasVisitadas[ipos1], ipet1 = v1.petVisitadas[ipos1];
                        String S = "El camió " + icam1 + " intercanvia la seva petició "+ipet1+" per la petició "+ipet2+" de les gasolineres " +igas1+" i "+igas2+", respectivament. Coste(" + v + ")";
                        retVal.add(new Successor(S, newBoard));
                    }
                    else condicions = false;
                }
                else condicions = false;
            }
        }
        return retVal;
    }
    
    // Método auxiliar para buscar viajes con peticiones atendidas
    private ArrayList<int[]> buscarViajesConPeticiones(GasolinaBoard board) {
        ArrayList<int[]> viajesConPeticiones = new ArrayList<>();
        for (int ic = 0; ic < board.viajesPorCamion.size(); ic++) {
            for (int iv = 0; iv < board.viajesPorCamion.get(ic).size(); iv++) {
                GasolinaBoard.Viaje viaje = board.viajesPorCamion.get(ic).get(iv);
                for (int pos = 0; pos < viaje.gasCount; pos++) {
                    viajesConPeticiones.add(new int[]{ic, iv, pos});
                }
            }
        }
        return viajesConPeticiones;
    }
    
    // Método auxiliar para buscar peticiones no atendidas
    private ArrayList<int[]> buscarPeticionesNoAtendidas(GasolinaBoard board) {
        ArrayList<int[]> peticionesNoAtendidas = new ArrayList<>();
        for (int ig = 0; ig < board.gasolineras.size(); ig++) {
            for (int ip = 0; ip < board.getNPeticionsGasolinera(ig); ip++) {
                if (!board.isPeticioAtesa(ig, ip)) {
                    peticionesNoAtendidas.add(new int[]{ig, ip});
                }
            }
        }
        return peticionesNoAtendidas;
    }
}