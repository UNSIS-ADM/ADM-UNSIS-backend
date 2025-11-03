package com.unsis.admunsisbackend.security;

import com.unsis.admunsisbackend.service.AccessRestrictionService;
import org.springframework.lang.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.http.*;
import jakarta.servlet.*;
import java.io.IOException;

/**
 * Filtro de restricción de acceso basado en roles que intercepta las
 * solicitudes HTTP
 * y verifica si el usuario tiene permitido el acceso según su rol.
 * Extiende OncePerRequestFilter para asegurar que se ejecute una vez por
 * solicitud.
 */
@Component
public class RoleAccessRestrictionFilter extends OncePerRequestFilter {

    @Autowired
    private AccessRestrictionService accessRestrictionService;

    /**
     * Método que se ejecuta para cada solicitud HTTP.
     *
     * @param request  La solicitud HTTP.
     * @param response La respuesta HTTP.
     * @param chain    La cadena de filtros.
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        // Permitir auth endpoints (login/register) siempre
        if (path.startsWith("/auth/")) {
            chain.doFilter(request, response);
            return;
        }

        // Obtener autenticación del contexto de seguridad
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            chain.doFilter(request, response);
            return;
        }

        // Verificar roles del usuario
        for (GrantedAuthority ga : auth.getAuthorities()) {
            String role = ga.getAuthority();
            if (!accessRestrictionService.isAccessAllowed(role)) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Acceso temporalmente restringido\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
