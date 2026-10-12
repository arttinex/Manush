package com.manush.auth.service;

import com.manush.auth.model.User;
import com.manush.auth.repository.UserRepository;
import com.manush.common.model.Role;
import com.manush.common.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleOAuthSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) throws IOException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        String email = oauthUser.getAttribute("email");
        String name  = oauthUser.getAttribute("name");
        String googleId = oauthUser.getAttribute("sub");

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User newUser = User.builder()
                            .email(email)
                            .name(name)
                            .googleId(googleId)
                            .roles(Set.of(Role.USER))
                            .emailVerified(true)
                            .active(true)
                            .banned(false)
                            .profileComplete(false)  // must complete profile (blood group, area)
                            .helpingPoints(0)
                            .build();
                    return userRepository.save(newUser);
                });

        // If user exists via email but has no googleId, link it
        if (user.getGoogleId() == null) {
            user.setGoogleId(googleId);
            userRepository.save(user);
        }

        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        log.info("Google OAuth success for user: {}", user.getId());
        response.sendRedirect("/auth/callback?access=" + accessToken + "&refresh=" + refreshToken);
    }
}
