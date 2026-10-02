package com.tpximpact.trainingtool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.seed-demo-data=false")
@AutoConfigureMockMvc
class CertificationApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private String register(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"password123\",\"displayName\":\"Cert Person\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(body).get("token").asText();
        mvc.perform(put("/api/me").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"roleId\":\"software-engineer\",\"currentGrade\":\"G8\",\"targetGrade\":\"G9\"}"))
                .andExpect(status().isOk());
        return token;
    }

    private JsonNode call(String token, MockHttpServletRequestBuilder b, int status) throws Exception {
        String body = mvc.perform(b.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is(status)).andReturn().getResponse().getContentAsString();
        return body.isEmpty() ? null : mapper.readTree(body);
    }

    @Test
    void certificationsNeedToken() throws Exception {
        mvc.perform(get("/api/certifications")).andExpect(status().isUnauthorized());
    }

    @Test
    void addCertificationAndSeeItInGapAnalysisAndExport() throws Exception {
        String token = register("certs@example.com");

        JsonNode known = call(token, get("/api/certifications/known"), 200);
        assertThat(known.size()).isGreaterThan(20);

        String name = "AWS Certified Developer - Associate";
        JsonNode suggestions = call(token, post("/api/certifications/suggest")
                .content("{\"name\":\"" + name + "\",\"issuer\":\"Amazon Web Services\"}"), 200);
        List<String> refs = new ArrayList<>();
        suggestions.forEach(s -> refs.add(s.get("ref").asText()));
        assertThat(refs).contains("BEHAVIOUR:developing-your-craft");
        assertThat(refs).anyMatch(r -> r.startsWith("SKILL:"));

        ObjectNode input = mapper.createObjectNode()
                .put("name", name).put("issuer", "Amazon Web Services").put("status", "EARNED")
                .put("credentialId", "ABC-123").put("credentialUrl", "https://aws.amazon.com/verification")
                .put("issuedOn", "2025-03-01").put("expiresOn", LocalDate.now().plusYears(2).toString());
        ArrayNode refArray = input.putArray("refs");
        refs.forEach(refArray::add);
        refArray.add("SKILL:not-a-real-skill");

        JsonNode created = call(token, post("/api/certifications").content(input.toString()), 201);
        assertThat(created.get("expiry").asText()).isEqualTo("ACTIVE");
        assertThat(created.get("refs").size()).as("unknown refs are dropped").isEqualTo(refs.size());
        long id = created.get("id").asLong();

        // Shows next to the matching items in gap analysis.
        JsonNode gap = call(token, get("/api/progression/gap"), 200);
        List<JsonNode> all = new ArrayList<>();
        for (String section : List.of("skills", "behaviours", "impacts")) gap.get(section).forEach(all::add);
        JsonNode tagged = all.stream().filter(i -> refs.contains(i.get("ref").asText())).findFirst().orElseThrow();
        assertThat(tagged.get("certifications").get(0).get("name").asText()).isEqualTo(name);
        assertThat(all.stream().filter(i -> !refs.contains(i.get("ref").asText()))
                .allMatch(i -> i.get("certifications").isEmpty())).isTrue();

        // And in the evidence export.
        JsonNode export = call(token, get("/api/journal/export"), 200);
        String text = export.get("text").asText();
        assertThat(text).contains("Certification: " + name);
        assertThat(text).contains("== CERTIFICATIONS ==").contains("Credential ID: ABC-123");

        // Validation.
        call(token, post("/api/certifications").content("{\"name\":\"X\",\"credentialUrl\":\"javascript:alert(1)\"}"), 400);
        call(token, post("/api/certifications").content("{\"name\":\"X\",\"issuedOn\":\"2025-01-01\",\"expiresOn\":\"2024-01-01\"}"), 400);
        call(token, post("/api/certifications").content("{\"name\":\"X\",\"status\":\"MAYBE\"}"), 400);

        // In progress, with an expired one sorted last.
        call(token, post("/api/certifications").content("{\"name\":\"CISSP\",\"status\":\"IN_PROGRESS\"}"), 201);
        call(token, post("/api/certifications").content("{\"name\":\"Old cert\",\"issuedOn\":\"2019-01-01\",\"expiresOn\":\"2022-01-01\"}"), 201);
        JsonNode list = call(token, get("/api/certifications"), 200);
        assertThat(list.size()).isEqualTo(3);
        assertThat(list.get(0).get("status").asText()).isEqualTo("IN_PROGRESS");
        assertThat(list.get(2).get("expiry").asText()).isEqualTo("EXPIRED");
        assertThat(StreamSupport.stream(list.spliterator(), false).map(c -> c.get("name").asText())).contains(name);

        // Other people can't touch it.
        String other = register("other-certs@example.com");
        call(other, delete("/api/certifications/" + id), 403);

        call(token, delete("/api/certifications/" + id), 204);
        assertThat(call(token, get("/api/certifications"), 200).size()).isEqualTo(2);
    }
}
