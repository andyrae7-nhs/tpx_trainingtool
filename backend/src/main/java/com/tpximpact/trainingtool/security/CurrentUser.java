package com.tpximpact.trainingtool.security;

import com.tpximpact.trainingtool.common.ApiException;
import com.tpximpact.trainingtool.user.User;
import com.tpximpact.trainingtool.user.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Resolves the signed-in user from the security context. */
@Component
public class CurrentUser {

    private final UserRepository users;

    public CurrentUser(UserRepository users) {
        this.users = users;
    }

    public Long id() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long id)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Please sign in");
        }
        return id;
    }

    public User get() {
        return users.findById(id()).orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Please sign in"));
    }
}
