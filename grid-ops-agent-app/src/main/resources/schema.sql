CREATE DATABASE IF NOT EXISTS power_aiops DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE power_aiops;

CREATE TABLE IF NOT EXISTS chat_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL UNIQUE,
    user_id VARCHAR(64),
    current_device_id VARCHAR(64),
    current_alarm_id VARCHAR(64),
    current_task_id VARCHAR(64),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL,
    role VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    intent VARCHAR(64),
    agent_name VARCHAR(64),
    tool_calls JSON,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_id (session_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS alarm_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(64) NOT NULL UNIQUE,
    alarm_id VARCHAR(64) NOT NULL,
    station VARCHAR(128),
    device_id VARCHAR(64),
    device_name VARCHAR(128),
    device_type VARCHAR(64),
    alarm_type VARCHAR(128),
    alarm_level VARCHAR(32),
    alarm_source VARCHAR(128),
    current_value VARCHAR(64),
    threshold VARCHAR(64),
    duration VARCHAR(64),
    status VARCHAR(32) DEFAULT 'RECEIVED',
    selected_skill VARCHAR(128),
    diagnosis_result JSON,
    checkpoint_data JSON,
    error_message TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_alarm_id (alarm_id),
    INDEX idx_device_id (device_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS agent_execution_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trace_id VARCHAR(64) NOT NULL,
    span_id VARCHAR(64),
    parent_span_id VARCHAR(64),
    task_id VARCHAR(64),
    session_id VARCHAR(64),
    node_name VARCHAR(128),
    agent_name VARCHAR(64) NOT NULL,
    step_name VARCHAR(128),
    input_summary TEXT,
    output_summary TEXT,
    status VARCHAR(32),
    duration_ms BIGINT,
    token_count INT,
    model_name VARCHAR(64),
    error_type VARCHAR(64),
    error_message TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_trace_id (trace_id),
    INDEX idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tool_call_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trace_id VARCHAR(64) NOT NULL,
    span_id VARCHAR(64),
    task_id VARCHAR(64),
    session_id VARCHAR(64),
    tool_name VARCHAR(64) NOT NULL,
    tool_source VARCHAR(32),
    request_param JSON,
    response_data JSON,
    status VARCHAR(32),
    duration_ms BIGINT,
    error_type VARCHAR(64),
    retry_count INT,
    error_message TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_trace_id (trace_id),
    INDEX idx_tool_name (tool_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS knowledge_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    document_id VARCHAR(64) NOT NULL UNIQUE,
    document_name VARCHAR(256) NOT NULL,
    document_type VARCHAR(64) NOT NULL,
    source VARCHAR(128),
    file_path VARCHAR(512),
    file_type VARCHAR(32),
    file_size BIGINT,
    status VARCHAR(32) DEFAULT 'UPLOADED',
    chunk_count INT DEFAULT 0,
    embedding_count INT DEFAULT 0,
    description TEXT,
    tags VARCHAR(512),
    version INT DEFAULT 1,
    enabled TINYINT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_document_type (document_type),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS knowledge_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    chunk_id VARCHAR(64) NOT NULL UNIQUE,
    document_id VARCHAR(64) NOT NULL,
    chunk_index INT NOT NULL,
    chapter VARCHAR(256),
    page_no INT,
    content TEXT NOT NULL,
    token_count INT,
    status VARCHAR(32) DEFAULT 'ACTIVE',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_document_id (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS knowledge_process_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(64) NOT NULL UNIQUE,
    document_id VARCHAR(64) NOT NULL,
    task_type VARCHAR(32) DEFAULT 'INDEX',
    status VARCHAR(32) DEFAULT 'PENDING',
    current_step VARCHAR(64),
    error_message TEXT,
    started_at DATETIME,
    completed_at DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_document_id (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS tool_registry (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tool_id VARCHAR(64) NOT NULL UNIQUE,
    tool_name VARCHAR(64) NOT NULL,
    description TEXT,
    input_schema JSON,
    output_schema JSON,
    tags VARCHAR(256),
    permission_level VARCHAR(32) DEFAULT 'READONLY',
    risk_level VARCHAR(32) DEFAULT 'LOW',
    owner_system VARCHAR(128),
    enabled TINYINT DEFAULT 1,
    version INT DEFAULT 1,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS skill_definition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    skill_id VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    version VARCHAR(32),
    description TEXT,
    category VARCHAR(64),
    applicable_scenarios JSON,
    recommended_tools JSON,
    diagnosis_workflow JSON,
    output_schema JSON,
    prompt_template TEXT,
    examples JSON,
    enabled TINYINT DEFAULT 1,
    priority INT DEFAULT 50,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS checkpoint_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    task_id VARCHAR(64) NOT NULL,
    step_name VARCHAR(64) NOT NULL,
    agent_state JSON,
    plan_steps JSON,
    completed_steps JSON,
    rag_results JSON,
    tool_results JSON,
    subagent_results JSON,
    diagnosis_draft TEXT,
    approval_status VARCHAR(32),
    error_message TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
