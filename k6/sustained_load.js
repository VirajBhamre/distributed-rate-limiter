import http from "k6/http";
import { check, sleep } from "k6";
import { SharedArray } from "k6/data";
import { Counter } from "k6/metrics";

const BASE_URL = __ENV.BASE_URL || "http://127.0.0.1:8090";

// Pool size: keep moderate so setup can finish under per-IP rate limits (sequential register from one IP).
const USER_COUNT = parseInt(__ENV.SUSTAINED_USER_COUNT || "500", 10);
const SUSTAINED_VUS = parseInt(__ENV.SUSTAINED_VUS || "400", 10);
const SUSTAINED_ITERATIONS = parseInt(__ENV.SUSTAINED_ITERATIONS || "80000", 10);

const status200 = new Counter("status_200");
const status429 = new Counter("status_429");
const status500 = new Counter("status_500");

function buildUsers(n) {
  const arr = [];
  for (let i = 0; i < n; i++) {
    arr.push({
      username: `sustained_user_${i}`,
      email: `sustained_user_${i}@example.com`,
      password: "passwordpassword",
    });
  }
  return arr;
}

const users = new SharedArray("users", () => buildUsers(USER_COUNT));

// Default k6 setup timeout is 60s — throttled register of hundreds of users needs longer.
const SETUP_TIMEOUT = __ENV.SUSTAINED_SETUP_TIMEOUT || "25m";

export const options = {
  setupTimeout: SETUP_TIMEOUT,
  scenarios: {
    heavy_load: {
      executor: "shared-iterations",
      vus: SUSTAINED_VUS,
      iterations: SUSTAINED_ITERATIONS,
      maxDuration: "15m",
    },
  },
  thresholds: {
    checks: ["rate>0.95"],
  },
};

function registerUser(user) {
  return http.post(`${BASE_URL}/auth/register`, JSON.stringify(user), {
    headers: { "Content-Type": "application/json" },
    tags: { name: "SetupRegister" },
  });
}

/**
 * Register every user from one client IP — must stay under IP rate limits or logins return 401.
 * Pace successes + retry on 429 (same pattern as profile_cache setup).
 */
export function setup() {
  const maxAttempts = 40;
  for (const user of users) {
    let lastStatus = 0;
    let ok = false;
    for (let a = 0; a < maxAttempts; a++) {
      const r = registerUser(user);
      lastStatus = r.status;
      if (r.status === 201 || r.status === 409) {
        ok = true;
        sleep(0.12);
        break;
      }
      if (r.status === 429) {
        sleep(1);
        continue;
      }
      throw new Error(`setup register failed for ${user.username}: ${r.status} ${r.body}`);
    }
    if (!ok) {
      throw new Error(
        `setup register exhausted retries for ${user.username} (last status ${lastStatus})`,
      );
    }
  }
  return { userCount: users.length };
}

export default function () {
  const user = users[__VU % users.length];

  const login = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({
      username: user.username,
      password: user.password,
    }),
    {
      headers: { "Content-Type": "application/json" },
      tags: { name: "Login" },
    },
  );

  if (login.status === 429) {
    status429.add(1);
  } else if (login.status >= 500) {
    status500.add(1);
  }

  check(login, {
    "login 200 or 429": (r) => r.status === 200 || r.status === 429,
  });

  if (login.status === 200) {
    const token = login.json("data.token");

    const res = http.get(`${BASE_URL}/api/users/me`, {
      headers: { Authorization: `Bearer ${token}` },
      tags: { name: "Profile" },
    });

    if (res.status === 200) {
      status200.add(1);
    } else if (res.status === 429) {
      status429.add(1);
    } else if (res.status >= 500) {
      status500.add(1);
    }

    check(res, {
      "profile 200 or 429": (r) => r.status === 200 || r.status === 429,
    });
  }

  sleep(0.05);
}
