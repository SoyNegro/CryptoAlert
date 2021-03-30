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
import java.time.format.DateTimeFormatter


class CommandHandler {

    val COMMON_REPLY = "Some parameters arent right. Do check them please"

    fun commandStrategy(message: Message): SendMessage {
        return when (message.getCommand()) {
            BotCommand.start -> onStartCommand(message)
            BotCommand.help -> onHelpCommand(message)
            BotCommand.price -> if (message.wordsCount() > 1) onPriceCommand(message) else message.defaultReply()
            BotCommand.alertLower -> if (message.wordsCount() > 2) onAlertCommand(
                message,
                "lower"
            ) else message.defaultReply()
            BotCommand.alertBigger -> if (message.wordsCount() > 2) onAlertCommand(
                message,
                "bigger"
            ) else message.defaultReply()
            BotCommand.daily -> if (message.wordsCount() > 1) onDailyCommand(message) else message.defaultReply()
            BotCommand.listalert -> if (message.wordsCount() > 1) onListAlertCommand(message) else message.defaultReply()
            BotCommand.listdaily -> onListDailyAlertCommand(message)
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
        println("WTF")
        val coiname = message.getFirstMessageParam()
        var text = "Price for coin not found. Have you double checked spelling?"
        var c: Map<String, Map<String, Double>>
        runBlocking {
            val coinGecko = nameInList(coiname)
            if (coinGecko!= null) {
                c = getCoinGeckoPrice(coinGecko.id)
                text = "current $coiname price in usd is $" + "${c[coinGecko.id]?.get("usd")}"
            }
        }
        return sendMessageBuilding(message.chatId.toString(), text)
    }

    private fun onAlertCommand(message: Message, lowerOrBigger: String): SendMessage {
        val price = message.getSecondMessageParam().toDoubleOrNull()
        val userId = message.from.id
        var text = COMMON_REPLY
        runBlocking {
            val coinGecko = nameInList(message.getFirstMessageParam())
            if (coinGecko != null && price != null) {
                if (price > 0) {
                    text = if (!existUserAlert(userId, coinGecko.id, price)) {
                        DB.userAlertCollection.insertOne(
                            UserAlert(
                                userId = userId,
                                price = price,
                                coinId = coinGecko.id,
                                lowerOrUpper = lowerOrBigger,
                                coinSymbol = coinGecko.symbol
                            )
                        )
                        "Alert created for ${coinGecko.id} with price $price"
                    } else "This alert already exist"
                }
            }
        }
        return sendMessageBuilding(message.chatId.toString(), text)
    }

    private fun onDailyCommand(message: Message): SendMessage {
        var text = COMMON_REPLY
        val coinGecko: CoinGecko?
        val userId = message.from.id
        val localTime = try {
            LocalTime.parse(message.getSecondMessageParam())
        } catch (e: Exception) {
            LocalTime.now()
        }
        runBlocking {
            coinGecko = nameInList(message.getFirstMessageParam())
            if (coinGecko != null) {
                text = if (!existDailyAlert(userId, coinGecko.id)) {
                    DB.dailyAlert.insertOne(
                        DailyAlert(
                            coinId = coinGecko.id,
                            userId = userId,
                            localTime = localTime.format(DateTimeFormatter.ofPattern("HH:mm"))
                        )
                    )
                    "Daily Alert for ${coinGecko.id} at ${localTime.format(DateTimeFormatter.ofPattern("HH:mm"))} created"
                } else "Daily alert for ${coinGecko.id} already exist"
            }
        }

        return sendMessageBuilding(message.chatId.toString(), text)
    }

    private fun onListAlertCommand(message: Message): SendMessage {
        var text = COMMON_REPLY
        val coinGecko: CoinGecko?

        runBlocking {
            coinGecko = nameInList(message.getFirstMessageParam())
            if (coinGecko != null) {
                val list: List<UserAlert> = DB.userAlertCollection.find(
                    UserAlert::userId eq message.from.id,
                    UserAlert::coinId eq coinGecko.id
                ).toList()
                text = "There are alerts created for ${coinGecko.id} with price :\n"
                list.forEach { text += "${it.price} /n" }
            }
        }
        return sendMessageBuilding(message.chatId.toString(), text)
    }

    private fun onListDailyAlertCommand(message: Message): SendMessage {
        var text = COMMON_REPLY
        val coinId: String?
        runBlocking {
            val list: List<DailyAlert> = DB.dailyAlert.find(
                DailyAlert::userId eq message.from.id
            ).toList()
            text = "There are daily alerts created for:\n"
            list.forEach { text += "${it.coinId} \n" }
        }
        return sendMessageBuilding(message.chatId.toString(), text)
    }


    private fun onDeleteAlertCommand(message: Message): SendMessage {
        var text = COMMON_REPLY
        val coinGecko: CoinGecko?
        val price = message.getSecondMessageParam()
        runBlocking {
            coinGecko = nameInList(message.getFirstMessageParam())
            if (coinGecko != null) {
                text = if (price.isBlank()) {
                    DB.userAlertCollection.deleteMany(
                        UserAlert::userId eq message.from.id,
                        UserAlert::coinId eq coinGecko.id,
                    )
                    "All alerts for ${coinGecko.id} were successfully deleted"
                } else {
                    DB.userAlertCollection.deleteOne(
                        UserAlert::userId eq message.from.id,
                        UserAlert::coinId eq coinGecko.id,
                        UserAlert::price eq price.toDouble()
                    )
                    "Alert for $coinGecko with price $price was successfully deleted"
                }
            }
        }
        return sendMessageBuilding(message.chatId.toString(), text)
    }

    private fun onDeleteDailyCommand(message: Message): SendMessage {
        var text = COMMON_REPLY
        val coinGecko: CoinGecko?
        runBlocking {
            coinGecko = nameInList(message.getFirstMessageParam())
            if (coinGecko != null) {
                text = if (existDailyAlert(message.from.id, coinGecko.id)) {

                    DB.dailyAlert.deleteOne(
                        DailyAlert::userId eq message.from.id,
                        DailyAlert::coinId eq coinGecko.id
                    )
                    "Daily Alert for ${coinGecko.id} deleted"
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
