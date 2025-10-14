package IA.Gasolina;

import aima.search.framework.GoalTest;

public class GasolinaGoalTest implements GoalTest {
  public boolean isGoalState(Object aState) {
    System.out.println("Hem entrat a goaltest");
    return(false);
  }
}