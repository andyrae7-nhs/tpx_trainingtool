package com.tpximpact.trainingtool.framework;

import com.tpximpact.trainingtool.framework.FrameworkModels.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/framework")
public class FrameworkController {

    private final FrameworkService framework;

    public FrameworkController(FrameworkService framework) {
        this.framework = framework;
    }

    public record RoleSummary(String id, String name, String capability, String practice, int skillCount) {}

    @GetMapping("/roles")
    public List<RoleSummary> roles() {
        return framework.roles().stream()
                .map(r -> new RoleSummary(r.id(), r.name(), r.capability(), r.practice(), r.skills().size()))
                .toList();
    }

    @GetMapping("/grades")
    public List<Grade> grades() {
        return framework.grades();
    }

    public record RoleSkillDetail(Skill skill, Map<String, String> expected) {}

    public record RoleDetail(String id, String name, String capability, String practice, List<RoleSkillDetail> skills) {}

    @GetMapping("/roles/{id}")
    public RoleDetail role(@PathVariable String id) {
        Role r = framework.role(id);
        List<RoleSkillDetail> skills = r.skills().stream()
                .map(rs -> new RoleSkillDetail(framework.skill(rs.skillId()), rs.expected()))
                .toList();
        return new RoleDetail(r.id(), r.name(), r.capability(), r.practice(), skills);
    }

    @GetMapping("/behaviours")
    public List<GradedItem> behaviours() {
        return framework.behaviours();
    }

    @GetMapping("/impacts")
    public Map<String, Object> impacts() {
        return Map.of("areas", framework.framework().impactAreas(), "items", framework.impacts());
    }

    @GetMapping("/skill-levels")
    public List<String> skillLevels() {
        return framework.skillLevels();
    }
}
