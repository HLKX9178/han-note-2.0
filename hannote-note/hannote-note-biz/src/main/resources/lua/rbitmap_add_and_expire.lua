-- Roaring Bitmap：单条置位并设置过期时间
-- KEYS[1] Key / ARGV[1] 笔记 ID / ARGV[2] 过期秒数
local key = KEYS[1]
redis.call('R64.SETBIT', key, ARGV[1], 1)
redis.call('EXPIRE', key, ARGV[2])
return 0
