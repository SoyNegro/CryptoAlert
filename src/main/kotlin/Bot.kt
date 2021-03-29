import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

fun main(args: Array<String>){
 val telegramBotApi = TelegramBotsApi(DefaultBotSession::class.java)
    telegramBotApi.registerBot(CryptoAlertBot())
}