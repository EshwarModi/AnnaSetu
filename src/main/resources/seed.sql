-- ===================================================================
-- AnnaSetu Seed Data (MySQL 8)
-- Idempotent insertions with INSERT IGNORE
-- Default Password for all accounts: Password@123
-- BCrypt Hash: $2a$10$dj5ibI/Wvm0CuNEwmogwFexCb/kc4uNxeqZ7q9M2/8dC0JZCD0J1i
-- ===================================================================

-- 1. Users (1 Admin, 8 Organizations, 5 Donors)
INSERT IGNORE INTO users (id, email, password_hash, role, enabled, created_at) VALUES
-- Admin
(1, 'admin@annasetu.org', '$2a$10$dj5ibI/Wvm0CuNEwmogwFexCb/kc4uNxeqZ7q9M2/8dC0JZCD0J1i', 'ADMIN', true, NOW()),

-- Organizations (Pune: 2, 3, 4 | Mumbai: 5, 6, 7 | Bengaluru: 8, 9)
(2, 'ashakiran@annasetu.org', '$2a$10$dj5ibI/Wvm0CuNEwmogwFexCb/kc4uNxeqZ7q9M2/8dC0JZCD0J1i', 'ORGANIZATION', true, NOW()),
(3, 'mauli@annasetu.org', '$2a$10$dj5ibI/Wvm0CuNEwmogwFexCb/kc4uNxeqZ7q9M2/8dC0JZCD0J1i', 'ORGANIZATION', true, NOW()),
(4, 'snehalaya@annasetu.org', '$2a$10$dj5ibI/Wvm0CuNEwmogwFexCb/kc4uNxeqZ7q9M2/8dC0JZCD0J1i', 'ORGANIZATION', true, NOW()),
(5, 'vatsalya@annasetu.org', '$2a$10$dj5ibI/Wvm0CuNEwmogwFexCb/kc4uNxeqZ7q9M2/8dC0JZCD0J1i', 'ORGANIZATION', true, NOW()),
(6, 'karuna@annasetu.org', '$2a$10$dj5ibI/Wvm0CuNEwmogwFexCb/kc4uNxeqZ7q9M2/8dC0JZCD0J1i', 'ORGANIZATION', true, NOW()),
(7, 'navjeevan@annasetu.org', '$2a$10$dj5ibI/Wvm0CuNEwmogwFexCb/kc4uNxeqZ7q9M2/8dC0JZCD0J1i', 'ORGANIZATION', true, NOW()),
(8, 'nammamane@annasetu.org', '$2a$10$dj5ibI/Wvm0CuNEwmogwFexCb/kc4uNxeqZ7q9M2/8dC0JZCD0J1i', 'ORGANIZATION', true, NOW()),
(9, 'shantinivas@annasetu.org', '$2a$10$dj5ibI/Wvm0CuNEwmogwFexCb/kc4uNxeqZ7q9M2/8dC0JZCD0J1i', 'ORGANIZATION', true, NOW()),

-- Donors (10 to 14)
(10, 'rahul.sharma@example.com', '$2a$10$dj5ibI/Wvm0CuNEwmogwFexCb/kc4uNxeqZ7q9M2/8dC0JZCD0J1i', 'DONOR', true, NOW()),
(11, 'priya.verma@example.com', '$2a$10$dj5ibI/Wvm0CuNEwmogwFexCb/kc4uNxeqZ7q9M2/8dC0JZCD0J1i', 'DONOR', true, NOW()),
(12, 'amit.patel@example.com', '$2a$10$dj5ibI/Wvm0CuNEwmogwFexCb/kc4uNxeqZ7q9M2/8dC0JZCD0J1i', 'DONOR', true, NOW()),
(13, 'sneha.nair@example.com', '$2a$10$dj5ibI/Wvm0CuNEwmogwFexCb/kc4uNxeqZ7q9M2/8dC0JZCD0J1i', 'DONOR', true, NOW()),
(14, 'vikram.reddy@example.com', '$2a$10$dj5ibI/Wvm0CuNEwmogwFexCb/kc4uNxeqZ7q9M2/8dC0JZCD0J1i', 'DONOR', true, NOW());

-- 2. Donor Profiles (5 Donors)
INSERT IGNORE INTO donor_profiles (id, user_id, full_name, phone, city, created_at) VALUES
(1, 10, 'Rahul Sharma', '+91 98111 22233', 'Mumbai', NOW()),
(2, 11, 'Priya Verma', '+91 98111 33344', 'Pune', NOW()),
(3, 12, 'Amit Patel', '+91 98111 44455', 'Bengaluru', NOW()),
(4, 13, 'Sneha Nair', '+91 98111 55566', 'Pune', NOW()),
(5, 14, 'Vikram Reddy', '+91 98111 66677', 'Bengaluru', NOW());

-- 3. Organization Profiles (8 Approved across Pune, Mumbai, Bengaluru)
INSERT IGNORE INTO organization_profiles (
    id, user_id, name, type, address, city, area, pincode, contact_person, phone, people_served, description, food_preference, approval_status, created_at
) VALUES
-- Pune Organizations
(1, 2, 'Asha Kiran Children Home', 'ORPHANAGE', 'Plot 45, FC Road, Deccan Gymkhana', 'Pune', 'Deccan Gymkhana', '411004', 'Sunita Deshmukh', '+91 98220 11223', 65, 'Home providing education, nutrition, and holistic care for 65 orphaned and destitute children.', 'VEG', 'APPROVED', NOW()),
(2, 3, 'Mauli Senior Citizen Ashram', 'OLD_AGE_HOME', 'Survey No 12, Paud Road, Kothrud', 'Pune', 'Kothrud', '411038', 'Ramesh Kulkarni', '+91 98220 22334', 45, 'Loving elder care home caring for 45 senior citizens with round-the-clock nursing and sattvic meals.', 'VEG', 'APPROVED', NOW()),
(3, 4, 'Snehalaya Community Shelter', 'SHELTER', 'Near Magarpatta City, Hadapsar', 'Pune', 'Hadapsar', '411028', 'Pooja Patil', '+91 98220 33445', 80, 'Community rehabilitation shelter assisting daily wage laborers, destitute mothers, and abandoned youth.', 'BOTH', 'APPROVED', NOW()),

-- Mumbai Organizations
(4, 5, 'Vatsalya Child Welfare Centre', 'ORPHANAGE', '12 B, Hindu Colony, Dadar East', 'Mumbai', 'Dadar East', '400014', 'Anil Shinde', '+91 98200 44556', 50, 'Registered child welfare institution nurturing 50 school-going children from underserved communities.', 'BOTH', 'APPROVED', NOW()),
(5, 6, 'Karuna Elders Care Retreat', 'OLD_AGE_HOME', 'Opposite Gilbert Hill, Andheri West', 'Mumbai', 'Andheri West', '400058', 'Meena Iyer', '+91 98200 55667', 40, 'Safe haven for elderly citizens needing dignified assisted living, medical attention, and nutritious food.', 'VEG', 'APPROVED', NOW()),
(6, 7, 'Navjeevan Night Shelter', 'SHELTER', 'Station Road, Chembur East', 'Mumbai', 'Chembur', '400071', 'Sanjay Joshi', '+91 98200 66778', 95, 'Urban transit shelter offering warm beds, evening meals, and skill training for displaced individuals.', 'BOTH', 'APPROVED', NOW()),

-- Bengaluru Organizations
(7, 8, 'Namma Mane Child Haven', 'ORPHANAGE', '100 Feet Road, HAL 2nd Stage, Indiranagar', 'Bengaluru', 'Indiranagar', '560038', 'Geetha Rao', '+91 98450 77889', 70, 'Nurturing shelter providing shelter, education, and balanced meals to 70 underprivileged boys and girls.', 'BOTH', 'APPROVED', NOW()),
(8, 9, 'Shanti Nivas Elders Care', 'OLD_AGE_HOME', '4th Block, 9th Main, Jayanagar', 'Bengaluru', 'Jayanagar', '560041', 'Venkatesh Murthy', '+91 98450 88990', 55, 'Comfortable old age home committed to healthcare, companionship, and pure vegetarian dining for seniors.', 'VEG', 'APPROVED', NOW());

-- 4. 30 Days of Meal Slots for All 8 Organizations (720 slots)
-- Breakfast: capacity 1 | Lunch: capacity 2 | Dinner: capacity 1
INSERT IGNORE INTO slot_availabilities (organization_id, slot_date, meal_slot, capacity, booked_count, created_at)
WITH RECURSIVE day_generator AS (
    SELECT 0 AS d_offset
    UNION ALL
    SELECT d_offset + 1 FROM day_generator WHERE d_offset < 29
),
meal_types AS (
    SELECT 'BREAKFAST' AS meal, 1 AS cap
    UNION ALL
    SELECT 'LUNCH' AS meal, 2 AS cap
    UNION ALL
    SELECT 'DINNER' AS meal, 1 AS cap
)
SELECT
    org.id AS organization_id,
    DATE_ADD(CURDATE(), INTERVAL dg.d_offset DAY) AS slot_date,
    mt.meal AS meal_slot,
    mt.cap AS capacity,
    0 AS booked_count,
    NOW() AS created_at
FROM organization_profiles org
CROSS JOIN day_generator dg
CROSS JOIN meal_types mt;

-- 5. Pre-book a Few Slots and Update Booked Counts
-- Booking 1: Org 1 (Asha Kiran), Tomorrow Lunch (Capacity 2 -> 1 booked) by Priya Verma (Donor 2)
UPDATE slot_availabilities
SET booked_count = 1
WHERE organization_id = 1
  AND slot_date = DATE_ADD(CURDATE(), INTERVAL 1 DAY)
  AND meal_slot = 'LUNCH';

INSERT IGNORE INTO bookings (slot_id, donor_id, occasion, people_count, food_description, status, created_at)
SELECT
    s.id,
    2,
    'BIRTHDAY',
    65,
    'Grand birthday feast for children: Paneer butter masala, jeera rice, butter naan, gulab jamun, and ice cream.',
    'CONFIRMED',
    NOW()
FROM slot_availabilities s
WHERE s.organization_id = 1
  AND s.slot_date = DATE_ADD(CURDATE(), INTERVAL 1 DAY)
  AND s.meal_slot = 'LUNCH'
LIMIT 1;

-- Booking 2: Org 2 (Mauli Senior Home), Day +2 Dinner (Capacity 1 -> fully booked) by Sneha Nair (Donor 4)
UPDATE slot_availabilities
SET booked_count = 1
WHERE organization_id = 2
  AND slot_date = DATE_ADD(CURDATE(), INTERVAL 2 DAY)
  AND meal_slot = 'DINNER';

INSERT IGNORE INTO bookings (slot_id, donor_id, occasion, people_count, food_description, status, created_at)
SELECT
    s.id,
    4,
    'MEMORIAL',
    45,
    'Sattvic dinner in loving remembrance of grandparents: Khichdi, kadhi, dry potato sabzi, and moong dal halwa.',
    'CONFIRMED',
    NOW()
FROM slot_availabilities s
WHERE s.organization_id = 2
  AND s.slot_date = DATE_ADD(CURDATE(), INTERVAL 2 DAY)
  AND s.meal_slot = 'DINNER'
LIMIT 1;

-- Booking 3: Org 4 (Vatsalya), Day +3 Lunch (Capacity 2 -> 1 booked) by Rahul Sharma (Donor 1)
UPDATE slot_availabilities
SET booked_count = 1
WHERE organization_id = 4
  AND slot_date = DATE_ADD(CURDATE(), INTERVAL 3 DAY)
  AND meal_slot = 'LUNCH';

INSERT IGNORE INTO bookings (slot_id, donor_id, occasion, people_count, food_description, status, created_at)
SELECT
    s.id,
    1,
    'ANNIVERSARY',
    50,
    '10th Wedding anniversary meal: Veg pulao, chana masala, puri, raita, and seasonal fruit custard.',
    'CONFIRMED',
    NOW()
FROM slot_availabilities s
WHERE s.organization_id = 4
  AND s.slot_date = DATE_ADD(CURDATE(), INTERVAL 3 DAY)
  AND s.meal_slot = 'LUNCH'
LIMIT 1;

-- Booking 4: Org 7 (Namma Mane), Day +4 Breakfast (Capacity 1 -> fully booked) by Amit Patel (Donor 3)
UPDATE slot_availabilities
SET booked_count = 1
WHERE organization_id = 7
  AND slot_date = DATE_ADD(CURDATE(), INTERVAL 4 DAY)
  AND meal_slot = 'BREAKFAST';

INSERT IGNORE INTO bookings (slot_id, donor_id, occasion, people_count, food_description, status, created_at)
SELECT
    s.id,
    3,
    'FESTIVAL',
    70,
    'Festival celebration breakfast: Hot ghee idli, crispy medu vada, vegetable sambar, coconut chutney, and kesari bath.',
    'CONFIRMED',
    NOW()
FROM slot_availabilities s
WHERE s.organization_id = 7
  AND s.slot_date = DATE_ADD(CURDATE(), INTERVAL 4 DAY)
  AND s.meal_slot = 'BREAKFAST'
LIMIT 1;

-- Booking 5: Org 8 (Shanti Nivas), Day +5 Lunch (Capacity 2 -> fully booked with 2 separate donors)
UPDATE slot_availabilities
SET booked_count = 2
WHERE organization_id = 8
  AND slot_date = DATE_ADD(CURDATE(), INTERVAL 5 DAY)
  AND meal_slot = 'LUNCH';

INSERT IGNORE INTO bookings (slot_id, donor_id, occasion, people_count, food_description, status, created_at)
SELECT
    s.id,
    5,
    'BIRTHDAY',
    55,
    'South Indian festive lunch with payasam to celebrate daughter birthday.',
    'CONFIRMED',
    NOW()
FROM slot_availabilities s
WHERE s.organization_id = 8
  AND s.slot_date = DATE_ADD(CURDATE(), INTERVAL 5 DAY)
  AND s.meal_slot = 'LUNCH'
LIMIT 1;

INSERT IGNORE INTO bookings (slot_id, donor_id, occasion, people_count, food_description, status, created_at)
SELECT
    s.id,
    3,
    'OTHER',
    55,
    'Gratitude sponsorship for elders: fresh seasonal fruits and dry sweets dessert addition.',
    'CONFIRMED',
    NOW()
FROM slot_availabilities s
WHERE s.organization_id = 8
  AND s.slot_date = DATE_ADD(CURDATE(), INTERVAL 5 DAY)
  AND s.meal_slot = 'LUNCH'
LIMIT 1;
