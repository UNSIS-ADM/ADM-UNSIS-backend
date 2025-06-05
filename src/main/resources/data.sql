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

-- Insertar roles
INSERT INTO roles (name, description) VALUES
('ADMIN', 'Administrador del sistema'),
('USER', 'Usuario regular'),
('APPLICANT', 'Postulante');

-- Insertar usuarios
INSERT INTO users (username, password, full_name) VALUES
('admin', 'admin123', 'Admin Principal'),
('usuario', 'user123', 'Usuario Normal'),
('aspirante', 'aspirante123', 'Aspirante Ejemplo');

-- Asignar roles (ids: 1=ADMIN, 2=USER, 3=APPLICANT)
INSERT INTO user_roles (user_id, role_id) VALUES
(1, 1),  -- admin → ADMIN
(2, 2),  -- usuario → USER
(3, 3);  -- aspirante → APPLICANT

-- Insertar datos de postulante (applicant)
INSERT INTO applicants (file_number, curp, phone, exam_assigned, exam_room, exam_date, status)
VALUES 
(3, 'XEXX010101HNEXXXA4', '5551234567', TRUE, 'Aula 1', '2025-06-10 09:00:00', 'PENDING');
*/

--Java 21 gradlew clean build
