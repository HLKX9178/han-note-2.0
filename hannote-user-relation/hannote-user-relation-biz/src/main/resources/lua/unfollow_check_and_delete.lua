-- LUA 脚本：校验并移除关注关系
-- 返回值：-1 ZSET 不存在（可能已过期，需回源）；-4 未关注该用户；0 取关成功

local key = KEYS[1]              -- 当前用户关注列表 ZSET Key
local unfollowUserId = ARGV[1]   -- 被取关的用户 ID

-- ZSET 不存在：无法在缓存判定，交由调用方回源 DB
if redis.call('EXISTS', key) == 0 then
    return -1
end

-- 校验目标用户是否在关注列表中
local score = redis.call('ZSCORE', key, unfollowUserId)
if score == false or score == nil then
    return -4
end

-- 移除关注关系
redis.call('ZREM', key, unfollowUserId)
return 0
