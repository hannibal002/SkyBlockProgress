package at.lorenz.skyblockprogress.compare

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File

class PlayerData {

    var fetchTime = 0L
    val statsKillsReason = mutableMapOf<String, Long>()
    val statsDeathsReason = mutableMapOf<String, Long>()
    val skillExperience = mutableMapOf<String, Long>()

    val bestiaryKills = mutableMapOf<String, Long>()
    val bestiaryDeaths = mutableMapOf<String, Long>()
    val collectionCount = mutableMapOf<String, Long>()
    val petExperience = mutableMapOf<String, Long>()
    val mythologyData = mutableMapOf<String, Long>()
    var coins = mutableMapOf<String, Long>()
    var dungeonFloor = mutableMapOf<String, Long>()

    val slayers = mutableMapOf<String, SlayerData>()
    val crimsonIsleReputation = mutableMapOf<String, Long>()

    class SlayerData(val experience: Long, val bossKills: Map<String, Long>)

    companion object {
        fun grab(uuid: String, file: File): PlayerData {
            val list = file.readLines()
            val fullText = list.joinToString("").replace("  ", "")
            val profile = JsonParser.parseString(fullText) as JsonObject

            return fillData(profile, uuid)
        }

        private fun fillData(profile: JsonObject, uuid: String): PlayerData {
            val data = PlayerData()
            if (profile.has("fetch_time")) {
                data.fetchTime = profile["fetch_time"].asLong
            }

            if (profile.has("members")) {
                val members = profile["members"].asJsonObject
                if (members.has(uuid)) {
                    val member = members[uuid].asJsonObject

                    if (member.has("death_count")) {
                        data.statsDeathsReason["Total Deaths (/deathcount)"] = member["death_count"].asLong
                    }

                    var totalCoins = 0L
                    if (member.has("coin_purse")) {
                        val purse = member["coin_purse"].asLong
                        totalCoins += purse
                        data.coins["purse"] = purse
                    }

                    if (profile.has("banking")) {
                        val bank = profile["banking"].asJsonObject["balance"].asLong
                        data.coins["bank"] = bank
                        totalCoins += bank
                    }
                    data.coins["Total Coins"] = totalCoins

                    if (member.has("stats")) {
                        val stats = member["stats"].asJsonObject
                        if (stats.has("deaths")) {
                            data.statsDeathsReason["Total Deaths"] = stats["deaths"].asLong
                        }
                        if (stats.has("kills")) {
                            data.statsKillsReason["Total Kills"] = stats["kills"].asLong
                        }
                        if (stats.has("total_pet_exp_gained")) {
                            data.petExperience["Total Pet XP"] = stats["total_pet_exp_gained"].asLong
                        }
                        if (stats.has("mythos_kills")) {
                            data.mythologyData["Total Kills"] = stats["mythos_kills"].asLong
                        }
                        for (key in stats.keySet()) {
                            if (key.startsWith("kills_")) {
                                val label = key.substring(6)
                                val kills = stats[key].asLong
                                data.statsKillsReason[label] = kills
                            }
                            if (key.startsWith("deaths_")) {
                                val label = key.substring(7)
                                val deaths = stats[key].asLong
                                data.statsDeathsReason[label] = deaths
                            }
                            if (key.startsWith("mythos_burrows_")) {
                                val label = key.substring(15)
                                val value = stats[key].asLong
                                data.mythologyData[label] = value
                            }
                        }
                    }
                    for (key in member.keySet()) {
                        if (key.startsWith("experience_skill_")) {
                            val label = key.substring(17)
                            val exp = member[key].asLong
                            data.skillExperience[label] = exp
                        }
                    }
                    if (member.has("bestiary")) {
                        val bestiary = member["bestiary"].asJsonObject
                        for (key in bestiary.keySet()) {
                            if (key.startsWith("kills_")) {
                                val label = key.substring(6)
                                val kills = bestiary[key].asLong
                                data.bestiaryKills[label] = kills
                            }
                            if (key.startsWith("deaths_")) {
                                val label = key.substring(7)
                                val deaths = bestiary[key].asLong
                                data.bestiaryDeaths[label] = deaths
                            }
                        }
                    }
                    if (member.has("dungeons")) {
                        val dungeonTypes = member["dungeons"].asJsonObject["dungeon_types"].asJsonObject
                        for (dungeonTypeEntry in dungeonTypes.entrySet()) {
                            val dungeonType = dungeonTypeEntry.value.asJsonObject
                            for (entry in dungeonType.entrySet()) {
                                val key = entry.key
                                val value = entry.value
                                if (key.equals("tier_completions")) {
                                    for (completions in value.asJsonObject.entrySet()) {
                                        data.dungeonFloor[completions.key] = data.dungeonFloor.getOrDefault(completions.key, 0) + completions.value.asLong
                                    }
                                }
                                if (key.equals("experience")) {
                                    data.skillExperience["catacombs"] = value.asLong
                                }
                            }
                        }
                    }
                    if (member.has("collection")) {
                        val collection = member["collection"].asJsonObject
                        for (key in collection.keySet()) {
                            val count = collection[key].asLong
                            data.collectionCount[key.lowercase()] = count

                        }
                    }
                    if (member.has("pets")) {
                        val pets = member["pets"].asJsonArray
                        for (entry in pets) {
                            val pet = entry.asJsonObject
                            val type = pet["type"].asString.lowercase()
                            val exp = pet["exp"].asLong
                            data.petExperience[type] = exp
                        }
                    }

                    if (member.has("slayer_bosses")) {
                        val slayerBosses = member["slayer_bosses"].asJsonObject
                        for (key in slayerBosses.keySet()) {
                            val slayer = slayerBosses[key].asJsonObject
                            if (slayer.has("xp")) {
                                val experience = slayer["xp"].asLong
                                val bossKills = mutableMapOf<String, Long>()
                                data.slayers[key] = SlayerData(experience, bossKills)
                                for (bossName in slayer.keySet()) {
                                    if (bossName == "xp") continue
                                    val kills = slayer[bossName].asLong
                                    bossKills[bossName] = kills
                                }
                            }
                        }
                    }

                    if (member.has("nether_island_player_data")) {
                        val netherIsland = member["nether_island_player_data"].asJsonObject
                        if (netherIsland.has("mages_reputation")) {
                            data.crimsonIsleReputation["mages"] = netherIsland["mages_reputation"].asLong
                        }
                        if (netherIsland.has("barbarians_reputation")) {
                            data.crimsonIsleReputation["barbarians"] = netherIsland["barbarians_reputation"].asLong
                        }
                    }
                }
            }

            return data
        }
    }
}