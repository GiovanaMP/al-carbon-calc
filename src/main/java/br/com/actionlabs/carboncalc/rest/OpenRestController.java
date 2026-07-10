package br.com.actionlabs.carboncalc.rest;

import br.com.actionlabs.carboncalc.calculation.CalculationService;
import br.com.actionlabs.carboncalc.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/open")
@RequiredArgsConstructor
@Slf4j
public class OpenRestController {

  private final CalculationService calculationService;

  @PostMapping("start-calc")
  public ResponseEntity<StartCalcResponseDTO> startCalculation(
      @Valid @RequestBody StartCalcRequestDTO request) {
    String id = calculationService.startCalculation(request);
    StartCalcResponseDTO response = new StartCalcResponseDTO();
    response.setId(id);
    return ResponseEntity.ok(response);
  }

  @PutMapping("info")
  public ResponseEntity<UpdateCalcInfoResponseDTO> updateInfo(
      @Valid @RequestBody UpdateCalcInfoRequestDTO request) {
    calculationService.updateInfo(request);
    UpdateCalcInfoResponseDTO response = new UpdateCalcInfoResponseDTO();
    response.setSuccess(true);
    return ResponseEntity.ok(response);
  }

  @GetMapping("result/{id}")
  public ResponseEntity<CarbonCalculationResultDTO> getResult(@PathVariable String id) {
    return ResponseEntity.ok(calculationService.getResult(id));
  }
}
