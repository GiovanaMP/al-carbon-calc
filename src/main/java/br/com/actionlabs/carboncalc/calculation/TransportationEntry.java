package br.com.actionlabs.carboncalc.calculation;

import br.com.actionlabs.carboncalc.enums.TransportationType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransportationEntry {
  private TransportationType type;
  private int monthlyDistance;
}
