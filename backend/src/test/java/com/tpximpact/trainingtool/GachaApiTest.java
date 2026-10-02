package com.tpximpact.trainingtool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "app.seed-demo-data=false")
@AutoConfigureMockMvc
class GachaApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private String token;

    private MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder b) {
        return b.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    private JsonNode json(MockHttpServletRequestBuilder b, int status) throws Exception {
        String body = mvc.perform(auth(b)).andExpect(status().is(status)).andReturn().getResponse().getContentAsString();
        return mapper.readTree(body);
    }

    @Test
    void gachaNeedsToken() throws Exception {
        mvc.perform(get("/api/gacha")).andExpect(status().isUnauthorized());
    }

    @Test
    void pullShopAndCollect() throws Exception {
        String reg = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"whale@example.com\",\"password\":\"password123\",\"displayName\":\"Wally Whale\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        token = mapper.readTree(reg).get("token").asText();

        // Starter gems cover exactly one standard 10-pull.
        JsonNode overview = json(get("/api/gacha"), 200);
        assertThat(overview.at("/wallet/gems").asLong()).isEqualTo(1000);
        assertThat(overview.get("banners").size()).isEqualTo(3);
        assertThat(overview.get("shop").size()).isGreaterThan(0);
        assertThat(overview.get("poolSize").asInt()).isGreaterThan(100);
        int startXp = overview.at("/wallet/xp").asInt();

        JsonNode first = json(post("/api/gacha/pull").content("{\"banner\":\"STANDARD\"}"), 200);
        assertThat(first.get("cards").size()).isEqualTo(10);
        assertThat(StreamSupport.stream(first.get("cards").spliterator(), false)
                .anyMatch(c -> !"COMMON".equals(c.at("/idea/rarity").asText())))
                .as("every 10-pull has at least one Rare or better").isTrue();
        assertThat(first.get("xpGained").asInt()).isGreaterThan(0);
        assertThat(first.at("/wallet/xp").asInt()).isGreaterThanOrEqualTo(startXp + first.get("xpGained").asInt());

        // Broke now (unless duplicate refunds were very generous).
        if (first.at("/wallet/gems").asLong() < 1000) {
            json(post("/api/gacha/pull").content("{\"banner\":\"STANDARD\"}"), 400);
        }

        JsonNode daily = json(post("/api/gacha/daily"), 200);
        assertThat(daily.get("gems").asLong()).isEqualTo(first.at("/wallet/gems").asLong() + 300);
        assertThat(daily.get("dailyAvailable").asBoolean()).isFalse();
        json(post("/api/gacha/daily"), 400);

        // Pay to win.
        JsonNode bought = json(post("/api/gacha/shop/whale-of-a-time"), 200);
        assertThat(bought.get("vipLevel").asInt()).isEqualTo(4);
        assertThat(bought.get("xpBonusPercent").asInt()).isEqualTo(40);
        json(post("/api/gacha/shop/not-a-pack"), 404);

        JsonNode whale = json(post("/api/gacha/pull").content("{\"banner\":\"WHALE\"}"), 200);
        assertThat(StreamSupport.stream(whale.get("cards").spliterator(), false)
                .anyMatch(c -> "LEGENDARY".equals(c.at("/idea/rarity").asText())))
                .as("whale pulls always include a Legendary").isTrue();
        assertThat(whale.at("/wallet/pityCount").asInt()).isLessThan(10);

        json(post("/api/gacha/pull").content("{\"banner\":\"NOPE\"}"), 400);

        // Trade XP for gems.
        long gemsBefore = whale.at("/wallet/gems").asLong();
        int xpBefore = whale.at("/wallet/xp").asInt();
        JsonNode traded = json(post("/api/gacha/exchange").content("{\"xp\":10}"), 200);
        assertThat(traded.get("gems").asLong()).isEqualTo(gemsBefore + 50);
        assertThat(traded.get("xp").asInt()).isEqualTo(xpBefore - 10);
        json(post("/api/gacha/exchange").content("{\"xp\":999999}"), 400);

        JsonNode collection = json(get("/api/gacha/collection"), 200);
        assertThat(collection.get("owned").asInt()).isBetween(1, 20);
        assertThat(collection.get("items").size()).isEqualTo(collection.get("poolSize").asInt());
        JsonNode locked = StreamSupport.stream(collection.get("items").spliterator(), false)
                .filter(i -> !i.get("owned").asBoolean()).findFirst().orElseThrow();
        assertThat(locked.has("title")).as("locked cards stay secret").isFalse();

        JsonNode history = json(get("/api/gacha/history"), 200);
        assertThat(history.size()).isEqualTo(2);
        assertThat(history.get(0).get("banner").asText()).isEqualTo("WHALE");
        assertThat(history.get(0).get("cards").size()).isEqualTo(10);
    }
}
