-- V1: Administrative Service Persistence Schema

CREATE TABLE IF NOT EXISTS role_assignments (
    identity_id VARCHAR(255) PRIMARY KEY,
    role VARCHAR(50) NOT NULL,
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(255),
    CONSTRAINT chk_role_valid CHECK (role IN ('MODERATOR', 'ADMIN'))
);

CREATE TABLE IF NOT EXISTS administrative_cases (
    id VARCHAR(255) PRIMARY KEY,
    created_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    subject VARCHAR(255) NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN',
    decided_by VARCHAR(255),
    decided_at TIMESTAMP WITH TIME ZONE,
    outcome VARCHAR(255),
    justification TEXT,
    CONSTRAINT chk_case_status CHECK (status IN ('OPEN', 'RESOLVED')),
    CONSTRAINT chk_case_decision_consistency CHECK (
        (status = 'OPEN' AND decided_by IS NULL AND decided_at IS NULL AND outcome IS NULL AND justification IS NULL) OR
        (status = 'RESOLVED' AND decided_by IS NOT NULL AND decided_at IS NOT NULL AND outcome IS NOT NULL AND justification IS NOT NULL)
    )
);

CREATE TABLE IF NOT EXISTS case_evidence (
    case_id VARCHAR(255) NOT NULL REFERENCES administrative_cases(id) ON DELETE CASCADE,
    evidence_order INT NOT NULL,
    evidence_reference TEXT NOT NULL,
    PRIMARY KEY (case_id, evidence_order)
);

CREATE TABLE IF NOT EXISTS case_locks (
    case_id VARCHAR(255) PRIMARY KEY REFERENCES administrative_cases(id) ON DELETE CASCADE,
    holder_id VARCHAR(255) NOT NULL,
    token VARCHAR(255) NOT NULL,
    acquired_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT chk_lock_expiration CHECK (expires_at >= acquired_at)
);

CREATE INDEX IF NOT EXISTS idx_administrative_cases_status ON administrative_cases(status);
CREATE INDEX IF NOT EXISTS idx_case_locks_expires_at ON case_locks(expires_at);
