CREATE TABLE attendance_sheets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL REFERENCES companies (id),
    period_month INT NOT NULL,
    period_year INT NOT NULL,
    file_name VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (company_id, period_month, period_year)
);

CREATE INDEX idx_attendance_sheets_company ON attendance_sheets (company_id, period_year, period_month);

CREATE TABLE attendance_days (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attendance_sheet_id UUID NOT NULL REFERENCES attendance_sheets (id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES employees (id),
    work_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (attendance_sheet_id, employee_id, work_date)
);

CREATE INDEX idx_attendance_days_sheet ON attendance_days (attendance_sheet_id, work_date);
