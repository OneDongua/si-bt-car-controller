require "import"
import "java.lang.String"

GO_END = "C\r"
LEFT_END = "L\r"
RIGHT_END = "R\r"

ble = activity.getBLE()

function repeatStr(str, times)
    local result = ""
    for i = 1, times do
        result = result .. str
    end
    return result
end
function go(times)
    ble.send(String(repeatStr("w", times)).getBytes())
end
function left(times)
    ble.send(String(repeatStr("a", times)).getBytes())
end
function right(times)
    ble.send(String(repeatStr("d", times)).getBytes())
end
function back(times)
    ble.send(String(repeatStr("s", times)).getBytes())
end
function goUntilEnd()
    ble.send(String("c").getBytes())
end
function turnLeft()
    ble.send(String("l").getBytes())
end
function turnRight()
    ble.send(String("r").getBytes())
end