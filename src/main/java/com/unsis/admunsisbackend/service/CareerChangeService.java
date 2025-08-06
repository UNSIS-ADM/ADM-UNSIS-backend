
package com.unsis.admunsisbackend.service;

import com.unsis.admunsisbackend.dto.*;
import java.util.List;

public interface CareerChangeService {
    CareerChangeRequestDTO submitChange(String username, CreateCareerChangeRequestDTO dto);
    List<CareerChangeRequestDTO> listPending();  // solo Admin/Secretaría
    CareerChangeRequestDTO processRequest(Long requestId, ProcessCareerChangeRequestDTO dto, String adminUsername);
}
