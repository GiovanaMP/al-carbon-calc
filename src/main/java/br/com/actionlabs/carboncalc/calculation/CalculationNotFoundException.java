package br.com.actionlabs.carboncalc.calculation;

public class CalculationNotFoundException extends RuntimeException {
  public CalculationNotFoundException(String id) {
    super("Calculation not found: " + id);
  }
}

class CalculationInfoNotProvidedException extends RuntimeException {
  CalculationInfoNotProvidedException(String id) {
    super("Info not provided yet for calculation: " + id);
  }
}
