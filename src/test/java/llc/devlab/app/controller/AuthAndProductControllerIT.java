package llc.devlab.app.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import llc.devlab.app.repository.OrderLineRepository;
import llc.devlab.app.repository.OrderStatusHistoryRepository;
import llc.devlab.app.repository.ProductRepository;
import llc.devlab.app.repository.PurchaseOrderRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthAndProductControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private OrderLineRepository orderLineRepository;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void cleanDomainData() {
        orderStatusHistoryRepository.deleteAll();
        orderLineRepository.deleteAll();
        purchaseOrderRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void registerRefreshAndProductAuthorizationFlow() throws Exception {
        String registrationPayload = """
                {
                  "email": "customer@example.com",
                  "password": "Secret123!",
                  "displayName": "Customer One"
                }
                """;

        String registrationResponse = mockMvc.perform(post("/api/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(registrationPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode registrationJson = objectMapper.readTree(registrationResponse);
        String userAccessToken = registrationJson.get("accessToken").asText();
        String userRefreshToken = registrationJson.get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}
                                """.formatted(userRefreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString());

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + userAccessToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "SKU-1",
                                  "name": "Demo Product",
                                  "description": "Starter product",
                                  "unitPrice": 19.99,
                                  "availableStock": 10
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousUsersCannotAccessProtectedRoutes() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanCreateAndListProductsWithPaginationMetadata() throws Exception {
        String adminToken = login("admin@example.com", "AdminPass123!");

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "CAT-001",
                                  "name": "Catalog Product",
                                  "description": "Created by admin",
                                  "unitPrice": 12.50,
                                  "availableStock": 6
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("CAT-001"));

        mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));
    }

    @Test
    void productListingRejectsPageSizesAboveConfiguredMaximum() throws Exception {
        String adminToken = login("admin@example.com", "AdminPass123!");

        mockMvc.perform(get("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "200"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Size exceeds the maximum page size of 50."));
    }

    private String login(String email, String password) throws Exception {
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
