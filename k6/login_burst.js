import http from "k6/http";
import { check, sleep } from "k6";

// All VUs share one client IP → IP scope in Redis rate limit (e.g. 30/10s + token bucket).
// A burst of register calls is expected to return many 429s; that is a successful outcome for this script.
const BASE_URL = __ENV.BASE_URL || "http://127.0.0.1:8090";

export const options = {
  stages: [
    { duration: "10s", target: 50 },
    { duration: "20s", target: 200 },
    { duration: "10s", target: 0 },
  ],
  thresholds: {
    // Do not use http_req_failed here: k6 treats 4xx (including 429) as failed requests by default.
    checks: ["rate>0.95"],
  },
};

export default function () {
  const payload = JSON.stringify({
    username: `u_${__VU}_${__ITER}`,
    email: `u_${__VU}_${__ITER}@example.com`,
    password: "passwordpassword",
  });
  const res = http.post(`${BASE_URL}/auth/register`, payload, {
    headers: { "Content-Type": "application/json" },
    tags: { name: "AuthRegisterBurst" },
  });
  check(res, {
    "register 201, 409, or 429": (r) =>
      r.status === 201 || r.status === 409 || r.status === 429,
  });
  sleep(0.05);
}
