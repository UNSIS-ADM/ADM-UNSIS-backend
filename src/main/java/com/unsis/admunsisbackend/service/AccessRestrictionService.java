package com.unsis.admunsisbackend.service;

import com.unsis.admunsisbackend.model.AccessRestriction;

// Servicio para gestionar las restricciones de acceso basadas en roles.
public interface AccessRestrictionService {
    // Verifica si el acceso está permitido para un rol dado.
    boolean isAccessAllowed(String roleName);
    
    /*
     * Verifica si el acceso está permitido para el ROLE_APPLICANT según la 
     * regla almacenada.
     */
    AccessRestriction getRestriction();

    // Guarda o actualiza una restricción de acceso.
    AccessRestriction saveOrUpdate(AccessRestriction restriction);
}
