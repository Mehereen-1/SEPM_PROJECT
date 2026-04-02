package com.example.project.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        String redirectUrl = "/";
        System.out.println("\n===== LOGIN SUCCESS =====");
        System.out.println("User: " + authentication.getName());
        System.out.println("All Authorities: " + authentication.getAuthorities());

        // Check for DELIVERY_PARTNER first (must be checked before USER to ensure correct match)
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            System.out.println("  - Checking: '" + role + "'");

            if ("ROLE_ADMIN".equals(role)) {
                redirectUrl = "/admin/dashboard";
                System.out.println("  ✓ MATCHED ADMIN");
                break;
            }
            
            if ("ROLE_DELIVERY_PARTNER".equals(role) || "ROLE_DELIVERY".equals(role)) {
                redirectUrl = "/delivery/dashboard";
                System.out.println("  ✓ MATCHED DELIVERY PARTNER");
                break;
            }
        }

        // Only check for reader roles if delivery wasn't matched
        if (redirectUrl.equals("/")) {
            for (GrantedAuthority authority : authentication.getAuthorities()) {
                String role = authority.getAuthority();
                if ("ROLE_BOOK_FRIEND".equals(role) || "ROLE_USER".equals(role) || "ROLE_READER".equals(role)) {
                    redirectUrl = "/reader/dashboard";
                    System.out.println("  ✓ MATCHED BOOK FRIEND/USER");
                    break;
                }
            }
        }

        System.out.println("FINAL REDIRECT: " + request.getContextPath() + redirectUrl);
        System.out.println("=====================\n");
        response.sendRedirect(request.getContextPath() + redirectUrl);
    }
}
