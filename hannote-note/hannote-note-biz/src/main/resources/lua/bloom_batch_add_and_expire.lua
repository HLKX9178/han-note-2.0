-- 布隆过滤器：全量批量加入并设置过期时间（回源用）
-- KEYS[1] 布隆过滤器 Key
-- ARGV[1..n-1] 笔记 ID 列表
-- ARGV[n] 过期时间（秒，末位）
local key = KEYS[1]
for i = 1, #ARGV - 1 do
    redis.call('BF.ADD', key, ARGV[i])
end
redis.call('EXPIRE', key, ARGV[#ARGV])
return 0
