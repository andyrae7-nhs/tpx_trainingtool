package com.tpximpact.trainingtool.progression;

import com.tpximpact.trainingtool.gamification.Activity;
import com.tpximpact.trainingtool.gamification.GamificationService;
import com.tpximpact.trainingtool.progression.SelfAssessment.ItemType;
import com.tpximpact.trainingtool.security.CurrentUser;
import com.tpximpact.trainingtool.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/progression")
public class ProgressionController {

    private final ProgressionService progression;
    private final SelfAssessmentRepository assessments;
    private final CurrentUser currentUser;
    private final GamificationService gamification;

    public ProgressionController(ProgressionService progression, SelfAssessmentRepository assessments,
                                CurrentUser currentUser, GamificationService gamification) {
        this.progression = progression;
        this.assessments = assessments;
        this.currentUser = currentUser;
        this.gamification = gamification;
    }

    /** Gap analysis for the signed-in user. Pass ?targetGrade=G10 to preview a different target. */
    @GetMapping("/gap")
    public ProgressionService.GapReport gap(@RequestParam(required = false) String targetGrade) {
        return progression.report(currentUser.get(), targetGrade);
    }

    @GetMapping("/assessments")
    public List<SelfAssessment> list() {
        return assessments.findByUserId(currentUser.id());
    }

    public record AssessmentInput(@NotNull ItemType itemType, @NotBlank String itemId, @NotBlank String level, String note) {}

    /** Save one or more self-assessments (upsert). */
    @PutMapping("/assessments")
    @Transactional
    public ProgressionService.GapReport save(@Valid @RequestBody List<@Valid AssessmentInput> inputs) {
        User me = currentUser.get();
        for (AssessmentInput in : inputs) {
            progression.validateLevel(in.itemType(), in.itemId(), in.level());
            SelfAssessment sa = assessments.findByUserIdAndItemTypeAndItemId(me.getId(), in.itemType(), in.itemId())
                    .orElseGet(() -> new SelfAssessment(me.getId(), in.itemType(), in.itemId()));
            sa.setLevel(in.level());
            sa.setNote(in.note());
            sa.setUpdatedAt(Instant.now());
            assessments.save(sa);
        }
        gamification.record(me, Activity.ASSESSMENT_SAVED);
        return progression.report(me, null);
    }
}
