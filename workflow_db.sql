CREATE TABLE IF NOT EXISTS `users` (
  `id` int PRIMARY KEY AUTO_INCREMENT,
  `email` varchar(255) UNIQUE NOT NULL,
  `password` varchar(255),
  `full_name` varchar(255),
  `department_id` int,
  `manager_id` int,
  `role` ENUM ('Admin', 'WorkflowOwner', 'Editor', 'Viewer', 'Approver') DEFAULT 'Viewer',
  `is_active` boolean DEFAULT true
);

CREATE TABLE IF NOT EXISTS `workflows` (
  `id` int PRIMARY KEY AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` text,
  `owner_id` int,
  `status` ENUM ('Draft', 'Published', 'Suspended', 'Deleted') DEFAULT 'Draft',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `deleted_at` timestamp NULL DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS `workflow_versions` (
  `id` int PRIMARY KEY AUTO_INCREMENT,
  `workflow_id` int,
  `version_number` varchar(255),
  `is_active` boolean DEFAULT false,
  `published_at` timestamp NULL DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS `forms` (
  `id` int PRIMARY KEY AUTO_INCREMENT,
  `name` varchar(255) NOT NULL,
  `description` text,
  `created_by` int,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `is_active` boolean DEFAULT true
);

CREATE TABLE IF NOT EXISTS `form_fields` (
  `id` int PRIMARY KEY AUTO_INCREMENT,
  `form_id` int NOT NULL,
  `field_key` varchar(100) NOT NULL,
  `label` varchar(255) NOT NULL,
  `type` varchar(50) NOT NULL,
  `placeholder` varchar(255),
  `help_text` varchar(255),
  `required` boolean DEFAULT false,
  `default_value` text,
  `min_val` int,
  `max_val` int,
  `options` json,
  `order_index` int DEFAULT 0,
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS `nodes` (
  `id` int PRIMARY KEY AUTO_INCREMENT,
  `workflow_id` int,
  `type` ENUM ('Start', 'FormInput', 'Approval', 'Review', 'Assignment', 'Notification', 'SystemAction', 'End'),
  `name` varchar(255),
  `config` json,
  `form_id` int
);

CREATE TABLE IF NOT EXISTS `transitions` (
  `id` int PRIMARY KEY AUTO_INCREMENT,
  `workflow_id` int,
  `source_node_id` int,
  `target_node_id` int,
  `label` varchar(255),
  `conditions` json
);

CREATE TABLE IF NOT EXISTS `workflow_instances` (
  `id` int PRIMARY KEY AUTO_INCREMENT,
  `workflow_id` int,
  `request_code` varchar(255),
  `creator_id` int,
  `status` ENUM ('Running', 'Completed', 'Rejected', 'Cancelled') DEFAULT 'Running',
  `start_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `end_time` timestamp NULL DEFAULT NULL,
  `variables` json
);

CREATE TABLE IF NOT EXISTS `task_instances` (
  `id` int PRIMARY KEY AUTO_INCREMENT,
  `workflow_instance_id` int,
  `node_id` int,
  `assigned_user_id` int,
  `status` ENUM ('Pending', 'Approved', 'Rejected') DEFAULT 'Pending',
  `due_date` timestamp NULL DEFAULT NULL,
  `completed_at` timestamp NULL DEFAULT NULL,
  `step_submitted_data` json
);

CREATE TABLE IF NOT EXISTS `change_logs` (
  `id` int PRIMARY KEY AUTO_INCREMENT,
  `workflow_id` int,
  `user_id` int,
  `action` varchar(255),
  `details` json,
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Foreign Key Constraints
ALTER TABLE `users` ADD FOREIGN KEY (`manager_id`) REFERENCES `users` (`id`);

ALTER TABLE `workflows` ADD FOREIGN KEY (`owner_id`) REFERENCES `users` (`id`);

ALTER TABLE `workflow_versions` ADD FOREIGN KEY (`workflow_id`) REFERENCES `workflows` (`id`);

ALTER TABLE `forms` ADD FOREIGN KEY (`created_by`) REFERENCES `users` (`id`);

ALTER TABLE `form_fields` ADD FOREIGN KEY (`form_id`) REFERENCES `forms` (`id`) ON DELETE CASCADE;

ALTER TABLE `nodes` ADD FOREIGN KEY (`workflow_id`) REFERENCES `workflows` (`id`);

ALTER TABLE `nodes` ADD FOREIGN KEY (`form_id`) REFERENCES `forms` (`id`);

ALTER TABLE `transitions` ADD FOREIGN KEY (`workflow_id`) REFERENCES `workflows` (`id`);

ALTER TABLE `transitions` ADD FOREIGN KEY (`source_node_id`) REFERENCES `nodes` (`id`);

ALTER TABLE `transitions` ADD FOREIGN KEY (`target_node_id`) REFERENCES `nodes` (`id`);

ALTER TABLE `workflow_instances` ADD FOREIGN KEY (`workflow_id`) REFERENCES `workflows` (`id`);

ALTER TABLE `workflow_instances` ADD FOREIGN KEY (`creator_id`) REFERENCES `users` (`id`);

ALTER TABLE `task_instances` ADD FOREIGN KEY (`workflow_instance_id`) REFERENCES `workflow_instances` (`id`);

ALTER TABLE `task_instances` ADD FOREIGN KEY (`node_id`) REFERENCES `nodes` (`id`);

ALTER TABLE `task_instances` ADD FOREIGN KEY (`assigned_user_id`) REFERENCES `users` (`id`);

ALTER TABLE `change_logs` ADD FOREIGN KEY (`workflow_id`) REFERENCES `workflows` (`id`);

ALTER TABLE `change_logs` ADD FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);
