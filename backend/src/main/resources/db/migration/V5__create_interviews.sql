CREATE TABLE interviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES applications (id),
    candidate_id UUID NOT NULL REFERENCES candidates (id),
    job_id UUID NOT NULL REFERENCES jobs (id),
    interviewer_id UUID REFERENCES users (id),
    scheduled_date DATE NOT NULL,
    scheduled_time TIME NOT NULL,
    mode VARCHAR(30) NOT NULL,
    meeting_link VARCHAR(500),
    location VARCHAR(255),
    remarks TEXT,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_interviews_application ON interviews (application_id);
CREATE INDEX idx_interviews_date ON interviews (scheduled_date);
CREATE INDEX idx_interviews_status ON interviews (status);

CREATE TABLE interview_feedback (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    interview_id UUID NOT NULL UNIQUE REFERENCES interviews (id) ON DELETE CASCADE,
    rating INT,
    strengths TEXT,
    weaknesses TEXT,
    recommendation VARCHAR(30),
    remarks TEXT,
    submitted_by UUID REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
