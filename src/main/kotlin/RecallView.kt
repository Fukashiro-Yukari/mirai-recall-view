package io.github.kamishirokalina

import net.mamoe.mirai.console.plugin.jvm.JvmPluginDescription
import net.mamoe.mirai.console.plugin.jvm.KotlinPlugin
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.event.GlobalEventChannel
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.event.events.MessageRecallEvent
import net.mamoe.mirai.message.action.Nudge.Companion.sendNudge
import net.mamoe.mirai.message.data.MessageChain
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.source
import net.mamoe.mirai.utils.info

object RecallView : KotlinPlugin(
    JvmPluginDescription(
        id = "io.github.kamishirokalina.recallview",
        name = "Recall View",
        version = "1.0",
    ) {
        author("Kamishiro Kalina")
        info("""查看群员撤回的消息""")
    }
) {
    private val groupMsgs = mutableMapOf<Long, MutableMap<Int, MessageChain>>()
    private val recallMsgs = mutableMapOf<Long, MutableList<MessageChain>>()

    override fun onEnable() {
        logger.info { "查看撤回消息插件加载中..." }

        val event = GlobalEventChannel.parentScope(this)

        event.subscribeAlways<GroupMessageEvent> {
            val cmd = message.contentToString().lowercase().split(" ")

            if (!groupMsgs.containsKey(group.id))
                groupMsgs[group.id] = mutableMapOf()

            if (cmd[0] == "/last" || cmd[0] == "/查看撤回") {
                if (cmd.size < 2) {
                    if (recallMsgs.containsKey(group.id) && recallMsgs[group.id]!!.isNotEmpty()) {
                        var msg = recallMsgs[group.id]!!.removeAt(recallMsgs[group.id]!!.size - 1)
                        val reCaller = group[msg.source.fromId]

                        if (reCaller != null) {
                            msg += PlainText("\n-- ${reCaller.nameCardOrNick} (${msg.source.fromId})")

                            group.sendMessage(msg)
                        } else
                            group.sendMessage("发送失败: 消息出现错误")
                    } else
                        group.sendMessage("查找失败: 没有找到相关的撤回记录")

                    return@subscribeAlways
                }

                var qq = cmd[1].toLongOrNull()

                if (qq == null) {
                    qq = cmd[1].substring(1).toLongOrNull()

                    if (qq == null) {
                        group.sendMessage("参数错误: 请输入正确的QQ号或At对象")

                        return@subscribeAlways
                    }
                }

                var rtime = 0
                var rmsg: MessageChain? = null
                var rkey: Int? = null

                for (i in 0 until recallMsgs[group.id]!!.size){
                    val msg = recallMsgs[group.id]!![i]

                    if (qq != msg.source.fromId) continue
                    if (msg.source.time > rtime){
                        rtime = msg.source.time
                        rmsg = msg
                        rkey = i
                    }
                }

                if (rmsg != null && rkey != null){
                    val reCaller = group[rmsg.source.fromId]

                    if (reCaller != null){
                        rmsg += PlainText("\n-- ${reCaller.nameCardOrNick} (${rmsg.source.fromId})")

                        group.sendMessage(rmsg)
                        recallMsgs[group.id]!!.removeAt(rkey)
                    }
                    else
                        group.sendMessage("发送失败: 消息出现错误")
                }
                else
                    group.sendMessage("查找失败: 没有找到相关的撤回记录")

                return@subscribeAlways
            }

            groupMsgs[group.id]!![time] = message
        }

        event.subscribeAlways<MessageRecallEvent.GroupRecall> {
            if (!groupMsgs.containsKey(group.id)) return@subscribeAlways

            val msg = groupMsgs[group.id]!!.remove(messageTime)

            if (msg != null) {
                if (!recallMsgs.containsKey(group.id))
                    recallMsgs[group.id] = mutableListOf()

                recallMsgs[group.id]!!.add(msg)
                group.sendMessage("输入 /last 可以查看撤回内容")
            }
        }
    }
}