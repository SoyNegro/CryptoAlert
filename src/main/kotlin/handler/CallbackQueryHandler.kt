package main.kotlin.handler

import api.getCoinGeckoPrice
import api.nameInList
import handler.inlineKeyboardMarkup
import kotlinx.coroutines.runBlocking
import org.telegram.telegrambots.meta.api.methods.ParseMode
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText
import org.telegram.telegrambots.meta.api.objects.CallbackQuery
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class CallbackQueryHandler {

    fun onPriceCallbackQuery(callbackQuery: CallbackQuery): EditMessageText? {
        val callData = callbackQuery.data
        var text: String
        runBlocking {
            val coinGecko = nameInList(callData)
            val c = getCoinGeckoPrice(callData)
            text = "Current ${coinGecko?.name} price in usd is $" + "${c[coinGecko?.id]?.get("usd")}" +
                    "\n Search powered by <a href = 'https://coingecko.com'>Coingecko</a>." +
                    "\n Updated at "+LocalTime.now().format(
                DateTimeFormatter.ofPattern("HH:mm:ss"))

        }
        val editMessageText = EditMessageText.builder()
            .text(text)
            .replyMarkup(inlineKeyboardMarkup(callData))
            .disableWebPagePreview(true).parseMode(ParseMode.HTML)
            .build()
        if(callbackQuery.message!= null) {editMessageText.messageId = callbackQuery.message.messageId
        editMessageText.chatId = callbackQuery.message.chatId.toString()}
        else editMessageText.inlineMessageId = callbackQuery.inlineMessageId
        return editMessageText
    }

}
