local releasedCount = 0
for i, key in ipairs(KEYS) do
    if redis.call('EXISTS', key) == 1 then
        local currentToken = redis.call('GET', key)
        if currentToken == ARGV[1] then
            redis.call('DEL', key)
            releasedCount = releasedCount + 1
        end
    end
end
return releasedCount
