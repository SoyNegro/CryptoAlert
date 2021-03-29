package handler


import alert.DailyAlert
import alert.UserAlert
import api.*
import configurer.BotCommand
import kotlinx.coroutines.runBlocking
import org.litote.kmongo.eq
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import java.time.LocalTime


class CommandHandler {

    val COMMON_REPLY = "Some parameters arent right. Do check them please"

    fun commandStrategy(message: Message): SendMessage {
        return when (message.getCommand()) {
            BotCommand.start -> onStartCommand(message)
            BotCommand.help -> onHelpCommand(message)
            BotCommand.price -> if (message.wordsCount() > 1) onPriceCommand(message) else message.defaultReply()
            BotCommand.alert -> if (message.wordsCount() > 2) onAlertCommand(message) else message.defaultReply()
            BotCommand.daily -> if (message.wordsCount() > 2) onDailyCommand(message) else message.defaultReply()
            BotCommand.deleteAlert -> if (message.wordsCount() > 1) onDeleteAlertCommand(message) else message.defaultReply()
            BotCommand.deleteDaily -> if (message.wordsCount() > 1) onDeleteDailyCommand(message) else message.defaultReply()
            else -> message.unknownMessage()
        }
    }

    private fun onStartCommand(message: Message): SendMessage {
        var text = "To richness and beyond"
        runBlocking {
            if (DB.coinCollection.countDocuments().compareTo(0) == 0) {
                text = "Congratulations you are this bot first user. Now wait for a couple years" +
                        "while database is being populated. Kidding!"
                val toSave = justOnce()
                toSave.forEach {
                    DB.coinCollection.insertOne(it)
                }
            }

        }
        return sendMessageBuilding(message.chatId.toString(), text)
    }

    private fun onHelpCommand(message: Message): SendMessage {
        return sendMessageBuilding(
            message.chatId.toString(),
            "Halp!, Halp!...Kidding. You can use /price coin e.g" +
                    "{/price btc or /price Bitcoin} to retrieve current coin price.\n" +
                    "You can use /alert coin price to create and alert for you in case " +
                    "coin reach such price. e.g /alert btc 40000.\n" +
                    "You can use /daily coin hour to create a daily reminder about a coin price."
        )

    }

    private fun onPriceCommand(message: Message): SendMessage {
        val coiname = message.getFirstMessageParam()
        var text = "Price for coin not found. Have you double checked spelling?"
        var c: Map<String, Map<String, Double>>
        runBlocking {
            val coinId = nameInList(coiname)
            if (coinId?.isNotBlank()!!) {
                c = getCoinPrice(coinId)
                text = "current $coiname price in usd is $" + "${c[coinId]?.get("usd")}"
            }
        }
        return sendMessageBuilding(message.chatId.toString(), text)
    }

    private fun onAlertCommand(message: Message): SendMessage {
        val price = message.getSecondMessageParam().toDoubleOrNull()
        val userId = message.from.id
        var text = COMMON_REPLY
        runBlocking {
            val coinId = nameInList(message.getFirstMessageParam())
            if (coinId != null && price != null) {
                if (price > 0) {
                    text = if (!existUserAlert(userId, coinId, price)) {
                        DB.userAlertCollection.insertOne(
                            UserAlert(
                                userId = userId,
                                price = price,
                                coinId = coinId
                            )
                        )
                        "Alert created for $coinId with price $price"
                    } else "This alert already exist"
                }
            }
        }
        return sendMessageBuilding(message.chatId.toString(), text)
    }

    private fun onDailyCommand(message: Message): SendMessage {
        var text = COMMON_REPLY
        val coinId: String?
        val userId = message.from.id
        val localTime = try{
            LocalTime.parse(message.getSecondMessageParam())
        } catch (e: Exception){
            LocalTime.now()
        }
        runBlocking {
            coinId = nameInList(message.getFirstMessageParam())
            if (coinId!=null){
                text = if (!existDailyAlert(userId,coinId)){
                    DB.dailyAlert.insertOne(
                        DailyAlert(
                            coinId = coinId,
                            userId = userId,
                            localTime = localTime
                        )
                   )
                    "Daily Alert for $coinId at $localTime created"
                } else "Daily alert for $coinId already exist"
            }
        }

        return sendMessageBuilding(message.chatId.toString(), text)
    }

    private fun onDeleteAlertCommand(message: Message): SendMessage {
        var text = COMMON_REPLY
        val coinId: String?
        val price = message.getSecondMessageParam()
        runBlocking {
            coinId = nameInList(message.getFirstMessageParam())
            if (coinId != null) {
                text = if (price.isBlank()) {
                    DB.userAlertCollection.deleteMany(
                        UserAlert::userId eq message.from.id,
                        UserAlert::coinId eq coinId,
                    )
                    "All alerts for $coinId were successfully deleted"
                } else {
                    DB.userAlertCollection.deleteOne(
                        UserAlert::userId eq message.from.id,
                        UserAlert::coinId eq coinId,
                        UserAlert::price eq price.toDouble()
                    )
                    "Alert for $coinId with price $price was successfully deleted"
                }
            }
        }
        return sendMessageBuilding(message.chatId.toString(), text)
    }

    private fun onDeleteDailyCommand(message: Message): SendMessage {
        var text = COMMON_REPLY
        val coinId: String?
        runBlocking {
            coinId = nameInList(message.getFirstMessageParam())
            if (coinId != null) {
                text = if (existDailyAlert(message.from.id, coinId)) {

                    DB.dailyAlert.deleteOne(
                        DailyAlert::userId eq message.from.id,
                        DailyAlert::coinId eq coinId
                    )
                    "Daily Alert for $coinId deleted"
                } else "Not Daily Alert for ${message.getFirstMessageParam()}"
            }
        }

        return sendMessageBuilding(message.chatId.toString(), text)

    }

}

fun Message.defaultReply() = sendMessageBuilding(
    chatId.toString(), "I dont understand this message.Some parameters may not be right do " +
            "check them please. Type /help for, well, help."
)

fun Message.unknownMessage() = sendMessageBuilding(chatId.toString(), "I dun understand. Wut this?")

fun Message.getCommand() = text.split(" ")[0]

fun Message.wordsCount() = text.split(" ").size

fun Message.getFirstMessageParam(): String = if (text.split(" ").size > 1) text.split(" ")[1].toLowerCase() else ""

fun Message.getSecondMessageParam(): String = if (text.split(" ").size > 2) text.split(" ")[2].toLowerCase() else ""

fun sendMessageBuilding(id: String, text: String): SendMessage = SendMessage.builder().chatId(id).text(text).build()
