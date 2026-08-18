USE techoptima;

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE application_dependencies;
TRUNCATE TABLE applications;
TRUNCATE TABLE transformation_budget;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- TRANSFORMATION BUDGET TEST DATA
-- Covers tiny, normal, large and very large budgets
-- ============================================================

INSERT INTO transformation_budget (budget_amount) VALUES
(0.00),
(1.00),
(100.00),
(1000.00),
(5000.00),
(10000.00),
(25000.00),
(50000.00),
(100000.00),
(250000.00),
(500000.00),
(1000000.00),
(5000000.00),
(10000000.00),
(50000000.00),
(100000000.00),
(9999999999.99);

-- ============================================================
-- APPLICATION TEST DATA
-- 100 applications
-- ============================================================

INSERT INTO applications
(application_id, application_name, modernization_cost, business_benefit, criticality, department)
VALUES
(1,   'Core Banking',             1000000.00, 100, 'CRITICAL', 'Finance'),
(2,   'Payment Gateway',           950000.00,  99, 'CRITICAL', 'Finance'),
(3,   'Fraud Detection',           850000.00,  98, 'CRITICAL', 'Security'),
(4,   'Customer Portal',           700000.00,  95, 'HIGH',     'Customer'),
(5,   'Mobile Banking',            800000.00,  97, 'CRITICAL', 'Customer'),
(6,   'Loan Processing',           600000.00,  90, 'HIGH',     'Finance'),
(7,   'Credit Scoring',            550000.00,  92, 'HIGH',     'Finance'),
(8,   'Risk Engine',               500000.00,  94, 'CRITICAL', 'Risk'),
(9,   'Identity Service',          450000.00,  96, 'CRITICAL', 'Security'),
(10,  'Notification Service',      100000.00,  70, 'MEDIUM',   'Communication'),

(11,  'Email Service',              50000.00,  60, 'LOW',      'Communication'),
(12,  'SMS Gateway',                75000.00,  65, 'MEDIUM',   'Communication'),
(13,  'Reporting Engine',          200000.00,  75, 'MEDIUM',   'Analytics'),
(14,  'Analytics Platform',        400000.00,  85, 'HIGH',     'Analytics'),
(15,  'Data Warehouse',            900000.00,  93, 'CRITICAL', 'Data'),
(16,  'Data Lake',                 950000.00,  91, 'HIGH',     'Data'),
(17,  'Audit Service',             150000.00,  80, 'HIGH',     'Security'),
(18,  'Authentication',            300000.00,  99, 'CRITICAL', 'Security'),
(19,  'Authorization',             250000.00,  97, 'CRITICAL', 'Security'),
(20,  'Session Manager',            80000.00,  88, 'HIGH',     'Security'),

(21,  'Inventory System',          300000.00,  72, 'MEDIUM',   'Operations'),
(22,  'Order Management',          350000.00,  78, 'HIGH',     'Operations'),
(23,  'Billing System',            450000.00,  89, 'HIGH',     'Finance'),
(24,  'CRM Platform',              500000.00,  82, 'HIGH',     'Sales'),
(25,  'HR System',                 200000.00,  55, 'MEDIUM',   'HR'),
(26,  'Payroll System',            350000.00,  88, 'HIGH',     'HR'),
(27,  'Recruitment System',        120000.00,  50, 'LOW',      'HR'),
(28,  'Attendance System',          90000.00,  45, 'LOW',      'HR'),
(29,  'Document Management',       180000.00,  62, 'MEDIUM',   'Operations'),
(30,  'Workflow Engine',           220000.00,  76, 'MEDIUM',   'Operations'),

(31,  'Legacy Mainframe',            0.00,   1, 'LOW',      'Legacy'),
(32,  'Legacy Payroll',              1.00,   2, 'LOW',      'Legacy'),
(33,  'Legacy CRM',                100.00,   5, 'LOW',      'Legacy'),
(34,  'Legacy Inventory',         1000.00,  10, 'LOW',      'Legacy'),
(35,  'Legacy Reporting',          5000.00,  15, 'LOW',      'Legacy'),
(36,  'Old Customer Database',    10000.00, 20, 'MEDIUM',   'Legacy'),
(37,  'Old Payment System',       25000.00, 25, 'HIGH',     'Legacy'),
(38,  'Old Authentication',       50000.00, 30, 'HIGH',     'Legacy'),
(39,  'Old API Gateway',          75000.00, 35, 'MEDIUM',   'Legacy'),
(40,  'Old Data Warehouse',      100000.00, 40, 'MEDIUM',   'Legacy'),

(41,  'Cloud Gateway',            125000.00, 68, 'HIGH',     'Cloud'),
(42,  'Cloud Storage',            175000.00, 73, 'MEDIUM',   'Cloud'),
(43,  'Cloud Database',           300000.00, 86, 'HIGH',     'Cloud'),
(44,  'Cloud Compute',            275000.00, 80, 'HIGH',     'Cloud'),
(45,  'Container Platform',       225000.00, 77, 'MEDIUM',   'Cloud'),
(46,  'Kubernetes Platform',      450000.00, 87, 'HIGH',     'Cloud'),
(47,  'Service Mesh',             200000.00, 74, 'MEDIUM',   'Cloud'),
(48,  'API Gateway',              325000.00, 91, 'CRITICAL', 'Cloud'),
(49,  'Message Queue',             90000.00, 69, 'MEDIUM',   'Cloud'),
(50,  'Cache Service',             60000.00, 64, 'LOW',      'Cloud'),

(51,  'Search Engine',            180000.00, 71, 'MEDIUM',   'Platform'),
(52,  'Recommendation Engine',    350000.00, 84, 'HIGH',     'AI'),
(53,  'Machine Learning Platform',500000.00, 90, 'HIGH',     'AI'),
(54,  'AI Fraud Model',            400000.00, 96, 'CRITICAL', 'AI'),
(55,  'AI Credit Model',           375000.00, 93, 'HIGH',     'AI'),
(56,  'AI Customer Model',         250000.00, 81, 'MEDIUM',   'AI'),
(57,  'Feature Store',             150000.00, 79, 'MEDIUM',   'AI'),
(58,  'Model Registry',             80000.00, 67, 'LOW',      'AI'),
(59,  'Data Pipeline',             275000.00, 88, 'HIGH',     'Data'),
(60,  'ETL Platform',              300000.00, 83, 'HIGH',     'Data'),

(61,  'Marketing Platform',        180000.00, 58, 'MEDIUM',   'Marketing'),
(62,  'Campaign Manager',          120000.00, 52, 'LOW',      'Marketing'),
(63,  'Customer Segmentation',     200000.00, 74, 'MEDIUM',   'Marketing'),
(64,  'Sales Dashboard',           100000.00, 61, 'MEDIUM',   'Sales'),
(65,  'Sales CRM',                 275000.00, 79, 'HIGH',     'Sales'),
(66,  'Partner Portal',            150000.00, 57, 'MEDIUM',   'Sales'),
(67,  'Vendor Management',        140000.00, 54, 'LOW',      'Operations'),
(68,  'Procurement System',        225000.00, 63, 'MEDIUM',   'Operations'),
(69,  'Supply Chain System',       450000.00, 85, 'HIGH',     'Operations'),
(70,  'Logistics Platform',        400000.00, 82, 'HIGH',     'Operations'),

(71,  'Security Dashboard',        200000.00, 89, 'HIGH',     'Security'),
(72,  'Threat Intelligence',       350000.00, 95, 'CRITICAL', 'Security'),
(73,  'SIEM Platform',             600000.00, 98, 'CRITICAL', 'Security'),
(74,  'Vulnerability Scanner',     250000.00, 87, 'HIGH',     'Security'),
(75,  'Incident Management',       175000.00, 90, 'HIGH',     'Security'),
(76,  'Access Management',         225000.00, 94, 'CRITICAL', 'Security'),
(77,  'Secrets Manager',            90000.00, 92, 'CRITICAL', 'Security'),
(78,  'Key Management',            125000.00, 93, 'CRITICAL', 'Security'),
(79,  'Security Analytics',        325000.00, 91, 'HIGH',     'Security'),
(80,  'Compliance Platform',       275000.00, 86, 'HIGH',     'Compliance'),

(81,  'API Analytics',              90000.00, 66, 'MEDIUM',   'Analytics'),
(82,  'Performance Monitor',       110000.00, 72, 'MEDIUM',   'Operations'),
(83,  'Application Monitor',       130000.00, 78, 'HIGH',     'Operations'),
(84,  'Log Management',             80000.00, 70, 'MEDIUM',   'Operations'),
(85,  'Backup System',             150000.00, 85, 'HIGH',     'Infrastructure'),
(86,  'Disaster Recovery',         500000.00, 99, 'CRITICAL', 'Infrastructure'),
(87,  'Infrastructure Manager',    250000.00, 73, 'MEDIUM',   'Infrastructure'),
(88,  'Network Manager',            200000.00, 76, 'MEDIUM',   'Infrastructure'),
(89,  'Load Balancer',               50000.00, 80, 'HIGH',     'Infrastructure'),
(90,  'DNS Service',                 25000.00, 60, 'MEDIUM',   'Infrastructure'),

(91,  'Zero Cost Test',                0.00,   0, 'LOW',      'Testing'),
(92,  'Minimum Benefit Test',           1.00,   1, 'LOW',      'Testing'),
(93,  'Boundary Benefit Test',         100.00, 100, 'CRITICAL', 'Testing'),
(94,  'Expensive Application',  9999999999.99, 50, 'HIGH',     'Testing'),
(95,  'Cheap High Benefit',              1.00, 100, 'CRITICAL', 'Testing'),
(96,  'Expensive Low Benefit',  9999999999.99, 1, 'LOW',      'Testing'),
(97,  'Balanced Application',       500000.00, 50, 'MEDIUM',   'Testing'),
(98,  'High Cost High Benefit',   9000000.00, 99, 'HIGH',     'Testing'),
(99,  'Low Cost Low Benefit',         10.00,  10, 'LOW',      'Testing'),
(100, 'Perfect Candidate',           100.00, 100, 'CRITICAL', 'Testing');

-- ============================================================
-- DEPENDENCY GRAPH
--
-- Deliberately ACYCLIC.
-- Every application can depend only on an application with
-- a smaller ID.
--
-- This gives the graph algorithms a large graph to process.
-- ============================================================

INSERT INTO application_dependencies
(application_id, dependency_application_id)
VALUES
(2,1),
(3,1),(3,2),
(4,1),(4,2),
(5,2),(5,4),
(6,1),(6,7),
(7,1),
(8,3),(8,7),
(9,1),(9,8),
(10,9),

(11,10),
(12,10),
(13,10),(13,14),
(14,15),(14,16),
(15,16),
(16,15),
(17,9),(17,18),
(18,1),
(19,18),
(20,18),(20,19),

(21,22),
(22,23),
(23,1),(23,2),
(24,4),(24,23),
(25,26),
(26,25),
(27,25),
(28,25),
(29,30),
(30,22),

(32,31),
(33,31),(33,32),
(34,31),
(35,34),
(36,33),
(37,2),(37,36),
(38,18),(38,37),
(39,38),
(40,36),(40,39),

(41,48),
(42,41),
(43,42),
(44,43),
(45,44),
(46,45),
(47,46),
(48,18),(48,41),
(49,48),
(50,43),

(51,43),
(52,51),(52,59),
(53,59),(53,60),
(54,53),(54,3),
(55,53),(55,7),
(56,53),
(57,59),
(58,53),
(59,16),
(60,16),(60,59),

(61,24),
(62,61),
(63,62),(63,14),
(64,65),
(65,24),
(66,65),
(67,68),
(68,69),
(69,70),
(70,22),

(71,73),
(72,73),
(73,79),
(74,73),
(75,73),(75,71),
(76,18),(76,19),
(77,76),
(78,77),
(79,72),(79,73),
(80,75),(80,79),

(81,14),(81,51),
(82,83),
(83,84),
(84,73),
(85,87),
(86,85),(86,87),
(87,88),
(88,89),
(89,90),
(90,88),

(92,91),
(93,91),(93,92),
(94,91),
(95,91),
(96,94),
(97,91),(97,95),
(98,97),(98,94),
(99,91),
(100,95),(100,93);

-- ============================================================
-- VERIFICATION
-- ============================================================

SELECT 'APPLICATION COUNT' AS test, COUNT(*) AS result
FROM applications;

SELECT 'DEPENDENCY COUNT' AS test, COUNT(*) AS result
FROM application_dependencies;

SELECT 'BUDGET COUNT' AS test, COUNT(*) AS result
FROM transformation_budget;

SELECT 'MIN COST' AS test, MIN(modernization_cost) AS result
FROM applications;

SELECT 'MAX COST' AS test, MAX(modernization_cost) AS result
FROM applications;

SELECT 'MIN BENEFIT' AS test, MIN(business_benefit) AS result
FROM applications;

SELECT 'MAX BENEFIT' AS test, MAX(business_benefit) AS result
FROM applications;

SELECT criticality, COUNT(*) AS count
FROM applications
GROUP BY criticality
ORDER BY criticality;

SELECT department, COUNT(*) AS count
FROM applications
GROUP BY department
ORDER BY department;

SELECT application_id, application_name, modernization_cost,
       business_benefit, criticality, department
FROM applications
ORDER BY application_id;

