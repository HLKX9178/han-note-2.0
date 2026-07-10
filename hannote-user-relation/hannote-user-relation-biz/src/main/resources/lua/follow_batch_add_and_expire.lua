-- 批量回源关注关系并设置过期时间
-- 适用场景：ZSET 关注列表不存在，但数据库中该用户已有关注记录，需全量同步到 Redis
-- KEYS[1]              : 关注列表 ZSET 的 Redis Key
-- ARGV[1..N-1]         : 成对的 (score, member)，即 (关注时间戳, 关注的用户ID) 依次排列
-- ARGV[N]（最后一个）   : 过期时间（秒）
-- 返回：0 成功

local key = KEYS[1]

-- 收集所有 (score, member) 参数，最后一个 ARGV 是过期时间，不纳入
local zaddArgs = {}
for i = 1, #ARGV - 1, 2 do
    table.insert(zaddArgs, ARGV[i])      -- score（关注时间戳）
    table.insert(zaddArgs, ARGV[i + 1])  -- member（关注的用户 ID）
end

-- 批量 ZADD 一次性写入
redis.call('ZADD', key, unpack(zaddArgs))

-- 最后一个参数为过期时间
local expireSeconds = ARGV[#ARGV]
redis.call('EXPIRE', key, expireSeconds)
return 0
