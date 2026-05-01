ALTER TABLE `appointments`
  ADD CONSTRAINT `uk_appointments_provider_start` UNIQUE (`id_provider`, `start`),
  ADD CONSTRAINT `uk_appointments_customer_start` UNIQUE (`id_customer`, `start`);
