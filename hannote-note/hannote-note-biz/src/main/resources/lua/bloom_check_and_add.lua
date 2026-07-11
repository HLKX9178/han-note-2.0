-- 布隆过滤器：check-and-add（点赞 / 收藏共用）
-- KEYS[1] 布隆过滤器 Key
-- ARGV[1] 笔记 ID
-- 返回：-1 布隆过滤器不存在 / 1 已存在（可能误判，需二次校验） / 0 新增成功
local key = KEYS[1]
local noteId = ARGV[1]

if redis.call('EXISTS', key) == 0 then
    return -1
end

if redis.call('BF.EXISTS', key, noteId) == 1 then
    return 1
end

redis.call('BF.ADD', key, noteId)
return 0
