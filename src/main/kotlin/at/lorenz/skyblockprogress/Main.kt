import at.lorenz.skyblockprogress.compare.CompareData
import at.lorenz.skyblockprogress.fetch.FetchData
import com.google.gson.*
import java.io.File

fun main(args: Array<String>) {

    val gson = GsonBuilder().setPrettyPrinting().create()

    val parent = File("data/")
    if (!parent.isDirectory) {
        parent.mkdirs()
    }

    val configFile = File("data/config.json")
    if (!configFile.exists()) {
        configFile.createNewFile()

        val configObject = JsonObject()
        configObject.add("apiKey", JsonPrimitive(""))
        configObject.add("playerUuidStripped", JsonPrimitive(""))
        configObject.add("profileName", JsonPrimitive(""))

        configFile.writeText(gson.toJson(configObject))
    }

    val map = HashMap<String, String>()

    val config = grab(configFile)
    val apiKey: String

    if (config["apiKey"].asString.isNotEmpty()) {
        apiKey = config["apiKey"].asString
    } else {
        println("Api Key has not been set!")
        return
    }
    if (config["playerUuidStripped"].asString.isNotEmpty() && config["profileName"].asString.isNotEmpty()) {
        map[config["playerUuidStripped"].asString] = config["profileName"].asString
    } else {
        println("Player UUID or Profile Name has not been set!")
        return
    }

    FetchData(apiKey, map)
    CompareData(apiKey, map)
}

fun grab(configFile: File): JsonObject {
    val list = configFile.readLines()
    val fullText = list.joinToString("").replace("  ", "")
    return JsonParser.parseString(fullText) as JsonObject
}