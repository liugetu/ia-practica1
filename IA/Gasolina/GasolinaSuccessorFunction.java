package IA.Gasolina;
import aima.search.framework.Successor;
import aima.search.framework.SuccessorFunction;
import java.util.ArrayList;
import java.util.List;

public class GasolinaSuccessorFunction implements SuccessorFunction {
    @SuppressWarnings("unchecked")
    public List getSuccessors(Object aState) {
        ArrayList retVal = new ArrayList();
        GasolinaBoard board = (GasolinaBoard) aState;
        GasolinaHeuristicFunction GasolinaHF = new GasolinaHeuristicFunction();
        
        // Operador d'afegir
        for (int igas = 0; igas < board.gasolineras.size(); igas++) {
            for (int ipet = 0; ipet < (board.gasolineras_info.get(igas).second).length; ipet++) {
                for (int icam = 0; icam < board.viajesPorCamion.size(); icam++) {
                    for (int iviatje = 0; iviatje < board.viajesPorCamion.get(icam).size(); iviatje++) {
                        GasolinaBoard newBoard = new GasolinaBoard(board.camions, board.gasolineras);
                        
                        Boolean condicions = true;
                        // comprovar que la gasolinera igas conte la peticio ipet
                        if (!((board.gasolineras_info.get(igas).second).length > ipet)) condicions = false; 
                        // la peticio no ha estat atesa encara
                        if (((board.gasolineras_info.get(igas)).second)[ipet]) condicions = false;
                        // viatje existeix
                        if (!(board.viajesPorCamion.get(icam).size() > iviatje)) condicions = false;
                        // viatje té menys de 2 gasolineres assignades
                        if (!(board.viajesPorCamion.get(icam).get(iviatje).getNGasolineras() < 2)) condicions = false;

                        if (condicions && newBoard.addPeticio(igas, ipet, icam, iviatje)) {
                            double v = GasolinaHF.getHeuristicValue(newBoard);
                            //String S = GasolinaBoard.INTERCAMBIO + " " + i + " " + j + " Coste(" + v + ") ---> " + newBoard.toString();

                            //retVal.add(new Successor(newBoard));
                        }
                    }
                }
            }
        }

        
        // successors de l'operador reasignar
        // recorrem totes les peticions (parades) de tots els viatges de tots els camions
        // i provem d'assignar-les a tots els camions
        for (int icam1 = 0; icam1 < board.getNCamions(); icam1++) {
            for (int iv = 0; iv < board.getNViajesCamion(icam1); iv++) {
                for (int igas = 0; igas < board.getNParadasViaje(icam1, iv); igas++) {
                    for (int icam2 = 0; icam2 < board.getNCamions(); icam2++) {
                        if (icam1 != icam2) {
                            GasolinaBoard newBoard = new GasolinaBoard(board.camions, board.gasolineras);
                            int ipet = board.getPeticioViaje(icam1, iv, igas);

                            if (newBoard.reasignar(icam1, iv, igas, ipet, icam2)) {  // s'ha pogut fer el canvi
                                double v = GasolinaHF.getHeuristicValue(newBoard);
                                //String S = GasolinaBoard.INTERCAMBIO + " " + i + " " + j + " Coste(" + v + ") ---> " + newBoard.toString();

                                //retVal.add(new Successor(newBoard));
                            }
                        }
                    }
                }
            }
        }

        // Operador de swap
        for (int igas1 = 0; igas1 < board.gasolineras.size(); igas1++) {
            for (int ipet1 = 0; ipet1 < (board.gasolineras_info.get(igas1).second).length; ipet1++) {
                for (int icam1 = 0; icam1 < board.viajesPorCamion.size(); icam1++) {
                    for (int iviatje1 = 0; iviatje1 < board.viajesPorCamion.get(icam1).size(); iviatje1++) {
                        for (int igas2 = 0; igas2 < board.gasolineras.size(); igas2++) {
                            for (int ipet2 = 0; ipet2 < (board.gasolineras_info.get(igas2).second).length; ipet2++) {
                                for (int icam2 = 0; icam2 < board.viajesPorCamion.size(); icam2++) {
                                    for (int iviatje2 = 0; iviatje2 < board.viajesPorCamion.get(icam2).size(); iviatje2++) {
                                        GasolinaBoard newBoard = new GasolinaBoard(board.camions, board.gasolineras);
                                        
                                        Boolean condicions = true;
                                        // comprovar que la gasolinera igas conte la peticio ipet
                                        if (!((board.gasolineras_info.get(igas1).second).length > ipet1)) condicions = false; 
                                        if (!((board.gasolineras_info.get(igas2).second).length > ipet2)) condicions = false; 
                                        // la peticio ha estat atesa
                                        if (!board.gasolineras_info.get(igas1).second[ipet1]) condicions = false;
                                        if (!board.gasolineras_info.get(igas2).second[ipet2]) condicions = false;
                                        // viatje existeix
                                        if (!(board.viajesPorCamion.get(icam1).size() > iviatje1)) condicions = false;
                                        if (!(board.viajesPorCamion.get(icam2).size() > iviatje2)) condicions = false;

                                        if (condicions && newBoard.swap(igas1, ipet1, icam1, iviatje1, igas2, ipet2, icam2, iviatje2)) {
                                            double v = GasolinaHF.getHeuristicValue(newBoard);
                                            //String S = GasolinaBoard.INTERCAMBIO + " " + i + " " + j + " Coste(" + v + ") ---> " + newBoard.toString();

                                            //retVal.add(new Successor(newBoard));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return retVal;
    }
}