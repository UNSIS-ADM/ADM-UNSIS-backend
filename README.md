# ADM-UNSIS-backend

# Levantar el servidor
./gradlew bootRun
fix: corregir warnings y mejorar validaciones en backend

- Agregar @NonNull en overrides de doFilterInternal (RoleAccessRestrictionFilter y JwtAuthenticationFilter)
- Manejar posible null en MultipartFile.getOriginalFilename() en ExcelController
- Actualizar build.gradle a Spring Boot 3.2.12 para usar último parche disponible
- Eliminar imports innecesarios y warnings de código (Micrometer NonNull -> Spring NonNull)
