-- 布隆过滤器：存在性校验（取消点赞 / 取消收藏共用）
-- KEYS[1] 布隆过滤器 Key
-- ARGV[1] 笔记 ID
-- 返回：-1 布隆过滤器不存在 / 1 已存在（可能误判） / 0 不存在（判断绝对正确）
local key = KEYS[1]
local noteId = ARGV[1]

if redis.call('EXISTS', key) == 0 then
    return -1
end

return redis.call('BF.EXISTS', key, noteId)
