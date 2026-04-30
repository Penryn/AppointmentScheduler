import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const PASSWORD = __ENV.K6_PASSWORD || 'qwerty123';

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
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1000'],
    checks: ['rate>0.99'],
    'http_req_failed{name:customer_r:login-submit}': ['rate<0.01'],
    'http_req_failed{name:provider:login-submit}': ['rate<0.01'],
    'http_req_failed{name:admin:login-submit}': ['rate<0.01'],
  },
};

export function setup() {
  const health = get(`${BASE_URL}/actuator/health`, 'actuator:health');
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

    assertPage(get(`${BASE_URL}/login`, 'anonymous:login-page'), 'login page', ['id="login-form"']);
    assertPage(get(`${BASE_URL}/customers/new/retail`, 'anonymous:retail-registration'), 'retail registration page', ['name="username"']);
    assertPage(get(`${BASE_URL}/customers/new/corporate`, 'anonymous:corporate-registration'), 'corporate registration page', ['name="username"']);

    const assets = http.batch([
      ['GET', `${BASE_URL}/css/style.css`, null, requestOptions('asset:style.css')],
      ['GET', `${BASE_URL}/webjars/bootstrap/5.3.8/css/bootstrap.min.css`, null, requestOptions('asset:bootstrap.css')],
      ['GET', `${BASE_URL}/actuator/info`, null, requestOptions('actuator:info')],
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

    assertPage(get(`${BASE_URL}/`, 'customer:home'), 'customer home page', ['id="calendar"']);
    assertPage(get(`${BASE_URL}/appointments/all`, 'customer:appointments'), 'customer appointments page', ['id="appointments"']);
    assertPage(get(`${BASE_URL}/appointments/all?status=CREATED&page=0&size=10`, 'customer:appointments-filtered'), 'filtered customer appointments page', ['id="appointments"']);
    assertPage(get(`${BASE_URL}/customers/${IDS.customer}`, 'customer:profile'), 'customer profile page', ['id="profile"']);
    assertPage(get(`${BASE_URL}/notifications`, 'customer:notifications'), 'customer notifications page', ['id="notifications"']);

    assertPage(get(`${BASE_URL}/appointments/new`, 'customer:appointment-new'), 'appointment provider selection page', ['选择']);
    assertPage(get(`${BASE_URL}/appointments/new/${IDS.provider}`, 'customer:appointment-provider'), 'appointment service selection page', ['id="customers"']);
    assertPage(get(`${BASE_URL}/appointments/new/${IDS.provider}/${IDS.work}`, 'customer:appointment-work'), 'appointment date selection page', ['id="calendar"']);

    const date = nextIsoDate(2);
    const availableHours = get(`${BASE_URL}/api/availableHours/${IDS.provider}/${IDS.work}/${date}`, 'customer:available-hours-api');
    check(availableHours, {
      'available hours api is available': (response) => response.status === 200,
      'available hours api returns json': (response) => String(response.headers['Content-Type'] || '').includes('application/json'),
    });

    const userAppointments = get(`${BASE_URL}/api/user/${IDS.customer}/appointments?${calendarWindowQuery()}`, 'customer:calendar-api');
    check(userAppointments, {
      'customer calendar api is available': (response) => response.status === 200,
      'customer calendar api returns json': (response) => String(response.headers['Content-Type'] || '').includes('application/json'),
    });

    const notifications = get(`${BASE_URL}/api/user/notifications`, 'customer:notifications-api');
    check(notifications, {
      'customer notifications api is available': (response) => response.status === 200,
    });
  });
}

function providerFlow() {
  group('provider dashboard flow', () => {
    loginAs(USERS.provider);

    assertPage(get(`${BASE_URL}/`, 'provider:home'), 'provider home page', ['id="calendar"']);
    assertPage(get(`${BASE_URL}/appointments/all`, 'provider:appointments'), 'provider appointments page', ['id="appointments"']);
    assertPage(get(`${BASE_URL}/appointments/all?status=CREATED&page=0&size=10`, 'provider:appointments-filtered'), 'filtered provider appointments page', ['id="appointments"']);
    assertPage(get(`${BASE_URL}/providers/${IDS.provider}`, 'provider:profile'), 'provider profile page', ['id="profile"']);
    assertPage(get(`${BASE_URL}/providers/availability`, 'provider:availability'), 'provider availability page', ['name="monday.workingHours.start"']);

    const providerAppointments = get(`${BASE_URL}/api/user/${IDS.provider}/appointments?${calendarWindowQuery()}`, 'provider:calendar-api');
    check(providerAppointments, {
      'provider calendar api is available': (response) => response.status === 200,
      'provider calendar api returns json': (response) => String(response.headers['Content-Type'] || '').includes('application/json'),
    });
  });
}

function adminFlow() {
  group('admin management flow', () => {
    loginAs(USERS.admin);

    assertPage(get(`${BASE_URL}/`, 'admin:home'), 'admin home page', ['id="calendar"']);
    assertPage(get(`${BASE_URL}/appointments/all`, 'admin:appointments'), 'admin appointments page', ['id="appointments"']);
    assertPage(get(`${BASE_URL}/customers/all`, 'admin:customers'), 'admin customers page', ['id="customers"']);
    assertPage(get(`${BASE_URL}/providers/all`, 'admin:providers'), 'admin providers page', ['id="providers"']);
    assertPage(get(`${BASE_URL}/works/all`, 'admin:works'), 'admin works page', ['id="works"']);
    assertPage(get(`${BASE_URL}/invoices/all`, 'admin:invoices'), 'admin invoices page', ['发票']);
    assertPage(get(`${BASE_URL}/customers/${IDS.customer}`, 'admin:customer-detail'), 'admin customer detail page', ['id="profile"']);
    assertPage(get(`${BASE_URL}/providers/${IDS.provider}`, 'admin:provider-detail'), 'admin provider detail page', ['id="profile"']);
    assertPage(get(`${BASE_URL}/works/${IDS.work}`, 'admin:work-detail'), 'admin work detail page', ['name="name"']);
  });
}

function loginAs(username) {
  http.cookieJar().clear(BASE_URL);

  const loginPage = get(`${BASE_URL}/login`, `${username}:login-page`);
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
    redirects: 1,
  }, `${username}:login-submit`);
  check(login, {
    [`${username} login succeeds`]: (response) => response.status === 200 && !bodyOf(response).includes('id="login-form"'),
    [`${username} login does not return error page`]: (response) => !String(response.url || '').includes('error') && !bodyOf(response).includes('用户名或密码错误'),
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

function requestOptions(name) {
  return {
    tags: requestTags(name),
  };
}

function requestTags(name) {
  return {
    name,
  };
}

function get(url, name) {
  const response = http.get(url, requestOptions(name));
  recordStatus(response);
  return response;
}

function post(url, payload, options, name) {
  const requestConfig = options || {};
  requestConfig.tags = Object.assign({}, requestConfig.tags || {}, requestTags(name));
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

  return [
    '',
    'k6 summary',
    `  http_reqs: ${formatNumber(httpRequests)}`,
    `  http_req_failed: ${formatPercent(failedRate)}`,
    `  checks: ${formatPercent(checkRate)}`,
    `  http_req_duration p95: ${formatMs(p95)}`,
    '',
  ].join('\n');
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
