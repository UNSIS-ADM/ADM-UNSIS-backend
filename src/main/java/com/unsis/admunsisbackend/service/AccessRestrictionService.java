package com.unsis.admunsisbackend.service;

import com.unsis.admunsisbackend.model.AccessRestriction;
import java.util.List;

public interface AccessRestrictionService {
    boolean isAccessAllowed(String roleName); // true = permitido ahora
    List<AccessRestriction> listForRole(String roleName);
    AccessRestriction create(AccessRestriction w);
    AccessRestriction update(Long id, AccessRestriction w); 
    void delete(Long id);
    //AccessRestriction save(AccessRestriction w);
    // otros métodos: delete, update, enable/disable
}
