-- 将值加入日增量布隆过滤器（落库成功后调用）
-- KEYS[1] = 布隆过滤器 Key
-- ARGV[1] = 待加入的值（userId 或 noteId）
return redis.call('BF.ADD', KEYS[1], ARGV[1])
