package br.com.actionlabs.carboncalc.calculation;

import br.com.actionlabs.carboncalc.dto.CarbonCalculationResultDTO;
import br.com.actionlabs.carboncalc.dto.StartCalcRequestDTO;
import br.com.actionlabs.carboncalc.dto.TransportationDTO;
import br.com.actionlabs.carboncalc.dto.UpdateCalcInfoRequestDTO;
import br.com.actionlabs.carboncalc.model.EnergyEmissionFactor;
import br.com.actionlabs.carboncalc.model.SolidWasteEmissionFactor;
import br.com.actionlabs.carboncalc.repository.EnergyEmissionFactorRepository;
import br.com.actionlabs.carboncalc.repository.SolidWasteEmissionFactorRepository;
import br.com.actionlabs.carboncalc.repository.TransportationEmissionFactorRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CalculationService {

  private final CalculationRepository calculationRepository;
  private final EnergyEmissionFactorRepository energyEmissionFactorRepository;
  private final TransportationEmissionFactorRepository transportationEmissionFactorRepository;
  private final SolidWasteEmissionFactorRepository solidWasteEmissionFactorRepository;

  public String startCalculation(StartCalcRequestDTO request) {
    Calculation calculation = new Calculation();
    calculation.setName(request.getName());
    calculation.setEmail(request.getEmail());
    calculation.setPhoneNumber(request.getPhoneNumber());
    calculation.setUf(request.getUf());

    Calculation saved = calculationRepository.save(calculation);
    return saved.getId();
  }

  public void updateInfo(UpdateCalcInfoRequestDTO request) {
    Calculation calculation =
        calculationRepository
            .findById(request.getId())
            .orElseThrow(() -> new CalculationNotFoundException(request.getId()));

    calculation.setEnergyConsumption(request.getEnergyConsumption());
    calculation.setSolidWasteTotal(request.getSolidWasteTotal());
    calculation.setRecyclePercentage(request.getRecyclePercentage());
    calculation.setTransportation(mapTransportation(request.getTransportation()));

    calculationRepository.save(calculation);
  }

  public CarbonCalculationResultDTO getResult(String id) {
    Calculation calculation =
        calculationRepository.findById(id).orElseThrow(() -> new CalculationNotFoundException(id));

    if (calculation.getEnergyConsumption() == null
        || calculation.getTransportation() == null
        || calculation.getSolidWasteTotal() == null
        || calculation.getRecyclePercentage() == null) {
      throw new CalculationInfoNotProvidedException(id);
    }

    EnergyEmissionFactor energyFactor =
        energyEmissionFactorRepository
            .findById(calculation.getUf())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Missing energy emission factor for UF: " + calculation.getUf()));

    SolidWasteEmissionFactor solidWasteFactor =
        solidWasteEmissionFactorRepository
            .findById(calculation.getUf())
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Missing solid waste emission factor for UF: " + calculation.getUf()));

    double energy = calculation.getEnergyConsumption() * energyFactor.getFactor();

    double transportation =
        calculation.getTransportation().stream()
            .mapToDouble(
                entry ->
                    entry.getMonthlyDistance()
                        * transportationEmissionFactorRepository
                            .findById(entry.getType())
                            .orElseThrow(
                                () ->
                                    new IllegalStateException(
                                        "Missing transportation emission factor for type: "
                                            + entry.getType()))
                            .getFactor())
            .sum();

    int solidWasteTotal = calculation.getSolidWasteTotal();
    double recyclePercentage = calculation.getRecyclePercentage();
    double solidWaste =
        solidWasteTotal * recyclePercentage * solidWasteFactor.getRecyclableFactor()
            + solidWasteTotal * (1 - recyclePercentage) * solidWasteFactor.getNonRecyclableFactor();

    double total = energy + transportation + solidWaste;

    CarbonCalculationResultDTO result = new CarbonCalculationResultDTO();
    result.setEnergy(energy);
    result.setTransportation(transportation);
    result.setSolidWaste(solidWaste);
    result.setTotal(total);
    return result;
  }

  private List<TransportationEntry> mapTransportation(List<TransportationDTO> transportation) {
    return transportation.stream()
        .map(dto -> new TransportationEntry(dto.getType(), dto.getMonthlyDistance()))
        .collect(Collectors.toList());
  }
}
