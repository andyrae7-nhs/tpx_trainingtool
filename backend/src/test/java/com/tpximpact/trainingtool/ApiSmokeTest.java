package com.tpximpact.trainingtool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.seed-demo-data=false")
@AutoConfigureMockMvc
class ApiSmokeTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void frameworkIsPublic() throws Exception {
        mvc.perform(get("/api/framework/roles")).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists());
    }

    @Test
    void protectedEndpointsNeedToken() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void registerOnboardAndAnalyseGaps() throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"test@example.com\",\"password\":\"password123\",\"displayName\":\"Test Person\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(body).get("token").asText();

        mvc.perform(put("/api/me").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleId\":\"software-engineer\",\"currentGrade\":\"G8\",\"targetGrade\":\"G9\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.onboarded").value(true));

        String gap = mvc.perform(get("/api/progression/gap").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        JsonNode report = mapper.readTree(gap);
        assertThat(report.get("skills").size()).isGreaterThan(0);
        assertThat(report.get("summary").get("gaps").asInt()).isGreaterThan(0);

        mvc.perform(post("/api/recommendations").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[0].suggestions").isArray());

        mvc.perform(post("/api/journal").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Did a thing\",\"body\":\"Details\",\"impact\":\"It helped\",\"refs\":[\"BEHAVIOUR:owning-and-delivering\"]}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/api/journal/export").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.text").value(org.hamcrest.Matchers.containsString("Owning and delivering")));

        mvc.perform(get("/api/achievements").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.level.level").exists());

        mvc.perform(get("/api/leaderboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].rank").value(1));
    }
}
