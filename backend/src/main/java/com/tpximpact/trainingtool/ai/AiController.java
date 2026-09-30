package com.tpximpact.trainingtool.ai;

import com.tpximpact.trainingtool.gamification.Activity;
import com.tpximpact.trainingtool.gamification.GamificationService;
import com.tpximpact.trainingtool.security.CurrentUser;
import com.tpximpact.trainingtool.user.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AiController {

    private final RecommendationService recommendations;
    private final TrainingPlanRepository plans;
    private final PipService pip;
    private final CurrentUser currentUser;
    private final GamificationService gamification;
    private final AnthropicClient ai;

    public AiController(RecommendationService recommendations, TrainingPlanRepository plans, PipService pip,
                        CurrentUser currentUser, GamificationService gamification, AnthropicClient ai) {
        this.recommendations = recommendations;
        this.plans = plans;
        this.pip = pip;
        this.currentUser = currentUser;
        this.gamification = gamification;
        this.ai = ai;
    }

    @GetMapping("/ai/status")
    public Map<String, Object> status() {
        return Map.of("enabled", ai.enabled(), "model", ai.enabled() ? ai.model() : "built-in rules");
    }

    /** Generate (and save) a new training plan from the user's current gaps. */
    @PostMapping("/recommendations")
    @Transactional
    public RecommendationService.Plan generate() {
        User me = currentUser.get();
        RecommendationService.Plan plan = recommendations.generate(me);
        plans.save(new TrainingPlan(me.getId(), recommendations.toJson(plan)));
        gamification.record(me, Activity.PLAN_GENERATED);
        return plan;
    }

    /** The most recently generated plan, or 204 if none yet. */
    @GetMapping("/recommendations/latest")
    public ResponseEntity<RecommendationService.Plan> latest() {
        return plans.findFirstByUserIdOrderByCreatedAtDesc(currentUser.id())
                .map(p -> ResponseEntity.ok(recommendations.fromJson(p.getJson())))
                .orElse(ResponseEntity.noContent().build());
    }

    public record PipRequest(@NotBlank @Size(max = 2000) String message, List<PipService.ChatMessage> history) {}

    @PostMapping("/pip/chat")
    @Transactional
    public PipService.PipReply chat(@Valid @RequestBody PipRequest req) {
        User me = currentUser.get();
        PipService.PipReply reply = pip.chat(me, req.message(), req.history());
        gamification.record(me, Activity.PIP_CHAT);
        return reply;
    }

    @GetMapping("/pip/tip")
    public Map<String, String> tip() {
        return Map.of("tip", pip.tip());
    }
}
