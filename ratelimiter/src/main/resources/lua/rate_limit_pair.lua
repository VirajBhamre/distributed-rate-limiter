-- Atomic token bucket + sliding window for one dimension.
-- KEYS[1] = token bucket hash key (rl:tb:...)
-- KEYS[2] = sliding window zset key (rl:sw:...)
-- ARGV[1] = capacity (number)
-- ARGV[2] = refill_per_second (number)
-- ARGV[3] = now_ms (number)
-- ARGV[4] = cost (number, usually 1)
-- ARGV[5] = sw_window_ms (number)
-- ARGV[6] = sw_max (number)
-- ARGV[7] = sw_member (string, unique per request)

local tb_key = KEYS[1]
local sw_key = KEYS[2]

local capacity = tonumber(ARGV[1])
local refill_per_sec = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local cost = tonumber(ARGV[4])
local sw_window = tonumber(ARGV[5])
local sw_max = tonumber(ARGV[6])
local sw_member = ARGV[7]

-- Token bucket on hash: tokens, last_ts
local tokens_s = redis.call('HGET', tb_key, 'tokens')
local last_ts_s = redis.call('HGET', tb_key, 'last_ts')

local tokens
local last_ts
if tokens_s == false or tokens_s == nil then
  tokens = capacity
  last_ts = now
else
  tokens = tonumber(tokens_s)
  last_ts = tonumber(last_ts_s)
end

local elapsed_ms = math.max(0, now - last_ts)
local elapsed_sec = elapsed_ms / 1000.0
tokens = math.min(capacity, tokens + (elapsed_sec * refill_per_sec))

if tokens < cost then
  return 0
end

tokens = tokens - cost
redis.call('HSET', tb_key, 'tokens', tostring(tokens), 'last_ts', tostring(now))
redis.call('EXPIRE', tb_key, 3600)

-- Sliding window log
redis.call('ZREMRANGEBYSCORE', sw_key, 0, now - sw_window)
local c = redis.call('ZCARD', sw_key)
if c >= sw_max then
  -- refund token bucket
  local t2_s = redis.call('HGET', tb_key, 'tokens')
  local t2 = tonumber(t2_s)
  t2 = math.min(capacity, t2 + cost)
  redis.call('HSET', tb_key, 'tokens', tostring(t2))
  return 0
end

redis.call('ZADD', sw_key, now, sw_member)
redis.call('EXPIRE', sw_key, math.ceil(sw_window / 1000) + 2)

return 1
