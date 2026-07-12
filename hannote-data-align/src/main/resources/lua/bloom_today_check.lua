-- 日增量布隆过滤器判重脚本（通用于全部日增量布隆）
-- KEYS[1] = 布隆过滤器 Key（按日创建）
-- ARGV[1] = 待校验的值（userId 或 noteId）
-- 返回：1 = 已存在（可能误判为存在），0 = 一定不存在（判定绝对正确）

local key = KEYS[1]
local value = ARGV[1]

-- 布隆过滤器不存在时先初始化并设置过期时间（约一天，20 小时）
local exists = redis.call('EXISTS', key)
if exists == 0 then
    redis.call('BF.ADD', key, '')
    redis.call('EXPIRE', key, 20 * 60 * 60)
end

-- 校验该值是否已存在
return redis.call('BF.EXISTS', key, value)
