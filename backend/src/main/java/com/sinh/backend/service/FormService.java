package com.sinh.backend.service;

import com.sinh.backend.dto.request.CreateFormRequest;
import com.sinh.backend.dto.request.UpdateFormRequest;
import com.sinh.backend.dto.response.FormResponseDTO;

import java.util.List;

public interface FormService {

    FormResponseDTO createForm(CreateFormRequest request);

    FormResponseDTO updateForm(Integer id, UpdateFormRequest request);

    FormResponseDTO getFormById(Integer id);

    List<FormResponseDTO> getAllForms(String keyword);

    void deleteForm(Integer id);
}
