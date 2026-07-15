-- Realistic demo dataset for the Hito 46 docker-compose demo: 28 vehicles (18 trucks, 5 light
-- vehicles, 5 heavy machinery), 10 clients, 20 suppliers, 18 workers (15 drivers, 3 technicians),
-- 10 login users, driver/vehicle assignment history, ~100 jobs, ~50 maintenance records +
-- workshop schedule entries, ~30 client invoices and ~70 supplier invoices — all spread across
-- January-July 2026 so the dashboard/profitability views open with real-looking history instead
-- of an empty state.
--
-- All demo users share the password "Demo1234!" (BCrypt cost 12, precomputed and verified against
-- Spring Security's BCryptPasswordEncoder before being embedded here).

-- =============================================================================
-- USERS
-- =============================================================================
INSERT INTO users (email, password_hash, app_role, enabled) VALUES
    ('admin@fleetmgm.demo',          '$2a$12$L7EMSGxPH4UWAV/3rBcuIOQCTj26HzSgT0v.AOjQnqASQtDNuPpn6', 'ADMIN', true),
    ('gerente@fleetmgm.demo',        '$2a$12$L7EMSGxPH4UWAV/3rBcuIOQCTj26HzSgT0v.AOjQnqASQtDNuPpn6', 'MANAGER', true),
    ('administrativo1@fleetmgm.demo','$2a$12$L7EMSGxPH4UWAV/3rBcuIOQCTj26HzSgT0v.AOjQnqASQtDNuPpn6', 'ADMINISTRATIVE', true),
    ('administrativo2@fleetmgm.demo','$2a$12$L7EMSGxPH4UWAV/3rBcuIOQCTj26HzSgT0v.AOjQnqASQtDNuPpn6', 'ADMINISTRATIVE', true),
    ('taller1@fleetmgm.demo',        '$2a$12$L7EMSGxPH4UWAV/3rBcuIOQCTj26HzSgT0v.AOjQnqASQtDNuPpn6', 'WORKSHOP_STAFF', true),
    ('taller2@fleetmgm.demo',        '$2a$12$L7EMSGxPH4UWAV/3rBcuIOQCTj26HzSgT0v.AOjQnqASQtDNuPpn6', 'WORKSHOP_STAFF', true),
    ('taller3@fleetmgm.demo',        '$2a$12$L7EMSGxPH4UWAV/3rBcuIOQCTj26HzSgT0v.AOjQnqASQtDNuPpn6', 'WORKSHOP_STAFF', true),
    ('conductor1@fleetmgm.demo',     '$2a$12$L7EMSGxPH4UWAV/3rBcuIOQCTj26HzSgT0v.AOjQnqASQtDNuPpn6', 'DRIVER', true),
    ('conductor2@fleetmgm.demo',     '$2a$12$L7EMSGxPH4UWAV/3rBcuIOQCTj26HzSgT0v.AOjQnqASQtDNuPpn6', 'DRIVER', true),
    ('conductor3@fleetmgm.demo',     '$2a$12$L7EMSGxPH4UWAV/3rBcuIOQCTj26HzSgT0v.AOjQnqASQtDNuPpn6', 'DRIVER', true);

-- =============================================================================
-- WORKERS — 15 DRIVER + 3 TECHNICIAN (WorkerRole has no WORKSHOP_STAFF value; TECHNICIAN is the
-- worker-side equivalent of the AppRole.WORKSHOP_STAFF login role used above)
-- =============================================================================
INSERT INTO workers (user_id, first_name, last_name, worker_role, phone, national_id, license_type, license_expiry) VALUES
    ((SELECT id FROM users WHERE email = 'conductor1@fleetmgm.demo'), 'Juan',        'Torres Delgado',   'DRIVER', '+34 611 234 567', '12345678A', 'C+E', '2028-05-15'),
    ((SELECT id FROM users WHERE email = 'conductor2@fleetmgm.demo'), 'Miguel Ángel','Ruiz Soto',        'DRIVER', '+34 622 345 678', '23456789B', 'C+E', '2027-11-20'),
    ((SELECT id FROM users WHERE email = 'conductor3@fleetmgm.demo'), 'Antonio',     'Navarro Gil',      'DRIVER', '+34 633 456 789', '34567890C', 'C',   '2029-02-10'),
    (NULL, 'Francisco Javier', 'Ortega Campos',   'DRIVER', '+34 644 567 890', '45678901D', 'C+E', '2027-07-01'),
    (NULL, 'José Luis',        'Molina Prat',     'DRIVER', '+34 655 678 901', '56789012E', 'D',   '2028-09-30'),
    (NULL, 'Carlos',           'Herrera Muñoz',   'DRIVER', '+34 666 789 012', '67890123F', 'C+E', '2027-03-22'),
    (NULL, 'Manuel',           'Ramírez Vidal',   'DRIVER', '+34 677 890 123', '78901234G', 'C',   '2028-12-05'),
    (NULL, 'David',            'Castillo Reyes',  'DRIVER', '+34 688 901 234', '89012345H', 'C+E', '2027-06-18'),
    (NULL, 'Alejandro',        'Serrano Bravo',   'DRIVER', '+34 699 012 345', '90123456J', 'C',   '2029-01-14'),
    (NULL, 'Pablo',            'Iglesias Vega',   'DRIVER', '+34 610 123 456', '01234567K', 'C+E', '2028-04-09'),
    (NULL, 'Rubén',            'Domínguez Cano',  'DRIVER', '+34 621 234 567', '11223344L', 'C',   '2027-10-27'),
    (NULL, 'Sergio',           'Blanco Mora',     'DRIVER', '+34 632 345 678', '22334455M', 'C+E', '2028-08-16'),
    (NULL, 'Adrián',           'Cabrera Ruiz',    'DRIVER', '+34 643 456 789', '33445566N', 'D',   '2027-05-03'),
    (NULL, 'Iván',             'Santos Fuentes',  'DRIVER', '+34 654 567 890', '44556677P', 'C',   '2029-03-25'),
    (NULL, 'Óscar',            'Nieto Campos',    'DRIVER', '+34 665 678 901', '55667788Q', 'C+E', '2027-12-11'),
    ((SELECT id FROM users WHERE email = 'taller1@fleetmgm.demo'), 'Laura',  'Jiménez Peña',   'TECHNICIAN', '+34 676 789 012', '66778899R', NULL, NULL),
    ((SELECT id FROM users WHERE email = 'taller2@fleetmgm.demo'), 'Marta',  'Fernández Casas','TECHNICIAN', '+34 687 890 123', '77889900S', NULL, NULL),
    ((SELECT id FROM users WHERE email = 'taller3@fleetmgm.demo'), 'Roberto','Álvarez Duque',  'TECHNICIAN', '+34 698 901 234', '88990011T', NULL, NULL);

-- =============================================================================
-- VEHICLES — 18 HEAVY_VEHICLE + 5 LIGHT_VEHICLE + 5 HEAVY_MACHINERY
-- =============================================================================
INSERT INTO vehicles (vehicle_category, usage_measure, heavy_subtype, license_plate, make, model, year, color, status, current_km, current_hours, acquisition_type, acquisition_date, purchase_price, amortization_years, monthly_fee, contract_end_date) VALUES
    ('HEAVY_VEHICLE', 'KILOMETERS', NULL, '4521BXK', 'Volvo',            'FH16',              2019, 'Blanco', 'ACTIVE',      320000, NULL, 'PURCHASED', '2019-02-10', 145000.00, 8, NULL, NULL),
    ('HEAVY_VEHICLE', 'KILOMETERS', NULL, '7832CDN', 'Scania',           'R450',              2020, 'Blanco', 'ACTIVE',      275000, NULL, 'PURCHASED', '2020-04-18', 138000.00, 8, NULL, NULL),
    ('HEAVY_VEHICLE', 'KILOMETERS', NULL, '2109BWL', 'Mercedes-Benz',    'Actros',            2018, 'Rojo',   'MAINTENANCE', 410000, NULL, 'PURCHASED', '2018-01-22', 132000.00, 8, NULL, NULL),
    ('HEAVY_VEHICLE', 'KILOMETERS', NULL, '8845CFR', 'MAN',              'TGX',               2021, 'Blanco', 'ACTIVE',      190000, NULL, 'PURCHASED', '2021-03-05', 151000.00, 8, NULL, NULL),
    ('HEAVY_VEHICLE', 'KILOMETERS', NULL, '3367BZP', 'Iveco',            'Stralis',           2019, 'Azul',   'ACTIVE',      350000, NULL, 'PURCHASED', '2019-06-30', 128000.00, 8, NULL, NULL),
    ('HEAVY_VEHICLE', 'KILOMETERS', NULL, '1256BVT', 'DAF',              'XF105',             2018, 'Blanco', 'ACTIVE',      430000, NULL, 'PURCHASED', '2018-05-14', 125000.00, 8, NULL, NULL),
    ('HEAVY_VEHICLE', 'KILOMETERS', NULL, '9901CGX', 'Renault Trucks',   'T480',              2022, 'Gris',   'ACTIVE',      120000, NULL, 'PURCHASED', '2022-01-11', 158000.00, 8, NULL, NULL),
    ('HEAVY_VEHICLE', 'KILOMETERS', NULL, '5544CBK', 'Volvo',            'FH16',              2020, 'Blanco', 'ACTIVE',      260000, NULL, 'PURCHASED', '2020-07-25', 146500.00, 8, NULL, NULL),
    ('HEAVY_VEHICLE', 'KILOMETERS', NULL, '6678CEM', 'Scania',           'R450',              2021, 'Rojo',   'ACTIVE',      175000, NULL, 'PURCHASED', '2021-02-19', 139500.00, 8, NULL, NULL),
    ('HEAVY_VEHICLE', 'KILOMETERS', NULL, '4432BYQ', 'Mercedes-Benz',    'Actros',            2019, 'Blanco', 'ACTIVE',      340000, NULL, 'PURCHASED', '2019-09-08', 133500.00, 8, NULL, NULL),
    ('HEAVY_VEHICLE', 'KILOMETERS', NULL, '2287CHN', 'MAN',              'TGX',               2022, 'Azul',   'ACTIVE',       98000, NULL, 'PURCHASED', '2022-05-30', 152500.00, 8, NULL, NULL),
    ('HEAVY_VEHICLE', 'KILOMETERS', NULL, '7712CDW', 'Iveco',            'Stralis',           2020, 'Blanco', 'ACTIVE',      245000, NULL, 'PURCHASED', '2020-10-12', 129500.00, 8, NULL, NULL),
    ('HEAVY_VEHICLE', 'KILOMETERS', NULL, '5501CJT', 'DAF',              'XF105',             2023, 'Gris',   'ACTIVE',       82000, NULL, 'PURCHASED', '2023-01-20', 162000.00, 8, NULL, NULL),
    ('HEAVY_VEHICLE', 'KILOMETERS', NULL, '3345BXR', 'Renault Trucks',   'T480',              2018, 'Blanco', 'MAINTENANCE', 455000, NULL, 'PURCHASED', '2018-03-02', 124000.00, 8, NULL, NULL),
    ('HEAVY_VEHICLE', 'KILOMETERS', NULL, '6689CFZ', 'Volvo',            'FH16',              2021, 'Rojo',   'ACTIVE',      210000, NULL, 'PURCHASED', '2021-08-16', 148500.00, 8, NULL, NULL),
    ('HEAVY_VEHICLE', 'KILOMETERS', NULL, '1123BWY', 'Scania',           'R450',              2019, 'Blanco', 'ACTIVE',      365000, NULL, 'PURCHASED', '2019-11-27', 137000.00, 8, NULL, NULL),
    ('HEAVY_VEHICLE', 'KILOMETERS', NULL, '8890CKL', 'Mercedes-Benz',    'Actros',            2023, 'Azul',   'ACTIVE',       75000, NULL, 'PURCHASED', '2023-04-09', 163500.00, 8, NULL, NULL),
    ('HEAVY_VEHICLE', 'KILOMETERS', NULL, '4467CAM', 'MAN',              'TGX',               2020, 'Blanco', 'ACTIVE',      268000, NULL, 'PURCHASED', '2020-12-01', 150000.00, 8, NULL, NULL),
    ('LIGHT_VEHICLE', 'KILOMETERS', NULL, '5523CDQ', 'Toyota',           'Hilux',             2021, 'Blanco', 'ACTIVE', 95000, NULL, 'PURCHASED', '2021-05-11', 32000.00, 6, NULL, NULL),
    ('LIGHT_VEHICLE', 'KILOMETERS', NULL, '3312BZK', 'Ford',             'Transit',           2020, 'Blanco', 'ACTIVE', 130000, NULL, 'LEASING', '2020-06-01', NULL, NULL, 320.00, '2027-06-30'),
    ('LIGHT_VEHICLE', 'KILOMETERS', NULL, '7789CGT', 'Renault',          'Kangoo',            2022, 'Blanco', 'ACTIVE', 62000, NULL, 'PURCHASED', '2022-02-14', 21500.00, 6, NULL, NULL),
    ('LIGHT_VEHICLE', 'KILOMETERS', NULL, '2245BXW', 'Peugeot',          'Partner',           2019, 'Gris',   'ACTIVE', 145000, NULL, 'LEASING', '2019-09-01', NULL, NULL, 260.00, '2026-12-31'),
    ('LIGHT_VEHICLE', 'KILOMETERS', NULL, '6690CEN', 'Mercedes-Benz',    'Vito',              2021, 'Negro',  'ACTIVE', 88000, NULL, 'PURCHASED', '2021-07-19', 29500.00, 6, NULL, NULL),
    ('HEAVY_MACHINERY', 'HOURS', 'Excavadora',           '4501CXK', 'Caterpillar', '320',    2020, 'Amarillo', 'ACTIVE', NULL, 3200, 'PURCHASED', '2020-03-15', 95000.00, 10, NULL, NULL),
    ('HEAVY_MACHINERY', 'HOURS', 'Excavadora',           NULL,      'Komatsu',     'PC210',  2019, 'Amarillo', 'ACTIVE', NULL, 4100, 'PURCHASED', '2019-08-22', 89000.00, 10, NULL, NULL),
    ('HEAVY_MACHINERY', 'HOURS', 'Retroexcavadora',      NULL,      'JCB',         '3CX',    2021, 'Amarillo', 'ACTIVE', NULL, 2200, 'PURCHASED', '2021-01-30', 78000.00, 10, NULL, NULL),
    ('HEAVY_MACHINERY', 'HOURS', 'Carretilla elevadora', NULL,      'Toyota',      '8FGU25', 2022, 'Naranja',  'ACTIVE', NULL, 1500, 'PURCHASED', '2022-06-05', 24000.00, 10, NULL, NULL),
    ('HEAVY_MACHINERY', 'HOURS', 'Carretilla elevadora', NULL,      'Linde',       'H25',    2020, 'Naranja',  'ACTIVE', NULL, 2800, 'PURCHASED', '2020-09-17', 26500.00, 10, NULL, NULL);

-- =============================================================================
-- DRIVER <-> VEHICLE ASSIGNMENTS
-- 15 active assignments (13 trucks + 2 light vehicles); 8 vehicles left currently unassigned;
-- 3 of the 15 drivers also carry a closed historical assignment on a different truck, to show
-- assignment history depth rather than a flat 1:1 mapping.
-- =============================================================================
INSERT INTO driver_vehicle_assignments (driver_id, vehicle_id, start_date, end_date, assigned_by_user_id, notes) VALUES
    ((SELECT id FROM workers WHERE national_id = '12345678A'), (SELECT id FROM vehicles WHERE license_plate = '4521BXK'), '2025-12-01', NULL, (SELECT id FROM users WHERE email = 'admin@fleetmgm.demo'), NULL),
    ((SELECT id FROM workers WHERE national_id = '23456789B'), (SELECT id FROM vehicles WHERE license_plate = '7832CDN'), '2025-09-15', NULL, (SELECT id FROM users WHERE email = 'admin@fleetmgm.demo'), NULL),
    ((SELECT id FROM workers WHERE national_id = '34567890C'), (SELECT id FROM vehicles WHERE license_plate = '2109BWL'), '2025-10-01', NULL, (SELECT id FROM users WHERE email = 'admin@fleetmgm.demo'), NULL),
    ((SELECT id FROM workers WHERE national_id = '45678901D'), (SELECT id FROM vehicles WHERE license_plate = '8845CFR'), '2025-11-05', NULL, (SELECT id FROM users WHERE email = 'admin@fleetmgm.demo'), NULL),
    ((SELECT id FROM workers WHERE national_id = '56789012E'), (SELECT id FROM vehicles WHERE license_plate = '3367BZP'), '2025-10-01', NULL, (SELECT id FROM users WHERE email = 'admin@fleetmgm.demo'), NULL),
    ((SELECT id FROM workers WHERE national_id = '67890123F'), (SELECT id FROM vehicles WHERE license_plate = '1256BVT'), '2025-12-10', NULL, (SELECT id FROM users WHERE email = 'admin@fleetmgm.demo'), NULL),
    ((SELECT id FROM workers WHERE national_id = '78901234G'), (SELECT id FROM vehicles WHERE license_plate = '9901CGX'), '2025-09-01', NULL, (SELECT id FROM users WHERE email = 'admin@fleetmgm.demo'), NULL),
    ((SELECT id FROM workers WHERE national_id = '89012345H'), (SELECT id FROM vehicles WHERE license_plate = '5544CBK'), '2025-11-20', NULL, (SELECT id FROM users WHERE email = 'admin@fleetmgm.demo'), NULL),
    ((SELECT id FROM workers WHERE national_id = '90123456J'), (SELECT id FROM vehicles WHERE license_plate = '6678CEM'), '2025-08-16', NULL, (SELECT id FROM users WHERE email = 'admin@fleetmgm.demo'), NULL),
    ((SELECT id FROM workers WHERE national_id = '01234567K'), (SELECT id FROM vehicles WHERE license_plate = '4432BYQ'), '2025-12-15', NULL, (SELECT id FROM users WHERE email = 'admin@fleetmgm.demo'), NULL),
    ((SELECT id FROM workers WHERE national_id = '11223344L'), (SELECT id FROM vehicles WHERE license_plate = '2287CHN'), '2026-01-05', NULL, (SELECT id FROM users WHERE email = 'admin@fleetmgm.demo'), NULL),
    ((SELECT id FROM workers WHERE national_id = '22334455M'), (SELECT id FROM vehicles WHERE license_plate = '7712CDW'), '2025-09-22', NULL, (SELECT id FROM users WHERE email = 'admin@fleetmgm.demo'), NULL),
    ((SELECT id FROM workers WHERE national_id = '33445566N'), (SELECT id FROM vehicles WHERE license_plate = '5501CJT'), '2026-01-20', NULL, (SELECT id FROM users WHERE email = 'admin@fleetmgm.demo'), NULL),
    ((SELECT id FROM workers WHERE national_id = '44556677P'), (SELECT id FROM vehicles WHERE license_plate = '5523CDQ'), '2025-10-15', NULL, (SELECT id FROM users WHERE email = 'admin@fleetmgm.demo'), NULL),
    ((SELECT id FROM workers WHERE national_id = '55667788Q'), (SELECT id FROM vehicles WHERE license_plate = '3312BZK'), '2025-11-01', NULL, (SELECT id FROM users WHERE email = 'admin@fleetmgm.demo'), NULL),
    -- Historical (closed) assignments on the 3 currently-unassigned trucks not covered above
    ((SELECT id FROM workers WHERE national_id = '12345678A'), (SELECT id FROM vehicles WHERE license_plate = '1123BWY'), '2025-06-01', '2025-11-30', (SELECT id FROM users WHERE email = 'admin@fleetmgm.demo'), 'Reasignado a otro vehículo'),
    ((SELECT id FROM workers WHERE national_id = '56789012E'), (SELECT id FROM vehicles WHERE license_plate = '8890CKL'), '2025-03-01', '2025-09-30', (SELECT id FROM users WHERE email = 'admin@fleetmgm.demo'), 'Reasignado a otro vehículo'),
    ((SELECT id FROM workers WHERE national_id = '90123456J'), (SELECT id FROM vehicles WHERE license_plate = '4467CAM'), '2025-01-01', '2025-08-15', (SELECT id FROM users WHERE email = 'admin@fleetmgm.demo'), 'Reasignado a otro vehículo');

-- =============================================================================
-- CLIENTS
-- =============================================================================
INSERT INTO clients (name, tax_id, email, phone, address) VALUES
    ('Transportes Ibérica S.L.',      'B12345678', 'contacto@transportesiberica.example', '+34 910 111 222', 'Polígono Industrial Norte, Nave 12, Madrid'),
    ('Construcciones Marín S.A.',     'A23456789', 'administracion@construccionesmarin.example', '+34 933 222 333', 'Calle de la Industria 45, Barcelona'),
    ('Logística del Sur S.L.',       'B34567890', 'info@logisticadelsur.example', '+34 954 333 444', 'Avenida de la Innovación 8, Sevilla'),
    ('Almacenes Cortés S.A.',        'A45678901', 'compras@almacenescortes.example', '+34 963 444 555', 'Polígono El Bony, Nave 3, Valencia'),
    ('Distribuciones Nortec S.L.',   'B56789012', 'pedidos@distribucionesnortec.example', '+34 944 555 666', 'Calle Ribera 22, Bilbao'),
    ('Grupo Vera Hermanos S.A.',     'A67890123', 'contacto@grupovera.example', '+34 976 666 777', 'Polígono Malpica, Nave 9, Zaragoza'),
    ('Reformas Aguilar S.L.',        'B78901234', 'info@reformasaguilar.example', '+34 952 777 888', 'Calle Larios 14, Málaga'),
    ('Agropecuaria Castilla S.A.',   'A89012345', 'administracion@agropecuariacastilla.example', '+34 983 888 999', 'Carretera de Burgos km 5, Valladolid'),
    ('Suministros Peña S.L.',        'B90123456', 'compras@suministrospena.example', '+34 968 999 000', 'Polígono Oeste, Nave 7, Murcia'),
    ('Constructora Del Río S.A.',    'A01234567', 'obras@constructoradelrio.example', '+34 965 000 111', 'Avenida de Elche 31, Alicante');

-- =============================================================================
-- SUPPLIERS
-- =============================================================================
INSERT INTO suppliers (name, tax_id, email, phone, address) VALUES
    ('Talleres Hermanos Ruiz S.L.',          'B11111111', 'taller@hermanosruiz.example',       '+34 911 100 100', 'Calle del Taller 3, Madrid'),
    ('Neumáticos Castilla S.A.',             'A22222222', 'ventas@neumaticoscastilla.example', '+34 983 200 200', 'Polígono San Cristóbal, Valladolid'),
    ('Repsol Flotas S.A.',                   'A33333333', 'flotas@repsol.example',             '+34 900 300 300', 'Paseo de la Castellana 278, Madrid'),
    ('Recambios Diesel Norte S.L.',          'B44444444', 'pedidos@recambiosdieselnorte.example','+34 944 400 400', 'Calle Industria 12, Bilbao'),
    ('Grúas Rápidas del Centro S.L.',        'B55555555', 'info@gruasrapidascentro.example',   '+34 913 500 500', 'Carretera Toledo km 8, Madrid'),
    ('Talleres Mecánicos Vega S.L.',         'B66666666', 'taller@mecanicosvega.example',      '+34 963 600 600', 'Polígono Fuente del Jarro, Valencia'),
    ('Cepsa Card Flotas S.A.',               'A77777777', 'flotas@cepsa.example',              '+34 900 700 700', 'Paseo de la Castellana 259, Madrid'),
    ('Neumáticos y Mecánica Soto S.L.',      'B88888888', 'contacto@mecanicasoto.example',     '+34 954 800 800', 'Polígono Store, Sevilla'),
    ('Recambios Europa S.A.',                'A99999999', 'ventas@recambioseuropa.example',    '+34 976 900 900', 'Polígono Malpica, Zaragoza'),
    ('Taller Diésel Sánchez S.L.',           'B10101010', 'info@dieselsanchez.example',        '+34 952 101 010', 'Calle Industria 9, Málaga'),
    ('Frenos y Embragues Ortiz S.L.',        'B20202020', 'taller@frenosortiz.example',        '+34 968 202 020', 'Polígono Oeste, Murcia'),
    ('Grúas del Mediterráneo S.A.',          'A30303030', 'info@gruasmediterraneo.example',    '+34 965 303 030', 'Carretera Nacional 340 km 3, Alicante'),
    ('Electricidad del Automóvil López S.L.','B40404040', 'taller@electricidadlopez.example',  '+34 944 404 040', 'Calle Ribera 5, Bilbao'),
    ('Talleres Autorizados Pérez S.L.',      'B50505050', 'taller@autorizadosperez.example',   '+34 910 505 050', 'Polígono Industrial Norte, Madrid'),
    ('BP Flotas España S.A.',                'A60606060', 'flotas@bp.example',                 '+34 900 606 060', 'Calle Serrano 41, Madrid'),
    ('Recambios Hidráulicos García S.L.',    'B70707070', 'ventas@hidraulicosgarcia.example',  '+34 933 707 070', 'Calle Industria 18, Barcelona'),
    ('Neumáticos Express Ibérica S.L.',      'B80808080', 'pedidos@neumaticosexpress.example', '+34 954 808 080', 'Polígono El Pino, Sevilla'),
    ('Talleres del Puerto S.L.',             'B90909090', 'taller@tallerespuerto.example',     '+34 963 909 090', 'Muelle de la Mercancía 2, Valencia'),
    ('Maquinaria y Repuestos Ferrán S.A.',   'A15151515', 'ventas@repuestosferran.example',    '+34 933 151 515', 'Polígono Zona Franca, Barcelona'),
    ('Suministros Industriales Roca S.L.',   'B25252525', 'info@industrialroca.example',       '+34 976 252 525', 'Polígono Cogullada, Zaragoza');

-- =============================================================================
-- JOBS + USAGE_LOGS
-- One block per driveable vehicle (trucks + light vehicles — machinery does not run "jobs" in
-- this domain model, only maintenance): 3-5 completed jobs each, spread Jan-Jul 2026, ending at
-- (approximately) the vehicle's seeded current_km. Mirrors what JobEventListener would have done:
-- one usage_log per completed job and the vehicle's current_km left at the last completed value.
-- =============================================================================
DO $$
DECLARE
    v_client_ids UUID[];
    v_vehicle RECORD;
    v_job_id UUID;
    v_n_jobs INT;
    v_span BIGINT;
    v_step BIGINT;
    v_start_usage BIGINT;
    v_running_usage BIGINT;
    v_final_usage BIGINT;
    v_job_date DATE;
    v_client_id UUID;
    v_has_client BOOLEAN;
    v_price NUMERIC(12,2);
    v_origins TEXT[] := ARRAY['Madrid','Barcelona','Valencia','Sevilla','Bilbao','Zaragoza','Málaga','Murcia','Alicante','Valladolid','Vitoria','Pamplona','Toledo','Burgos','León'];
    v_cargos TEXT[] := ARRAY['Transporte de mercancía general','Reparto de material de construcción','Transporte de maquinaria industrial','Distribución de palets','Transporte refrigerado','Traslado de mobiliario','Transporte de piezas de automoción','Reparto de electrodomésticos','Transporte de productos agrícolas','Distribución urbana de paquetería'];
    i INT;
    v_counter INT := 0;
BEGIN
    SELECT array_agg(id ORDER BY name) INTO v_client_ids FROM clients;

    FOR v_vehicle IN
        SELECT v.id, v.current_km,
               (SELECT dva.driver_id FROM driver_vehicle_assignments dva
                WHERE dva.vehicle_id = v.id AND dva.end_date IS NULL LIMIT 1) AS driver_id
        FROM vehicles v
        WHERE v.vehicle_category IN ('HEAVY_VEHICLE', 'LIGHT_VEHICLE')
        ORDER BY v.license_plate
    LOOP
        -- 8 to 14 jobs per vehicle over the 7-month window (~1-2/month) — the original 3-5 was too
        -- sparse for an active fleet and, combined with the low price band below, made total
        -- income a fraction of the seeded maintenance/supplier expenses (see planning.md note).
        v_n_jobs := 8 + floor(random() * 7)::int; -- 8 to 14
        v_span := v_n_jobs * (300 + floor(random() * 400)::int);
        v_start_usage := GREATEST(v_vehicle.current_km - v_span, 100);
        v_step := (v_vehicle.current_km - v_start_usage) / v_n_jobs;
        v_running_usage := v_start_usage;
        v_final_usage := v_start_usage;

        FOR i IN 1..v_n_jobs LOOP
            v_counter := v_counter + 1;
            v_job_date := DATE '2026-01-08'
                + floor(((i::float / v_n_jobs) * 175))::int * INTERVAL '1 day'
                + floor(random() * 6)::int * INTERVAL '1 day';
            v_has_client := random() < 0.90;
            v_client_id := CASE WHEN v_has_client THEN v_client_ids[1 + (v_counter % array_length(v_client_ids, 1))] ELSE NULL END;
            -- 400-1800 (was 250-1150) — realistic freight-job pricing for a truck/light-vehicle fleet.
            v_price := CASE WHEN v_has_client THEN round((400 + random() * 1400)::numeric, 2) ELSE NULL END;
            v_job_id := gen_random_uuid();

            IF i = v_n_jobs AND random() < 0.15 THEN
                -- Leave the most recent job for this vehicle IN_PROGRESS instead of COMPLETED.
                INSERT INTO jobs (id, title, description, vehicle_id, assigned_driver_id, client_id, status,
                                  origin_location, destination_location, scheduled_start, actual_start,
                                  start_usage_value, price, created_at, updated_at)
                VALUES (v_job_id, v_cargos[1 + (i % array_length(v_cargos, 1))],
                        v_cargos[1 + (i % array_length(v_cargos, 1))],
                        v_vehicle.id, v_vehicle.driver_id, v_client_id, 'IN_PROGRESS',
                        v_origins[1 + (v_counter % array_length(v_origins, 1))],
                        v_origins[1 + ((v_counter + 3) % array_length(v_origins, 1))],
                        v_job_date::timestamptz, v_job_date::timestamptz,
                        v_running_usage, v_price, v_job_date::timestamptz, v_job_date::timestamptz);
            ELSE
                v_running_usage := v_start_usage + (v_step * i);
                v_final_usage := v_running_usage;
                INSERT INTO jobs (id, title, description, vehicle_id, assigned_driver_id, client_id, status,
                                  origin_location, destination_location, scheduled_start, scheduled_end,
                                  actual_start, actual_end, start_usage_value, end_usage_value, price,
                                  created_at, updated_at)
                VALUES (v_job_id, v_cargos[1 + (i % array_length(v_cargos, 1))],
                        v_cargos[1 + (i % array_length(v_cargos, 1))],
                        v_vehicle.id, v_vehicle.driver_id, v_client_id, 'COMPLETED',
                        v_origins[1 + (v_counter % array_length(v_origins, 1))],
                        v_origins[1 + ((v_counter + 3) % array_length(v_origins, 1))],
                        v_job_date::timestamptz, v_job_date::timestamptz,
                        v_job_date::timestamptz, v_job_date::timestamptz + INTERVAL '6 hours',
                        v_start_usage + (v_step * (i - 1)), v_running_usage, v_price,
                        v_job_date::timestamptz, v_job_date::timestamptz);

                INSERT INTO usage_logs (vehicle_id, value, measure_type, recorded_at, source, job_id)
                VALUES (v_vehicle.id, v_running_usage, 'KILOMETERS',
                        v_job_date::timestamptz + INTERVAL '6 hours', 'JOB_COMPLETION', v_job_id);
            END IF;
        END LOOP;

        UPDATE vehicles SET current_km = v_final_usage WHERE id = v_vehicle.id;
    END LOOP;
END $$;

-- A handful of PENDING jobs scheduled just after "today" (2026-07-14), for realism — not yet
-- started, no usage-log side effects.
DO $$
DECLARE
    v_vehicle_ids UUID[];
    v_client_ids UUID[];
    v_origins TEXT[] := ARRAY['Madrid','Barcelona','Valencia','Sevilla','Bilbao','Zaragoza','Málaga'];
    v_cargos TEXT[] := ARRAY['Transporte de mercancía general','Reparto de material de construcción','Distribución de palets','Transporte refrigerado'];
    i INT;
BEGIN
    SELECT array_agg(id) INTO v_vehicle_ids FROM vehicles WHERE vehicle_category IN ('HEAVY_VEHICLE', 'LIGHT_VEHICLE');
    SELECT array_agg(id) INTO v_client_ids FROM clients;

    FOR i IN 1..9 LOOP
        INSERT INTO jobs (title, description, vehicle_id, client_id, status, origin_location, destination_location,
                           scheduled_start, created_at, updated_at)
        VALUES (v_cargos[1 + (i % array_length(v_cargos, 1))], v_cargos[1 + (i % array_length(v_cargos, 1))],
                v_vehicle_ids[1 + (i % array_length(v_vehicle_ids, 1))],
                v_client_ids[1 + (i % array_length(v_client_ids, 1))], 'PENDING',
                v_origins[1 + (i % array_length(v_origins, 1))],
                v_origins[1 + ((i + 2) % array_length(v_origins, 1))],
                (DATE '2026-07-15' + i * INTERVAL '1 day'), now(), now());
    END LOOP;
END $$;

-- =============================================================================
-- MAINTENANCE RECORDS + WORKSHOP SCHEDULES
-- 1-3 records per vehicle (all 28), Jan-Jul 2026, mixing PREVENTIVE/CORRECTIVE. The two vehicles
-- seeded with status='MAINTENANCE' (Mercedes-Benz Actros 2109BWL, Renault Trucks T480 3345BXR)
-- each get one currently-open IN_PROGRESS record near "today" with no exit date yet.
-- =============================================================================
DO $$
DECLARE
    v_tech_ids UUID[];
    v_vehicle RECORD;
    v_maintenance_id UUID;
    v_n_records INT;
    v_entry_date DATE;
    v_exit_date DATE;
    v_type TEXT;
    v_category TEXT;
    v_cost NUMERIC(10,2);
    v_priority TEXT;
    v_types TEXT[] := ARRAY['Cambio de aceite y filtros','Revisión general','Cambio de neumáticos','Reparación de frenos','Revisión de motor','Cambio de embrague','Reparación de sistema hidráulico','Revisión de suspensión','Cambio de batería','Reparación eléctrica'];
    v_priorities TEXT[] := ARRAY['LOW','MEDIUM','HIGH','URGENT'];
    i INT;
BEGIN
    SELECT array_agg(id) INTO v_tech_ids FROM workers WHERE worker_role = 'TECHNICIAN';

    FOR v_vehicle IN SELECT id, status, usage_measure, current_km, current_hours FROM vehicles ORDER BY license_plate NULLS LAST, make LOOP
        v_n_records := 1 + floor(random() * 3)::int; -- 1 to 3

        FOR i IN 1..v_n_records LOOP
            v_type := v_types[1 + ((i + array_length(v_tech_ids,1)) % array_length(v_types, 1))];
            v_category := CASE WHEN i % 2 = 0 THEN 'CORRECTIVE' ELSE 'PREVENTIVE' END;
            v_cost := round((120 + random() * 3100)::numeric, 2);
            v_priority := v_priorities[1 + (i % array_length(v_priorities, 1))];
            v_entry_date := DATE '2026-01-10' + floor(((i::float / (v_n_records + 1)) * 175))::int * INTERVAL '1 day';
            v_exit_date := v_entry_date + (1 + floor(random() * 4)::int) * INTERVAL '1 day';
            v_maintenance_id := gen_random_uuid();

            IF i = v_n_records AND v_vehicle.status = 'MAINTENANCE' THEN
                -- Currently open record for the 2 vehicles seeded as under maintenance.
                INSERT INTO maintenance_records (id, vehicle_id, type, description, usage_at_service, cost,
                                                  workshop_entry_date, technician_id, status, category,
                                                  workshop_entry_time, created_at, updated_at)
                VALUES (v_maintenance_id, v_vehicle.id, v_type, v_type,
                        COALESCE(v_vehicle.current_km, v_vehicle.current_hours), v_cost,
                        DATE '2026-07-10', v_tech_ids[1 + (i % array_length(v_tech_ids, 1))], 'IN_PROGRESS', v_category,
                        TIME '08:00', now(), now());

                INSERT INTO workshop_schedules (vehicle_id, technician_id, maintenance_record_id, scheduled_date,
                                                 type, priority, status, scheduled_start_time, created_at, updated_at)
                VALUES (v_vehicle.id, v_tech_ids[1 + (i % array_length(v_tech_ids, 1))], v_maintenance_id,
                        DATE '2026-07-10', v_type, 'HIGH', 'IN_PROGRESS', TIME '08:00', now(), now());
            ELSE
                INSERT INTO maintenance_records (id, vehicle_id, type, description, usage_at_service, cost,
                                                  workshop_entry_date, workshop_exit_date, technician_id, status,
                                                  category, workshop_entry_time, workshop_exit_time, created_at, updated_at)
                VALUES (v_maintenance_id, v_vehicle.id, v_type, v_type,
                        COALESCE(v_vehicle.current_km, v_vehicle.current_hours), v_cost,
                        v_entry_date, v_exit_date, v_tech_ids[1 + (i % array_length(v_tech_ids, 1))], 'COMPLETED',
                        v_category, TIME '08:00', TIME '13:00', v_entry_date::timestamptz, v_exit_date::timestamptz);

                INSERT INTO workshop_schedules (vehicle_id, technician_id, maintenance_record_id, scheduled_date,
                                                 type, priority, status, scheduled_start_time, scheduled_end_time,
                                                 created_at, updated_at)
                VALUES (v_vehicle.id, v_tech_ids[1 + (i % array_length(v_tech_ids, 1))], v_maintenance_id,
                        v_entry_date, v_type, v_priority, 'COMPLETED', TIME '08:00', TIME '13:00',
                        v_entry_date::timestamptz, v_exit_date::timestamptz);
            END IF;
        END LOOP;
    END LOOP;
END $$;

-- =============================================================================
-- CLIENT INVOICES + LINE ITEMS
-- ~3 invoices per client, each billing 1-3 of that client's own completed+priced jobs. Status by
-- month: Jan-Apr -> PAID, May-Jun -> ISSUED, Jul -> DRAFT (mirrors InvoiceService.issue()'s
-- HALF_UP scale-2 subtotal/tax/total computation with the 0.2100 default tax rate).
-- =============================================================================
DO $$
DECLARE
    v_client RECORD;
    v_job RECORD;
    v_invoice_id UUID;
    v_invoice_number TEXT;
    v_issue_date DATE;
    v_subtotal NUMERIC(12,2);
    v_tax NUMERIC(12,2);
    v_total NUMERIC(12,2);
    v_status TEXT;
    v_month INT;
    v_job_ids UUID[];
    v_job_id UUID;
    n INT;
BEGIN
    FOR v_client IN SELECT id FROM clients ORDER BY name LOOP
        -- 7 invoices/client (was 3) so the higher job volume above actually gets billed instead of
        -- leaving most priced jobs uninvoiced — see planning.md note on the income/expense balance.
        FOR n IN 1..7 LOOP
            -- Uniform random month per invoice (not per client) so income actually spreads across
            -- Jan-Jul instead of clustering into the same fixed months for every client — a prior
            -- version of this formula ((n-1)*2)+1, then LEAST(+ (n-1), 7) collapsed to exactly
            -- 1, 4, 7 for every client regardless of `random()`, leaving only Jan/Apr with PAID
            -- issue_date data (month 7 is always DRAFT below, so it never got an issue_date either).
            v_month := 1 + floor(random() * 7)::int;
            v_issue_date := (DATE '2026-01-01' + ((v_month - 1) * INTERVAL '1 month'))::date + (floor(random() * 20))::int * INTERVAL '1 day';

            -- Excludes jobs already picked up by a previous invoice in this same loop — without
            -- this, a job could be sampled onto two different invoices and double-count its
            -- revenue (no unique constraint stops the same linked_job_id appearing on 2 invoices).
            SELECT array_agg(id) INTO v_job_ids FROM (
                SELECT id FROM jobs
                WHERE client_id = v_client.id AND status = 'COMPLETED' AND price IS NOT NULL
                  AND id NOT IN (SELECT linked_job_id FROM invoice_line_items WHERE linked_job_id IS NOT NULL)
                ORDER BY random()
                LIMIT (2 + floor(random() * 4)::int)
            ) sub;

            IF v_job_ids IS NULL OR array_length(v_job_ids, 1) IS NULL THEN
                CONTINUE;
            END IF;

            v_invoice_id := gen_random_uuid();
            v_invoice_number := 'INV-2026-' || lpad(nextval('invoice_number_seq')::text, 5, '0');
            v_status := CASE WHEN v_month <= 4 THEN 'PAID' WHEN v_month <= 6 THEN 'ISSUED' ELSE 'DRAFT' END;

            IF v_status = 'DRAFT' THEN
                INSERT INTO invoices (id, invoice_number, client_id, status, tax_rate, created_at, updated_at)
                VALUES (v_invoice_id, v_invoice_number, v_client.id, 'DRAFT', 0.2100, v_issue_date::timestamptz, v_issue_date::timestamptz);
            ELSE
                SELECT round(sum(price)::numeric, 2) INTO v_subtotal FROM jobs WHERE id = ANY(v_job_ids);
                v_tax := round(v_subtotal * 0.2100, 2);
                v_total := v_subtotal + v_tax;

                INSERT INTO invoices (id, invoice_number, client_id, status, issue_date, due_date, payment_date,
                                       tax_rate, subtotal, tax_amount, total, created_at, updated_at)
                VALUES (v_invoice_id, v_invoice_number, v_client.id, v_status, v_issue_date,
                        -- Every non-DRAFT invoice must carry a due date in the data (net-30 from
                        -- issuance, standard term) — DRAFT is the only status exempt, since it
                        -- hasn't been issued yet and has no issue_date either.
                        v_issue_date + 30,
                        CASE WHEN v_status = 'PAID' THEN v_issue_date + (5 + floor(random() * 20))::int * INTERVAL '1 day' ELSE NULL END,
                        0.2100, v_subtotal, v_tax, v_total, v_issue_date::timestamptz, v_issue_date::timestamptz);
            END IF;

            FOREACH v_job_id IN ARRAY v_job_ids LOOP
                INSERT INTO invoice_line_items (invoice_id, description, quantity, unit_price, subtotal, linked_job_id)
                SELECT v_invoice_id, 'Servicio de transporte: ' || j.title, 1, j.price, j.price, j.id
                FROM jobs j WHERE j.id = v_job_id;
            END LOOP;
        END LOOP;
    END LOOP;
END $$;

-- =============================================================================
-- SUPPLIER INVOICES + LINE ITEMS
-- ~3-4 invoices per supplier, Jan-Jul 2026, feeding realistic "gastos" (expenses) for the
-- profitability views. Older invoices (Jan-May) are PAID, June-July skew toward PENDING.
-- =============================================================================
DO $$
DECLARE
    v_supplier RECORD;
    v_vehicle_ids UUID[];
    v_invoice_id UUID;
    v_invoice_date DATE;
    v_subtotal NUMERIC(12,2);
    v_tax NUMERIC(12,2);
    v_total NUMERIC(12,2);
    v_status TEXT;
    v_category TEXT;
    v_categories TEXT[] := ARRAY['MAINTENANCE','FUEL','INSURANCE','LEASING_RENTING','TOLL','OTHER'];
    v_vehicle_id UUID;
    n INT;
BEGIN
    SELECT array_agg(id) INTO v_vehicle_ids FROM vehicles;

    FOR v_supplier IN SELECT id FROM suppliers ORDER BY name LOOP
        FOR n IN 1..(3 + floor(random() * 2)::int) LOOP
            v_category := v_categories[1 + ((n + length(v_supplier.id::text)) % array_length(v_categories, 1))];
            v_invoice_date := DATE '2026-01-05' + floor(random() * 190)::int * INTERVAL '1 day';
            v_subtotal := round((80 + random() * 2900)::numeric, 2);
            v_tax := round(v_subtotal * 0.2100, 2);
            v_total := v_subtotal + v_tax;
            v_status := CASE WHEN v_invoice_date < DATE '2026-06-01' THEN 'PAID' ELSE
                            CASE WHEN random() < 0.6 THEN 'PENDING' ELSE 'PAID' END
                        END;
            v_vehicle_id := CASE WHEN v_category IN ('MAINTENANCE', 'FUEL', 'TOLL')
                                  THEN v_vehicle_ids[1 + floor(random() * array_length(v_vehicle_ids, 1))::int]
                                  ELSE NULL END;
            v_invoice_id := gen_random_uuid();

            INSERT INTO supplier_invoices (id, supplier_id, supplier_invoice_number, category, invoice_date,
                                           due_date, payment_date, status, subtotal, tax_amount, total, vehicle_id,
                                           created_at, updated_at)
            VALUES (v_invoice_id, v_supplier.id, 'FC-' || to_char(v_invoice_date, 'YYYYMMDD') || '-' || substr(v_invoice_id::text, 1, 6),
                    v_category, v_invoice_date,
                    -- SupplierInvoiceStatus has no DRAFT concept — every row is either PENDING or
                    -- PAID, so every row must carry a due date (net-30 from the invoice date).
                    v_invoice_date + 30,
                    CASE WHEN v_status = 'PAID' THEN v_invoice_date + (3 + floor(random() * 15))::int * INTERVAL '1 day' ELSE NULL END,
                    v_status, v_subtotal, v_tax, v_total, v_vehicle_id,
                    v_invoice_date::timestamptz, v_invoice_date::timestamptz);

            INSERT INTO supplier_invoice_line_items (invoice_id, description, quantity, unit_price, subtotal, vehicle_id)
            VALUES (v_invoice_id,
                    CASE v_category
                        WHEN 'MAINTENANCE' THEN 'Servicio de mantenimiento'
                        WHEN 'FUEL' THEN 'Suministro de combustible'
                        WHEN 'INSURANCE' THEN 'Póliza de seguro'
                        WHEN 'LEASING_RENTING' THEN 'Cuota de leasing/renting'
                        WHEN 'TOLL' THEN 'Peajes'
                        ELSE 'Otros gastos de flota'
                    END,
                    1, v_subtotal, v_subtotal, v_vehicle_id);
        END LOOP;
    END LOOP;
END $$;
