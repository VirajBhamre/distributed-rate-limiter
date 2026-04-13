import http from "k6/http";
import { check, sleep } from "k6";
import { Trend, Counter } from "k6/metrics";

// Many VUs hit one IP and one user; 429 is expected. k6 forbids http.* inside SharedArray/init — use setup() instead.
const BASE_URL = __ENV.BASE_URL || "http://127.0.0.1:8090";
const USERNAME = __ENV.USERNAME || "cacheuser";
const PASSWORD = __ENV.PASSWORD || "passwordpassword";

// Metrics
const cacheLatency = new Trend("cache_latency");
const status200 = new Counter("status_200");
const status429 = new Counter("status_429");

function postJson(url, body, tags) {
  return http.post(url, JSON.stringify(body), {
    headers: { "Content-Type": "application/json" },
    tags,
  });
}

/** Runs once before VUs; HTTP is allowed here (unlike SharedArray / init). */
export function setup() {
  const maxAttempts = 15;
  let register;
  for (let i = 0; i < maxAttempts; i++) {
    register = postJson(
      `${BASE_URL}/auth/register`,
      { username: USERNAME, email: `${USERNAME}@example.com`, password: PASSWORD },
      { name: "AuthRegisterSetup" },
    );
    if (register.status === 201 || register.status === 409) {
      break;
    }
    if (register.status === 429) {
      sleep(1);
      continue;
    }
    throw new Error(`setup register failed: ${register.status} ${register.body}`);
  }
  if (register.status !== 201 && register.status !== 409) {
    throw new Error(`setup register failed after retries: ${register.status} ${register.body}`);
  }

  let login;
  for (let i = 0; i < maxAttempts; i++) {
    login = postJson(
      `${BASE_URL}/auth/login`,
      { username: USERNAME, password: PASSWORD },
      { name: "AuthLoginSetup" },
    );
    if (login.status === 200) {
      break;
    }
    if (login.status === 429) {
      sleep(1);
      continue;
    }
    throw new Error(`setup login failed: ${login.status} ${login.body}`);
  }
  if (login.status !== 200) {
    throw new Error(`setup login failed after retries: ${login.status} ${login.body}`);
  }

  return { token: login.json("data.token") };
}

export const options = {
  vus: 20,
  duration: "45s",
  thresholds: {
    checks: ["rate>0.95"],
  },
};

export default function (data) {
  const token = data.token;

  const res = http.get(`${BASE_URL}/api/users/${USERNAME}`, {
    headers: { Authorization: `Bearer ${token}` },
    tags: { name: "UserProfileCache" },
  });

  // Metrics
  cacheLatency.add(res.timings.duration);

  if (res.status === 200) status200.add(1);
  else if (res.status === 429) status429.add(1);

  check(res, {
    "profile 200 or 429": (r) => r.status === 200 || r.status === 429,
  });

  sleep(0.1); // realistic load, not DDoS
}