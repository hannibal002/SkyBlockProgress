package at.lorenz.skyblockprogress.fetch

import at.lorenz.skyblockprogress.utils.Utils
import com.google.gson.*
import java.io.File
import java.text.SimpleDateFormat

class FetchData(private val apiKey: String, players: MutableMap<String, String>) {

    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val dateFormat = SimpleDateFormat("yyyy_MM_dd_HH_mm_ss")

    init {
        println(" ")
        println("Fetch Data")

        for ((uuid, profileName) in players) {
            getPlayerData(uuid, profileName)
        }
    }

    private fun getPlayerData(uuid: String, profileName: String) {
        val skyblockData = Utils.getJSONResponse("https://api.hypixel.net/skyblock/profiles?key=$apiKey&uuid=$uuid")

        if (skyblockData.toString() == "{\"success\":false,\"cause\":\"Invalid API key\"}") {
            System.err.println("Invalid API key")
            return
        }

        val parent = File("data/$uuid")
        if (!parent.isDirectory) {
            parent.mkdirs()
        }

        val profiles = skyblockData["profiles"].asJsonArray
        for (entry in profiles) {
            val profile = entry.asJsonObject
            val cuteName = profile["cute_name"].asString
            if (profileName == cuteName) {

                grabSkyblockData(profile, uuid, cuteName)
                return
            }
        }
    }

    private fun grabSkyblockData(originalData: JsonObject, uuid: String, profileName: String) {
        val parent = File("data/$uuid/$profileName")
        if (!parent.isDirectory) {
            parent.mkdirs()
        }

        val time = dateFormat.format(System.currentTimeMillis())
        val formattedData = originalData.deepCopy()
        cleanupData(formattedData, uuid)

        val text = gson.toJson(formattedData)
        for (file in parent.listFiles()) {
            val data = file.readText()
            val jsonObject = JsonParser.parseString(data) as JsonObject
            jsonObject.remove("fetch_time")
            if (gson.toJson(jsonObject) == text) {
                println("No changes!")
                return
            }
        }

        println("Saved data.")
        File(parent, "${time}_original.json").writeText(gson.toJson(originalData))

        val leftoverData = formattedData.deepCopy()
        removeLeftovers(leftoverData, uuid)
        File(parent, "${time}_unused.json").writeText(gson.toJson(leftoverData))

        formattedData.add("fetch_time", JsonPrimitive(System.currentTimeMillis()))
        File(parent, "${time}_formatted.json").writeText(gson.toJson(formattedData))
    }

    private fun removeLeftovers(profile: JsonObject, uuid: String) {
        val members = profile["members"].asJsonObject
        for (memberUuid in members.keySet()) {
            val member = members[memberUuid].asJsonObject
            member.remove("death_count")
            if (member.has("stats")) {
                val stats = member["stats"].asJsonObject
                stats.remove("deaths")
                stats.remove("kills")
                stats.remove("total_pet_exp_gained")
                stats.remove("mythos_kills")
                for (key in stats.keySet().toMutableList()) {
                    if (key.startsWith("kills_")) {
                        stats.remove(key)
                    }
                    if (key.startsWith("deaths_")) {
                        stats.remove(key)
                    }
                    if (key.startsWith("mythos_burrows_")) {
                        stats.remove(key)
                    }
                }
                for (key in member.keySet().toMutableList()) {
                    if (key.startsWith("experience_skill_")) {
                        member.remove(key)
                    }
                }
            }
            if (member.has("bestiary")) {
                val bestiary = member["bestiary"].asJsonObject
                for (key in bestiary.keySet().toMutableList()) {
                    if (key.startsWith("kills_")) {
                        bestiary.remove(key)
                    }
                    if (key.startsWith("deaths_")) {
                        bestiary.remove(key)
                    }
                }
            }

            if (member.has("dungeons")) {
                val dungeonTypes = member["dungeons"].asJsonObject["dungeon_types"].asJsonObject
                for (entry in dungeonTypes.entrySet()) {
                    val key = entry.key

                    if (key.equals("tier_completions")) {
                        dungeonTypes.remove(key)
                    }
                    if (key.equals("experience")) {
                        dungeonTypes.remove(key)
                    }
                }
            }

            member.remove("collection")
            member.remove("pets")
            member.remove("slayer_bosses")

            if (member.has("nether_island_player_data")) {
                val netherIsland = member["nether_island_player_data"].asJsonObject
                netherIsland.remove("mages_reputation")
                netherIsland.remove("barbarians_reputation")
            }
        }
    }

    private fun cleanupData(profile: JsonObject, uuid: String) {
        profile.remove("selected")
        profile.remove("last_save")
        val members = profile["members"].asJsonObject
        for (memberUuid in members.keySet()) {
            val member = members[memberUuid].asJsonObject

            if (memberUuid != uuid) {
                for (key in member.keySet().toMutableList()) {
                    if (key != "crafted_generators") {
                        member.remove(key)
                    }
                }
            }

            member.remove("active_effects")
            member.remove("paused_effects")
            member.remove("disabled_potion_effects")

            member.remove("talisman_bag")
            member.remove("inv_contents")
            member.remove("personal_vault_contents")
            member.remove("backpack_contents")
            member.remove("inv_armor")
            member.remove("backpack_icons")
            member.remove("ender_chest_contents")
            member.remove("wardrobe_contents")
            member.remove("potion_bag")
            member.remove("fishing_bag")
            member.remove("quiver")
            member.remove("equippment_contents") // Hypixel moment
            member.remove("sacks_counts")
            member.remove("candy_inventory_contents")

            member.remove("inv_armor")

            member.remove("essence_undead")
            member.remove("essence_crimson")
            member.remove("essence_diamond")
            member.remove("essence_dragon")
            member.remove("essence_gold")
            member.remove("essence_ice")
            member.remove("essence_wither")
            member.remove("essence_spider")
            member.remove("favorite_arrow")

            member.remove("last_death")
            member.remove("last_save")
            member.remove("soulflow")
            member.remove("slayer_quest")
            member.remove("wardrobe_equipped_slot")
            member.remove("autopet")
            member.remove("temp_stat_buffs")
            member.remove("trapper_quest")
            member.remove("coop_invitation")

            member.remove("first_join")
            member.remove("first_join_hub")
            member.remove("fairy_souls")
            member.remove("fairy_exchanges")
            member.remove("achievement_spawned_island_types")
            member.remove("perks")

            //TODO maybe use later
            member.remove("quests")
            member.remove("objectives")
            member.remove("visited_zones")
            member.remove("tutorial")
            member.remove("visited_modes")

            if (member.has("pets")) {
                val pets = member["pets"].asJsonArray
                for (pet in pets) {
                    pet.asJsonObject.remove("active")
                    pet.asJsonObject.remove("heldItem")
                    pet.asJsonObject.remove("candyUsed")
                }
            }

            if (member.has("dungeons")) {
                val dungeons = member["dungeons"].asJsonObject
                dungeons.remove("selected_dungeon_class")
                dungeons.remove("daily_runs")
                dungeons.remove("treasures")
                if (dungeons.has("dungeon_types")) {
                    val dungeonTypes = dungeons.get("dungeon_types").asJsonObject
                    for (typeName in dungeonTypes.keySet()) {
                        val type = dungeonTypes[typeName].asJsonObject
                        type.remove("best_runs")
                        for (key in type.keySet().toMutableList()) {
                            if (key.startsWith("most_damage") || key == "most_damage") {
                                type.remove(key)
                            }
                        }
                    }
                }
            }

            if (member.has("nether_island_player_data")) {
                val netherIsland = member["nether_island_player_data"].asJsonObject
                netherIsland.remove("matriarch")
                netherIsland.remove("selected_faction")
                netherIsland.remove("last_minibosses_killed")

                if (netherIsland.has("abiphone")) {
                    val abiphone = netherIsland["abiphone"].asJsonObject
                    abiphone.remove("active_contacts")

                    if (abiphone.has("contact_data")) {
                        val contactData = abiphone["contact_data"].asJsonObject
                        for (contactType in contactData.keySet()) {
                            val contact = contactData[contactType].asJsonObject
                            contact.remove("last_call")
                        }
                    }
                }
            }

            if (member.has("accessory_bag_storage")) {
                val accessoryBag = member["accessory_bag_storage"].asJsonObject
                accessoryBag.remove("selected_power")
                accessoryBag.remove("tuning")
            }

            if (member.has("forge")) {
                val forge = member["forge"].asJsonObject
                forge.remove("forge_processes")
            }

            if (member.has("mining_core")) {
                val miningCore = member["mining_core"].asJsonObject
                miningCore.remove("nodes")
                if (miningCore.has("crystals")) {
                    val crystals = miningCore["crystals"].asJsonObject
                    for (crystalType in crystals.keySet()) {
                        val crystal = crystals[crystalType].asJsonObject
                        crystal.remove("state")
                    }
                }
            }

            if (member.has("stats")) {
                val stats = member["stats"].asJsonObject
                stats.remove("auctions_created")
                stats.remove("auctions_fees")
                stats.remove("auctions_bids")
                stats.remove("auctions_highest_bid")
                stats.remove("auctions_no_bids")
                stats.remove("auctions_completed")
                stats.remove("auctions_won")
                stats.remove("auctions_gold_spent")
                stats.remove("auctions_gold_earned")

                stats.remove("auctions_sold_common")
                stats.remove("auctions_sold_uncommon")
                stats.remove("auctions_sold_rare")
                stats.remove("auctions_sold_epic")
                stats.remove("auctions_sold_legendary")

                stats.remove("auctions_bought_common")
                stats.remove("auctions_bought_uncommon")
                stats.remove("auctions_bought_epic")
                stats.remove("auctions_bought_rare")
                stats.remove("auctions_bought_legendary")
                stats.remove("auctions_bought_special")
            }

            if (member.has("jacob2")) {
                val jacob2 = member["jacob2"].asJsonObject
                jacob2.remove("contests")
            }

            if (member.has("jacob2")) {
                val jacob2 = member["jacob2"].asJsonObject
                jacob2.remove("contests")
            }
            if (member.has("slayer_bosses")) {
                val slayerBosses = member["slayer_bosses"].asJsonObject
                for (bossType in slayerBosses.keySet()) {
                    val boss = slayerBosses[bossType].asJsonObject
                    boss.remove("claimed_levels")
                }
            }
        }

        recursiveDeleteEmpty(profile)
    }

    private fun recursiveDeleteEmpty(element: JsonElement) {
        if (element is JsonObject) {
            for (key in element.keySet().toMutableList()) {
                val jsonElement = element[key]
                recursiveDeleteEmpty(jsonElement)
                if (isEmpty(jsonElement)) {
                    element.remove(key)
                }
            }
        } else if (element is JsonArray) {
            for (key in element) {
                recursiveDeleteEmpty(key)
                if (isEmpty(key)) {
                    element.remove(key)
                }
            }
        }
    }

    private fun isEmpty(element: JsonElement): Boolean {
        if (element is JsonObject) {
            return element.keySet().isEmpty()
        } else if (element is JsonArray) {
            return element.isEmpty
        }

        return false
    }
}