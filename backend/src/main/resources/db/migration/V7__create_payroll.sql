CREATE TABLE salary_component_definitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(80) NOT NULL,
    code VARCHAR(40) NOT NULL UNIQUE,
    component_type VARCHAR(20) NOT NULL,
    calculation_type VARCHAR(30) NOT NULL,
    default_value NUMERIC(14, 2) NOT NULL DEFAULT 0,
    display_order INT NOT NULL DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE salary_structures (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employees (id),
    effective_date DATE NOT NULL,
    end_date DATE,
    reason VARCHAR(500),
    changed_by UUID REFERENCES users (id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    gross_amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    deduction_amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    net_amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_salary_structures_employee ON salary_structures (employee_id, effective_date DESC);

CREATE TABLE salary_revisions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employees (id),
    previous_structure_id UUID REFERENCES salary_structures (id),
    new_structure_id UUID NOT NULL REFERENCES salary_structures (id),
    effective_date DATE NOT NULL,
    previous_net NUMERIC(14, 2),
    new_net NUMERIC(14, 2) NOT NULL,
    reason VARCHAR(500),
    changed_by UUID REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_salary_revisions_employee ON salary_revisions (employee_id, effective_date);

CREATE TABLE employee_salary_components (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    salary_structure_id UUID NOT NULL REFERENCES salary_structures (id) ON DELETE CASCADE,
    component_id UUID NOT NULL REFERENCES salary_component_definitions (id),
    calculation_type VARCHAR(30) NOT NULL,
    value NUMERIC(14, 4) NOT NULL,
    calculated_amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_emp_salary_components_structure ON employee_salary_components (salary_structure_id);

CREATE TABLE payroll_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies (id),
    period_month INT NOT NULL,
    period_year INT NOT NULL,
    status VARCHAR(20) NOT NULL,
    total_gross NUMERIC(16, 2) NOT NULL DEFAULT 0,
    total_deductions NUMERIC(16, 2) NOT NULL DEFAULT 0,
    total_net NUMERIC(16, 2) NOT NULL DEFAULT 0,
    generated_by UUID REFERENCES users (id),
    approved_by UUID REFERENCES users (id),
    locked_by UUID REFERENCES users (id),
    approved_at TIMESTAMPTZ,
    locked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (company_id, period_month, period_year)
);

CREATE INDEX idx_payroll_runs_period ON payroll_runs (period_year, period_month);

CREATE TABLE payroll_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payroll_run_id UUID NOT NULL REFERENCES payroll_runs (id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES employees (id),
    salary_structure_id UUID REFERENCES salary_structures (id),
    employee_code VARCHAR(40) NOT NULL,
    employee_name VARCHAR(150) NOT NULL,
    designation VARCHAR(150),
    department VARCHAR(120),
    total_days INT NOT NULL,
    working_days INT NOT NULL,
    payable_days NUMERIC(6, 2) NOT NULL,
    leave_days NUMERIC(6, 2) NOT NULL DEFAULT 0,
    basic_amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    gross_amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    deduction_amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    net_amount NUMERIC(14, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (payroll_run_id, employee_id)
);

CREATE INDEX idx_payroll_records_employee ON payroll_records (employee_id);

CREATE TABLE payroll_lines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payroll_record_id UUID NOT NULL REFERENCES payroll_records (id) ON DELETE CASCADE,
    component_code VARCHAR(40) NOT NULL,
    component_name VARCHAR(80) NOT NULL,
    line_type VARCHAR(20) NOT NULL,
    amount NUMERIC(14, 2) NOT NULL,
    display_order INT NOT NULL DEFAULT 0
);

CREATE INDEX idx_payroll_lines_record ON payroll_lines (payroll_record_id);

CREATE TABLE payroll_adjustments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payroll_run_id UUID NOT NULL REFERENCES payroll_runs (id),
    payroll_record_id UUID REFERENCES payroll_records (id),
    amount NUMERIC(14, 2) NOT NULL,
    reason TEXT NOT NULL,
    created_by UUID REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE salary_slips (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    payroll_record_id UUID NOT NULL UNIQUE REFERENCES payroll_records (id),
    slip_number VARCHAR(40) NOT NULL UNIQUE,
    storage_path VARCHAR(500),
    generated_at TIMESTAMPTZ,
    generated_by UUID REFERENCES users (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE SEQUENCE slip_no_seq START 1;
