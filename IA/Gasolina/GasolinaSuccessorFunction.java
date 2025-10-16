package IA.Gasolina;
import aima.search.framework.Successor;
import aima.search.framework.SuccessorFunction;
import java.util.ArrayList;
import java.util.List;

public class GasolinaSuccessorFunction implements SuccessorFunction {
    @SuppressWarnings("unchecked")
    public List getSuccessors(Object aState) {
        System.out.println("Hem entrat a successor");
        ArrayList retVal = new ArrayList();
        GasolinaBoard board = (GasolinaBoard) aState;
        GasolinaHeuristicFunction GasolinaHF = new GasolinaHeuristicFunction();
        
        // Operador d'afegir
        for (int igas = 0; igas < board.getNGasolineras(); igas++) {
            for (int ipet = 0; ipet < board.getNPeticionsGasolinera(igas); ipet++) {
                for (int icam = 0; icam < board.viajesPorCamion.size(); icam++) {
                    for (int iviatje = 0; iviatje < board.viajesPorCamion.get(icam).size(); iviatje++) {
                        GasolinaBoard newBoard = board.copy();
                        
                        Boolean condicions = true;
                        // comprovar que la gasolinera igas conte la peticio ipet
                        if (!((board.gasolineras_info.get(igas).second).length > ipet)) condicions = false; 
                        // la peticio no ha estat atesa encara
                        if (board.test(igas, ipet)) condicions = false;
                        // viatje existeix
                        if (!(board.viajesPorCamion.get(icam).size() > iviatje)) condicions = false;
                        // viatje té menys de 2 gasolineres assignades
                        if (!(board.viajesPorCamion.get(icam).get(iviatje).getNGasolineras() < 2)) condicions = false;

                        if (condicions && newBoard.addPeticio(igas, ipet, icam, iviatje)) {
                            double v = GasolinaHF.getHeuristicValue(newBoard);
                            //String S = GasolinaBoard.INTERCAMBIO + " " + i + " " + j + " Coste(" + v + ") ---> " + newBoard.toString();

                            retVal.add(new Successor("hola1", newBoard));
                        }
                    }
                }
            }
        }
        System.out.println("Acaba bucle afegir");

        
        // successors de l'operador reasignar
        // recorrem totes les peticions (parades) de tots els viatges de tots els camions
        // i provem d'assignar-les a tots els camions
        for (int icam1 = 0; icam1 < board.getNCamions(); icam1++) {
            for (int iv = 0; iv < board.getNViajesCamion(icam1); iv++) {
                for (int iparada = 0; iparada < board.getNParadasViaje(icam1, iv); iparada++) {
                    for (int icam2 = 0; icam2 < board.getNCamions(); icam2++) {
                        if (icam1 != icam2) {
                            GasolinaBoard newBoard = board.copy();
                            int igas = board.getGasolineraViaje(icam1, iv, iparada);
                            int ipet = board.getPeticioViaje(icam1, iv, iparada);

                            if (newBoard.reasignar(icam1, iv, igas, ipet, icam2)) {  // s'ha pogut fer el canvi
                                double v = GasolinaHF.getHeuristicValue(newBoard);
                                //String S = GasolinaBoard.INTERCAMBIO + " " + i + " " + j + " Coste(" + v + ") ---> " + newBoard.toString();
                                retVal.add(new Successor("hola2", newBoard));
                            }
                        }
                    }
                }
            }
        }
        System.out.println("Acaba bucle reasignar");

        // Operador de swap
        // icam2 = icam1 + 1, igas1 = igas2 + 1...
        for (int igas1 = 0; igas1 < board.gasolineras.size(); igas1++) {
            for (int ipet1 = 0; ipet1 < (board.gasolineras_info.get(igas1).second).length; ipet1++) {
                for (int icam1 = 0; icam1 < board.viajesPorCamion.size(); icam1++) {
                    for (int iviatje1 = 0; iviatje1 < board.viajesPorCamion.get(icam1).size(); iviatje1++) {
                        for (int igas2 = 0; igas2 < board.gasolineras.size(); igas2++) {
                            for (int ipet2 = 0; ipet2 < (board.gasolineras_info.get(igas2).second).length; ipet2++) {
                                for (int icam2 = 0; icam2 < board.viajesPorCamion.size(); icam2++) {
                                    for (int iviatje2 = 0; iviatje2 < board.viajesPorCamion.get(icam2).size(); iviatje2++) {
                                        GasolinaBoard newBoard = board.copy();
                                        
                                        Boolean condicions = true;
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
                                            double v = GasolinaHF.getHeuristicValue(newBoard);
                                            //String S = GasolinaBoard.INTERCAMBIO + " " + i + " " + j + " Coste(" + v + ") ---> " + newBoard.toString();

                                            retVal.add(new Successor("hola3", newBoard));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println("Acaba bucle swap");


        // operador intercanvi
        for (int igas1 = 0; igas1 < board.gasolineras.size(); igas1++) {
            for (int ipet1 = 0; ipet1 < (board.gasolineras_info.get(igas1).second).length; ipet1++) {
                for (int icam1 = 0; icam1 < board.viajesPorCamion.size(); icam1++) {
                    for (int iviatje1 = 0; iviatje1 < board.viajesPorCamion.get(icam1).size(); iviatje1++) {
                        for (int igas2 = 0; igas2 < board.gasolineras.size(); igas2++) {
                            for (int ipet2 = 0; ipet2 < (board.gasolineras_info.get(igas2).second).length; ipet2++) {
                                GasolinaBoard newBoard = board.copy();
                                        
                                Boolean condicions = true;
                                // comprovar que la gasolinera igas conte la peticio ipet
                                if (!((board.gasolineras_info.get(igas1).second).length > ipet1)) condicions = false; 
                                if (condicions && !((board.gasolineras_info.get(igas2).second).length > ipet2)) condicions = false; 
                                // Verificar que la petició 1 està atesa
                                if (condicions && !board.test(igas1, ipet1)) condicions = false;
                                // Verificar que la petició 2 NO està atesa
                                //System.out.println("**Ha passat 3 "+condicions);
                                if (condicions && board.test(igas2, ipet2)) condicions = false;
                                // viatje existeix
                                //System.out.println("**Ha passat 4 "+condicions);
                                if (condicions && !(board.viajesPorCamion.get(icam1).size() > iviatje1)) condicions = false;

                                if (condicions && newBoard.intercanvi(igas1, ipet1, icam1, iviatje1, igas2, ipet2)) {
                                    double v = GasolinaHF.getHeuristicValue(newBoard);
                                    //String S = GasolinaBoard.INTERCAMBIO + " " + i + " " + j + " Coste(" + v + ") ---> " + newBoard.toString();

                                    retVal.add(new Successor("hola4", newBoard));
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println("Acaba bucle intercanvi");

        return retVal;
    }
}