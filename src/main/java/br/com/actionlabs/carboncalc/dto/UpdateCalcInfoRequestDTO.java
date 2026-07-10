package br.com.actionlabs.carboncalc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UpdateCalcInfoRequestDTO {
  @NotBlank private String id;
  private int energyConsumption;
  @NotNull private List<TransportationDTO> transportation;
  private int solidWasteTotal;
  private double recyclePercentage;
}
