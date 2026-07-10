package br.com.actionlabs.carboncalc.rest;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.actionlabs.carboncalc.calculation.CalculationNotFoundException;
import br.com.actionlabs.carboncalc.calculation.CalculationService;
import br.com.actionlabs.carboncalc.dto.CarbonCalculationResultDTO;
import br.com.actionlabs.carboncalc.dto.StartCalcRequestDTO;
import br.com.actionlabs.carboncalc.dto.TransportationDTO;
import br.com.actionlabs.carboncalc.dto.UpdateCalcInfoRequestDTO;
import br.com.actionlabs.carboncalc.enums.TransportationType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OpenRestController.class)
@AutoConfigureMockMvc(addFilters = false)
class OpenRestControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockBean private CalculationService calculationService;

  @Test
  void shouldStartCalculationAndReturnGeneratedId() throws Exception {
    StartCalcRequestDTO request = new StartCalcRequestDTO();
    request.setName("Jane Doe");
    request.setEmail("jane@example.com");
    request.setUf("SP");
    request.setPhoneNumber("11999999999");

    when(calculationService.startCalculation(any(StartCalcRequestDTO.class)))
        .thenReturn("abc123");

    mockMvc
        .perform(
            post("/open/start-calc")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id", is("abc123")));
  }

  @Test
  void shouldReturnBadRequestWhenStartCalculationMissingRequiredFields() throws Exception {
    StartCalcRequestDTO request = new StartCalcRequestDTO();
    request.setName("");
    request.setEmail("jane@example.com");
    request.setUf("SP");
    request.setPhoneNumber("11999999999");

    mockMvc
        .perform(
            post("/open/start-calc")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldUpdateInfoAndReturnSuccess() throws Exception {
    UpdateCalcInfoRequestDTO request = new UpdateCalcInfoRequestDTO();
    request.setId("calc-1");
    request.setEnergyConsumption(100);
    request.setSolidWasteTotal(50);
    request.setRecyclePercentage(0.4);
    TransportationDTO transportationDTO = new TransportationDTO();
    transportationDTO.setType(TransportationType.CAR);
    transportationDTO.setMonthlyDistance(120);
    request.setTransportation(List.of(transportationDTO));

    mockMvc
        .perform(
            put("/open/info")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success", is(true)));
  }

  @Test
  void shouldReturnResultForExistingCalculation() throws Exception {
    CarbonCalculationResultDTO result = new CarbonCalculationResultDTO();
    result.setEnergy(50.0);
    result.setTransportation(24.0);
    result.setSolidWaste(12.0);
    result.setTotal(86.0);

    when(calculationService.getResult("calc-1")).thenReturn(result);

    mockMvc
        .perform(get("/open/result/{id}", "calc-1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.energy", is(50.0)))
        .andExpect(jsonPath("$.transportation", is(24.0)))
        .andExpect(jsonPath("$.solidWaste", is(12.0)))
        .andExpect(jsonPath("$.total", is(86.0)));
  }

  @Test
  void shouldReturnNotFoundWhenCalculationDoesNotExist() throws Exception {
    doThrow(new CalculationNotFoundException("missing-id"))
        .when(calculationService)
        .getResult("missing-id");

    mockMvc.perform(get("/open/result/{id}", "missing-id")).andExpect(status().isNotFound());
  }
}
