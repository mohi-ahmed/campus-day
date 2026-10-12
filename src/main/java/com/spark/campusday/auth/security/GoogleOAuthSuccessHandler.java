package com.spark.campusday.auth.security;

import com.spark.campusday.auth.config.AuthProperties;
import com.spark.campusday.auth.model.AuthAccount;
import com.spark.campusday.auth.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class GoogleOAuthSuccessHandler implements AuthenticationSuccessHandler {
    private final AuthService authService;
    private final AuthProperties properties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User user = (OAuth2User) authentication.getPrincipal();
        String email = user.getAttribute("email");
        String subject = user.getAttribute("sub");
        Boolean emailVerified = user.getAttribute("email_verified");
        if (email == null || subject == null || !Boolean.TRUE.equals(emailVerified)) {
            response.sendRedirect(properties.getFrontendOauthFailureUri().toString());
            return;
        }
        AuthAccount account = authService.loginWithGoogle(email, subject);
        String exchangeCode = authService.createOAuthExchangeCode(account);
        HttpSession oauthSession = request.getSession(false);
        if (oauthSession != null) {
            oauthSession.invalidate();
        }
        String redirect = UriComponentsBuilder.fromUri(properties.getFrontendOauthSuccessUri())
                .queryParam("code", exchangeCode)
                .build(true)
                .toUriString();
        response.sendRedirect(redirect);
    }
}
