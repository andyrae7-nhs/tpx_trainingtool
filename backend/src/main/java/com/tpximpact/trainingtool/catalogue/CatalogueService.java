package com.tpximpact.trainingtool.catalogue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpximpact.trainingtool.common.ApiException;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

@Service
public class CatalogueService {

    private final List<Resource> resources;
    private final Map<String, Resource> byId = new LinkedHashMap<>();

    public CatalogueService() throws IOException {
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try (InputStream in = new ClassPathResource("data/catalogue.json").getInputStream()) {
            this.resources = mapper.readValue(in, new TypeReference<List<Resource>>() {});
        }
        resources.forEach(r -> byId.put(r.id(), r));
    }

    public List<Resource> all() { return resources; }

    public Optional<Resource> find(String id) { return Optional.ofNullable(byId.get(id)); }

    public Resource get(String id) { return find(id).orElseThrow(() -> ApiException.notFound("Resource")); }

    public List<Resource> search(String q, String type, String tag) {
        String query = q == null ? "" : q.trim().toLowerCase(Locale.UK);
        return resources.stream()
                .filter(r -> type == null || type.isBlank() || r.type().equalsIgnoreCase(type))
                .filter(r -> tag == null || tag.isBlank() || r.tags().contains(tag))
                .filter(r -> query.isEmpty()
                        || r.title().toLowerCase(Locale.UK).contains(query)
                        || (r.description() != null && r.description().toLowerCase(Locale.UK).contains(query))
                        || r.tags().stream().anyMatch(t -> t.contains(query))
                        || (r.provider() != null && r.provider().toLowerCase(Locale.UK).contains(query)))
                .toList();
    }

    public record Scored(Resource resource, int score) {}

    /**
     * Ranks resources for a framework item.
     * @param stretch 0 = foundation, 1 = intermediate, 2 = advanced (from the target level)
     * @param preferInternal favour internal programmes (used for behaviours and impact)
     */
    public List<Scored> rank(String name, String definition, int stretch, boolean preferInternal, int limit) {
        Map<String, Integer> wanted = TagMatcher.tagsFor(name, definition);
        if (wanted.isEmpty()) return List.of();
        List<Scored> scored = new ArrayList<>();
        for (Resource r : resources) {
            int score = 0;
            for (String t : r.tags()) score += wanted.getOrDefault(t, 0);
            if (score == 0) continue;
            int lvl = switch (Objects.requireNonNullElse(r.level(), "Foundation")) {
                case "Advanced" -> 2;
                case "Intermediate" -> 1;
                default -> 0;
            };
            score += 2 - Math.abs(lvl - stretch);
            if (preferInternal && "PROGRAMME".equals(r.type()) && r.url() == null) score += 2;
            scored.add(new Scored(r, score));
        }
        scored.sort(Comparator.comparingInt(Scored::score).reversed().thenComparing(s -> s.resource().title()));
        return scored.stream().limit(limit).toList();
    }

    public List<String> tags() {
        return resources.stream().flatMap(r -> r.tags().stream()).distinct().sorted().toList();
    }
}
