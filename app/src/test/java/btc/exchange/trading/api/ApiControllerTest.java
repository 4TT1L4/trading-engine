package btc.exchange.trading.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"exchange.fill-worker-enabled=false"})
@AutoConfigureMockMvc
class ApiControllerTest {

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper om;

  @Test
  void createAccount_validationError_returns400() throws Exception {
    String body = "{\"name\":\"\",\"usdBalance\":10}";

    mvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
  }

  @Test
  void orderStatusFilter_invalidStatus_returns400() throws Exception {
    mvc.perform(get("/api/orders").param("status", "nope"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_STATUS"));
  }

  @Test
  void createAndFetchOrder_overHttp_works() throws Exception {
    // create account
    var accReq = new LinkedHashMap<String, Object>();
    accReq.put("name", "api-user");
    accReq.put("usdBalance", new BigDecimal("1000"));
    var accJson = om.writeValueAsString(accReq);

    var accRes =
        mvc.perform(post("/api/accounts").contentType(MediaType.APPLICATION_JSON).content(accJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andReturn()
            .getResponse()
            .getContentAsString();

    var accId = om.readTree(accRes).get("id").asText();

    // create order
    var ordReq = new LinkedHashMap<String, Object>();
    ordReq.put("accountId", accId);
    ordReq.put("priceLimitUsdPerBtc", new BigDecimal("30000"));
    ordReq.put("amountBtc", new BigDecimal("0.01"));
    var ordJson = om.writeValueAsString(ordReq);

    var ordRes =
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(ordJson))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("OPEN"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    var orderId = om.readTree(ordRes).get("id").asText();

    // fetch order
    mvc.perform(get("/api/orders/" + orderId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(orderId))
        .andExpect(jsonPath("$.accountId").value(accId));
  }

  @Test
  void getMissingOrder_returns400WithDomainError() throws Exception {
    mvc.perform(get("/api/orders/does-not-exist"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
  }
}
