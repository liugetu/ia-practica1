package IA.Gasolina;

import aima.search.framework.HeuristicFunction;

public class GasolinaHeuristicFunction implements HeuristicFunction  {
  public boolean equals(Object obj) {
      return super.equals(obj);;
  }
  
  public double getHeuristicValue(Object state) {
   ProbTSPBoard board=(GasolinaBoard)state;
   return -board.getBeneficio();
  }
}
