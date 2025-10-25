package IA.Gasolina;
import aima.search.framework.Successor;
import aima.search.framework.SuccessorFunction;
import java.util.ArrayList;
import java.util.List;

public class GasolinaSuccessorFunction implements SuccessorFunction {
    
    public static final int limitViatgesCamio = 5;
    private int limit;
    
    // Constructor que acepta un límite personalizado
    public GasolinaSuccessorFunction(int limit) {
        // Si el límite es -1, eliminar la restricción (establecer a infinito)
        this.limit = (limit == -1) ? Integer.MAX_VALUE : limit;
    }

    @SuppressWarnings("unchecked")
    public List getSuccessors(Object aState) {
        ArrayList retVal = new ArrayList();
        GasolinaBoard board = (GasolinaBoard) aState;
        GasolinaHeuristicFunction GasolinaHF = new GasolinaHeuristicFunction();
        double currentValue = GasolinaHF.getHeuristicValue(board);
        //board.printBeneKm();
        int counter = 0;
        
        // Operador d'afegir (optimizado para iterar solo sobre peticiones no atendidas)
        for (int igas = 0; igas < board.getNGasolineras() && counter < limit; igas++) {
            for (int ipet = 0; ipet < board.getNPeticionsGasolinera(igas) && counter < limit; ipet++) {
                // Solo procesar si la petición NO está atendida
                if (!board.isPeticioAtesa(igas, ipet)) {
                    for (int icam = 0; icam < board.viajesPorCamion.size() && counter < limit; icam++) {
                        for (int iviatje = 0; iviatje < limitViatgesCamio && counter < limit; iviatje++) {
                            ++counter;
                            GasolinaBoard newBoard = board.copy();
                            
                            // Si el camión no tiene el viaje iviatje, crearlo (vacío)
                            while (newBoard.viajesPorCamion.get(icam).size() <= iviatje) {
                                newBoard.viajesPorCamion.get(icam).add(newBoard.new Viaje());
                            }
                            
                            // Verificar que el viaje tiene menos de 2 gasolineras asignadas
                            if (newBoard.viajesPorCamion.get(icam).get(iviatje).getNGasolineras() < 2) {
                                if (newBoard.addPeticio(igas, ipet, icam, iviatje)) {
                                    double v = GasolinaHF.getHeuristicValue(newBoard);
                                    if (v < currentValue) {
                                        String S = "El camió " + icam + " afegeix la petició " + ipet + " de la gasolinera " + igas + " al viatge " + iviatje + ". Coste(" + v + ")";
                                        retVal.add(new Successor(S, newBoard));
                                        return retVal;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        counter = 0;

        // operadores de intercanvi + swap (con nueva iteracion por camion/viaje/peticion)
        for (int icam1 = 0; icam1 < board.viajesPorCamion.size() && counter < limit; icam1++) {
            for (int iviatje1 = 0; iviatje1 < board.viajesPorCamion.get(icam1).size() && counter < limit; iviatje1++) {
                // obtener el viaje actual
                GasolinaBoard.Viaje viaje1 = board.viajesPorCamion.get(icam1).get(iviatje1);
                
                // iterar sobre las peticiones dentro del viaje (posiciones 0 y 1)
                for (int ipos1 = 0; ipos1 < viaje1.gasCount && counter < limit; ipos1++) {
                    int igas1 = viaje1.gasVisitadas[ipos1];
                    int ipet1 = viaje1.petVisitadas[ipos1];
                    
                    // aplicar operador intercanvi: buscar peticiones no atendidas para intercambiar
                    for (int igas2 = 0; igas2 < board.gasolineras.size() && counter < limit; igas2++) {
                        for (int ipet2 = 0; ipet2 < (board.gasolineras_info.get(igas2)).length && counter < limit; ipet2++) {
                            ++counter;
                            
                            // verificar que la peticion 2 NO esta atendida
                            if (!board.isPeticioAtesa(igas2, ipet2)) {
                                GasolinaBoard newBoard = board.copy();
                                
                                if (newBoard.intercanviByPosition(icam1, iviatje1, ipos1, igas2, ipet2)) {
                                    double v = GasolinaHF.getHeuristicValue(newBoard);
                                    if (v < currentValue) {
                                        String S = "El camió " + icam1 + " intercanvia la seva petició "+ipet1+" de la gasolinera " +igas1+" per la petició "+ipet2+" de la gasolinera "+igas2+". Coste(" + v + ")";
                                        retVal.add(new Successor(S, newBoard));
                                        return retVal;
                                    }
                                }
                            }
                        }
                    }
                    
                    // aplicar operador swap: buscar otras peticiones atendidas para intercambiar
                    for (int icam2 = icam1; icam2 < board.viajesPorCamion.size() && counter < limit; icam2++) {
                        int startViaje = (icam2 == icam1) ? iviatje1 : 0; // evitar duplicados
                        for (int iviatje2 = startViaje; iviatje2 < board.viajesPorCamion.get(icam2).size() && counter < limit; iviatje2++) {
                            GasolinaBoard.Viaje viaje2 = board.viajesPorCamion.get(icam2).get(iviatje2);
                            
                            for (int ipos2 = 0; ipos2 < viaje2.gasCount && counter < limit; ipos2++) {
                                ++counter;
                                
                                // evitar swap de la misma peticion consigo misma
                                if (icam1 == icam2 && iviatje1 == iviatje2 && ipos1 == ipos2) continue;
                                
                                GasolinaBoard newBoard = board.copy();
                                
                                if (newBoard.swapByPosition(icam1, iviatje1, ipos1, icam2, iviatje2, ipos2)) {
                                    double v = GasolinaHF.getHeuristicValue(newBoard);
                                    if (v < currentValue) {
                                        int igas2 = viaje2.gasVisitadas[ipos2];
                                        int ipet2 = viaje2.petVisitadas[ipos2];
                                        String S = "El camió " + icam1 + " i el camió " + icam2 + " intercanvien les seves peticions "+ipet1+" i "+ipet2+" de les gasolineres " +igas1+" i "+igas2+", respectivament. Coste(" + v + ")";
                                        retVal.add(new Successor(S, newBoard));
                                        return retVal;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // comentem l'operador reasignar perque no s'arriba a utilitzar
        // successors de l'operador reasignar
        // recorrem totes les peticions (parades) de tots els viatges de tots els camions
        // i provem d'assignar-les a tots els camions
        /*for (int icam1 = 0; icam1 < board.getNCamions() && counter < limit; icam1++) {
            for (int iv = 0; iv < board.getNViajesCamion(icam1) && counter < limit; iv++) {
                for (int iparada = 0; iparada < board.getNParadasViaje(icam1, iv) && counter < limit; iparada++) {
                    for (int icam2 = 0; icam2 < board.getNCamions() && counter < limit; icam2++) {
                        ++counter;
                        if (icam1 != icam2) {
                            GasolinaBoard newBoard = board.copy();
                            int igas = board.getGasolineraViaje(icam1, iv, iparada);
                            int ipet = board.getPeticioViaje(icam1, iv, iparada);

                            if (newBoard.reasignar(icam1, iv, igas, ipet, icam2)) {  // s'ha pogut fer el canvi
                                double v = GasolinaHF.getHeuristicValue(newBoard);
                                if(v < currentValue) {
                                    String S = "El camió " + icam1 + " dona la petició " + ipet + " de la gasolinera " + igas + " al viatge " + iv + " del camió "+icam2+". Coste(" + v + ")";
                                    retVal.add(new Successor(S, newBoard));
                                    return retVal;
                                }
                            }
                        }
                    }
                }
            }
        }*/

        return retVal;
    }
}