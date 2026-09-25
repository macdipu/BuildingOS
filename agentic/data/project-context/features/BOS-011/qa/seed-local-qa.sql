-- BOS-011 QA fixture for the LOCAL dev stack only (building_db). Mirrors the integration-test seeding.
-- User df8f5016-... is phone 01711111112 in auth_db.
BEGIN;
INSERT INTO building_application (id, application_number, applicant_user_id, source, status, version, created_at, updated_at)
VALUES ('11111111-0000-0000-0000-000000000001', 'QA-BOS011-0001', 'df8f5016-32a1-486c-816a-6e21376c9432',
        'SELF_SERVICE', 'APPROVED', 0, now(), now());
INSERT INTO building (id, application_id, name, building_type, address, area, district, contact_phone, status, version,
                      created_at, updated_at)
VALUES ('22222222-0000-0000-0000-000000000001', '11111111-0000-0000-0000-000000000001', 'Green Valley Heights',
        'RESIDENTIAL', 'Road 12, Banani', 'Banani', 'Dhaka', '01711111112', 'ACTIVE', 0, now(), now());
INSERT INTO building_membership (id, building_id, user_id, role, status, created_at) VALUES
  (gen_random_uuid(), '22222222-0000-0000-0000-000000000001', 'df8f5016-32a1-486c-816a-6e21376c9432', 'BUILDING_ADMIN', 'ACTIVE', now()),
  (gen_random_uuid(), '22222222-0000-0000-0000-000000000001', 'df8f5016-32a1-486c-816a-6e21376c9432', 'OWNER', 'ACTIVE', now());
INSERT INTO building_floor (id, building_id, label, normalized_label, kind, display_order, version, created_at, updated_at) VALUES
  ('33333333-0000-0000-0000-000000000001', '22222222-0000-0000-0000-000000000001', 'Ground', 'GROUND', 'GROUND', 0, 0, now(), now()),
  ('33333333-0000-0000-0000-000000000002', '22222222-0000-0000-0000-000000000001', '4th Floor', '4TH FLOOR', 'REGULAR', 4, 0, now(), now());
INSERT INTO building_unit (id, building_id, floor_id, number, normalized_number, unit_type, area_sqft, bedrooms,
                           default_maintenance_rate, notes, version, created_at, updated_at) VALUES
  ('44444444-0000-0000-0000-000000000001', '22222222-0000-0000-0000-000000000001', '33333333-0000-0000-0000-000000000002', '4A', '4A', 'FLAT', 2150, 3, 6000, NULL, 0, now(), now()),
  ('44444444-0000-0000-0000-000000000002', '22222222-0000-0000-0000-000000000001', '33333333-0000-0000-0000-000000000002', '4B', '4B', 'FLAT', 1850, 3, 5500, NULL, 0, now(), now()),
  ('44444444-0000-0000-0000-000000000003', '22222222-0000-0000-0000-000000000001', '33333333-0000-0000-0000-000000000001', 'G1', 'G1', 'COMMERCIAL', 900, NULL, 4000, NULL, 0, now(), now());
INSERT INTO ownership_period (id, building_id, unit_id, owner_user_id, share, start_at, start_revision, created_by) VALUES
  (gen_random_uuid(), '22222222-0000-0000-0000-000000000001', '44444444-0000-0000-0000-000000000001',
   'df8f5016-32a1-486c-816a-6e21376c9432', 100, now(), 1, 'df8f5016-32a1-486c-816a-6e21376c9432');
COMMIT;
