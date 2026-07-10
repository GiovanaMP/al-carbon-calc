package br.com.actionlabs.carboncalc.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class StartCalcRequestDTO {
  @NotBlank private String name;
  @NotBlank private String email;
  @NotBlank private String uf;
  @NotBlank private String phoneNumber;
}
