package at.lorenz.skyblockprogress.compare

import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat

class CompareData(private val apiKey: String, players: MutableMap<String, String>) {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss")

    init {
        println(" ")
        println("Compare Data")
        for ((uuid, profileName) in players) {
            comparePlayerData(uuid, profileName)
        }
    }

    private fun comparePlayerData(uuid: String, profileName: String) {
        val parent = File("data/$uuid/$profileName")
        if (!parent.isDirectory) {
            println("No folder: $parent")
            return
        }

        val map = mutableMapOf<PlayerData, Long>()
        for (file in parent.listFiles()) {
            val name = file.name
            if (name.endsWith("_formatted.json")) {
                val data = PlayerData.grab(uuid, file)
                map[data] = data.fetchTime
            }
        }

        if (map.size < 2) {
            println("Not enough data points saved!")
            return
        }

        val sorted = map.toSortedMap { a, b -> if (a.fetchTime < b.fetchTime) 1 else -1 }

        var first: PlayerData? = null
        var second: PlayerData? = null

        var i = 0
        for (entry in sorted.entries) {
            if (i == 0) first = entry.key
            if (i == 1) second = entry.key
            if (i == 2) break
            i++
        }
        compare(uuid, second!!, first!!)
    }

    private fun compare(profileName: String, first: PlayerData, second: PlayerData) {
        val firstLastSave = dateFormat.format(first.fetchTime)
        val secondLastSave = dateFormat.format(second.fetchTime)

        val result = linkedSetOf<String>()

        result.add(" ")
        result.add("compare for $profileName: $firstLastSave with $secondLastSave")
        result.add(" ")

        compareSlayers(result, first.slayers, second.slayers)

        result.addAll(
            printListChange(
                "Crimson Isle Reputation",
                first.crimsonIsleReputation,
                second.crimsonIsleReputation
            )
        )
        result.addAll(printListChange("Mythology Burrows", first.mythologyData, second.mythologyData))

        result.addAll(printListChange("Skill XP", first.skillExperience, second.skillExperience))
        result.addAll(printListChange("Coins", first.coins, second.coins))

        result.addAll(printListChange("Pet XP", first.petExperience, second.petExperience))

        result.addAll(printListChange("Collections", first.collectionCount, second.collectionCount))

        result.addAll(printListChange("Dungeon Floor Completions", first.dungeonFloor, second.dungeonFloor))

        result.addAll(printListChange("stats-kills", first.statsKillsReason, second.statsKillsReason))
        result.addAll(printListChange("stats-deaths", first.statsDeathsReason, second.statsDeathsReason))
        result.addAll(printListChange("bestiary-kills", first.bestiaryKills, second.bestiaryKills))
        result.addAll(printListChange("bestiary-deaths", first.bestiaryDeaths, second.bestiaryDeaths))

        for (line in result) {
            if (line.isNotEmpty()) {
                println(line)
            }
        }
    }

    private fun compareSlayers(
        result: LinkedHashSet<String>,
        first: MutableMap<String, PlayerData.SlayerData>,
        second: MutableMap<String, PlayerData.SlayerData>,
    ) {
        for (entry in second) {
            val slayerType = entry.key
            val slayerData = entry.value
            val expFirst = first[slayerType]?.experience ?: 0

            val expSecond = slayerData.experience
            if (expFirst == expSecond) continue

            val listFirst = first[slayerType]!!.bossKills.fixSlayerName()
            val listSecond = slayerData.bossKills.fixSlayerName()
            listFirst["experience"] = expFirst
            listSecond["experience"] = expSecond
            result.addAll(printListChange("$slayerType slayer", listFirst, listSecond))
        }
    }

    private fun printListChange(
        listLabel: String,
        first: Map<String, Long>,
        second: Map<String, Long>,
    ): List<String> {
        val changedStats = mutableMapOf<String, Long>()
        for (entry in second) {
            val label = entry.key
            val newValue = entry.value
            val oldValue = first.getOrDefault(label, 0)
            if (newValue != oldValue) {
                changedStats[makeCompareText(label, oldValue, newValue, true)] = newValue - oldValue
            }
        }
        val result = mutableListOf<String>()
        if (changedStats.isNotEmpty()) {
            result.add("\n===============[$listLabel]===============")
            for (value in changedStats.toList().sortedBy { (_, value) -> value }.reversed().toMap().keys) {
                result.add("   $value")
            }
        }
        return result
    }


    private fun makeCompareText(label: String, a: Long, b: Long, isList: Boolean = false): String {
        val numberFormatter = DecimalFormat("#,##0")
        val diff = b - a
        if (diff == 0L) return ""
        val plus = if (diff > 0) "+" else ""
        val aa = numberFormatter.format(a)
        val bb = numberFormatter.format(b)
        val diffFormat = numberFormatter.format(diff)
        val newLine = if (isList) "" else "\n"
        return "$newLine$label: $plus$diffFormat ($aa -> $bb)"
    }
}

private fun Map<String, Long>.fixSlayerName(): MutableMap<String, Long> {
    return mapKeys { (key, _) ->
        "tier ${key.takeLast(1).toInt() + 1} kills"
    }.toMutableMap()
}
