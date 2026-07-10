-- 添加首条关注关系并设置过期时间
-- 适用场景：ZSET 关注列表不存在，且数据库中该用户暂无任何关注记录
-- KEYS[1] : 关注列表 ZSET 的 Redis Key
-- ARGV[1] : 关注的用户 ID（作为 ZSET member）
-- ARGV[2] : 时间戳（作为 ZSET score）
-- ARGV[3] : 过期时间（秒）
-- 返回：0 成功

local key = KEYS[1]
local followUserId = ARGV[1]
local timestamp = ARGV[2]
local expireSeconds = ARGV[3]

-- ZADD 添加关注关系
redis.call('ZADD', key, timestamp, followUserId)
-- 设置过期时间，防止 ZSET 长期占用内存
redis.call('EXPIRE', key, expireSeconds)
return 0
