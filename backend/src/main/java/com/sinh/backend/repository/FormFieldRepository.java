package com.sinh.backend.repository;

import com.sinh.backend.entity.FormField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FormFieldRepository extends JpaRepository<FormField, Integer> {

    List<FormField> findByFormIdOrderByOrderIndexAsc(Integer formId);

    Optional<FormField> findByFormIdAndFieldKey(Integer formId, String fieldKey);

    void deleteByFormId(Integer formId);
}
