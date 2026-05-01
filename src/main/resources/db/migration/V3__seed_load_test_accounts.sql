-- Additional deterministic accounts for CI smoke/load testing.
-- All accounts use password: qwerty123

INSERT INTO `users` (id, username, password, first_name, last_name, email, mobile, street, city, postcode)
VALUES
  (1001, 'load_customer_01', '$2a$10$EqKcp1WFKVQISheBxkQJoOqFbsWDzGJXRz/tjkGq85IZKJJ1IipYi', 'Load', 'Customer 01', 'load_customer_01@example.com', '13800001001', 'Load Street 1', 'Load City', '100001'),
  (1002, 'load_customer_02', '$2a$10$EqKcp1WFKVQISheBxkQJoOqFbsWDzGJXRz/tjkGq85IZKJJ1IipYi', 'Load', 'Customer 02', 'load_customer_02@example.com', '13800001002', 'Load Street 2', 'Load City', '100002'),
  (1003, 'load_customer_03', '$2a$10$EqKcp1WFKVQISheBxkQJoOqFbsWDzGJXRz/tjkGq85IZKJJ1IipYi', 'Load', 'Customer 03', 'load_customer_03@example.com', '13800001003', 'Load Street 3', 'Load City', '100003'),
  (1004, 'load_customer_04', '$2a$10$EqKcp1WFKVQISheBxkQJoOqFbsWDzGJXRz/tjkGq85IZKJJ1IipYi', 'Load', 'Customer 04', 'load_customer_04@example.com', '13800001004', 'Load Street 4', 'Load City', '100004'),
  (1005, 'load_customer_05', '$2a$10$EqKcp1WFKVQISheBxkQJoOqFbsWDzGJXRz/tjkGq85IZKJJ1IipYi', 'Load', 'Customer 05', 'load_customer_05@example.com', '13800001005', 'Load Street 5', 'Load City', '100005'),
  (1006, 'load_customer_06', '$2a$10$EqKcp1WFKVQISheBxkQJoOqFbsWDzGJXRz/tjkGq85IZKJJ1IipYi', 'Load', 'Customer 06', 'load_customer_06@example.com', '13800001006', 'Load Street 6', 'Load City', '100006'),
  (1007, 'load_customer_07', '$2a$10$EqKcp1WFKVQISheBxkQJoOqFbsWDzGJXRz/tjkGq85IZKJJ1IipYi', 'Load', 'Customer 07', 'load_customer_07@example.com', '13800001007', 'Load Street 7', 'Load City', '100007'),
  (1008, 'load_customer_08', '$2a$10$EqKcp1WFKVQISheBxkQJoOqFbsWDzGJXRz/tjkGq85IZKJJ1IipYi', 'Load', 'Customer 08', 'load_customer_08@example.com', '13800001008', 'Load Street 8', 'Load City', '100008'),
  (1009, 'load_customer_09', '$2a$10$EqKcp1WFKVQISheBxkQJoOqFbsWDzGJXRz/tjkGq85IZKJJ1IipYi', 'Load', 'Customer 09', 'load_customer_09@example.com', '13800001009', 'Load Street 9', 'Load City', '100009'),
  (1010, 'load_customer_10', '$2a$10$EqKcp1WFKVQISheBxkQJoOqFbsWDzGJXRz/tjkGq85IZKJJ1IipYi', 'Load', 'Customer 10', 'load_customer_10@example.com', '13800001010', 'Load Street 10', 'Load City', '100010'),
  (1011, 'load_customer_11', '$2a$10$EqKcp1WFKVQISheBxkQJoOqFbsWDzGJXRz/tjkGq85IZKJJ1IipYi', 'Load', 'Customer 11', 'load_customer_11@example.com', '13800001011', 'Load Street 11', 'Load City', '100011'),
  (1012, 'load_customer_12', '$2a$10$EqKcp1WFKVQISheBxkQJoOqFbsWDzGJXRz/tjkGq85IZKJJ1IipYi', 'Load', 'Customer 12', 'load_customer_12@example.com', '13800001012', 'Load Street 12', 'Load City', '100012'),
  (1013, 'load_customer_13', '$2a$10$EqKcp1WFKVQISheBxkQJoOqFbsWDzGJXRz/tjkGq85IZKJJ1IipYi', 'Load', 'Customer 13', 'load_customer_13@example.com', '13800001013', 'Load Street 13', 'Load City', '100013'),
  (1014, 'load_customer_14', '$2a$10$EqKcp1WFKVQISheBxkQJoOqFbsWDzGJXRz/tjkGq85IZKJJ1IipYi', 'Load', 'Customer 14', 'load_customer_14@example.com', '13800001014', 'Load Street 14', 'Load City', '100014'),
  (1015, 'load_customer_15', '$2a$10$EqKcp1WFKVQISheBxkQJoOqFbsWDzGJXRz/tjkGq85IZKJJ1IipYi', 'Load', 'Customer 15', 'load_customer_15@example.com', '13800001015', 'Load Street 15', 'Load City', '100015'),
  (1016, 'load_customer_16', '$2a$10$EqKcp1WFKVQISheBxkQJoOqFbsWDzGJXRz/tjkGq85IZKJJ1IipYi', 'Load', 'Customer 16', 'load_customer_16@example.com', '13800001016', 'Load Street 16', 'Load City', '100016'),
  (1017, 'load_customer_17', '$2a$10$EqKcp1WFKVQISheBxkQJoOqFbsWDzGJXRz/tjkGq85IZKJJ1IipYi', 'Load', 'Customer 17', 'load_customer_17@example.com', '13800001017', 'Load Street 17', 'Load City', '100017'),
  (1018, 'load_customer_18', '$2a$10$EqKcp1WFKVQISheBxkQJoOqFbsWDzGJXRz/tjkGq85IZKJJ1IipYi', 'Load', 'Customer 18', 'load_customer_18@example.com', '13800001018', 'Load Street 18', 'Load City', '100018'),
  (1019, 'load_customer_19', '$2a$10$EqKcp1WFKVQISheBxkQJoOqFbsWDzGJXRz/tjkGq85IZKJJ1IipYi', 'Load', 'Customer 19', 'load_customer_19@example.com', '13800001019', 'Load Street 19', 'Load City', '100019'),
  (1020, 'load_customer_20', '$2a$10$EqKcp1WFKVQISheBxkQJoOqFbsWDzGJXRz/tjkGq85IZKJJ1IipYi', 'Load', 'Customer 20', 'load_customer_20@example.com', '13800001020', 'Load Street 20', 'Load City', '100020'),
  (1101, 'load_provider_01', '$2a$10$EqKcp1WFKVQISheBxkQJoOqFbsWDzGJXRz/tjkGq85IZKJJ1IipYi', 'Load', 'Provider 01', 'load_provider_01@example.com', '13800001101', 'Provider Street 1', 'Load City', '101101'),
  (1102, 'load_provider_02', '$2a$10$EqKcp1WFKVQISheBxkQJoOqFbsWDzGJXRz/tjkGq85IZKJJ1IipYi', 'Load', 'Provider 02', 'load_provider_02@example.com', '13800001102', 'Provider Street 2', 'Load City', '101102'),
  (1103, 'load_provider_03', '$2a$10$EqKcp1WFKVQISheBxkQJoOqFbsWDzGJXRz/tjkGq85IZKJJ1IipYi', 'Load', 'Provider 03', 'load_provider_03@example.com', '13800001103', 'Provider Street 3', 'Load City', '101103');

INSERT INTO `customers` (id_customer)
VALUES
  (1001), (1002), (1003), (1004), (1005),
  (1006), (1007), (1008), (1009), (1010),
  (1011), (1012), (1013), (1014), (1015),
  (1016), (1017), (1018), (1019), (1020);

INSERT INTO `retail_customers` (id_customer)
VALUES
  (1001), (1002), (1003), (1004), (1005),
  (1006), (1007), (1008), (1009), (1010),
  (1011), (1012), (1013), (1014), (1015),
  (1016), (1017), (1018), (1019), (1020);

INSERT INTO `providers` (id_provider)
VALUES (1101), (1102), (1103);

INSERT INTO `users_roles` (user_id, role_id)
VALUES
  (1001, 3), (1001, 5),
  (1002, 3), (1002, 5),
  (1003, 3), (1003, 5),
  (1004, 3), (1004, 5),
  (1005, 3), (1005, 5),
  (1006, 3), (1006, 5),
  (1007, 3), (1007, 5),
  (1008, 3), (1008, 5),
  (1009, 3), (1009, 5),
  (1010, 3), (1010, 5),
  (1011, 3), (1011, 5),
  (1012, 3), (1012, 5),
  (1013, 3), (1013, 5),
  (1014, 3), (1014, 5),
  (1015, 3), (1015, 5),
  (1016, 3), (1016, 5),
  (1017, 3), (1017, 5),
  (1018, 3), (1018, 5),
  (1019, 3), (1019, 5),
  (1020, 3), (1020, 5),
  (1101, 2),
  (1102, 2),
  (1103, 2);

INSERT INTO `works_providers` (id_user, id_work)
VALUES
  (1101, 1),
  (1102, 1),
  (1103, 1);

INSERT INTO `working_plans`
VALUES
  (1101,
   JSON_OBJECT('workingHours', JSON_OBJECT('start', JSON_ARRAY(6, 0), 'end', JSON_ARRAY(18, 0)), 'breaks', JSON_ARRAY()),
   JSON_OBJECT('workingHours', JSON_OBJECT('start', JSON_ARRAY(6, 0), 'end', JSON_ARRAY(18, 0)), 'breaks', JSON_ARRAY()),
   JSON_OBJECT('workingHours', JSON_OBJECT('start', JSON_ARRAY(6, 0), 'end', JSON_ARRAY(18, 0)), 'breaks', JSON_ARRAY()),
   JSON_OBJECT('workingHours', JSON_OBJECT('start', JSON_ARRAY(6, 0), 'end', JSON_ARRAY(18, 0)), 'breaks', JSON_ARRAY()),
   JSON_OBJECT('workingHours', JSON_OBJECT('start', JSON_ARRAY(6, 0), 'end', JSON_ARRAY(18, 0)), 'breaks', JSON_ARRAY()),
   JSON_OBJECT('workingHours', JSON_OBJECT('start', JSON_ARRAY(6, 0), 'end', JSON_ARRAY(18, 0)), 'breaks', JSON_ARRAY()),
   JSON_OBJECT('workingHours', JSON_OBJECT('start', JSON_ARRAY(6, 0), 'end', JSON_ARRAY(18, 0)), 'breaks', JSON_ARRAY())),
  (1102,
   JSON_OBJECT('workingHours', JSON_OBJECT('start', JSON_ARRAY(6, 0), 'end', JSON_ARRAY(18, 0)), 'breaks', JSON_ARRAY()),
   JSON_OBJECT('workingHours', JSON_OBJECT('start', JSON_ARRAY(6, 0), 'end', JSON_ARRAY(18, 0)), 'breaks', JSON_ARRAY()),
   JSON_OBJECT('workingHours', JSON_OBJECT('start', JSON_ARRAY(6, 0), 'end', JSON_ARRAY(18, 0)), 'breaks', JSON_ARRAY()),
   JSON_OBJECT('workingHours', JSON_OBJECT('start', JSON_ARRAY(6, 0), 'end', JSON_ARRAY(18, 0)), 'breaks', JSON_ARRAY()),
   JSON_OBJECT('workingHours', JSON_OBJECT('start', JSON_ARRAY(6, 0), 'end', JSON_ARRAY(18, 0)), 'breaks', JSON_ARRAY()),
   JSON_OBJECT('workingHours', JSON_OBJECT('start', JSON_ARRAY(6, 0), 'end', JSON_ARRAY(18, 0)), 'breaks', JSON_ARRAY()),
   JSON_OBJECT('workingHours', JSON_OBJECT('start', JSON_ARRAY(6, 0), 'end', JSON_ARRAY(18, 0)), 'breaks', JSON_ARRAY())),
  (1103,
   JSON_OBJECT('workingHours', JSON_OBJECT('start', JSON_ARRAY(6, 0), 'end', JSON_ARRAY(18, 0)), 'breaks', JSON_ARRAY()),
   JSON_OBJECT('workingHours', JSON_OBJECT('start', JSON_ARRAY(6, 0), 'end', JSON_ARRAY(18, 0)), 'breaks', JSON_ARRAY()),
   JSON_OBJECT('workingHours', JSON_OBJECT('start', JSON_ARRAY(6, 0), 'end', JSON_ARRAY(18, 0)), 'breaks', JSON_ARRAY()),
   JSON_OBJECT('workingHours', JSON_OBJECT('start', JSON_ARRAY(6, 0), 'end', JSON_ARRAY(18, 0)), 'breaks', JSON_ARRAY()),
   JSON_OBJECT('workingHours', JSON_OBJECT('start', JSON_ARRAY(6, 0), 'end', JSON_ARRAY(18, 0)), 'breaks', JSON_ARRAY()),
   JSON_OBJECT('workingHours', JSON_OBJECT('start', JSON_ARRAY(6, 0), 'end', JSON_ARRAY(18, 0)), 'breaks', JSON_ARRAY()),
   JSON_OBJECT('workingHours', JSON_OBJECT('start', JSON_ARRAY(6, 0), 'end', JSON_ARRAY(18, 0)), 'breaks', JSON_ARRAY()));
