package com.tpximpact.trainingtool.framework;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tpximpact.trainingtool.common.ApiException;
import com.tpximpact.trainingtool.framework.FrameworkModels.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Loads the progression framework generated from the Progression Assessment spreadsheets
 * (see tools/extract_framework.py) and offers lookups.
 */
@Service
public class FrameworkService {

    private final Framework framework;
    private final Map<String, Role> roles;
    private final Map<String, Skill> skills;
    private final Map<String, GradedItem> behaviours;
    private final Map<String, GradedItem> impacts;
    private final List<String> gradeCodes;

    public FrameworkService() throws IOException {
        ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        try (InputStream in = new ClassPathResource("data/framework.json").getInputStream()) {
            this.framework = mapper.readValue(in, Framework.class);
        }
        this.roles = index(framework.roles(), Role::id);
        this.skills = index(framework.skills(), Skill::id);
        this.behaviours = index(framework.behaviours(), GradedItem::id);
        this.impacts = index(framework.impacts(), GradedItem::id);
        this.gradeCodes = framework.grades().stream().map(Grade::code).toList();
    }

    private static <T> Map<String, T> index(List<T> list, Function<T, String> key) {
        return list.stream().collect(Collectors.toMap(key, Function.identity(), (a, b) -> a, LinkedHashMap::new));
    }

    public Framework framework() { return framework; }

    public List<Role> roles() { return framework.roles(); }

    public List<Grade> grades() { return framework.grades(); }

    public List<String> skillLevels() { return framework.skillLevels(); }

    public Role role(String id) {
        Role r = roles.get(id);
        if (r == null) throw ApiException.notFound("Role '" + id + "'");
        return r;
    }

    public Optional<Role> findRole(String id) { return Optional.ofNullable(id == null ? null : roles.get(id)); }

    public Skill skill(String id) {
        Skill s = skills.get(id);
        if (s == null) throw ApiException.notFound("Skill '" + id + "'");
        return s;
    }

    public Optional<Skill> findSkill(String id) { return Optional.ofNullable(skills.get(id)); }

    public List<GradedItem> behaviours() { return framework.behaviours(); }

    public List<GradedItem> impacts() { return framework.impacts(); }

    public Optional<GradedItem> findBehaviour(String id) { return Optional.ofNullable(behaviours.get(id)); }

    public Optional<GradedItem> findImpact(String id) { return Optional.ofNullable(impacts.get(id)); }

    public boolean isGrade(String code) { return gradeCodes.contains(code); }

    /** Index of a grade code (G6 = 0). Returns -1 if unknown. */
    public int gradeIndex(String code) { return gradeCodes.indexOf(code); }

    public Grade grade(String code) {
        return framework.grades().stream().filter(g -> g.code().equals(code)).findFirst()
                .orElseThrow(() -> ApiException.badRequest("Unknown grade '" + code + "'"));
    }

    public Optional<String> nextGrade(String code) {
        int i = gradeIndex(code);
        if (i < 0 || i + 1 >= gradeCodes.size()) return Optional.empty();
        return Optional.of(gradeCodes.get(i + 1));
    }

    /** Index of a skill level (Learner = 0). Returns -1 for null/unknown. */
    public int skillLevelIndex(String level) {
        return level == null ? -1 : framework.skillLevels().indexOf(level);
    }

    /**
     * Expected technical skill level for a role skill at a grade. Grades above the
     * top of the skills matrix (e.g. Head of) inherit the highest defined level.
     * Returns null where the spreadsheet says "Not defined".
     */
    public String expectedSkillLevel(RoleSkill rs, String gradeCode) {
        if (rs.expected().containsKey(gradeCode)) {
            return rs.expected().get(gradeCode);
        }
        String best = null;
        for (String code : gradeCodes) {
            String v = rs.expected().get(code);
            if (v != null) best = v;
        }
        return best;
    }
}
