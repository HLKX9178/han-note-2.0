-- Roaring Bitmap：仅校验是否置位（不写入），供 isLikedAndCollected 使用
-- KEYS[1] Key / ARGV[1] 笔记 ID
-- 返回：-1 位图不存在 / 1 已置位 / 0 未置位
local key = KEYS[1]
local noteId = ARGV[1]

if redis.call('EXISTS', key) == 0 then
    return -1
end

return redis.call('R64.GETBIT', key, noteId)
