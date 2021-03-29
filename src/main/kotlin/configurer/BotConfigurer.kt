package configurer

object BotConfigurer{
    const val botName = "Your bot name goes here"
    const val botToken = "Your bot token goes here"
}

object BotCommand{
    private const val initCommandChar = '/'
    val start = initCommandChar + "start"
    val help = initCommandChar + "help"
    val price = initCommandChar + "price"
    val alert = initCommandChar + "alert"
    val daily = initCommandChar + "daily"
    val listalert = initCommandChar + "listalert"
    val listdaily = initCommandChar + "listdaily"
    val deleteAlert = initCommandChar + "deletealert"
    val deleteDaily = initCommandChar + "deletedaily"
}


