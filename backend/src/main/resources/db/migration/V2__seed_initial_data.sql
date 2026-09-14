-- ==========================================================
-- Flyway Migration V2: Dữ liệu khởi tạo (Seed Data)
-- Chèn các tài khoản mẫu ban đầu với INSERT IGNORE
-- Mật khẩu mặc định: 123456
-- ==========================================================

INSERT IGNORE INTO `users` (`id`, `email`, `password`, `full_name`, `department_id`, `manager_id`, `role`, `is_active`)
VALUES 
(1, 'admin@example.com', '123456', 'System Admin', 1, NULL, 'Admin', TRUE),
(2, 'manager@example.com', '123456', 'Department Manager', 1, 1, 'WorkflowOwner', TRUE),
(3, 'approver@example.com', '123456', 'Direct Approver', 1, 2, 'Approver', TRUE),
(4, 'editor@example.com', '123456', 'Process Editor', 1, 2, 'Editor', TRUE);
