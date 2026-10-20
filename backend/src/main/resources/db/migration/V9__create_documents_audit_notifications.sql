CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    entity_type VARCHAR(40) NOT NULL,
    entity_id UUID NOT NULL,
    document_type VARCHAR(60) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    storage_path VARCHAR(500) NOT NULL,
    mime_type VARCHAR(120) NOT NULL,
    file_size BIGINT NOT NULL,
    public_visible BOOLEAN NOT NULL DEFAULT FALSE,
    uploaded_by UUID REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_documents_entity ON documents (entity_type, entity_id);

CREATE TABLE document_requirements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(40) NOT NULL UNIQUE,
    label VARCHAR(120) NOT NULL,
    description VARCHAR(300),
    required BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    user_email VARCHAR(180),
    action VARCHAR(80) NOT NULL,
    entity_type VARCHAR(60),
    entity_id VARCHAR(64),
    ip_address VARCHAR(64),
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_created ON audit_logs (created_at DESC);
CREATE INDEX idx_audit_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_action ON audit_logs (action);

CREATE TABLE notification_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    channel VARCHAR(20) NOT NULL,
    recipient VARCHAR(180),
    template_key VARCHAR(80) NOT NULL,
    subject VARCHAR(200),
    body TEXT,
    status VARCHAR(20) NOT NULL,
    related_entity_type VARCHAR(40),
    related_entity_id UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    sent_at TIMESTAMPTZ
);

CREATE INDEX idx_notifications_status ON notification_messages (status, created_at);

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX idx_candidates_name_trgm ON candidates USING gin (full_name gin_trgm_ops);
CREATE INDEX idx_jobs_title_trgm ON jobs USING gin (title gin_trgm_ops);
CREATE INDEX idx_companies_name_trgm ON companies USING gin (company_name gin_trgm_ops);
CREATE INDEX idx_employees_name_trgm ON employees USING gin (full_name gin_trgm_ops);
