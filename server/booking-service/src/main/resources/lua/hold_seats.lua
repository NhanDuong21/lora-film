for i, key in ipairs(KEYS) do
    if redis.call('EXISTS', key) == 1 then
        return 0
    end
end
for i, key in ipairs(KEYS) do
    redis.call('SET', key, ARGV[1], 'EX', tonumber(ARGV[2]))
end
return 1
