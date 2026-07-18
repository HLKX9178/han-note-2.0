-- Roaring Bitmap：批量校验一批笔记是否被点赞（R64 位宽，只读不写）
-- KEYS[1] 用户笔记点赞位图 Key / ARGV[1..n] 笔记 ID 列表
-- 返回：位图不存在返回 {-1}；否则返回与 ARGV 等长的数组，元素 1 已点赞 / 0 未点赞
local key = KEYS[1]
local results = {}

-- 位图不存在（未初始化或已过期）：返回 {-1} 标识，让调用方回源 DB 判定
if redis.call('EXISTS', key) == 0 then
    results[1] = -1
    return results
end

-- 逐个笔记 ID 取位
for i = 1, #ARGV do
    results[i] = redis.call('R64.GETBIT', key, ARGV[i])
end

return results
