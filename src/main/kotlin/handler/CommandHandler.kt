package handler


import alert.DailyAlert
import alert.UserAlert
import api.*
import configurer.BotCommand
import kotlinx.coroutines.runBlocking
import org.litote.kmongo.eq
import org.telegram.telegrambots.meta.api.methods.ParseMode
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
            BotCommand.alertHigher -> if (message.wordsCount() > 2) onAlertCommand(
                message,
                "higher"
            ) else message.defaultReply()
            //BotCommand.daily -> if (message.wordsCount() > 1) onDailyCommand(message) else message.defaultReply()
            BotCommand.listalert -> if (message.wordsCount() > 1) onListAlertCommand(message) else message.defaultReply()
            //BotCommand.listdaily -> onListDailyAlertCommand(message)
            BotCommand.deleteAlert -> if (message.wordsCount() > 1) onDeleteAlertCommand(message) else message.defaultReply()
            //BotCommand.deleteDaily -> if (message.wordsCount() > 1) onDeleteDailyCommand(message) else message.defaultReply()
            BotCommand.about -> onAboutCommand(message)
            BotCommand.donate -> onDonateCommand(message)
            BotCommand.disclaimer -> onDisclaimerCommand(message)
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
                    "{/p btc or /p Bitcoin} to retrieve current coin price.\n" +
                    "You can use {/al coin or /ah coin} price to create an alert for you in case " +
                    "coin price goes lower or higher respectively. e.g /al btc 40000 alert if btc price goes" +
                    "below $40000, hopefully so i can buy."
        )

    }

    private fun onPriceCommand(message: Message): SendMessage {
        val coiname = message.getFirstMessageParam()
        var text = "Price for coin not found. Have you double checked spelling?"
        var c: Map<String, Map<String, Double>>
        runBlocking {
            val coinGecko = nameInList(coiname)
            if (coinGecko != null) {
                c = getCoinGeckoPrice(coinGecko.id)
                text = "Current ${coinGecko.name} price in usd is $" + "${c[coinGecko.id]?.get("usd")}" +
                        "\n Search powered by <a href = 'https://coingecko.com'>Coingecko</a>"
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
                        "Alert created for ${coinGecko.id} with price $lowerOrBigger than $$price"
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
                            localTime = localTime.commonFormatter()
                        )
                    )
                    "Daily Alert for ${coinGecko.id} at ${localTime.commonFormatter()} created"
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
                if (list.isNotEmpty()) {
                    text = "There are alerts recorded for ${coinGecko.name} with price :\n"
                    list.forEach { text += "${it.lowerOrUpper} than $" + "${it.price} \n" }
                } else text = "There are not alerts created for ${coinGecko.name}"
            }
        }
        return sendMessageBuilding(message.chatId.toString(), text)
    }

    private fun onListDailyAlertCommand(message: Message): SendMessage {
        var text = COMMON_REPLY
        val coinGecko: CoinGecko?
        runBlocking {
            coinGecko = nameInList(message.getFirstMessageParam())
            val list: List<DailyAlert> = if (coinGecko != null) DB.dailyAlert.find(
                DailyAlert::userId eq message.from.id,
                DailyAlert::coinId eq coinGecko.id
            ).toList() else DB.dailyAlert.find(
                DailyAlert::userId eq message.from.id
            ).toList()
            if (list.isNotEmpty()) {
                text = "There are daily alerts created for:\n"
                list.forEach { text += "${it.coinId}  at ${it.localTime}\n" }
            } else text = "There are not daily alerts created for ${coinGecko?.name}"

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
                    "All alerts for ${coinGecko.name} were successfully deleted"
                } else {
                    DB.userAlertCollection.deleteOne(
                        UserAlert::userId eq message.from.id,
                        UserAlert::coinId eq coinGecko.id,
                        UserAlert::price eq price.toDouble()
                    )
                    "Alert for ${coinGecko.name} with price $price was successfully deleted"
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

    private fun onAboutCommand(message: Message): SendMessage {
        return sendMessageBuilding(
            message.chatId.toString(), "Ha, do you really thought you would find anything here?." +
                    "\n Jokes aside, this is just a little test bot made for some random dude. The code is available" +
                    "at <a href='https://github.com/SoyNegro/CryptoAlert'>Github</a> Use wisely."
        )
    }

    private fun onDisclaimerCommand(message: Message): SendMessage {
        return sendMessageBuilding(
            message.chatId.toString(), "Just wanted to say, i dont own the bot profile picture. " +
                    "\n Nor do i vouch for the data being credible, you  may look at <a href='coingecko.com'>Coingecko</a> or" +
                    "<a href='cryptocompare.com'>Cryptocompare</a> for that." +
                    "\n Also 'alert' feature may have between 30s to 1min delay." +
                    "\n And since i am poor, and cuban. And Cuban! The bot use free services " +
                    "like <a href='heroku.com'>Heroku</a> and the free api from " +
                    "<a href='cryptocompare.com'>Cryptocompare</a>. " +
                    "So dont fret if the bot stop working someday."
        )
    }

    private fun onDonateCommand(message: Message): SendMessage {
        return sendMessageBuilding(
            message.chatId.toString(), "What do i even waste time." +
                    "Anyways if you feel generous you can send some" +
                    "\n TRX - TFBJxnB7hyCuXhBwbBomiRFMuj9reKMhGA" +
                    "\n or maybe some btc?? " +
                    "\n SegWit - 3Bp8rKwahuieL5s9maEgB11dX1c72Y2G68 " +
                    "\n Legacy - 1K373wy5sdpZnA4UEhX96jTQZBU5LVrM18"
        )
    }

}

fun Message.defaultReply() = sendMessageBuilding(
    chatId.toString(), "I dont understand this message.Some parameters may not be right do " +
            "check them please. Type /help for, well, help."
)

fun LocalTime.commonFormatter(): String = format(DateTimeFormatter.ofPattern("HH:mm"))
fun Message.unknownMessage() = sendMessageBuilding(chatId.toString(), "I dun understand. Wut this?")

fun Message.getCommand() = text.split(" ")[0]

fun Message.wordsCount() = text.split(" ").size

fun Message.getFirstMessageParam(): String = if (text.split(" ").size > 1) text.split(" ")[1].toLowerCase() else ""

fun Message.getSecondMessageParam(): String = if (text.split(" ").size > 2) text.split(" ")[2].toLowerCase() else ""

fun sendMessageBuilding(id: String, text: String): SendMessage = SendMessage
    .builder()
    .chatId(id)
    .text(text)
    .parseMode(ParseMode.HTML)
    .disableWebPagePreview(true)
    .build()
