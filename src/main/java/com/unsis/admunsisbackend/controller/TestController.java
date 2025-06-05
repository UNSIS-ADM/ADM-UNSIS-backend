package com.unsis.admunsisbackend.controller;

import com.unsis.admunsisbackend.model.User;
import com.unsis.admunsisbackend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TestController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/test/users")
    public List<User> getAllUsers() {
        return userRepository.findAll(); // Debe devolver todos los usuarios de la tabla `users`
    }
}
