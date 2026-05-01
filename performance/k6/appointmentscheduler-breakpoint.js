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

const BREAKPOINT = {
  preAllocatedVus: Number(__ENV.K6_PRE_ALLOCATED_VUS || 50),
  maxVus: Number(__ENV.K6_MAX_VUS || 250),
  startRate: Number(__ENV.K6_START_RATE || 5),
  timeUnit: __ENV.K6_RATE_TIME_UNIT || '1s',
  stageDuration: __ENV.K6_STAGE_DURATION || '2m',
  cooldownDuration: __ENV.K6_COOLDOWN_DURATION || '1m',
  targets: numberListFromEnv('K6_BREAKPOINT_RATES', [10, 20, 40, 60, 80, 100, 125, 150]),
};

export { handleSummary, setup };

export const options = {
  scenarios: {
    breakpoint: {
      executor: 'ramping-arrival-rate',
      startRate: BREAKPOINT.startRate,
      timeUnit: BREAKPOINT.timeUnit,
      preAllocatedVUs: BREAKPOINT.preAllocatedVus,
      maxVUs: BREAKPOINT.maxVus,
      stages: BREAKPOINT.targets
        .map((target) => ({ duration: BREAKPOINT.stageDuration, target }))
        .concat([{ duration: BREAKPOINT.cooldownDuration, target: 0 }]),
    },
  },
  thresholds: {
    // Breakpoint runs should complete and report degradation instead of stopping at the first bad stage.
    http_req_duration: ['p(95)<5000'],
    http_req_failed: ['rate<0.20'],
    checks: ['rate>0.80'],
  },
};

export default function () {
  const bucket = (__VU + __ITER) % 100;

  if (bucket < 60) {
    customerFlow();
    thinkTime(1, 3);
    return;
  }

  if (bucket < 75) {
    providerFlow();
    thinkTime(1, 3);
    return;
  }

  if (bucket < 90) {
    anonymousFlow();
    thinkTime(1, 2);
    return;
  }

  if (bucket < 97) {
    customerWriteFlow();
    thinkTime(2, 5);
    return;
  }

  adminFlow();
  thinkTime(2, 4);
}

function numberListFromEnv(name, defaults) {
  const value = __ENV[name];
  if (!value) {
    return defaults;
  }

  const values = value
    .split(',')
    .map((item) => Number(item.trim()))
    .filter((item) => !Number.isNaN(item) && item > 0);

  return values.length > 0 ? values : defaults;
}
