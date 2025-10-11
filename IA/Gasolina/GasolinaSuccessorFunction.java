package IA.Gasolina;

//~--- non-JDK imports --------------------------------------------------------


import aima.search.framework.Successor;
import aima.search.framework.SuccessorFunction;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ravi Mohan
 *
 */
public class GasolinaSuccessorFunction implements SuccessorFunction {
    @SuppressWarnings("unchecked")
    public List getSuccessors(Object aState) {
        ArrayList retVal = new ArrayList();
        GasolinaBoard board = (GasolinaBoard) aState;
        GasolinaHeuristicFunction GasolinaHF = new GasolinaHeuristicFunction();
        
        // Operador d'afegir
        for (int igas = 0; igas < board.gasolineras.length; igas++) {
            for (int ipet = 0; ipet < board.gasolineras.get(igas).length; ipet++) {
                for (int icam = 0; icam < board.viajesPorCamion.length; icam++) {
                    for (int iviatje = 0; iviatje < board.viajesPorCamion.get(icam).length; iviatje++) {
                        GasolinaBoard newBoard = new GasolinaBoard(board.camions, board.gasolineras);
                        
                        Boolean condicions = true;
                        // comprovar que la gasolinera igas conte la peticio ipet
                        if (!((board.gasolineras_info.get(igas).second).length > ipet)) condicions = false; 
                        // la peticio no ha estat atesa encara
                        if (board.gasolineras_info.get(igas).second.get(ipet)) condicions = false;
                        // viatje existeix
                        if (!(board.viajesPorCamion.get(icam).length > iviatje)) condicions = false;
                        // viatje té menys de 2 gasolineres assignades
                        if (!(board.viajesPorCamion.get(icam).get(iviatje).getNGasolineras() < 2)) condicions = false;

                        if (condicions) {
                            newBoard.addPeticio(igas, ipet, icam, iviatje);

                            double v = GasolinaHF.getHeuristicValue(newBoard);
                            //String S = GasolinaBoard.INTERCAMBIO + " " + i + " " + j + " Coste(" + v + ") ---> " + newBoard.toString();

                            retVal.add(new Successor(newBoard));
                        }
                    }
                }
            }
        }

        return retVal;
    }
}


//~ Formatted by Jindent --- http://www.jindent.com
