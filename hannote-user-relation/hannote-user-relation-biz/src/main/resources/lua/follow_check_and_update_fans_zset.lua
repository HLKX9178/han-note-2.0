-- 校验并更新粉丝 ZSET（增量维护，不负责初始化）
-- KEYS[1] : 被关注用户的粉丝列表 ZSET Key
-- ARGV[1] : 粉丝的用户 ID（作为 ZSET member）
-- ARGV[2] : 时间戳（作为 ZSET score）
-- ARGV[3] : 粉丝 ZSET 最大缓存数量（由业务配置传入）
-- 返回：-1 ZSET 不存在（未初始化，跳过） / 0 成功

local key = KEYS[1]
local fansUserId = ARGV[1]
local timestamp = ARGV[2]
local maxCacheCount = tonumber(ARGV[3])

-- ZSET 不存在：粉丝列表尚未被访问初始化，无需增量维护
local exists = redis.call('EXISTS', key)
if exists == 0 then
    return -1
end

-- 达到缓存上限：淘汰最早关注的粉丝（score 最小）
local size = redis.call('ZCARD', key)
if size >= maxCacheCount then
    redis.call('ZPOPMIN', key)
end

-- 添加新粉丝关系
redis.call('ZADD', key, timestamp, fansUserId)
return 0
