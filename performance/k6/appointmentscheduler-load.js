import {
  adminFlow,
  anonymousFlow,
  customerFlow,
  customerWriteFlow,
  handleSummary,
  providerFlow,
  setup,
  thinkTime,
} from './appointmentscheduler-smoke.js';

const LOAD = {
  customerVus: Number(__ENV.K6_CUSTOMER_VUS || 20),
  providerVus: Number(__ENV.K6_PROVIDER_VUS || 5),
  adminVus: Number(__ENV.K6_ADMIN_VUS || 2),
  anonymousVus: Number(__ENV.K6_ANONYMOUS_VUS || 3),
  writeVus: Number(__ENV.K6_WRITE_VUS || 3),
  duration: __ENV.K6_LOAD_DURATION || '5m',
};

export { handleSummary, setup };

export const options = {
  scenarios: {
    customer_browsing: {
      executor: 'constant-vus',
      exec: 'customerBrowsing',
      vus: LOAD.customerVus,
      duration: LOAD.duration,
    },
    provider_dashboard: {
      executor: 'constant-vus',
      exec: 'providerDashboard',
      vus: LOAD.providerVus,
      duration: LOAD.duration,
    },
    admin_management: {
      executor: 'constant-vus',
      exec: 'adminManagement',
      vus: LOAD.adminVus,
      duration: LOAD.duration,
    },
    anonymous_browsing: {
      executor: 'constant-vus',
      exec: 'anonymousBrowsing',
      vus: LOAD.anonymousVus,
      duration: LOAD.duration,
    },
    customer_writes: {
      executor: 'constant-vus',
      exec: 'customerWrites',
      vus: LOAD.writeVus,
      duration: LOAD.duration,
    },
  },
  thresholds: {
    'http_req_failed{type:page}': ['rate<0.01'],
    'http_req_failed{type:api}': ['rate<0.01'],
    'http_req_failed{type:asset}': ['rate<0.01'],
    'http_req_failed{type:write}': ['rate<0.02'],
    'http_req_duration{type:page}': ['p(95)<800'],
    'http_req_duration{type:api}': ['p(95)<300'],
    'http_req_duration{type:asset}': ['p(95)<300'],
    'http_req_duration{type:write}': ['p(95)<1000'],
    'http_req_duration{endpoint:availableHours}': ['p(95)<500'],
    'http_req_duration{endpoint:calendar}': ['p(95)<500'],
    'http_req_duration{endpoint:notifications}': ['p(95)<300'],
    'http_req_duration{endpoint:createAppointment}': ['p(95)<1000'],
    checks: ['rate>0.99'],
  },
};

export function customerBrowsing() {
  customerFlow();
  thinkTime(1, 4);
}

export function providerDashboard() {
  providerFlow();
  thinkTime(2, 5);
}

export function adminManagement() {
  adminFlow();
  thinkTime(3, 6);
}

export function anonymousBrowsing() {
  anonymousFlow();
  thinkTime(1, 3);
}

export function customerWrites() {
  customerWriteFlow();
  thinkTime(8, 15);
}
