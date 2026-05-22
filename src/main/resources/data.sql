	/*
    -- 1) (Re)crear la base de datos
	DROP DATABASE IF EXISTS applicants;
	CREATE DATABASE applicants;
	USE applicants;

	-- 2) Tabla de roles
	CREATE TABLE roles (
		id BIGINT AUTO_INCREMENT PRIMARY KEY,	
		name VARCHAR(50) UNIQUE NOT NULL,
		description VARCHAR(255)
	);

	-- 3) Tabla de usuarios
	CREATE TABLE users (
		id BIGINT AUTO_INCREMENT PRIMARY KEY,
		username VARCHAR(100) UNIQUE NOT NULL,
		password VARCHAR(255) NOT NULL,
		full_name VARCHAR(255) NOT NULL,
		active BOOLEAN DEFAULT TRUE,
		created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
		last_login TIMESTAMP NULL       -- Último login
	);

	-- 4) Relación N-M usuarios ↔ roles
	CREATE TABLE user_roles (
		user_id BIGINT NOT NULL,
		role_id BIGINT NOT NULL,
		PRIMARY KEY (user_id, role_id),
		FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
		FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
	);
    
	-- 5) Aspirantes
	CREATE TABLE applicants (
		id             BIGINT AUTO_INCREMENT PRIMARY KEY,
		user_id        BIGINT UNIQUE NOT NULL,      -- FK → users(id) file_ number
		-- ficha          BIGINT    NOT NULL,          -- Número Ficha del Excel
        ficha VARCHAR(20) NOT NULL,
		curp           VARCHAR(18) UNIQUE NOT NULL,
		career         VARCHAR(255),
		location       VARCHAR(100),                -- Ubicación/Sede
		exam_room      VARCHAR(100),                -- Aula asignada
		exam_assigned  BOOLEAN   DEFAULT FALSE,      -- ¿Examen ya asignado?
		exam_date      DATETIME,                    -- Fech	a/hora examen
		status         VARCHAR(50) DEFAULT 'PENDING',
		admission_year INT NOT NULL,
		attendance_status VARCHAR(20) NULL,  
		UNIQUE KEY ux_ficha (ficha),
		FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
	);

	-- 6) Resultados de admisión
	CREATE TABLE admission_results (
		id BIGINT AUTO_INCREMENT PRIMARY KEY,
		applicant_id BIGINT NOT NULL,       -- FK → applicants.id
		career_at_result VARCHAR(255) NULL,
		status VARCHAR(20) NOT NULL,        -- 'ACEPTADO', 'RECHAZADO'
		comment VARCHAR(255),
		score DECIMAL(5,2) NULL,            -- Puntaje opcional  
        final_grade DECIMAL(5,2) NULL,		-- Calificación de prope
		admission_year INT NOT NULL,
		created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,    
		FOREIGN KEY (applicant_id) REFERENCES applicants(id) ON DELETE CASCADE
	);	

	-- 7) Vacantes por carrera y año
	CREATE TABLE vacancies (
	  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
	  career          VARCHAR(255) NOT NULL,
	  admission_year  INT NOT NULL,
	  limit_count INT NOT NULL DEFAULT 0, 
	  inscritos_count INT NOT NULL DEFAULT 0,
	  cupos_inserted INT NOT NULL DEFAULT 0,
	  reserved_count INT NOT NULL DEFAULT 0,
	  available_slots INT NOT NULL DEFAULT 0,
      released_count INT NOT NULL DEFAULT 0,
	  created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
	  updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	  UNIQUE KEY ux_career_year (career, admission_year)
	);	

	-- 8) Tabla de solicitudes de cambio de carrera
	CREATE TABLE career_change_requests (
	  id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
	  applicant_id       BIGINT   NOT NULL,            -- FK → applicants.id
	  old_career         VARCHAR(255) NOT NULL,
	  old_status		 VARCHAR(20),
	  new_career         VARCHAR(255) NOT NULL,
	  status             VARCHAR(20)  NOT NULL,        -- PENDING, APPROVED, DENIED
	  request_comment    VARCHAR(255),                 -- opcional (aporta contexto)
	  response_comment   VARCHAR(255),                 -- comentario de Admin/Secretaría
	  requested_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
	  processed_at       TIMESTAMP    NULL,
	  processed_by       BIGINT       NULL,            -- FK → users.id del Admin/Secretaría
	  FOREIGN KEY (applicant_id)     REFERENCES applicants(id) ON DELETE CASCADE,  
	  FOREIGN KEY (processed_by)     REFERENCES users(id)      ON DELETE SET NULL
	);

	 -- 9) Tabla de ventanas de inactividad
	 CREATE TABLE access_restriction (
	  id BIGINT AUTO_INCREMENT PRIMARY KEY,
	  role_name VARCHAR(100) NOT NULL,
	  activation_date DATE NULL,
	  activation_time TIME NULL,
	  enabled BOOLEAN DEFAULT TRUE,
	  description VARCHAR(255),
	  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
	);

	 -- 10) Tabla para refrescar tokens
	CREATE TABLE refresh_tokens (
		id BIGINT AUTO_INCREMENT PRIMARY KEY,
		token VARCHAR(200) NOT NULL UNIQUE,
		expiry_date DATETIME NOT NULL,
		user_id BIGINT NOT NULL
	);
    
	-- 11) Tabla para contenido mensajes del front
CREATE TABLE IF NOT EXISTS contents (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  key_name VARCHAR(200) NOT NULL UNIQUE,
  title VARCHAR(255),
  language VARCHAR(10) DEFAULT 'es',
  active BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 12) Tabla para almacenar partes de contenido
CREATE TABLE IF NOT EXISTS content_parts (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  content_id BIGINT NOT NULL,
  part_key VARCHAR(100) NOT NULL,
  title VARCHAR(255),
  html_content TEXT NOT NULL,
  order_index INT NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT fk_content_parts_content FOREIGN KEY (content_id) REFERENCES contents(id) ON DELETE CASCADE,
  UNIQUE KEY ux_content_part_key (content_id, part_key)
);

-- 3) seed: crear los contents (aceptado/reprobado)
INSERT IGNORE INTO contents (key_name, title, language, active) VALUES
('mensaje_aceptado', 'Mensaje Aceptado', 'es', true),
('mensaje_reprobado', 'Mensaje Reprobado', 'es', true);

-- 4) obtener los ids recién creados (o existentes)
SET @id_aceptado = (SELECT id FROM contents WHERE key_name='mensaje_aceptado' LIMIT 1);
SET @id_reprobado = (SELECT id FROM contents WHERE key_name='mensaje_reprobado' LIMIT 1);

-- 5) insertar partes para mensaje_aceptado_2025 (keys inmutables)
INSERT INTO content_parts (content_id, part_key, title, html_content, order_index) VALUES
(@id_aceptado, 'greeting', 'Aceptado', '<p class="text-center font-semibold text-[#14645c]">¡Felicidades!</p>', 0),
(@id_aceptado, 'welcome_note', 'Bienvenida', '<p>Estimado aspirante, le damos la bienvenida al curso propedéutico 2025.</p>', 1),
(@id_aceptado, 'inscription_dates', 'Fechas e inscripciones', '<p>Se informa que las inscripciones serán del <span class="font-semibold">14 al 25 de julio de 2025</span> de forma presencial en el Departamento de Servicios Escolares de la Universidad de la Sierra Sur en un horario de <span class="font-semibold">9:00-14:00</span> y de <span class="font-semibold">16:00-19:00 hrs.</span>, se sugiere que realice su inscripción en los primeros días para evitar aglomeraciones.</p>', 2),
(@id_aceptado, 'survey', 'Encuesta', '<p>1. Ingrese en el siguiente enlace: <a href="https://survey.unsis.edu.mx/index.php/32638?lang=es-MX" class="text-blue-700 underline" target="_blank">https://survey.unsis.edu.mx/index.php/32638?lang=es-MX</a> y responda el cuestionario de estudio socioeconómico, para lo cual debe contar con comprobante de domicilio, comprobante de ingresos mensual (puede ser una constancia de ingresos expedida por tu municipio o agencia si no se cuenta con recibos de nómina) y fotografía de la fachada de su domicilio familiar.</p>', 3),
(@id_aceptado, 'documents_list', 'Documentos requeridos', '<p>2. Acudir al Departamento de Servicios Escolares de la UNSIS dentro de las fechas señaladas y presentar en original los siguientes documentos:</p><ul class="list-disc list-inside ml-4"><li>Acta de nacimiento</li><li>Certificado médico (expedido por IMSS, DIF, ISSSTE, Cruz Roja)</li><li>CURP</li><li>Certificado de bachillerato o constancia de estudios con tira de calificaciones</li><li>Dos fotografías tamaño infantil a blanco y negro</li></ul>', 4),
(@id_aceptado, 'note', 'Nota', '<p class="text-red-700 font-semibold">NOTA: El acuse del estudio socioeconómico realizado, debe guardarlo en formato digital para su respaldo.</p>', 5),
(@id_aceptado, 'start_date', 'Inicio curso', '<p>El curso propedéutico inicia el <span class="font-semibold">28 de julio de 2025</span>. Los horarios se publicarán oportunamente en la página oficial de la universidad <a href="https://www.unsis.edu.mx" class="text-blue-700 underline" target="_blank">www.unsis.edu.mx</a> de acuerdo al grupo asignado al momento de su inscripción.</p>', 6),
(@id_aceptado, 'contact', 'Contacto', '<p>En caso de dudas llamar al teléfono <span class="font-semibold">9515724100 EXt. 1203, 1204</span> o escribir a <a href="mailto:admision.unsis@gmail.com" class="text-blue-700 underline">admision.unsis@gmail.com</a>.</p>', 7);

-- 6) insertar partes para mensaje_reprobado_2025 (usar placeholder %CARRERAS_LIST% para la lista dinámica)
INSERT IGNORE INTO content_parts (content_id, part_key, title, html_content, order_index) VALUES
(@id_reprobado, 'header', 'Rechazado', '<p class="text-center font-semibold text-[#6a1b1b]">Oportunidad de continuar</p>', 0),
(@id_reprobado, 'body', 'Bienvenida', '<p>Estimado aspirante, desafortunadamente no puedes inscribirte a la licenciatura en medicina, pero queremos que seas parte de nuestra Comunidad UNSIS, para lo cual te ofrecemos la posibilidad de elegir alguna de la siguientes opciones, para continuar con tus estudios:</p>', 1),
(@id_reprobado, 'suggested_programs', 'Carreras dinámicas', '%CARRERAS_LIST%', 2),
(@id_reprobado, 'suneo_options', 'Opciones SUNEO', '<p>Si deseas ser parte de la comunidad SUNEO, tenemos las siguientes opciones:</p><ul class="list-disc list-inside ml-4"><li>Ingeniería Química (UNISTMO)</li><li>Licenciatura en Matemáticas Aplicadas (UNISTMO)</li><li>Ingeniería Química en Procesos Sustentables (UTM)</li></ul>', 3),
(@id_reprobado, 'contact', 'Contacto', '<p>Si te interesa formar parte de alguno de nuestros programas envía un correo electrónico a <a href="mailto:admision.unsis@gmail.com" class="text-blue-700 underline">admision.unsis@gmail.com</a> o llama al <span class="font-semibold"></span> indicando en asunto: &quot;Solicitud de otra carrera&quot; y en el cuerpo del correo describe el nombre de la carrera que eliges, tu nombre completo y el número de ficha para que se te agregue a la licenciatura solicitada.</p>', 4),
(@id_reprobado, 'deadline_note', 'Plazo', '<p class="text-red-700 font-semibold">Tienes hasta las 19:00 horas del 12 de julio de 2024 para realizar esta solicitud, ya que el cupo de los cursos es limitado.</p>', 5);

	-- Datos iniciales
	INSERT INTO roles (name, description) VALUES
	  ('ROLE_ADMIN',     'Administrador del sistema'),
	  ('ROLE_USER',      'Secretaría'),
	  ('ROLE_APPLICANT', 'Aspirante');
 */