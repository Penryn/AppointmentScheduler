import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PASSWORD = __ENV.K6_PASSWORD || 'qwerty123';

http.setResponseCallback(http.expectedStatuses({ min: 200, max: 399 }));

const USERS = {
  admin: __ENV.K6_ADMIN_USERNAME || 'admin',
  provider: __ENV.K6_PROVIDER_USERNAME || 'provider',
  customer: __ENV.K6_CUSTOMER_USERNAME || 'customer_r',
};

const IDS = {
  admin: Number(__ENV.K6_ADMIN_ID || 1),
  provider: Number(__ENV.K6_PROVIDER_ID || 2),
  customer: Number(__ENV.K6_CUSTOMER_ID || 3),
  work: Number(__ENV.K6_WORK_ID || 1),
};

const LOAD = {
  rampUp: __ENV.K6_RAMP_UP_DURATION || '20s',
  steady: __ENV.K6_STEADY_DURATION || '40s',
  rampDown: __ENV.K6_RAMP_DOWN_DURATION || '20s',
  warmVus: Number(__ENV.K6_WARM_VUS || 5),
  peakVus: Number(__ENV.K6_PEAK_VUS || 10),
};

const httpStatusCount = new Counter('http_status_count');

export const options = {
  scenarios: {
    smoke_load: {
      executor: 'ramping-vus',
      stages: [
        { duration: LOAD.rampUp, target: LOAD.warmVus },
        { duration: LOAD.steady, target: LOAD.peakVus },
        { duration: LOAD.rampDown, target: 0 },
      ],
    },
  },
  thresholds: {
    'http_req_failed{type:page}': ['rate<0.01'],
    'http_req_failed{type:api}': ['rate<0.01'],
    'http_req_failed{type:asset}': ['rate<0.01'],
    http_req_duration: ['p(95)<1000'],
    checks: ['rate>0.99'],
  },
};

export function setup() {
  const health = get(`${BASE_URL}/actuator/health`, 'actuator:health', 'api');
  check(health, {
    'application is healthy': (response) => response.status === 200 && bodyOf(response).includes('UP'),
  });
}

export default function () {
  anonymousFlow();
  customerFlow();

  if (__ITER % 3 === 0) {
    providerFlow();
  }

  if (__ITER % 5 === 0) {
    adminFlow();
  }

  sleep(1);
}

function anonymousFlow() {
  group('anonymous pages and assets', () => {
    http.cookieJar().clear(BASE_URL);

    assertPage(get(`${BASE_URL}/login`, 'anonymous:login-page', 'page'), 'login page', ['id="login-form"']);
    assertPage(get(`${BASE_URL}/customers/new/retail`, 'anonymous:retail-registration', 'page'), 'retail registration page', ['注册个人客户账号']);
    assertPage(get(`${BASE_URL}/customers/new/corporate`, 'anonymous:corporate-registration', 'page'), 'corporate registration page', ['注册企业客户账号']);

    const assets = http.batch([
      ['GET', `${BASE_URL}/css/style.css`, null, requestOptions('asset:style.css', 'asset')],
      ['GET', `${BASE_URL}/webjars/bootstrap/5.3.8/css/bootstrap.min.css`, null, requestOptions('asset:bootstrap.css', 'asset')],
      ['GET', `${BASE_URL}/actuator/info`, null, requestOptions('actuator:info', 'api')],
    ]);
    assets.forEach(recordStatus);
    check(assets[0], { 'style.css is available': (response) => response.status === 200 });
    check(assets[1], { 'bootstrap css is available': (response) => response.status === 200 });
    check(assets[2], { 'actuator info is available': (response) => response.status === 200 });
  });
}

function customerFlow() {
  group('customer appointment discovery flow', () => {
    loginAs(USERS.customer);

    assertPage(get(`${BASE_URL}/`, 'customer:home', 'page'), 'customer home page', ['id="calendar"']);
    assertPage(get(`${BASE_URL}/appointments/all`, 'customer:appointments', 'page'), 'customer appointments page', ['id="appointments"']);
    assertPage(get(`${BASE_URL}/appointments/all?status=SCHEDULED&page=0&size=10`, 'customer:appointments-filtered', 'page'), 'filtered customer appointments page', ['id="appointments"']);
    assertPage(get(`${BASE_URL}/customers/${IDS.customer}`, 'customer:profile', 'page'), 'customer profile page', ['id="profile"']);
    assertPage(get(`${BASE_URL}/notifications`, 'customer:notifications', 'page'), 'customer notifications page', ['id="notifications"']);

    assertPage(get(`${BASE_URL}/appointments/new`, 'customer:appointment-new', 'page'), 'appointment provider selection page', ['选择']);
    assertPage(get(`${BASE_URL}/appointments/new/${IDS.provider}`, 'customer:appointment-provider', 'page'), 'appointment service selection page', ['id="customers"']);
    assertPage(get(`${BASE_URL}/appointments/new/${IDS.provider}/${IDS.work}`, 'customer:appointment-work', 'page'), 'appointment date selection page', ['id="calendar"']);

    const date = nextIsoDate(2);
    const availableHours = get(`${BASE_URL}/api/availableHours/${IDS.provider}/${IDS.work}/${date}`, 'customer:available-hours-api', 'api');
    check(availableHours, {
      'available hours api is available': (response) => response.status === 200,
      'available hours api returns json': (response) => String(response.headers['Content-Type'] || '').includes('application/json'),
    });

    const userAppointments = get(`${BASE_URL}/api/user/${IDS.customer}/appointments?${calendarWindowQuery()}`, 'customer:calendar-api', 'api');
    check(userAppointments, {
      'customer calendar api is available': (response) => response.status === 200,
      'customer calendar api returns json': (response) => String(response.headers['Content-Type'] || '').includes('application/json'),
    });

    const notifications = get(`${BASE_URL}/api/user/notifications`, 'customer:notifications-api', 'api');
    check(notifications, {
      'customer notifications api is available': (response) => response.status === 200,
    });
  });
}

function providerFlow() {
  group('provider dashboard flow', () => {
    loginAs(USERS.provider);

    assertPage(get(`${BASE_URL}/`, 'provider:home', 'page'), 'provider home page', ['id="calendar"']);
    assertPage(get(`${BASE_URL}/appointments/all`, 'provider:appointments', 'page'), 'provider appointments page', ['id="appointments"']);
    assertPage(get(`${BASE_URL}/appointments/all?status=SCHEDULED&page=0&size=10`, 'provider:appointments-filtered', 'page'), 'filtered provider appointments page', ['id="appointments"']);
    assertPage(get(`${BASE_URL}/providers/${IDS.provider}`, 'provider:profile', 'page'), 'provider profile page', ['id="profile"']);
    assertPage(get(`${BASE_URL}/providers/availability`, 'provider:availability', 'page'), 'provider availability page', ['name="monday.workingHours.start"']);

    const providerAppointments = get(`${BASE_URL}/api/user/${IDS.provider}/appointments?${calendarWindowQuery()}`, 'provider:calendar-api', 'api');
    check(providerAppointments, {
      'provider calendar api is available': (response) => response.status === 200,
      'provider calendar api returns json': (response) => String(response.headers['Content-Type'] || '').includes('application/json'),
    });
  });
}

function adminFlow() {
  group('admin management flow', () => {
    loginAs(USERS.admin);

    assertPage(get(`${BASE_URL}/`, 'admin:home', 'page'), 'admin home page', ['id="calendar"']);
    assertPage(get(`${BASE_URL}/appointments/all`, 'admin:appointments', 'page'), 'admin appointments page', ['id="appointments"']);
    assertPage(get(`${BASE_URL}/customers/all`, 'admin:customers', 'page'), 'admin customers page', ['id="customers"']);
    assertPage(get(`${BASE_URL}/providers/all`, 'admin:providers', 'page'), 'admin providers page', ['id="providers"']);
    assertPage(get(`${BASE_URL}/works/all`, 'admin:works', 'page'), 'admin works page', ['id="works"']);
    assertPage(get(`${BASE_URL}/invoices/all`, 'admin:invoices', 'page'), 'admin invoices page', ['发票']);
    assertPage(get(`${BASE_URL}/customers/${IDS.customer}`, 'admin:customer-detail', 'page'), 'admin customer detail page', ['id="profile"']);
    assertPage(get(`${BASE_URL}/providers/${IDS.provider}`, 'admin:provider-detail', 'page'), 'admin provider detail page', ['id="profile"']);
    assertPage(get(`${BASE_URL}/works/${IDS.work}`, 'admin:work-detail', 'page'), 'admin work detail page', ['name="name"']);
  });
}

function loginAs(username) {
  http.cookieJar().clear(BASE_URL);

  const loginPage = get(`${BASE_URL}/login`, `${username}:login-page`, 'login');
  check(loginPage, {
    'login page is available before authentication': (response) => response.status === 200,
    'login form is rendered before authentication': (response) => bodyOf(response).includes('id="login-form"'),
  });

  const csrfToken = extractCsrfToken(bodyOf(loginPage));
  const payload = {
    username,
    password: PASSWORD,
  };

  if (csrfToken) {
    payload._csrf = csrfToken;
  }

  const login = post(`${BASE_URL}/perform_login`, payload, {
    redirects: 0,
  }, `${username}:login-submit`, 'login');
  check(login, {
    [`${username} login redirects after success`]: (response) => response.status === 302,
    [`${username} login does not return error redirect`]: (response) => !String(response.headers.Location || '').includes('error'),
  });
}

function assertPage(response, name, expectedFragments = []) {
  const checks = {
    [`${name} returns 200`]: (res) => res.status === 200,
  };

  expectedFragments.forEach((fragment) => {
    checks[`${name} contains ${fragment}`] = (res) => bodyOf(res).includes(fragment);
  });

  check(response, checks);
}

function requestOptions(name, type) {
  return {
    tags: requestTags(name, type),
  };
}

function requestTags(name, type) {
  return {
    name,
    type: type || 'other',
  };
}

function get(url, name, type) {
  const response = http.get(url, requestOptions(name, type));
  recordStatus(response);
  return response;
}

function post(url, payload, options, name, type) {
  const requestConfig = options || {};
  requestConfig.tags = Object.assign({}, requestConfig.tags || {}, requestTags(name, type));
  const response = http.post(url, payload, requestConfig);
  recordStatus(response);
  return response;
}

function recordStatus(response) {
  httpStatusCount.add(1, { status: String(response.status || 0) });
}

function bodyOf(response) {
  return String(response.body || '');
}

function nextIsoDate(offsetDays) {
  const date = new Date(Date.now() + offsetDays * 24 * 60 * 60 * 1000);
  return date.toISOString().slice(0, 10);
}

function calendarWindowQuery() {
  const start = new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString();
  const end = new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString();
  return `start=${encodeURIComponent(start)}&end=${encodeURIComponent(end)}`;
}

function extractCsrfToken(body) {
  const match = String(body).match(/<input[^>]*name="_csrf"[^>]*value="([^"]+)"[^>]*>/)
    || String(body).match(/<input[^>]*value="([^"]+)"[^>]*name="_csrf"[^>]*>/);
  return match ? match[1] : null;
}

export function handleSummary(data) {
  return {
    stdout: simpleSummary(data),
    'target/k6/summary.json': JSON.stringify(data, null, 2),
    'target/k6/checks.json': JSON.stringify(checkSummary(data), null, 2),
    'target/k6/http-status.json': JSON.stringify(httpStatusSummary(data), null, 2),
  };
}

function checkSummary(data) {
  const checks = [];
  collectChecks(data.root_group, checks);
  return checks.map((result) => ({
    name: result.name,
    path: result.path,
    passes: result.passes,
    fails: result.fails,
    rate: result.passes + result.fails === 0 ? null : result.passes / (result.passes + result.fails),
  }));
}

function collectChecks(group, checks) {
  (group.checks || []).forEach((result) => checks.push(result));
  (group.groups || []).forEach((child) => collectChecks(child, checks));
}

function simpleSummary(data) {
  const metrics = data.metrics;
  const failedRate = metricValue(metrics, 'http_req_failed', 'rate');
  const checkRate = metricValue(metrics, 'checks', 'rate');
  const p95 = metricValue(metrics, 'http_req_duration', 'p(95)');
  const httpRequests = metricValue(metrics, 'http_reqs', 'count');
  const failedChecks = checkSummary(data)
    .filter((result) => result.fails > 0)
    .sort((left, right) => right.fails - left.fails)
    .slice(0, 10);
  const statuses = httpStatusSummary(data);

  const lines = [
    '',
    'k6 summary',
    `  http_reqs: ${formatNumber(httpRequests)}`,
    `  http_req_failed: ${formatPercent(failedRate)}`,
    `  checks: ${formatPercent(checkRate)}`,
    `  http_req_duration p95: ${formatMs(p95)}`,
  ];

  if (failedChecks.length > 0) {
    lines.push('', 'failed checks:');
    failedChecks.forEach((result) => {
      lines.push(`  ${result.path || result.name}: ${result.fails} failed, ${result.passes} passed, ${formatPercent(result.rate)} success`);
    });
  }

  if (statuses.length > 0) {
    lines.push('', 'http status distribution:');
    statuses.forEach((result) => {
      lines.push(`  ${result.status}: ${result.count}`);
    });
  }

  lines.push('');
  return lines.join('\n');
}

function httpStatusSummary(data) {
  const statuses = {};
  Object.entries(data.metrics || {}).forEach(([metricName, metric]) => {
    const match = metricName.match(/^http_status_count\{.*status:([^,}]+).*}$/);
    if (!match) {
      return;
    }
    statuses[match[1]] = metric.values.count;
  });
  return Object.entries(statuses)
    .map(([status, count]) => ({ status, count }))
    .sort((left, right) => Number(left.status) - Number(right.status));
}

function formatNumber(value) {
  return value === undefined || value === null ? 'n/a' : String(value);
}

function formatPercent(value) {
  return value === undefined || value === null ? 'n/a' : `${Math.round(value * 10000) / 100}%`;
}

function formatMs(value) {
  return value === undefined || value === null ? 'n/a' : `${Math.round(value * 100) / 100} ms`;
}

function metricValue(metrics, metricName, valueName) {
  const metric = metrics[metricName];
  if (!metric || !metric.values) {
    return null;
  }
  return metric.values[valueName];
}
