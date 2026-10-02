package com.tpximpact.trainingtool.user;

import com.tpximpact.trainingtool.common.ApiException;
import com.tpximpact.trainingtool.framework.FrameworkService;
import com.tpximpact.trainingtool.gamification.Activity;
import com.tpximpact.trainingtool.gamification.GamificationService;
import com.tpximpact.trainingtool.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ProfileController {

    private final CurrentUser currentUser;
    private final UserRepository users;
    private final UserViews views;
    private final FrameworkService framework;
    private final GamificationService gamification;

    public ProfileController(CurrentUser currentUser, UserRepository users, UserViews views,
                             FrameworkService framework, GamificationService gamification) {
        this.currentUser = currentUser;
        this.users = users;
        this.views = views;
        this.framework = framework;
        this.gamification = gamification;
    }

    @GetMapping("/me")
    @Transactional
    public UserViews.UserView me() {
        User me = currentUser.get();
        gamification.touch(me);
        return views.full(me, me.getId());
    }

    public record UpdateProfileRequest(@Size(max = 80) String displayName,
                                       @Size(max = 1000) String bio,
                                       String avatarColor,
                                       String roleId,
                                       String currentGrade,
                                       String targetGrade) {}

    @PutMapping("/me")
    @Transactional
    public UserViews.UserView update(@Valid @RequestBody UpdateProfileRequest req) {
        User me = currentUser.get();
        boolean wasOnboarded = me.isOnboarded();
        if (req.displayName() != null && !req.displayName().isBlank()) me.setDisplayName(req.displayName().trim());
        if (req.bio() != null) me.setBio(req.bio().trim());
        if (req.avatarColor() != null) me.setAvatarColor(req.avatarColor());
        if (req.roleId() != null) {
            framework.role(req.roleId()); // validates
            me.setRoleId(req.roleId());
        }
        if (req.currentGrade() != null) {
            if (!framework.isGrade(req.currentGrade())) throw ApiException.badRequest("Unknown grade " + req.currentGrade());
            me.setCurrentGrade(req.currentGrade());
            if (me.getTargetGrade() == null && req.targetGrade() == null) {
                me.setTargetGrade(framework.nextGrade(req.currentGrade()).orElse(req.currentGrade()));
            }
        }
        if (req.targetGrade() != null) {
            if (!framework.isGrade(req.targetGrade())) throw ApiException.badRequest("Unknown grade " + req.targetGrade());
            me.setTargetGrade(req.targetGrade());
        }
        if (me.getCurrentGrade() != null && me.getTargetGrade() != null
                && framework.gradeIndex(me.getTargetGrade()) < framework.gradeIndex(me.getCurrentGrade())) {
            throw ApiException.badRequest("Your target grade can't be below your current grade");
        }
        users.save(me);
        if (!wasOnboarded && me.isOnboarded()) {
            gamification.record(me, Activity.PROFILE_COMPLETED);
        } else {
            gamification.evaluate(me);
        }
        return views.full(me, me.getId());
    }

    @GetMapping("/users")
    public List<UserViews.UserView> search(@RequestParam(defaultValue = "") String q) {
        Long me = currentUser.id();
        List<User> found = q.isBlank()
                ? users.findTop50ByOrderByXpDesc()
                : users.findByDisplayNameContainingIgnoreCaseOrderByDisplayName(q.trim());
        return found.stream().map(u -> views.full(u, me)).toList();
    }

    @GetMapping("/users/{id}")
    public UserViews.UserView user(@PathVariable Long id) {
        User u = users.findById(id).orElseThrow(() -> ApiException.notFound("User"));
        return views.full(u, currentUser.id());
    }
}
