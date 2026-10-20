CREATE TABLE employees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_code VARCHAR(40) NOT NULL UNIQUE,
    candidate_id UUID NOT NULL UNIQUE REFERENCES candidates (id),
    company_id UUID REFERENCES companies (id),
    full_name VARCHAR(150) NOT NULL,
    designation VARCHAR(150),
    department VARCHAR(120),
    joining_date DATE,
    employment_type VARCHAR(40),
    work_location VARCHAR(180),
    reporting_manager VARCHAR(150),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    photo_path VARCHAR(500),
    bank_account_holder VARCHAR(150),
    bank_name VARCHAR(150),
    bank_account_encrypted TEXT,
    bank_ifsc VARCHAR(20),
    bank_branch VARCHAR(150),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_employees_status ON employees (status);
CREATE INDEX idx_employees_company ON employees (company_id);
CREATE INDEX idx_employees_name ON employees (full_name);

CREATE TABLE employee_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employees (id) ON DELETE CASCADE,
    old_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    changed_by UUID REFERENCES users (id),
    reason VARCHAR(500),
    note TEXT,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_employee_status_history ON employee_status_history (employee_id, changed_at);

CREATE TABLE placements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_id UUID NOT NULL REFERENCES candidates (id),
    company_id UUID NOT NULL REFERENCES companies (id),
    job_id UUID REFERENCES jobs (id),
    employee_id UUID REFERENCES employees (id),
    application_id UUID REFERENCES applications (id),
    designation VARCHAR(150),
    department VARCHAR(120),
    joining_date DATE,
    employment_type VARCHAR(40),
    work_location VARCHAR(180),
    reporting_manager VARCHAR(150),
    agreed_salary NUMERIC(14, 2),
    billing_salary NUMERIC(14, 2),
    payroll_type VARCHAR(40),
    placement_status VARCHAR(30) NOT NULL,
    offer_date DATE,
    joining_status VARCHAR(40),
    remarks TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_placements_candidate ON placements (candidate_id);
CREATE INDEX idx_placements_company ON placements (company_id);
CREATE INDEX idx_placements_employee ON placements (employee_id);
CREATE INDEX idx_placements_status ON placements (placement_status);

CREATE UNIQUE INDEX uq_open_placement
    ON placements (candidate_id, company_id, COALESCE(job_id, '00000000-0000-0000-0000-000000000000'::uuid))
    WHERE placement_status NOT IN ('EXITED', 'CANCELLED', 'TRANSFERRED');

CREATE TABLE placement_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    placement_id UUID NOT NULL REFERENCES placements (id) ON DELETE CASCADE,
    old_status VARCHAR(30),
    new_status VARCHAR(30) NOT NULL,
    changed_by UUID REFERENCES users (id),
    reason VARCHAR(500),
    note TEXT,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_placement_status_history ON placement_status_history (placement_id, changed_at);

CREATE SEQUENCE employee_code_seq START 1;
