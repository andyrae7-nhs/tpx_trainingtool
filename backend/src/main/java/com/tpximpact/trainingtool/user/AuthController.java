package com.tpximpact.trainingtool.user;

import com.tpximpact.trainingtool.common.ApiException;
import com.tpximpact.trainingtool.gamification.Activity;
import com.tpximpact.trainingtool.gamification.GamificationService;
import com.tpximpact.trainingtool.security.JwtService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final List<String> AVATAR_COLOURS = List.of("#c8e9ff", "#ffcfca", "#cafce5", "#e7d2ff");

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final UserViews views;
    private final GamificationService gamification;
    private final String allowedDomain;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt, UserViews views,
                          GamificationService gamification,
                          @Value("${app.allowed-email-domain:}") String allowedDomain) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.views = views;
        this.gamification = gamification;
        this.allowedDomain = allowedDomain == null ? "" : allowedDomain.trim().toLowerCase(Locale.UK);
    }

    public record RegisterRequest(@NotBlank @Email String email,
                                  @NotBlank @Size(min = 8, message = "must be at least 8 characters") String password,
                                  @NotBlank @Size(max = 80) String displayName) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record AuthResponse(String token, UserViews.UserView user) {}

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        String email = req.email().trim().toLowerCase(Locale.UK);
        if (!allowedDomain.isEmpty() && !email.endsWith("@" + allowedDomain)) {
            throw ApiException.badRequest("Please sign up with your @" + allowedDomain + " email address");
        }
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with that email already exists");
        }
        User u = new User();
        u.setEmail(email);
        u.setDisplayName(req.displayName().trim());
        u.setPasswordHash(encoder.encode(req.password()));
        u.setAvatarColor(AVATAR_COLOURS.get(Math.floorMod(email.hashCode(), AVATAR_COLOURS.size())));
        users.save(u);
        gamification.record(u, Activity.SIGNED_UP);
        gamification.touch(u);
        return new AuthResponse(jwt.issue(u.getId(), u.getEmail()), views.full(u, u.getId()));
    }

    @PostMapping("/login")
    @Transactional
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        User u = users.findByEmailIgnoreCase(req.email().trim())
                .filter(x -> encoder.matches(req.password(), x.getPasswordHash()))
                .orElseThrow(() -> new BadCredentialsException("bad credentials"));
        gamification.touch(u);
        return new AuthResponse(jwt.issue(u.getId(), u.getEmail()), views.full(u, u.getId()));
    }
}
