import alert.UserAlert
import api.DB
import api.getCryptoCompareListPrice
import configurer.BotConfigurer
import handler.CommandHandler
import handler.defaultReply
import handler.sendMessageBuilding
import handler.unknownMessage
import kotlinx.coroutines.delay
import org.litote.kmongo.eq
import org.litote.kmongo.gte
import org.litote.kmongo.lte
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

class CryptoAlertBot(
    private val commandHandler: CommandHandler = CommandHandler()
) : TelegramLongPollingBot() {

    override fun getBotToken() = BotConfigurer.botToken

    override fun getBotUsername() = BotConfigurer.botName

    override fun onUpdateReceived(update: Update) {
        when (update.hasMessage()) {
            update.message.isCommand -> caughtMessage(
                commandHandler.commandStrategy(
                    update.message
                )
            )
            update.message.hasText() -> caughtMessage(
                update.message.defaultReply()
            )
            else -> caughtMessage(update.message.unknownMessage())
        }
    }

    private fun caughtMessage(sendMessage: SendMessage) {
        try {
            execute(
                sendMessage
            )
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }

    }

    suspend fun checkUserAlert() {
        while (true) {
            delay(15000)
            var coins = DB.userAlertCollection.distinct(UserAlert::coinSymbol).toList().joinToString(",")
            val userAlert = DB.userAlertCollection.find().toList()
            if (!userAlert.isNullOrEmpty()) {
                var coinList = getCryptoCompareListPrice(coins)
                userAlert.forEach {
                    var text =
                        "Current ${it.coinId} price is ${coinList[it.coinSymbol.toUpperCase()]?.get("USD")} which is "
                    if (it.lowerOrUpper == "lower" && coinList[it.coinSymbol.toUpperCase()]?.get("USD")!! < it.price) {
                        text += "lower than ${it.price} " +
                                "\n Result powered by <a href='https://cryptocompare.com'>Cryptocompare</a>"
                        caughtMessage(sendMessageBuilding(it.userId.toString(), text))
                        DB.userAlertCollection.deleteOne(
                            UserAlert::userId eq it.userId,
                            UserAlert::price lte it.price,
                            UserAlert::lowerOrUpper eq it.lowerOrUpper,
                            UserAlert::coinId eq it.coinId
                        )
                    } else if (it.lowerOrUpper == "bigger" && coinList[it.coinSymbol.toUpperCase()]?.get("USD")!! > it.price) {
                        text += "bigger than ${it.price}" + "\n Result powered by <a href='https://cryptocompare.com'>Cryptocompare</a>"
                        caughtMessage(sendMessageBuilding(it.userId.toString(), text))
                        DB.userAlertCollection.deleteOne(
                            UserAlert::userId eq it.userId,
                            UserAlert::price gte it.price,
                            UserAlert::lowerOrUpper eq it.lowerOrUpper,
                            UserAlert::coinId eq it.coinId
                        )
                    }
                }
            }
        }
    }
}



