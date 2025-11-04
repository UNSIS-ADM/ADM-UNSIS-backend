package com.unsis.admunsisbackend.security;

import com.unsis.admunsisbackend.model.User;
import com.unsis.admunsisbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.stream.Collectors;

/**
 * Servicio personalizado para cargar los detalles del usuario desde la base de datos.
 * Implementa la interfaz UserDetailsService de Spring Security.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    @Autowired
    private UserRepository userRepository;

    /**
     * Carga un usuario por su nombre de usuario.
     *
     * @param username El nombre de usuario del usuario a cargar.
     * @return Los detalles del usuario cargado.
     * @throws UsernameNotFoundException Si el usuario no se encuentra en la base de datos.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));

        var authorities = user.getRoles().stream()
                .map(role -> {
                    String roleName = role.getName(); // Ya viene con el prefijo ROLE_ de la BD
                    logger.debug("Role encontrado: " + roleName);
                    return new SimpleGrantedAuthority(roleName);
                })
                .collect(Collectors.toList());
        logger.info("Usuario " + username + " cargado con roles: " + authorities);

        // Retorna una instancia de UserDetails con el nombre de usuario, contraseña y roles del usuario
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                authorities);
    }
}
