package com.tpximpact.trainingtool.progression;

import com.tpximpact.trainingtool.common.ApiException;
import com.tpximpact.trainingtool.framework.FrameworkModels.*;
import com.tpximpact.trainingtool.framework.FrameworkService;
import com.tpximpact.trainingtool.journal.JournalEntryRepository;
import com.tpximpact.trainingtool.progression.SelfAssessment.ItemType;
import com.tpximpact.trainingtool.user.User;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Compares where someone is now with where their target grade expects them to be. */
@Service
public class ProgressionService {

    private final FrameworkService framework;
    private final SelfAssessmentRepository assessments;
    private final JournalEntryRepository journal;

    public ProgressionService(FrameworkService framework, SelfAssessmentRepository assessments,
                              JournalEntryRepository journal) {
        this.framework = framework;
        this.assessments = assessments;
        this.journal = journal;
    }

    public enum Status { MET, GAP, NOT_REQUIRED }

    public record GapItem(String ref, ItemType type, String id, String name, String definition,
                          String currentExpected, String targetExpected, String selfLevel, boolean selfAssessed,
                          int gap, Status status, String selfDescriptor, String targetDescriptor,
                          String note, long evidenceCount) {}

    public record Summary(int required, int met, int gaps, int readinessPercent, int assessed, int totalItems,
                          int skillGaps, int behaviourGaps, int impactGaps) {}

    public record GapReport(String roleId, String roleName, String capability, String practice,
                            String currentGrade, String currentGradeName, String targetGrade, String targetGradeName,
                            List<String> skillScale, List<Grade> gradeScale,
                            List<GapItem> skills, List<GapItem> behaviours, List<GapItem> impacts, Summary summary) {}

    public GapReport report(User user, String targetOverride) {
        if (user.getRoleId() == null || user.getCurrentGrade() == null) {
            throw ApiException.badRequest("Choose your role and grade first");
        }
        Role role = framework.role(user.getRoleId());
        String current = user.getCurrentGrade();
        String target = targetOverride != null && framework.isGrade(targetOverride) ? targetOverride
                : Optional.ofNullable(user.getTargetGrade()).orElse(current);

        Map<String, SelfAssessment> mine = assessments.findByUserId(user.getId()).stream()
                .collect(Collectors.toMap(a -> a.getItemType() + ":" + a.getItemId(), Function.identity(), (a, b) -> a));
        Map<String, Long> evidence = new HashMap<>();
        journal.findByUserIdOrderByEntryDateDescIdDesc(user.getId())
                .forEach(e -> e.getRefs().forEach(r -> evidence.merge(r, 1L, Long::sum)));

        List<GapItem> skills = new ArrayList<>();
        for (RoleSkill rs : role.skills()) {
            Skill s = framework.skill(rs.skillId());
            String curExp = framework.expectedSkillLevel(rs, current);
            String tgtExp = framework.expectedSkillLevel(rs, target);
            if (curExp == null && tgtExp == null) continue;
            String ref = "SKILL:" + s.id();
            SelfAssessment sa = mine.get(ref);
            String self = sa != null ? sa.getLevel() : curExp;
            int gap = tgtExp == null ? 0
                    : Math.max(0, framework.skillLevelIndex(tgtExp) - framework.skillLevelIndex(self));
            Status status = tgtExp == null ? Status.NOT_REQUIRED : gap > 0 ? Status.GAP : Status.MET;
            skills.add(new GapItem(ref, ItemType.SKILL, s.id(), s.name(), s.definition(), curExp, tgtExp, self,
                    sa != null, gap, status, descriptor(s.levels(), self), descriptor(s.levels(), tgtExp),
                    sa == null ? null : sa.getNote(), evidence.getOrDefault(ref, 0L)));
        }

        List<GapItem> behaviours = graded(framework.behaviours(), ItemType.BEHAVIOUR, current, target, mine, evidence);
        List<GapItem> impacts = graded(framework.impacts(), ItemType.IMPACT, current, target, mine, evidence);

        List<GapItem> all = new ArrayList<>();
        all.addAll(skills);
        all.addAll(behaviours);
        all.addAll(impacts);
        int required = (int) all.stream().filter(i -> i.status() != Status.NOT_REQUIRED).count();
        int met = (int) all.stream().filter(i -> i.status() == Status.MET).count();
        int assessed = (int) all.stream().filter(GapItem::selfAssessed).count();
        Summary summary = new Summary(required, met, required - met,
                required == 0 ? 100 : Math.round(100f * met / required), assessed, all.size(),
                countGaps(skills), countGaps(behaviours), countGaps(impacts));

        return new GapReport(role.id(), role.name(), role.capability(), role.practice(),
                current, framework.grade(current).name(), target, framework.grade(target).name(),
                framework.skillLevels(), framework.grades(), skills, behaviours, impacts, summary);
    }

    private List<GapItem> graded(List<GradedItem> items, ItemType type, String current, String target,
                                 Map<String, SelfAssessment> mine, Map<String, Long> evidence) {
        List<GapItem> out = new ArrayList<>();
        for (GradedItem g : items) {
            String ref = type + ":" + g.id();
            SelfAssessment sa = mine.get(ref);
            String self = sa != null ? sa.getLevel() : current;
            String tgtText = g.levels().get(target);
            boolean required = tgtText != null && !tgtText.isBlank();
            int gap = required ? Math.max(0, framework.gradeIndex(target) - framework.gradeIndex(self)) : 0;
            // Junior and Graduate share one descriptor, so treat G6 -> G7 as already met.
            if (gap > 0 && Objects.equals(g.levels().get(self), tgtText)) gap = 0;
            Status status = !required ? Status.NOT_REQUIRED : gap > 0 ? Status.GAP : Status.MET;
            out.add(new GapItem(ref, type, g.id(), g.name(), g.definition(), framework.grade(current).name(),
                    framework.grade(target).name(), self, sa != null, gap, status, g.levels().get(self), tgtText,
                    sa == null ? null : sa.getNote(), evidence.getOrDefault(ref, 0L)));
        }
        return out;
    }

    private static int countGaps(List<GapItem> items) {
        return (int) items.stream().filter(i -> i.status() == Status.GAP).count();
    }

    private static String descriptor(Map<String, String> levels, String level) {
        return level == null || levels == null ? null : levels.get(level);
    }

    /** All gap items, largest first. */
    public List<GapItem> topGaps(GapReport report, int limit) {
        List<GapItem> all = new ArrayList<>();
        all.addAll(report.skills());
        all.addAll(report.behaviours());
        all.addAll(report.impacts());
        return all.stream().filter(i -> i.status() == Status.GAP)
                .sorted(Comparator.comparingInt(GapItem::gap).reversed()
                        .thenComparing(i -> i.type() == ItemType.SKILL ? 0 : 1))
                .limit(limit)
                .toList();
    }

    /** Validates the level value for an item type. */
    public void validateLevel(ItemType type, String itemId, String level) {
        switch (type) {
            case SKILL -> {
                framework.skill(itemId);
                if (framework.skillLevelIndex(level) < 0)
                    throw ApiException.badRequest("Skill level must be one of " + framework.skillLevels());
            }
            case BEHAVIOUR -> {
                framework.findBehaviour(itemId).orElseThrow(() -> ApiException.notFound("Behaviour '" + itemId + "'"));
                if (!framework.isGrade(level)) throw ApiException.badRequest("Behaviour level must be a grade code");
            }
            case IMPACT -> {
                framework.findImpact(itemId).orElseThrow(() -> ApiException.notFound("Impact '" + itemId + "'"));
                if (!framework.isGrade(level)) throw ApiException.badRequest("Impact level must be a grade code");
            }
        }
    }

    /** Human-readable name of a framework ref such as "SKILL:agile-and-lean-knowledge". */
    public Optional<String> refName(String ref) {
        if (ref == null || !ref.contains(":")) return Optional.empty();
        String[] parts = ref.split(":", 2);
        return switch (parts[0]) {
            case "SKILL" -> framework.findSkill(parts[1]).map(Skill::name);
            case "BEHAVIOUR" -> framework.findBehaviour(parts[1]).map(GradedItem::name);
            case "IMPACT" -> framework.findImpact(parts[1]).map(GradedItem::name);
            default -> Optional.empty();
        };
    }
}
