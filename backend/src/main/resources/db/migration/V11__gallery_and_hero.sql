UPDATE website_sections
SET image_path = '/media/hero.svg'
WHERE page_key = 'HOME' AND section_key = 'HERO' AND image_path IS NULL;

INSERT INTO gallery_items (title, description, category, image_path, active, display_order)
SELECT title, description, category, image_path, active, display_order
FROM (VALUES
    ('Plant floor', 'A deployment on a manufacturing site near Pune.', 'Sites', '/media/gallery-plant.svg', true, 1),
    ('Site team', 'Supervisors and associates at the start of a shift.', 'People', '/media/gallery-team.svg', true, 2),
    ('Project site', 'Workforce supplied for a construction and engineering site.', 'Sites', '/media/gallery-site.svg', true, 3)
) AS seed(title, description, category, image_path, active, display_order)
WHERE NOT EXISTS (SELECT 1 FROM gallery_items);
