-- ==========================================================
-- Flyway Migration V2: Dữ liệu khởi tạo (Seed Data)
-- Chèn các tài khoản mẫu ban đầu với INSERT IGNORE
-- ==========================================================

INSERT IGNORE INTO `users` (`id`, `email`, `password`, `full_name`, `department_id`, `manager_id`, `role`, `is_active`)
VALUES 
(1, 'admin@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'System Admin', 1, NULL, 'Admin', TRUE),
(2, 'manager@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Department Manager', 1, 1, 'WorkflowOwner', TRUE),
(3, 'approver@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Direct Approver', 1, 2, 'Approver', TRUE),
(4, 'editor@example.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Process Editor', 1, 2, 'Editor', TRUE);
