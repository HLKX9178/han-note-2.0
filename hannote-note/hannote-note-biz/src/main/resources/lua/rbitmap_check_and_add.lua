-- Roaring Bitmap：check-and-add（点赞 / 收藏共用，KEY 不同）
-- KEYS[1] Roaring Bitmap Key
-- ARGV[1] 笔记 ID（作为 bit 偏移量）
-- 返回：-1 位图不存在 / 1 已置位（精确命中已点赞/已收藏） / 0 本次置位成功
local key = KEYS[1]
local noteId = ARGV[1]

if redis.call('EXISTS', key) == 0 then
    return -1
end

if redis.call('R64.GETBIT', key, noteId) == 1 then
    return 1
end

redis.call('R64.SETBIT', key, noteId, 1)
return 0
