require "import"
require "car"

--[[
lua语法介绍：
  赋值：a = 1
       b = "abc"

  定义函数：function abc(参数)
            print(参数)
          end

  调用函数：abc(参数)

  输出语句：print("abc")

  注释：--这是注释

  循环语句：for i = 1, 10 do
         print(i)
       end

  判断语句：if 条件 then

           elseif 条件 then

           else

          end

预留函数：
  go(times) -- 前进，times为前进次数
  left(times) -- 左转，times为左转次数
  right(times) -- 右转，times为右转次数
  back(times) -- 后退，times为后退次数
  -- 以上函数一次为60毫秒

  goUntilEnd() -- 前进直到路径结束，会发送GO_END
  turnLeft() -- 左转到下一路径，会发送LEFT_END
  turnRight() -- 右转到下一路径，会发送RIGHT_END

示例代码：
  function onCarReceive(msg)
    if msg == GO_END then
      print("前进结束")
     elseif msg == LEFT_END then
      print("左转结束")
     elseif msg == RIGHT_END then
      print("右转结束")
    end
  end

界面介绍：右上角第一个：检查错误
        第二个：格式化代码（使代码更整洁）
        第三个：运行（需要管理员确认）
]]

-- 当接收到消息时调用该函数，msg为接收到的消息
function onCarReceive(msg)

end
