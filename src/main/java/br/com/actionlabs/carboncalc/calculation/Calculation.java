package br.com.actionlabs.carboncalc.calculation;

import java.util.List;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document("calculation")
public class Calculation {
  @Id private String id;
  private String name;
  private String email;
  private String phoneNumber;
  private String uf;
  private Integer energyConsumption;
  private List<TransportationEntry> transportation;
  private Integer solidWasteTotal;
  private Double recyclePercentage;
}
