-- 布隆过滤器：单条加入并设置过期时间
-- KEYS[1] 布隆过滤器 Key
-- ARGV[1] 笔记 ID
-- ARGV[2] 过期时间（秒）
local key = KEYS[1]
redis.call('BF.ADD', key, ARGV[1])
redis.call('EXPIRE', key, ARGV[2])
return 0
