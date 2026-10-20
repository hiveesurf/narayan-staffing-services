UPDATE website_sections
SET image_path = '/media/hero-workforce.jpg'
WHERE page_key = 'HOME' AND section_key = 'HERO';

UPDATE gallery_items SET image_path = '/media/hero-workforce.jpg' WHERE display_order = 1 AND image_path LIKE '%.svg';
UPDATE gallery_items SET image_path = '/media/team-desk.jpg' WHERE display_order = 2 AND image_path LIKE '%.svg';
UPDATE gallery_items SET image_path = '/media/site-work.jpg' WHERE display_order = 3 AND image_path LIKE '%.svg';
