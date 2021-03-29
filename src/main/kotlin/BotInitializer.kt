import configurer.BotConfigurer
import handler.CommandHandler
import handler.defaultReply
import handler.unknownMessage
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

}



