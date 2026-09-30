package com.tpximpact.trainingtool.catalogue;

import java.util.*;

/** Maps framework items (skills, behaviours, impact) to catalogue tags using keyword rules. */
public final class TagMatcher {
    private TagMatcher() {}

    private static final Map<String, List<String>> RULES = new LinkedHashMap<>();

    private static void rule(String keyword, String... tags) {
        RULES.put(keyword, List.of(tags));
    }

    static {
        rule("agile", "agile", "delivery");
        rule("lean", "agile", "process");
        rule("financ", "finance", "commercial");
        rule("project management", "delivery", "planning", "finance");
        rule("process", "process", "continuous-improvement");
        rule("lifecycle", "lifecycle", "delivery");
        rule("life cycle", "lifecycle", "delivery");
        rule("collaborat", "collaboration", "teams", "facilitation");
        rule("wellbeing", "wellbeing", "teams");
        rule("momentum", "delivery", "time-management", "risk");
        rule("strateg", "strategy", "planning");
        rule("problem", "problem-solving");
        rule("product", "product", "roadmaps");
        rule("outcome", "outcomes", "benefits");
        rule("benefit", "benefits", "outcomes");
        rule("constraint", "ambiguity", "stakeholders");
        rule("value release", "product", "outcomes");
        rule("business analysis", "business-analysis", "requirements");
        rule("modelling", "modelling", "business-analysis");
        rule("option analysis", "options", "problem-solving", "business-analysis");
        rule("systems analysis", "business-analysis", "data");
        rule("systems thinking", "systems-thinking");
        rule("accessib", "accessibility", "inclusion");
        rule("inclusi", "inclusion");
        rule("content design", "content-design", "writing");
        rule("research", "research");
        rule("impact analysis", "impact", "research");
        rule("interaction", "interaction-design");
        rule("org design", "org-design");
        rule("organisation design", "org-design");
        rule("service design", "service-design");
        rule("design strategy", "design-strategy", "strategy");
        rule("emerging tech", "emerging-tech", "ai");
        rule("capability building", "capability-building", "coaching", "mentoring");
        rule("prototyp", "interaction-design", "delivery");
        rule("client", "client", "stakeholders");
        rule("cloud", "cloud", "infrastructure");
        rule("infrastructure", "infrastructure", "cloud", "devops");
        rule("security", "security");
        rule("test", "testing");
        rule("automat", "automation", "testing", "devops");
        rule("coding", "coding", "software-engineering");
        rule("scripting", "coding", "automation");
        rule("programming", "coding", "software-engineering");
        rule("software", "software-engineering");
        rule("architect", "architecture");
        rule("data", "data");
        rule("data engineering", "data-engineering");
        rule("data development", "data-engineering");
        rule("governance", "data-governance");
        rule("integration", "integration", "architecture");
        rule("analy", "analytics");
        rule("service desk", "service-management", "support");
        rule("customer service", "service-management", "support", "stakeholders");
        rule("incident", "service-management", "support");
        rule("problem management", "service-management", "risk");
        rule("configuration", "service-management");
        rule("availability", "infrastructure", "cloud");
        rule("capacity", "infrastructure", "cloud");
        rule("service improvement", "continuous-improvement", "service-management");
        rule("communicat", "communication", "presenting");
        rule("storytelling", "storytelling", "presenting", "communication");
        rule("feedback", "feedback");
        rule("stakeholder", "stakeholders", "influence");
        rule("facilitat", "facilitation", "workshops");
        rule("workshop", "facilitation", "workshops");
        rule("negotia", "negotiation");
        rule("functional", "business-applications");
        rule("business application", "business-applications");
        rule("devops", "devops");
        rule("machine learning", "ai");
        // Behaviours and impact
        rule("developing your craft", "learning", "feedback", "sharing", "reflection");
        rule("supporting and developing others", "coaching", "mentoring", "feedback", "conflict", "leadership");
        rule("communicating and collaborating", "communication", "collaboration", "stakeholders", "trust");
        rule("owning and delivering", "ownership", "time-management", "risk", "delivery");
        rule("navigating scope and complexity", "ambiguity", "problem-solving", "systems-thinking", "strategy");
        rule("client contributions", "client", "commercial", "trust", "consulting");
        rule("inclusive teams", "inclusion", "teams", "leadership");
        rule("time management", "time-management", "commercial", "planning");
        rule("practice area", "sharing", "presenting", "writing", "practice");
        rule("growth", "growth", "bids", "commercial");
    }

    /** Tags for a framework item based on its name (weighted) and definition. */
    public static Map<String, Integer> tagsFor(String name, String definition) {
        Map<String, Integer> weights = new HashMap<>();
        String n = name == null ? "" : name.toLowerCase(Locale.UK);
        String d = definition == null ? "" : definition.toLowerCase(Locale.UK);
        RULES.forEach((kw, tags) -> {
            if (n.contains(kw)) tags.forEach(t -> weights.merge(t, 3, Integer::sum));
            else if (d.contains(kw)) tags.forEach(t -> weights.merge(t, 1, Integer::sum));
        });
        return weights;
    }
}
