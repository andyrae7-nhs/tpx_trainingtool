package com.tpximpact.trainingtool.framework;

import java.util.List;
import java.util.Map;

/** Immutable model of the progression framework, loaded from data/framework.json. */
public final class FrameworkModels {
    private FrameworkModels() {}

    public record Grade(String code, String name, int number) {}

    public record RoleSkill(String skillId, Map<String, String> expected) {}

    public record Role(String id, String name, String capability, String practice, List<RoleSkill> skills) {}

    public record Skill(String id, String name, String definition, Map<String, String> levels) {}

    /** A behaviour or impact item. Levels are keyed by grade code (G6..G12). */
    public record GradedItem(String id, String name, String type, String definition, Map<String, String> levels) {}

    public record ImpactArea(String name, String definition) {}

    public record Framework(Map<String, String> source,
                            List<Grade> grades,
                            List<String> skillLevels,
                            List<Role> roles,
                            List<Skill> skills,
                            List<GradedItem> behaviours,
                            List<ImpactArea> impactAreas,
                            List<GradedItem> impacts) {}
}
