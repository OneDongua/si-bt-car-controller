require "import"

local src = ...
local _, data = loadstring(src)
if data then
    local _, _, line, data = data:find(".(%d+).(.+)")
    print("第" .. line .. "行:" .. data)
    return true
else
    print("没有语法错误")
end