package configurer

object BotConfigurer{
    const val botName = ""
    const val botToken = ""
}

object BotCommand{
    private const val initCommandChar = '/'
    val start = initCommandChar + "start"
    val help = initCommandChar + "help"
    val price = initCommandChar + "price"
    val alertLower = initCommandChar + "alertlower"
    val alertBigger = initCommandChar+"alertbigger"
    val daily = initCommandChar + "daily"
    val listalert = initCommandChar + "listalert"
    val listdaily = initCommandChar + "listdaily"
    val deleteAlert = initCommandChar + "deletealert"
    val deleteDaily = initCommandChar + "deletedaily"
}

