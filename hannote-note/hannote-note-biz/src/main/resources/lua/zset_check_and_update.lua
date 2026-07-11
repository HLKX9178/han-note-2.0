-- ZSET 列表：校验并更新（点赞 / 收藏共用，上限作运行时参数）
-- KEYS[1] ZSET Key
-- ARGV[1] 笔记 ID（member）
-- ARGV[2] 时间戳（score）
-- ARGV[3] ZSET 上限（点赞 100 / 收藏 300）
-- 返回：-1 ZSET 不存在（需回源初始化） / 0 更新成功
local key = KEYS[1]
local noteId = ARGV[1]
local timestamp = ARGV[2]
local maxSize = tonumber(ARGV[3])

if redis.call('EXISTS', key) == 0 then
    return -1
end

-- 已达上限则移除最早的一条（score 最小）
if redis.call('ZCARD', key) >= maxSize then
    redis.call('ZPOPMIN', key)
end

redis.call('ZADD', key, timestamp, noteId)
return 0
