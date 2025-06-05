/* 
DROP DATABASE IF EXISTS applicants;
CREATE DATABASE applicants;
USE applicants;

CREATE TABLE roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) UNIQUE NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL, 
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE TABLE applicants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    file_number BIGINT UNIQUE NOT NULL,
    curp VARCHAR(18) UNIQUE NOT NULL,
    phone VARCHAR(15),
    exam_assigned BOOLEAN DEFAULT FALSE,
    exam_room VARCHAR(20),
    exam_date DATETIME,
    status VARCHAR(20) DEFAULT 'PENDING',
    FOREIGN KEY (file_number) REFERENCES users(id) ON DELETE CASCADE
);

USE applicants;

-- Limpiar tablas existentes
DELETE FROM user_roles;
DELETE FROM roles;
DELETE FROM applicants;
DELETE FROM users;

-- Insertar roles (con el prefijo ROLE_)
INSERT INTO roles (name, description) VALUES
('ROLE_ADMIN', 'Administrador del sistema'),
('ROLE_USER', 'Usuario regular'),
('ROLE_APPLICANT', 'Aspirante');

-- Insertar usuarios (con contraseñas encriptadas)
INSERT INTO users (username, password, full_name, active, created_at) VALUES
('admin', '$2a$10$YYzfO.bg3YwEKEHWqmJ/PeDsSSz5zQnYvRbPiEZ1yTHwo0fWvM7XS', 'Admin Principal', true, NOW()),
('usuario', '$2a$10$YYzfO.bg3YwEKEHWqmJ/PeDsSSz5zQnYvRbPiEZ1yTHwo0fWvM7XS', 'Usuario Normal', true, NOW()),
('aspirante', '$2a$10$YYzfO.bg3YwEKEHWqmJ/PeDsSSz5zQnYvRbPiEZ1yTHwo0fWvM7XS', 'Aspirante Ejemplo', true, NOW());

-- Asignar roles
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'admin' AND r.name = 'ROLE_ADMIN';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'usuario' AND r.name = 'ROLE_USER';

INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id FROM users u, roles r WHERE u.username = 'aspirante' AND r.name = 'ROLE_APPLICANT';

-- Insertar datos de aspirante
INSERT INTO applicants (curp, phone, exam_assigned, exam_room, status, file_number)
SELECT 'XXXX000000XXXXXX00', '1234567890', false, NULL, 'PENDING', u.id
FROM users u WHERE u.username = 'aspirante';
*/

--Java 21 gradlew clean build


