package br.com.actionlabs.carboncalc.calculation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.actionlabs.carboncalc.dto.CarbonCalculationResultDTO;
import br.com.actionlabs.carboncalc.dto.StartCalcRequestDTO;
import br.com.actionlabs.carboncalc.dto.TransportationDTO;
import br.com.actionlabs.carboncalc.dto.UpdateCalcInfoRequestDTO;
import br.com.actionlabs.carboncalc.enums.TransportationType;
import br.com.actionlabs.carboncalc.model.EnergyEmissionFactor;
import br.com.actionlabs.carboncalc.model.SolidWasteEmissionFactor;
import br.com.actionlabs.carboncalc.model.TransportationEmissionFactor;
import br.com.actionlabs.carboncalc.repository.EnergyEmissionFactorRepository;
import br.com.actionlabs.carboncalc.repository.SolidWasteEmissionFactorRepository;
import br.com.actionlabs.carboncalc.repository.TransportationEmissionFactorRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CalculationServiceTest {

  @Mock private CalculationRepository calculationRepository;
  @Mock private EnergyEmissionFactorRepository energyEmissionFactorRepository;
  @Mock private TransportationEmissionFactorRepository transportationEmissionFactorRepository;
  @Mock private SolidWasteEmissionFactorRepository solidWasteEmissionFactorRepository;

  @InjectMocks private CalculationService calculationService;

  @Test
  void shouldCreateCalculationAndReturnGeneratedId() {
    StartCalcRequestDTO request = new StartCalcRequestDTO();
    request.setName("Jane Doe");
    request.setEmail("jane@example.com");
    request.setUf("SP");
    request.setPhoneNumber("11999999999");

    Calculation saved = new Calculation();
    saved.setId("abc123");
    saved.setName(request.getName());
    saved.setEmail(request.getEmail());
    saved.setUf(request.getUf());
    saved.setPhoneNumber(request.getPhoneNumber());

    when(calculationRepository.save(any(Calculation.class))).thenReturn(saved);

    String id = calculationService.startCalculation(request);

    assertEquals("abc123", id);

    ArgumentCaptor<Calculation> captor = ArgumentCaptor.forClass(Calculation.class);
    verify(calculationRepository).save(captor.capture());
    Calculation captured = captor.getValue();
    assertEquals(request.getName(), captured.getName());
    assertEquals(request.getEmail(), captured.getEmail());
    assertEquals(request.getPhoneNumber(), captured.getPhoneNumber());
    assertEquals(request.getUf(), captured.getUf());
  }

  @Test
  void shouldUpdateInfoAndPersistAllFields() {
    Calculation existing = new Calculation();
    existing.setId("calc-1");
    existing.setName("John Doe");
    existing.setEmail("john@example.com");
    existing.setPhoneNumber("11888888888");
    existing.setUf("RJ");

    when(calculationRepository.findById("calc-1")).thenReturn(Optional.of(existing));
    when(calculationRepository.save(any(Calculation.class))).thenAnswer(inv -> inv.getArgument(0));

    UpdateCalcInfoRequestDTO request = new UpdateCalcInfoRequestDTO();
    request.setId("calc-1");
    request.setEnergyConsumption(150);
    request.setSolidWasteTotal(30);
    request.setRecyclePercentage(0.4);
    TransportationDTO carEntry = new TransportationDTO();
    carEntry.setType(TransportationType.CAR);
    carEntry.setMonthlyDistance(200);
    TransportationDTO bikeEntry = new TransportationDTO();
    bikeEntry.setType(TransportationType.BICYCLE);
    bikeEntry.setMonthlyDistance(50);
    request.setTransportation(List.of(carEntry, bikeEntry));

    calculationService.updateInfo(request);

    ArgumentCaptor<Calculation> captor = ArgumentCaptor.forClass(Calculation.class);
    verify(calculationRepository).save(captor.capture());
    Calculation captured = captor.getValue();

    assertEquals(150, captured.getEnergyConsumption());
    assertEquals(30, captured.getSolidWasteTotal());
    assertEquals(0.4, captured.getRecyclePercentage());
    assertEquals(2, captured.getTransportation().size());
    assertEquals(TransportationType.CAR, captured.getTransportation().get(0).getType());
    assertEquals(200, captured.getTransportation().get(0).getMonthlyDistance());
    assertEquals(TransportationType.BICYCLE, captured.getTransportation().get(1).getType());
    assertEquals(50, captured.getTransportation().get(1).getMonthlyDistance());
    // fields untouched by the request remain as they were
    assertEquals("John Doe", captured.getName());
    assertEquals("RJ", captured.getUf());
  }

  @Test
  void shouldOverwritePreviousInfoRatherThanMergingIt() {
    Calculation existing = new Calculation();
    existing.setId("calc-2");
    existing.setEnergyConsumption(999);
    existing.setSolidWasteTotal(999);
    existing.setRecyclePercentage(0.99);
    existing.setTransportation(
        List.of(new TransportationEntry(TransportationType.PUBLIC_TRANSPORT, 999)));

    when(calculationRepository.findById("calc-2")).thenReturn(Optional.of(existing));
    when(calculationRepository.save(any(Calculation.class))).thenAnswer(inv -> inv.getArgument(0));

    UpdateCalcInfoRequestDTO request = new UpdateCalcInfoRequestDTO();
    request.setId("calc-2");
    request.setEnergyConsumption(10);
    request.setSolidWasteTotal(20);
    request.setRecyclePercentage(0.5);
    TransportationDTO motorcycleEntry = new TransportationDTO();
    motorcycleEntry.setType(TransportationType.MOTORCYCLE);
    motorcycleEntry.setMonthlyDistance(15);
    request.setTransportation(List.of(motorcycleEntry));

    calculationService.updateInfo(request);

    ArgumentCaptor<Calculation> captor = ArgumentCaptor.forClass(Calculation.class);
    verify(calculationRepository).save(captor.capture());
    Calculation captured = captor.getValue();

    assertEquals(10, captured.getEnergyConsumption());
    assertEquals(20, captured.getSolidWasteTotal());
    assertEquals(0.5, captured.getRecyclePercentage());
    assertEquals(1, captured.getTransportation().size());
    assertEquals(TransportationType.MOTORCYCLE, captured.getTransportation().get(0).getType());
    assertEquals(15, captured.getTransportation().get(0).getMonthlyDistance());
  }

  @Test
  void shouldThrowCalculationNotFoundWhenUpdatingUnknownId() {
    when(calculationRepository.findById("missing-id")).thenReturn(Optional.empty());

    UpdateCalcInfoRequestDTO request = new UpdateCalcInfoRequestDTO();
    request.setId("missing-id");
    request.setTransportation(List.of());

    assertThrows(
        CalculationNotFoundException.class, () -> calculationService.updateInfo(request));

    verify(calculationRepository, never()).save(any());
  }

  @Test
  void shouldComputeResultCorrectlyFromEmissionFactors() {
    Calculation calculation = new Calculation();
    calculation.setId("calc-3");
    calculation.setUf("SP");
    calculation.setEnergyConsumption(100);
    calculation.setSolidWasteTotal(50);
    calculation.setRecyclePercentage(0.4);
    calculation.setTransportation(
        List.of(
            new TransportationEntry(TransportationType.CAR, 120),
            new TransportationEntry(TransportationType.BICYCLE, 30)));

    when(calculationRepository.findById("calc-3")).thenReturn(Optional.of(calculation));

    EnergyEmissionFactor energyFactor = new EnergyEmissionFactor();
    energyFactor.setUf("SP");
    energyFactor.setFactor(0.5);
    when(energyEmissionFactorRepository.findById("SP")).thenReturn(Optional.of(energyFactor));

    SolidWasteEmissionFactor solidWasteFactor = new SolidWasteEmissionFactor();
    solidWasteFactor.setUf("SP");
    solidWasteFactor.setRecyclableFactor(0.1);
    solidWasteFactor.setNonRecyclableFactor(0.3);
    when(solidWasteEmissionFactorRepository.findById("SP"))
        .thenReturn(Optional.of(solidWasteFactor));

    TransportationEmissionFactor carFactor = new TransportationEmissionFactor();
    carFactor.setType(TransportationType.CAR);
    carFactor.setFactor(0.2);
    TransportationEmissionFactor bikeFactor = new TransportationEmissionFactor();
    bikeFactor.setType(TransportationType.BICYCLE);
    bikeFactor.setFactor(0.0);
    when(transportationEmissionFactorRepository.findById(TransportationType.CAR))
        .thenReturn(Optional.of(carFactor));
    when(transportationEmissionFactorRepository.findById(TransportationType.BICYCLE))
        .thenReturn(Optional.of(bikeFactor));

    CarbonCalculationResultDTO result = calculationService.getResult("calc-3");

    double expectedEnergy = 100 * 0.5;
    double expectedTransportation = 120 * 0.2 + 30 * 0.0;
    double expectedSolidWaste = 50 * 0.4 * 0.1 + 50 * (1 - 0.4) * 0.3;
    double expectedTotal = expectedEnergy + expectedTransportation + expectedSolidWaste;

    assertEquals(expectedEnergy, result.getEnergy(), 0.0001);
    assertEquals(expectedTransportation, result.getTransportation(), 0.0001);
    assertEquals(expectedSolidWaste, result.getSolidWaste(), 0.0001);
    assertEquals(expectedTotal, result.getTotal(), 0.0001);
  }

  @Test
  void shouldThrowCalculationNotFoundWhenGettingResultForUnknownId() {
    when(calculationRepository.findById("missing-id")).thenReturn(Optional.empty());

    assertThrows(
        CalculationNotFoundException.class, () -> calculationService.getResult("missing-id"));
  }

  @Test
  void shouldThrowInfoNotProvidedWhenEnergyConsumptionIsMissing() {
    Calculation calculation = new Calculation();
    calculation.setId("calc-4");
    calculation.setUf("SP");
    calculation.setEnergyConsumption(null);
    calculation.setSolidWasteTotal(50);
    calculation.setRecyclePercentage(0.4);
    calculation.setTransportation(List.of(new TransportationEntry(TransportationType.CAR, 120)));

    when(calculationRepository.findById("calc-4")).thenReturn(Optional.of(calculation));

    assertThrows(
        CalculationInfoNotProvidedException.class, () -> calculationService.getResult("calc-4"));
  }
}
