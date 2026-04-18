-- Sanctions List (mock OFAC / UN entries)
INSERT INTO sanctions_entity (id, full_name, aliases, country, entity_type, program, listed_date, active) VALUES
(1, 'SHADOW FINANCE LLC',       'Shadow Finance|SF LLC',          'IR', 'ENTITY',     'IRAN-SDN',  '2020-01-15', true),
(2, 'VIKTOR BLACKMONEY',        'V. Blackmoney|Viktor B',         'RU', 'INDIVIDUAL', 'RUSSIA-SDN','2022-03-04', true),
(3, 'GLOBAL SHELL TRADING CO',  'GST Co|Global Shell',            'KP', 'ENTITY',     'DPRK-SDN',  '2019-06-20', true),
(4, 'HASSAN AL-RASHEED',        'H. Rasheed|Al Rasheed',          'SY', 'INDIVIDUAL', 'SYRIA-SDN', '2018-11-30', true),
(5, 'PACIFIC GHOST HOLDINGS',   'PGH|Pacific Ghost',              'CN', 'ENTITY',     'CAATSA',    '2023-01-10', true);

-- PEP Registry (mock Politically Exposed Persons)
INSERT INTO pep_entity (id, full_name, country, position, risk_level, active_since) VALUES
(1, 'JAMES WELLINGTON',  'GB', 'Former Finance Minister',      'HIGH',   '2010-05-01'),
(2, 'MARIA GONZALEZ',    'MX', 'State Governor',               'HIGH',   '2018-03-15'),
(3, 'CHEN WEI',          'CN', 'Senior Government Official',   'HIGH',   '2015-09-01'),
(4, 'DAVID OKONKWO',     'NG', 'Central Bank Deputy Governor', 'MEDIUM', '2020-07-10'),
(5, 'ELENA VOLKOV',      'RU', 'Regional Assembly Member',     'MEDIUM', '2019-02-28');

-- Customer Registry
INSERT INTO customer (id, full_name, date_of_birth, nationality, address, kyc_status, kyc_document_s3_key, created_at) VALUES
(1, 'JOHN DOE',          '1980-04-12', 'US', '123 Main St, New York, NY',       'VERIFIED',   'kyc-documents/john_doe_kyc.pdf',        NOW()),
(2, 'VIKTOR BLACKMONEY', '1975-08-23', 'RU', '45 Red Square, Moscow',           'PENDING',    'kyc-documents/viktor_blackmoney_kyc.pdf', NOW()),
(3, 'ALICE CHEN',        '1990-11-05', 'SG', '88 Marina Bay, Singapore',        'VERIFIED',   'kyc-documents/alice_chen_kyc.pdf',       NOW()),
(4, 'JAMES WELLINGTON',  '1965-03-17', 'GB', '10 Downing Lane, London',         'PENDING',    'kyc-documents/james_wellington_kyc.pdf', NOW()),
(5, 'ACME CORP LTD',     NULL,         'BS', 'PO Box 1234, Nassau, Bahamas',     'UNDER_REVIEW','kyc-documents/acme_corp_kyc.pdf',      NOW());

-- Transaction History
INSERT INTO transaction (id, customer_id, amount, currency, counterparty, counterparty_country, transaction_type, transaction_date, flagged) VALUES
(1,  2, 95000.00,  'USD', 'SHADOW FINANCE LLC', 'IR', 'WIRE_TRANSFER', NOW() - INTERVAL '5' DAY,  false),
(2,  2, 47500.00,  'USD', 'GLOBAL SHELL TRADING CO', 'KP', 'WIRE_TRANSFER', NOW() - INTERVAL '10' DAY, false),
(3,  2, 12000.00,  'USD', 'UNKNOWN ENTITY XYZ',  'AE', 'CASH_DEPOSIT',  NOW() - INTERVAL '12' DAY, false),
(4,  4, 250000.00, 'GBP', 'PACIFIC GHOST HOLDINGS', 'CN', 'WIRE_TRANSFER', NOW() - INTERVAL '3' DAY, false),
(5,  5, 9900.00,   'USD', 'RANDOM BUYER LLC',    'US', 'PAYMENT',       NOW() - INTERVAL '1' DAY,  false),
(6,  5, 9800.00,   'USD', 'RANDOM BUYER LLC',    'US', 'PAYMENT',       NOW() - INTERVAL '2' DAY,  false),
(7,  5, 9700.00,   'USD', 'RANDOM BUYER LLC',    'US', 'PAYMENT',       NOW() - INTERVAL '3' DAY,  false),
(8,  5, 9600.00,   'USD', 'RANDOM BUYER LLC',    'US', 'PAYMENT',       NOW() - INTERVAL '4' DAY,  false),
(9,  5, 9500.00,   'USD', 'RANDOM BUYER LLC',    'US', 'PAYMENT',       NOW() - INTERVAL '5' DAY,  false),
(10, 1, 500.00,    'USD', 'LEGIT STORE INC',     'US', 'PAYMENT',       NOW() - INTERVAL '1' DAY,  false);
