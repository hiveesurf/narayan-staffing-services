ALTER TABLE companies
    ADD COLUMN cover_paths jsonb NOT NULL DEFAULT '[]'::jsonb;

UPDATE companies
SET address = 'Plot 14, MIDC Phase 2',
    pincode = '410501',
    cover_paths = '["/media/site-work.jpg"]'::jsonb
WHERE company_name = 'Deccan Auto Components'
  AND address IS NULL;

UPDATE companies
SET address = 'Survey 88, Talegaon Road',
    pincode = '411033',
    cover_paths = '["/media/hero-workforce.jpg"]'::jsonb
WHERE company_name = 'Western Warehousing'
  AND address IS NULL;

UPDATE companies
SET address = 'B-2, Pimpri Industrial Estate',
    pincode = '411018',
    cover_paths = '["/media/team-desk.jpg"]'::jsonb
WHERE company_name = 'Shakti Facilities'
  AND address IS NULL;
