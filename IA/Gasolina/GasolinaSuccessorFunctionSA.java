package IA.Gasolina;

import aima.search.framework.Successor;
import aima.search.framework.SuccessorFunction;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GasolinaSuccessorFunctionSA implements SuccessorFunction {
    public List getSuccessors(Object aState) {
        ArrayList                retVal = new ArrayList();
        GasolinaBoard             board  = (GasolinaBoard) aState;
        GasolinaHeuristicFunction gasolinaHF  = new GasolinaHeuristicFunction();
        Random myRandom = new Random();
        board.printBeneKm();
               
        boolean condicions = false;
        while (!condicions) {
            condicions = true;
            // Nos ahorramos generar todos los sucesores escogiendo un par de ciudades al aza
            int k = myRandom.nextInt(4);
            // Operador d'afegir
            if(k == 0) {
                int icam, iviatje, igas, ipet;
                GasolinaBoard newBoard = board.copy();
                icam = myRandom.nextInt(board.viajesPorCamion.size());
                iviatje = myRandom.nextInt(5);  // Valor random entre 0 i 4
                igas = myRandom.nextInt(board.gasolineras.size());
                int nPet = board.getNPeticionsGasolinera(igas);
                if (nPet > 0) {
                    ipet = myRandom.nextInt(board.getNPeticionsGasolinera(igas));
                    // comprovar que la gasolinera igas conte la peticio ipet
                    if (!((newBoard.gasolineras_info.get(igas).second).length > ipet)) condicions = false; 
                    // la peticio no ha estat atesa encara
                    if (newBoard.test(igas, ipet)) condicions = false;
                    
                    // Si el camió no té el viatge iviatje, crear-lo (buit)
                    while (newBoard.viajesPorCamion.get(icam).size() <= iviatje) {
                        newBoard.viajesPorCamion.get(icam).add(newBoard.new Viaje());
                    }
                    
                    // viatje té menys de 2 gasolineres assignades
                    if (!(newBoard.viajesPorCamion.get(icam).get(iviatje).getNGasolineras() < 2)) condicions = false;

                    if (condicions && newBoard.addPeticio(igas, ipet, icam, iviatje)) {
                        double v = gasolinaHF.getHeuristicValue(newBoard);
                        String S = "El camió " + icam + " afegeix la petició " + ipet + " de la gasolinera " + igas + " al viatge " + iviatje + ". Coste(" + v + ")";
                        retVal.add(new Successor(S, newBoard));
                    }
                    else condicions = false;
                }
            }
            // Operador de swap
            else if(k == 1) {
                int icam1, iviatje1, igas1, ipet1, icam2, iviatje2, igas2, ipet2;
                GasolinaBoard newBoard = board.copy();
                icam1 = myRandom.nextInt(board.viajesPorCamion.size());
                // Comprovar que el camió té viatges
                if (board.viajesPorCamion.get(icam1).size() > 0) {
                    iviatje1 = myRandom.nextInt(board.viajesPorCamion.get(icam1).size());
                    igas1 = myRandom.nextInt(board.gasolineras.size());
                    int nPet1 = board.getNPeticionsGasolinera(igas1);
                    if (nPet1 > 0) {
                        ipet1 = myRandom.nextInt(board.getNPeticionsGasolinera(igas1));
                        icam2 = myRandom.nextInt(board.viajesPorCamion.size());
                        // Comprovar que el camió 2 també té viatges
                        if (board.viajesPorCamion.get(icam2).size() > 0) {
                            iviatje2 = myRandom.nextInt(board.viajesPorCamion.get(icam2).size());
                            igas2 = myRandom.nextInt(board.gasolineras.size());
                            int nPet2 = board.getNPeticionsGasolinera(igas2);
                            if (nPet2 > 0) {
                                ipet2 = myRandom.nextInt(board.getNPeticionsGasolinera(igas2));
                                // comprovar que la gasolinera igas conte la peticio ipet
                                if (!((board.gasolineras_info.get(igas1).second).length > ipet1)) condicions = false; 
                                if (!((board.gasolineras_info.get(igas2).second).length > ipet2)) condicions = false; 
                                // la peticio ha estat atesa
                                if (!board.test(igas1, ipet1)) condicions = false;
                                if (!board.test(igas2, ipet2)) condicions = false;
                                // viatje existeix
                                if (!(board.viajesPorCamion.get(icam1).size() > iviatje1)) condicions = false;
                                if (!(board.viajesPorCamion.get(icam2).size() > iviatje2)) condicions = false;

                                if (icam1 == icam2 && iviatje1 == iviatje2) condicions = false;
                                if (condicions && newBoard.swap(igas1, ipet1, icam1, iviatje1, igas2, ipet2, icam2, iviatje2)) {
                                    double v = gasolinaHF.getHeuristicValue(newBoard);
                                    String S = "El camió " + icam1 + " i el camió "+icam2+" s'intercanvien les peticions "+ipet1+" i "+ipet2+" de les gasolineres " +igas1+" i "+igas2+", respectivament. Coste(" + v + ")";
                                    retVal.add(new Successor(S, newBoard));
                                }
                                else condicions = false;
                            }
                            else condicions = false;
                        }
                        else condicions = false;
                    }
                    else condicions = false;
                }
                else condicions = false;
            }
            // Operador de reasignar
            else if (k == 2) {
                GasolinaBoard newBoard = board.copy();
                int icam1 = myRandom.nextInt(board.viajesPorCamion.size());
                // Comprovar que el camió té viatges
                if (board.viajesPorCamion.get(icam1).size() > 0) {
                    int iv = myRandom.nextInt(board.viajesPorCamion.get(icam1).size());
                    // Comprovar que el viatge té parades
                    if (board.viajesPorCamion.get(icam1).get(iv).getNGasolineras() > 0) {
                        int iparada = myRandom.nextInt(board.viajesPorCamion.get(icam1).get(iv).getNGasolineras());
                        int icam2 = myRandom.nextInt(board.viajesPorCamion.size());
                        int igas = board.getGasolineraViaje(icam1, iv, iparada);
                        int ipet = board.getPeticioViaje(icam1, iv, iparada);

                        if (newBoard.reasignar(icam1, iv, igas, ipet, icam2)) {  // s'ha pogut fer el canvi
                            double v = gasolinaHF.getHeuristicValue(newBoard);
                            String S = "El camió " + icam1 + " dona la petició " + ipet + " de la gasolinera " + igas + " al viatge " + iv + " del camió "+icam2+". Coste(" + v + ")";
                            retVal.add(new Successor(S, newBoard));
                        }
                        else condicions = false;
                    }
                    else condicions = false;
                }
                else condicions = false;
            }
            else {
                // operador intercanvi
                int icam1, iviatje1, igas1, ipet1, igas2, ipet2;
                GasolinaBoard newBoard = board.copy();
                icam1 = myRandom.nextInt(board.viajesPorCamion.size());
                // Comprovar que el camió té viatges
                if (board.viajesPorCamion.get(icam1).size() > 0) {
                    iviatje1 = myRandom.nextInt(board.viajesPorCamion.get(icam1).size());
                    igas1 = myRandom.nextInt(board.gasolineras.size());
                    int nPet1 = board.getNPeticionsGasolinera(igas1);
                    if (nPet1 > 0) {
                        ipet1 = myRandom.nextInt(board.getNPeticionsGasolinera(igas1));
                        igas2 = myRandom.nextInt(board.gasolineras.size());
                        int nPet2 = board.getNPeticionsGasolinera(igas2);
                        if (nPet2 > 0) {
                            ipet2 = myRandom.nextInt(board.getNPeticionsGasolinera(igas2));
                            
                            // comprovar que la gasolinera igas conte la peticio ipet
                            if (!((board.gasolineras_info.get(igas1).second).length > ipet1)) condicions = false; 
                            if (condicions && !((board.gasolineras_info.get(igas2).second).length > ipet2)) condicions = false; 
                            // Verificar que la petició 1 està atesa
                            if (condicions && !board.test(igas1, ipet1)) condicions = false;
                            // Verificar que la petició 2 NO està atesa
                            if (condicions && board.test(igas2, ipet2)) condicions = false;
                            // viatje existeix
                            if (condicions && !(board.viajesPorCamion.get(icam1).size() > iviatje1)) condicions = false;

                            if (condicions && newBoard.intercanvi(igas1, ipet1, icam1, iviatje1, igas2, ipet2)) {
                                double v = gasolinaHF.getHeuristicValue(newBoard);
                                String S = "El camió " + icam1 + " intercanvia la seva petició "+ipet1+" per la petició "+ipet2+" de les gasolineres " +igas1+" i "+igas2+", respectivament. Coste(" + v + ")";
                                retVal.add(new Successor(S, newBoard));
                            }
                            else condicions = false;
                        }
                        else condicions = false;
                    }
                    else condicions = false;
                }
                else condicions = false;
            }
        }
        return retVal;
    }
}