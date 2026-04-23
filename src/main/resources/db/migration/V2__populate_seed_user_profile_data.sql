UPDATE `users`
SET `first_name` = 'Admin',
    `last_name` = 'User',
    `email` = 'admin@example.com'
WHERE `id` = 1
  AND `first_name` IS NULL
  AND `last_name` IS NULL;

UPDATE `users`
SET `first_name` = 'Demo',
    `last_name` = 'Provider',
    `email` = 'provider@example.com'
WHERE `id` = 2
  AND `first_name` IS NULL
  AND `last_name` IS NULL;

UPDATE `users`
SET `first_name` = 'Retail',
    `last_name` = 'Customer',
    `email` = 'customer_r@example.com'
WHERE `id` = 3
  AND `first_name` IS NULL
  AND `last_name` IS NULL;

UPDATE `users`
SET `first_name` = 'Corporate',
    `last_name` = 'Customer',
    `email` = 'customer_c@example.com'
WHERE `id` = 4
  AND `first_name` IS NULL
  AND `last_name` IS NULL;
