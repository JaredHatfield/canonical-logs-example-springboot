package com.example.canonicallogs;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class CanonicalLoggingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void turboEncabulatorEndpointReturnsValidResponse() throws Exception {
        mockMvc.perform(post("/v1/turboencabulators/test-123/runs")
                        .header("X-User-Id", "test-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").isNumber());
    }

    @Test
    void turboEncabulatorEndpointWorksWithoutUserId() throws Exception {
        mockMvc.perform(post("/v1/turboencabulators/test-456/runs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.value").isNumber());
    }

    @Test
    void churnEndpointReturnsCompletedStatus() throws Exception {
        mockMvc.perform(post("/v1/turboencabulators/test-789/churn")
                        .header("X-User-Id", "churn-user"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"));
    }

    @Test
    void churnEndpointWorksWithoutUserId() throws Exception {
        mockMvc.perform(post("/v1/turboencabulators/test-999/churn"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("completed"));
    }

    @Test
    void helloControllerStillWorks() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());
    }
}
