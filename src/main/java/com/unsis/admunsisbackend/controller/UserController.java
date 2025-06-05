package com.unsis.admunsisbackend.controller;

import com.unsis.admunsisbackend.dto.UserResponseDTO;
import com.unsis.admunsisbackend.dto.ApplicantResponseDTO;
import com.unsis.admunsisbackend.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController {
    
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @GetMapping("/admin/users")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")  // Usar hasAuthority en lugar de hasRole
    public ResponseEntity<List<UserResponseDTO>> getAllUsers() {
        logger.info("Request to get all users by admin");
        List<UserResponseDTO> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/applicant/profile")
    @PreAuthorize("hasAuthority('ROLE_APPLICANT')")
    public ResponseEntity<ApplicantResponseDTO> getApplicantProfile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        logger.info("Request to get applicant profile for user: {}", username);
        
        ApplicantResponseDTO profile = userService.getApplicantProfile(username);
        return ResponseEntity.ok(profile);
    }
}