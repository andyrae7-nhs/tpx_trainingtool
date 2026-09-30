package com.tpximpact.trainingtool.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpximpact.trainingtool.catalogue.CatalogueService;
import com.tpximpact.trainingtool.catalogue.Resource;
import com.tpximpact.trainingtool.framework.FrameworkService;
import com.tpximpact.trainingtool.progression.ProgressionService;
import com.tpximpact.trainingtool.progression.ProgressionService.GapItem;
import com.tpximpact.trainingtool.progression.ProgressionService.GapReport;
import com.tpximpact.trainingtool.progression.SelfAssessment.ItemType;
import com.tpximpact.trainingtool.user.User;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

/** Turns progression gaps into a personalised training plan, using Claude when configured. */
@Service
public class RecommendationService {

    private static final int MAX_GAPS = 8;

    private final ProgressionService progression;
    private final CatalogueService catalogue;
    private final FrameworkService framework;
    private final AnthropicClient ai;
    private final ObjectMapper mapper;

    public RecommendationService(ProgressionService progression, CatalogueService catalogue,
                                 FrameworkService framework, AnthropicClient ai, ObjectMapper mapper) {
        this.progression = progression;
        this.catalogue = catalogue;
        this.framework = framework;
        this.ai = ai;
        this.mapper = mapper;
    }

    public record GapRef(String ref, String type, String name, String from, String to, int gap, String targetDescriptor) {}

    public record Suggestion(Resource resource, String reason) {}

    public record PlanItem(GapRef gap, List<Suggestion> suggestions, String action) {}

    public record Plan(String generatedBy, String model, String generatedAt, String roleName,
                       String currentGradeName, String targetGradeName, String summary,
                       List<PlanItem> items, List<String> quickWins) {}

    public Plan generate(User user) {
        GapReport report = progression.report(user, null);
        List<GapItem> gaps = progression.topGaps(report, MAX_GAPS);

        Map<String, List<CatalogueService.Scored>> candidates = new LinkedHashMap<>();
        for (GapItem g : gaps) {
            candidates.put(g.ref(), catalogue.rank(g.name(), g.definition(), stretch(g), g.type() != ItemType.SKILL, 6));
        }

        if (gaps.isEmpty()) {
            return new Plan("rules", null, Instant.now().toString(), report.roleName(), report.currentGradeName(),
                    report.targetGradeName(),
                    "You're meeting every expectation for " + report.targetGradeName() + " based on your self-assessment. "
                            + "Keep adding evidence to your journal, and consider setting a stretch target.",
                    List.of(), defaultQuickWins());
        }

        if (ai.enabled()) {
            Optional<Plan> plan = aiPlan(report, gaps, candidates);
            if (plan.isPresent()) return plan.get();
        }
        return rulePlan(report, gaps, candidates);
    }

    private int stretch(GapItem g) {
        if (g.type() == ItemType.SKILL) {
            int idx = framework.skillLevelIndex(g.targetExpected());
            return idx <= 1 ? 0 : idx == 2 ? 1 : 2;
        }
        return 1;
    }

    private GapRef gapRef(GapItem g) {
        String from = g.type() == ItemType.SKILL ? (g.selfLevel() == null ? "Not yet started" : g.selfLevel())
                : framework.grade(g.selfLevel()).name();
        return new GapRef(g.ref(), g.type().name(), g.name(), from, g.targetExpected(), g.gap(),
                truncate(g.targetDescriptor(), 600));
    }

    private Plan rulePlan(GapReport report, List<GapItem> gaps, Map<String, List<CatalogueService.Scored>> candidates) {
        List<PlanItem> items = new ArrayList<>();
        for (GapItem g : gaps) {
            List<Suggestion> s = candidates.getOrDefault(g.ref(), List.of()).stream().limit(3)
                    .map(c -> new Suggestion(c.resource(), ruleReason(g, c.resource())))
                    .toList();
            items.add(new PlanItem(gapRef(g), s, ruleAction(g)));
        }
        String summary = String.format(
                "To move from %s to %s as a %s you have %d gap%s to close. Start with %s, then work down the list. "
                        + "Add a journal entry each time you apply something so your evidence builds as you go.",
                report.currentGradeName(), report.targetGradeName(), report.roleName(), report.summary().gaps(),
                report.summary().gaps() == 1 ? "" : "s", gaps.get(0).name());
        return new Plan("rules", null, Instant.now().toString(), report.roleName(), report.currentGradeName(),
                report.targetGradeName(), summary, items, defaultQuickWins());
    }

    private String ruleReason(GapItem g, Resource r) {
        String kind = switch (r.type()) {
            case "BOOK" -> "This book";
            case "EVENT" -> "This event";
            case "PROGRAMME" -> "This programme";
            case "ARTICLE" -> "This guide";
            default -> "This course";
        };
        return kind + " covers " + String.join(", ", r.tags().stream().limit(3).toList()).replace('-', ' ')
                + ", which supports moving towards " + g.targetExpected() + " in " + g.name() + ".";
    }

    private String ruleAction(GapItem g) {
        return switch (g.type()) {
            case SKILL -> "Ask your project lead for one task this month that stretches you towards "
                    + g.targetExpected() + " in " + g.name() + ", then log what you did and its impact in your journal.";
            case BEHAVIOUR -> "Pick one bullet from the " + g.targetExpected() + " description for '" + g.name()
                    + "' and try it deliberately on your current project. Ask a colleague for feedback afterwards.";
            case IMPACT -> "Look for one opportunity to show '" + g.name() + "' at " + g.targetExpected()
                    + " level, such as a blog post, bid contribution or client showcase, and record it as evidence.";
        };
    }

    private List<String> defaultQuickWins() {
        return List.of(
                "Book a 30-minute chat with your line manager to agree your top two gaps.",
                "Add one journal entry this week for something you did well.",
                "Share a review of the last course, book or event you did.");
    }

    private Optional<Plan> aiPlan(GapReport report, List<GapItem> gaps, Map<String, List<CatalogueService.Scored>> candidates) {
        // Offer the model each gap's ranked candidates plus a broad shortlist so it can pick well.
        Map<String, Resource> offered = new LinkedHashMap<>();
        candidates.values().forEach(list -> list.forEach(s -> offered.put(s.resource().id(), s.resource())));
        StringBuilder cat = new StringBuilder();
        offered.values().forEach(r -> cat.append("- id=").append(r.id()).append(" | ").append(r.type()).append(" | ")
                .append(r.title()).append(" | ").append(Objects.requireNonNullElse(r.provider(), "")).append(" | ")
                .append(r.description()).append('\n'));
        StringBuilder gapText = new StringBuilder();
        for (GapItem g : gaps) {
            GapRef ref = gapRef(g);
            gapText.append("- ref=").append(ref.ref()).append(" | ").append(ref.type()).append(" | ").append(ref.name())
                    .append(" | now: ").append(ref.from()).append(" | needed: ").append(ref.to())
                    .append(" | what 'needed' looks like: ").append(ref.targetDescriptor() == null ? "" : ref.targetDescriptor().replace('\n', ' '))
                    .append('\n');
        }

        String system = """
                You are the learning adviser inside TPX Grow, TPXimpact's personalised training tool.
                You help consultants close the gaps between their current level and the level expected for promotion,
                using TPXimpact's progression framework (technical skills, behaviours and impact).
                Write in plain British English, in an active voice, warm and encouraging, with no jargon or buzzwords
                (avoid words like 'unlock', 'deep-dive', 'leverage', 'key'). Be specific and practical.
                Only recommend resources from the catalogue you are given, by id.
                Reply with a single JSON object and nothing else.""";

        String prompt = "Person: " + report.roleName() + " (" + report.capability() + ", " + report.practice() + "), currently "
                + report.currentGradeName() + ", aiming for " + report.targetGradeName() + ".\n\n"
                + "Their biggest gaps:\n" + gapText + "\n"
                + "Catalogue you may choose from:\n" + cat + "\n"
                + """
                Return JSON in exactly this shape:
                {
                  "summary": "2-3 sentences on where to focus first and why",
                  "items": [
                    {"ref": "<gap ref>", "suggestions": [{"resourceId": "<catalogue id>", "reason": "one sentence on how it closes this specific gap"}],
                     "action": "one practical thing to try on their current project this month"}
                  ],
                  "quickWins": ["3 short things they can do this week"]
                }
                Include every gap, in priority order, with 1 to 3 suggestions each.""";

        Optional<String> reply = ai.complete(system, List.of(new AnthropicClient.Message("user", prompt)), 3000);
        if (reply.isEmpty()) return Optional.empty();
        Optional<JsonNode> json = ai.extractJson(reply.get());
        if (json.isEmpty()) return Optional.empty();

        Map<String, GapItem> gapByRef = new LinkedHashMap<>();
        gaps.forEach(g -> gapByRef.put(g.ref(), g));
        List<PlanItem> items = new ArrayList<>();
        Set<String> covered = new HashSet<>();
        for (JsonNode item : json.get().path("items")) {
            GapItem g = gapByRef.get(item.path("ref").asText());
            if (g == null || !covered.add(g.ref())) continue;
            List<Suggestion> suggestions = new ArrayList<>();
            for (JsonNode s : item.path("suggestions")) {
                Resource r = offered.get(s.path("resourceId").asText());
                if (r != null && suggestions.size() < 3) suggestions.add(new Suggestion(r, s.path("reason").asText()));
            }
            if (suggestions.isEmpty()) {
                candidates.getOrDefault(g.ref(), List.of()).stream().limit(2)
                        .forEach(c -> suggestions.add(new Suggestion(c.resource(), ruleReason(g, c.resource()))));
            }
            String action = item.path("action").asText("");
            items.add(new PlanItem(gapRef(g), suggestions, action.isBlank() ? ruleAction(g) : action));
        }
        // Anything the model skipped falls back to rules.
        for (GapItem g : gaps) {
            if (covered.contains(g.ref())) continue;
            List<Suggestion> s = candidates.getOrDefault(g.ref(), List.of()).stream().limit(2)
                    .map(c -> new Suggestion(c.resource(), ruleReason(g, c.resource()))).toList();
            items.add(new PlanItem(gapRef(g), s, ruleAction(g)));
        }
        List<String> quickWins = new ArrayList<>();
        json.get().path("quickWins").forEach(q -> quickWins.add(q.asText()));
        return Optional.of(new Plan("claude", ai.model(), Instant.now().toString(), report.roleName(),
                report.currentGradeName(), report.targetGradeName(), json.get().path("summary").asText(""),
                items, quickWins.isEmpty() ? defaultQuickWins() : quickWins));
    }

    public String toJson(Plan plan) {
        try {
            return mapper.writeValueAsString(plan);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public Plan fromJson(String json) {
        try {
            return mapper.readValue(json, Plan.class);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
