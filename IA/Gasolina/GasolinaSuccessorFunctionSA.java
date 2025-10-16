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
        
        // Nos ahorramos generar todos los sucesores escogiendo un par de ciudades al aza
       
        int k = myRandom.nextInt(2);

        boolean condicions = false;

        // Operador d'afegir
        if(k == 0) {
            int icam, iviatje, igas, ipet;
            GasolinaBoard newBoard = board.copy();
            System.out.println("Entrem Afegir");
            while(!condicions) {
                condicions = true;
                icam = myRandom.nextInt(board.viajesPorCamion.size());
                iviatje = myRandom.nextInt(board.viajesPorCamion.get(icam).size());
                igas = myRandom.nextInt(board.gasolineras.size());
                ipet = myRandom.nextInt(board.getNPeticionsGasolinera(igas));
                // comprovar que la gasolinera igas conte la peticio ipet
                if (!((board.gasolineras_info.get(igas).second).length > ipet)) condicions = false; 
                // la peticio no ha estat atesa encara
                if (((board.gasolineras_info.get(igas)).second)[ipet]) condicions = false;
                // viatje existeix
                if (!(board.viajesPorCamion.get(icam).size() > iviatje)) condicions = false;
                // viatje té menys de 2 gasolineres assignades
                if (!(board.viajesPorCamion.get(icam).get(iviatje).getNGasolineras() < 2)) condicions = false;

                if (condicions && newBoard.addPeticio(igas, ipet, icam, iviatje)) {
                    double v = gasolinaHF.getHeuristicValue(newBoard);
                    //String S = GasolinaBoard.INTERCAMBIO + " " + i + " " + j + " Coste(" + v + ") ---> " + newBoard.toString();

                    retVal.add(new Successor("hola", newBoard));
                }
            }
            System.out.println("Sortim Afegir");
        }
        // Operador d'intercanviar
        else if(k == 1) {
            int icam1, iviatje1, igas1, ipet1, icam2, iviatje2, igas2, ipet2;
            GasolinaBoard newBoard = board.copy();
            System.out.println("Entrem Intercanviar");
            while(!condicions) {

                condicions = true;
                icam1 = myRandom.nextInt(board.viajesPorCamion.size());
                iviatje1 = myRandom.nextInt(board.viajesPorCamion.get(icam1).size());
                igas1 = myRandom.nextInt(board.gasolineras.size());
                ipet1 = myRandom.nextInt(board.getNPeticionsGasolinera(igas1));
                icam2 = myRandom.nextInt(board.viajesPorCamion.size());
                iviatje2 = myRandom.nextInt(board.viajesPorCamion.get(icam2).size());
                igas2 = myRandom.nextInt(board.gasolineras.size());
                ipet2 = myRandom.nextInt(board.getNPeticionsGasolinera(igas2));
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
                    double v = gasolinaHF.getHeuristicValue(newBoard);
                    //String S = GasolinaBoard.INTERCAMBIO + " " + i + " " + j + " Coste(" + v + ") ---> " + newBoard.toString();

                    retVal.add(new Successor("hola", newBoard));
                }
            }
            System.out.println("Sortim Intercanviar");
        }
        // Operador de reasignar
        else {
            GasolinaBoard newBoard = board.copy();
            System.out.println("Entrem Reasignar");
            while(!condicions) {
                condicions = true;
                int icam1 = myRandom.nextInt(board.viajesPorCamion.size());
                int iv = myRandom.nextInt(board.viajesPorCamion.get(icam1).size());
                int iparada = myRandom.nextInt(board.viajesPorCamion.get(icam1).get(iv).getNGasolineras());
                int icam2 = myRandom.nextInt(board.viajesPorCamion.size());
                int igas = board.getGasolineraViaje(icam1, iv, iparada);
                int ipet = board.getPeticioViaje(icam1, iv, iparada);

                if (newBoard.reasignar(icam1, iv, igas, ipet, icam2)) {  // s'ha pogut fer el canvi
                    double v = gasolinaHF.getHeuristicValue(newBoard);
                    //String S = GasolinaBoard.INTERCAMBIO + " " + i + " " + j + " Coste(" + v + ") ---> " + newBoard.toString();
                    condicions = false; // per sortir del while

                    retVal.add(new Successor("hola", newBoard));
                }
            }
            System.out.println("Sortim Reasignar");
        }

        return retVal;
    }
}