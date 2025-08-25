package com.unsis.admunsisbackend.security;

import com.unsis.admunsisbackend.service.AccessRestrictionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;

@Component
public class RoleAccessRestrictionFilter extends OncePerRequestFilter {

    @Autowired
    private AccessRestrictionService accessRestrictionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        // permitir auth endpoints (login/register) siempre
        if (path.startsWith("/auth/")) {
            chain.doFilter(request, response);
            return;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            chain.doFilter(request, response);
            return;
        }

        for (GrantedAuthority ga : auth.getAuthorities()) {
            String role = ga.getAuthority();
            // aplica sólo si hay reglas para el role
            if (!accessRestrictionService.isAccessAllowed(role)) {
                // bloquear acceso (403) a rutas protegidas
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Acceso temporalmente restringido\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }
}
