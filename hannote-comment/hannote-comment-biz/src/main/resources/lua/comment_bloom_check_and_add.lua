local key = KEYS[1]
local commentId = ARGV[1]
if redis.call('EXISTS', key) == 0 then
    return -1
end
if redis.call('BF.EXISTS', key, commentId) == 1 then
    return 1
end
redis.call('BF.ADD', key, commentId)
return 0
