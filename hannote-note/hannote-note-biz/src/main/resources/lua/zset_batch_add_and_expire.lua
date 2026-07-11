-- ZSET 列表：批量回源初始化并设置过期时间（点赞 / 收藏共用）
-- KEYS[1] ZSET Key
-- ARGV 依次为 score1, member1, score2, member2, ..., 末位为过期时间（秒）
local key = KEYS[1]

local zaddArgs = {}
for i = 1, #ARGV - 1, 2 do
    table.insert(zaddArgs, ARGV[i])       -- score（时间戳）
    table.insert(zaddArgs, ARGV[i + 1])   -- member（笔记 ID）
end

redis.call('ZADD', key, unpack(zaddArgs))
redis.call('EXPIRE', key, ARGV[#ARGV])
return 0
