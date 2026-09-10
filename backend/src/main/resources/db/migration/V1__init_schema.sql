-- ==========================================================
-- Flyway Migration V1: Khởi tạo schema cơ sở dữ liệu hệ thống
-- Chuẩn hóa theo 10 JPA Entities của backend (MySQL Dialect)
-- ==========================================================

-- 1. Bảng users
CREATE TABLE IF NOT EXISTS `users` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `email` VARCHAR(255) NOT NULL UNIQUE,
  `password` VARCHAR(255) NULL,
  `full_name` VARCHAR(255) NULL,
  `department_id` INT NULL,
  `manager_id` INT NULL,
  `role` ENUM('Admin', 'WorkflowOwner', 'Editor', 'Viewer', 'Approver') DEFAULT 'Viewer',
  `is_active` BOOLEAN DEFAULT TRUE,
  CONSTRAINT `fk_users_manager` FOREIGN KEY (`manager_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Bảng workflows
CREATE TABLE IF NOT EXISTS `workflows` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(255) NOT NULL,
  `description` TEXT NULL,
  `owner_id` INT NULL,
  `status` ENUM('Draft', 'Published', 'Suspended', 'Deleted') DEFAULT 'Draft',
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted_at` TIMESTAMP NULL DEFAULT NULL,
  CONSTRAINT `fk_workflows_owner` FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Bảng workflow_versions
CREATE TABLE IF NOT EXISTS `workflow_versions` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `workflow_id` INT NULL,
  `version_number` VARCHAR(255) NULL,
  `is_active` BOOLEAN DEFAULT FALSE,
  `published_at` TIMESTAMP NULL DEFAULT NULL,
  CONSTRAINT `fk_workflow_versions_workflow` FOREIGN KEY (`workflow_id`) REFERENCES `workflows` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Bảng forms
CREATE TABLE IF NOT EXISTS `forms` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `name` VARCHAR(255) NOT NULL,
  `description` TEXT NULL,
  `created_by` INT NULL,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `is_active` BOOLEAN DEFAULT TRUE,
  CONSTRAINT `fk_forms_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Bảng form_fields
CREATE TABLE IF NOT EXISTS `form_fields` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `form_id` INT NOT NULL,
  `field_key` VARCHAR(100) NOT NULL,
  `label` VARCHAR(255) NOT NULL,
  `type` VARCHAR(50) NOT NULL,
  `placeholder` VARCHAR(255) NULL,
  `help_text` VARCHAR(255) NULL,
  `required` BOOLEAN DEFAULT FALSE,
  `default_value` TEXT NULL,
  `min_val` INT NULL,
  `max_val` INT NULL,
  `options` JSON NULL,
  `order_index` INT DEFAULT 0,
  `created_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  CONSTRAINT `fk_form_fields_form` FOREIGN KEY (`form_id`) REFERENCES `forms` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. Bảng nodes
CREATE TABLE IF NOT EXISTS `nodes` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `workflow_id` INT NULL,
  `type` ENUM('Start', 'FormInput', 'Approval', 'Review', 'Assignment', 'Notification', 'SystemAction', 'End') NULL,
  `name` VARCHAR(255) NULL,
  `config` JSON NULL,
  `form_id` INT NULL,
  CONSTRAINT `fk_nodes_workflow` FOREIGN KEY (`workflow_id`) REFERENCES `workflows` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_nodes_form` FOREIGN KEY (`form_id`) REFERENCES `forms` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. Bảng transitions
CREATE TABLE IF NOT EXISTS `transitions` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `workflow_id` INT NULL,
  `source_node_id` INT NULL,
  `target_node_id` INT NULL,
  `label` VARCHAR(255) NULL,
  `conditions` JSON NULL,
  CONSTRAINT `fk_transitions_workflow` FOREIGN KEY (`workflow_id`) REFERENCES `workflows` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_transitions_source_node` FOREIGN KEY (`source_node_id`) REFERENCES `nodes` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_transitions_target_node` FOREIGN KEY (`target_node_id`) REFERENCES `nodes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. Bảng workflow_instances
CREATE TABLE IF NOT EXISTS `workflow_instances` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `workflow_id` INT NULL,
  `request_code` VARCHAR(255) NULL,
  `creator_id` INT NULL,
  `status` ENUM('Running', 'Completed', 'Rejected', 'Cancelled') DEFAULT 'Running',
  `start_time` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP,
  `end_time` TIMESTAMP NULL DEFAULT NULL,
  `variables` JSON NULL,
  CONSTRAINT `fk_workflow_instances_workflow` FOREIGN KEY (`workflow_id`) REFERENCES `workflows` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_workflow_instances_creator` FOREIGN KEY (`creator_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 9. Bảng task_instances
CREATE TABLE IF NOT EXISTS `task_instances` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `workflow_instance_id` INT NULL,
  `node_id` INT NULL,
  `assigned_user_id` INT NULL,
  `status` ENUM('Pending', 'Approved', 'Rejected') DEFAULT 'Pending',
  `due_date` TIMESTAMP NULL DEFAULT NULL,
  `completed_at` TIMESTAMP NULL DEFAULT NULL,
  `step_submitted_data` JSON NULL,
  CONSTRAINT `fk_task_instances_instance` FOREIGN KEY (`workflow_instance_id`) REFERENCES `workflow_instances` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_task_instances_node` FOREIGN KEY (`node_id`) REFERENCES `nodes` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_task_instances_user` FOREIGN KEY (`assigned_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. Bảng change_logs
CREATE TABLE IF NOT EXISTS `change_logs` (
  `id` INT AUTO_INCREMENT PRIMARY KEY,
  `workflow_id` INT NULL,
  `user_id` INT NULL,
  `action` VARCHAR(255) NULL,
  `details` JSON NULL,
  `updated_at` TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CONSTRAINT `fk_change_logs_workflow` FOREIGN KEY (`workflow_id`) REFERENCES `workflows` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_change_logs_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
