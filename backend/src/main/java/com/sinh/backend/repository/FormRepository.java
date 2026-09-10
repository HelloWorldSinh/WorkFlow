package com.sinh.backend.repository;

import com.sinh.backend.entity.Form;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FormRepository extends JpaRepository<Form, Integer> {

       /**
        * Lấy toàn bộ biểu mẫu đang hoạt động cùng danh sách fields và createdBy (tránh
        * N+1 query)
        */
       @Query("SELECT DISTINCT f FROM Form f " +
                     "LEFT JOIN FETCH f.fields " +
                     "LEFT JOIN FETCH f.createdBy " +
                     "WHERE f.isActive = true " +
                     "ORDER BY f.createdAt DESC")
       List<Form> findAllActiveFormsWithFields();

       /**
        * Tìm kiếm biểu mẫu theo từ khóa (tên hoặc mô tả) kèm danh sách fields
        */
       @Query("SELECT DISTINCT f FROM Form f " +
                     "LEFT JOIN FETCH f.fields " +
                     "LEFT JOIN FETCH f.createdBy " +
                     "WHERE f.isActive = true " +
                     "AND (LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(f.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
                     +
                     "ORDER BY f.createdAt DESC")
       List<Form> searchActiveFormsWithFields(@Param("keyword") String keyword);

       /**
        * Xem chi tiết 1 biểu mẫu theo ID kèm các fields và createdBy
        */
       @Query("SELECT f FROM Form f " +
                     "LEFT JOIN FETCH f.fields " +
                     "LEFT JOIN FETCH f.createdBy " +
                     "WHERE f.id = :id AND f.isActive = true")
       Optional<Form> findByIdWithFields(@Param("id") Integer id);

       Optional<Form> findByIdAndIsActiveTrue(Integer id);

       boolean existsByNameIgnoreCaseAndIsActiveTrue(String name);

       boolean existsByNameIgnoreCaseAndIdNotAndIsActiveTrue(String name, Integer id);
}
