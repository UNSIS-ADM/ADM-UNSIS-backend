package com.unsis.admunsisbackend.security;

import org.springframework.lang.NonNull;
import java.io.IOException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import io.jsonwebtoken.ExpiredJwtException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Filtro de autenticación JWT que intercepta las solicitudes HTTP para validar
 * el token JWT.
 * Extiende OncePerRequestFilter para asegurar que se ejecute una vez por
 * solicitud.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UserDetailsService userDetailsService;
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    /**
     * Constructor del filtro de autenticación JWT.
     *
     * @param tokenProvider      El proveedor de tokens JWT.
     * @param userDetailsService El servicio para cargar los detalles del usuario.
     */
    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, UserDetailsService userDetailsService) {
        this.tokenProvider = tokenProvider;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Método que se ejecuta para cada solicitud HTTP.
     *
     * @param request     La solicitud HTTP.
     * @param response    La respuesta HTTP.
     * @param filterChain La cadena de filtros.
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);
            logger.info("URL solicitada: " + request.getRequestURL());
            logger.info("Token JWT recibido: "
                    + (jwt != null ? jwt.substring(0, Math.min(10, jwt.length())) + "..." : "null"));

            if (StringUtils.hasText(jwt)) {
                if (tokenProvider.validateToken(jwt)) {
                    String username = tokenProvider.getUsernameFromToken(jwt);
                    logger.info("Token válido para el usuario: " + username);

                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
                    logger.info("Roles del usuario: " + userDetails.getAuthorities());

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null,
                            userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.info("Autenticación establecida exitosamente en el SecurityContext");
                } else {
                    logger.error("Token JWT inválido");
                }
            } else {
                logger.info("No se encontró token JWT en la solicitud");
            }

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException ex) {
            logger.error("Token expirado: {}", ex.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            new ObjectMapper().writeValue(response.getOutputStream(),
                    Map.of("error", "TOKEN_EXPIRED", "message", "Tu sesión ha expirado"));
        } catch (Exception ex) {
            logger.error("Error al procesar el token JWT", ex);
            filterChain.doFilter(request, response);
        }
    }

    /**
     * Extrae el token JWT de la cabecera Authorization de la solicitud HTTP.
     *
     * @param request La solicitud HTTP.
     * @return El token JWT si está presente, de lo contrario null.
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        logger.debug("Header Authorization: " + bearerToken);

        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}