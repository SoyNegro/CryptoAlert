package handler


import alert.UserAlert
import api.DB
import api.getCoinPrice
import api.justOnce
import api.nameInList
import com.mongodb.client.model.DeleteOptions
import configurer.BotCommand
import kotlinx.coroutines.runBlocking
import org.litote.kmongo.MongoOperator
import org.litote.kmongo.eq
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message



class CommandHandler {

    fun commandStrategy(message: Message): SendMessage {
        return when (message.getCommand()) {
            BotCommand.start -> onStartCommand(message)
            BotCommand.help -> onHelpCommand(message)
            BotCommand.price -> onPriceCommand(message)
            BotCommand.alert -> onAlertCommand(message)
            BotCommand.daily -> onDailyCommand(message)
            BotCommand.deleteAlert -> onDeleteAlertCommand(message)
            BotCommand.deleteDaily -> onDeleteDailyCommand(message)
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
        return SendMessage.builder()
            .chatId(message.chatId.toString())
            .text(text)
            .build()
    }

    private fun onHelpCommand(message: Message): SendMessage {
        return SendMessage.builder()
            .chatId(message.chatId.toString())
            .text(
                "Halp!, Halp!...Kidding. You can use /price coin e.g" +
                        "{/price btc or /price Bitcoin} to retrieve current coin price.\n" +
                        "You can use /alert coin price to create and alert for you in case " +
                        "coin reach such price. e.g /alert btc 40000.\n" +
                        "You can use /daily coin hour to create a daily reminder about a coin price."
            )
            .build()
    }

    private fun onPriceCommand(message: Message): SendMessage {
        var c: Map<String, Map<String, Double>>
        val coiname = message.getMessageParams().first
        var text = "Price for coin not found. Have you double checked spelling?"
        runBlocking {
            val coinId = nameInList(coiname)
            if (coinId.isNotBlank()) {
                c = getCoinPrice(coinId)
                text = "current $coiname price in usd is $" + "${c[coinId]?.get("usd")}"
            }
        }
        return SendMessage.builder()
            .chatId(message.chatId.toString())
            .text(text)
            .build()
    }

    private fun onAlertCommand(message: Message): SendMessage {
        val alertValue = message.getMessageParams().second?.toDouble()!!
        var text = "Some parameters arent right. Do check them please"
        runBlocking {
            val coinId = nameInList(message.getMessageParams().first)
            if (coinId.isNotBlank() && alertValue > 0) {
                DB.userAlertCollection.insertOne(
                    UserAlert(
                        userId = message.from.id,
                        price = alertValue,
                        coinId = coinId
                    )
                )
                text = "Alert create for $coinId with price $alertValue"
            }
        }
        return SendMessage.builder()
            .chatId(message.chatId.toString())
            .text(text)
            .build()
    }

    private fun onDailyCommand(message: Message): SendMessage {
        return SendMessage.builder()
            .chatId(message.chatId.toString())
            .text("Incoming")
            .build()
    }

    private fun onDeleteAlertCommand(message: Message): SendMessage {
       runBlocking {
            val coinId = nameInList(message.getMessageParams().first)
            DB.userAlertCollection.deleteOne(UserAlert::userId eq message.from.id,
                UserAlert::coinId eq coinId)
        }
        return SendMessage.builder()
            .chatId(message.chatId.toString())
            .text("Alert for ${message.getMessageParams().first} was successfully deleted")
            .build()
    }

    private fun onDeleteDailyCommand(message: Message): SendMessage {
        return SendMessage.builder()
            .chatId(message.chatId.toString())
            .text("Incoming")
            .build()
    }

}

fun Message.defaultReply() = sendMessageBuilding(
    chatId.toString(), "Sorry, I am a little dumb and dont " +
            "understand this message. You can try /help for example list of message " +
            "i can understand."
)

fun Message.unknownMessage() = sendMessageBuilding(chatId.toString(), "I dun understand. Wut this?")

fun Message.getCommand() = text.split(" ")[0]

fun Message.getMessageParams(): Pair<String, String?> = Pair(text.split(" ")[1], text.split(" ")[2])

fun sendMessageBuilding(id: String, text: String): SendMessage {
    return SendMessage.builder()
        .chatId(id)
        .text(text)
        .build()
}