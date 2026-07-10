-- 校验并添加关注关系（原子操作）
-- KEYS[1] : 关注列表 ZSET 的 Redis Key
-- ARGV[1] : 关注的用户 ID（作为 ZSET member）
-- ARGV[2] : 时间戳（作为 ZSET score，用于按关注时间排序）
-- ARGV[3] : 关注上限（由业务配置传入，不在脚本内硬编码）
-- 返回：-1 ZSET 不存在 / -2 已达关注上限 / -3 已关注该用户 / 0 关注成功

local key = KEYS[1]
local followUserId = ARGV[1]
local timestamp = ARGV[2]
local maxLimit = tonumber(ARGV[3])

-- 1. ZSET 不存在，交由上层回源同步后重试
local exists = redis.call('EXISTS', key)
if exists == 0 then
    return -1
end

-- 2. 校验关注数是否达到上限
local size = redis.call('ZCARD', key)
if size >= maxLimit then
    return -2
end

-- 3. 校验是否已关注该用户（ZSCORE 非 nil 即已存在）
if redis.call('ZSCORE', key, followUserId) then
    return -3
end

-- 4. 校验通过，添加关注关系
redis.call('ZADD', key, timestamp, followUserId)
return 0
