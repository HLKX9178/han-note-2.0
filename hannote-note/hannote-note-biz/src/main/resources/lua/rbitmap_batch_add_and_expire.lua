-- Roaring Bitmap：全量批量置位并设置过期时间（回源 DB 用）
-- KEYS[1] Key / ARGV[1..n-1] 笔记 ID 列表 / ARGV[n] 过期秒数（末位）
local key = KEYS[1]
for i = 1, #ARGV - 1 do
    redis.call('R64.SETBIT', key, ARGV[i], 1)
end
redis.call('EXPIRE', key, ARGV[#ARGV])
return 0
