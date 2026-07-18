-- Roaring Bitmap：校验并清位（取消点赞 / 取消收藏共用）
-- KEYS[1] Roaring Bitmap Key
-- ARGV[1] 笔记 ID
-- 返回：-1 位图不存在 / 0 未置位（精确命中未点赞/未收藏） / 1 原已置位（本次已清 0）
local key = KEYS[1]
local noteId = ARGV[1]

if redis.call('EXISTS', key) == 0 then
    return -1
end

if redis.call('R64.GETBIT', key, noteId) == 0 then
    return 0
end

redis.call('R64.SETBIT', key, noteId, 0)
return 1
