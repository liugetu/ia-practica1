package IA.Gasolina;

//~--- non-JDK imports --------------------------------------------------------

import IA.Gasolina.ProbTSPHeuristicFunction;

import aima.search.framework.Successor;
import aima.search.framework.SuccessorFunction;

//~--- JDK imports ------------------------------------------------------------

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GasolinaSuccessorFunctionSA implements SuccessorFunction {
    public List getSuccessors(Object aState) {
        ArrayList                retVal = new ArrayList();
        GasolinaBoard             board  = (GasolinaBoard) aState;
        GasolinaHeuristicFunction gasolinaHF  = new GasolinaHeuristicFunction();
        Random myRandom=new Random();
        int i,j;
        
        // Nos ahorramos generar todos los sucesores escogiendo un par de ciudades al azar
        
        i=myRandom.nextInt(board.getNCities());
       
        int k = myRandom.nextInt(2);

        boolean condicions = false;

        // Operador d'afegir
        if(k == 0) {
            int icam, iviatje, igas, ipet;
            while(!condicions) {
                condicions = true;
                icam = myRandom.nextInt(board.viajesPorCamion.size());
                iviatje = myRandom.nextInt(board.viajesPorCamion.get(icam).size());
                igas = myRandom.nextInt(board.gasolineras.size());
                ipet = myRandom.nextInt(board.gasolineras.get(igas).size());
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
        // Operador d'intercanviar
        else if(k == 1) {
            int icam, iviatje, igas, ipet;
            while(!condicions) {
                Boolean condicions = true;
                icam1 = myRandom.nextInt(board.viajesPorCamion.size());
                iviatje1 = myRandom.nextInt(board.viajesPorCamion.get(icam).size());
                igas1 = myRandom.nextInt(board.gasolineras.size());
                ipet1 = myRandom.nextInt(board.gasolineras.get(igas).size());
                icam2 = myRandom.nextInt(board.viajesPorCamion.size());
                iviatje2 = myRandom.nextInt(board.viajesPorCamion.get(icam).size());
                igas2 = myRandom.nextInt(board.gasolineras.size());
                ipet2 = myRandom.nextInt(board.gasolineras.get(igas).size());
                // comprovar que la gasolinera igas conte la peticio ipet
                if (!((board.gasolineras_info.get(igas1).second).length > ipet1)) condicions = false; 
                if (!((board.gasolineras_info.get(igas2).second).length > ipet2)) condicions = false; 
                // la peticio ha estat atesa
                if (!board.gasolineras_info.get(igas1).second[ipet1]) condicions = false;
                if (!board.gasolineras_info.get(igas2).second[ipet2]) condicions = false;
                // viatje existeix
                if (!(board.viajesPorCamion.get(icam1).size() > iviatje1)) condicions = false;
                if (!(board.viajesPorCamion.get(icam2).size() > iviatje2)) condicions = false;

                if(icam1 != icam2 && iviatje1 != iviatje2) condicions = false;

                if (condicions && newBoard.swap(igas1, ipet1, icam1, iviatje1, igas2, ipet2, icam2, iviatje2)) {
                    double v = GasolinaHF.getHeuristicValue(newBoard);
                    //String S = GasolinaBoard.INTERCAMBIO + " " + i + " " + j + " Coste(" + v + ") ---> " + newBoard.toString();

                    //retVal.add(new Successor(newBoard));
                }
            }
        }
        else {
            return retVal;
        }

        return retVal;
    }
}


//~ Formatted by Jindent --- http://www.jindent.com
