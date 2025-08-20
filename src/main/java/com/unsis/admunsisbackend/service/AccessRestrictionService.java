package com.unsis.admunsisbackend.service;

import com.unsis.admunsisbackend.model.AccessRestriction;
import java.util.Optional;

public interface AccessRestrictionService {
    boolean isAccessAllowed(String roleName);
    AccessRestriction getRestriction(); // devuelve la regla para ROLE_APPLICANT o null
    AccessRestriction saveOrUpdate(AccessRestriction restriction);
}
