package com.cricketmaster.manager.data

import android.content.Context
import com.cricketmaster.manager.model.*
import org.json.JSONObject

object RosterLoader {

    data class RosterData(
        val teams: MutableList<Team>,
        val squad: MutableList<Player>,
        val auctionPool: MutableList<Player>,
        val venues: List<Venue>
    )

    fun loadRoster(context: Context): RosterData {
        return runCatching {
            val jsonString = context.assets.open("roster.json").bufferedReader().use { it.readText() }
            val root = JSONObject(jsonString)

            val teams = mutableListOf<Team>()
            val teamsArray = root.optJSONArray("teams")
            if (teamsArray != null) {
                for (i in 0 until teamsArray.length()) {
                    val obj = teamsArray.getJSONObject(i)
                    teams.add(
                        Team(
                            name = obj.getString("name"),
                            budget = obj.getInt("budget"),
                            strategy = obj.optString("strategy", "Balanced"),
                            homeVenue = obj.optString("homeVenue", "${obj.getString("name")} Stadium")
                        )
                    )
                }
            }

            val squad = mutableListOf<Player>()
            val squadArray = root.optJSONArray("squad")
            if (squadArray != null) {
                for (i in 0 until squadArray.length()) {
                    val obj = squadArray.getJSONObject(i)
                    squad.add(parsePlayer(obj))
                }
            }

            val auctionPool = mutableListOf<Player>()
            val auctionArray = root.optJSONArray("auctionPool")
            if (auctionArray != null) {
                for (i in 0 until auctionArray.length()) {
                    val obj = auctionArray.getJSONObject(i)
                    auctionPool.add(parsePlayer(obj))
                }
            }

            val venues = mutableListOf<Venue>()
            val venuesArray = root.optJSONArray("venues")
            if (venuesArray != null) {
                for (i in 0 until venuesArray.length()) {
                    val obj = venuesArray.getJSONObject(i)
                    venues.add(
                        Venue(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            city = obj.getString("city"),
                            capacity = obj.getInt("capacity"),
                            pitch = PitchType.valueOf(obj.optString("pitch", "BALANCED")),
                            boundarySize = obj.optInt("boundarySize", 70)
                        )
                    )
                }
            }

            RosterData(
                teams = if (teams.isEmpty()) fallbackTeams() else teams,
                squad = if (squad.isEmpty()) fallbackSquad() else squad,
                auctionPool = if (auctionPool.isEmpty()) fallbackAuctionPool() else auctionPool,
                venues = if (venues.isEmpty()) fallbackVenues() else venues
            )
        }.getOrElse {
            RosterData(fallbackTeams(), fallbackSquad(), fallbackAuctionPool(), fallbackVenues())
        }
    }

    private fun parsePlayer(obj: JSONObject): Player {
        val id = obj.getInt("id")
        val batVal = obj.optInt("bat", 70)
        val powerVal = obj.optInt("power", 70)
        val paceVal = obj.optInt("pace", 50)
        val accuracyVal = obj.optInt("accuracy", 50)
        val spinVal = obj.optInt("spin", 30)
        val fieldingVal = obj.optInt("fielding", 70)
        val mentalVal = obj.optInt("mental", 70)

        return Player(
            id = id,
            name = obj.getString("name"),
            age = obj.getInt("age"),
            nationality = obj.getString("nationality"),
            role = obj.getString("role"),
            overall = obj.getInt("overall"),
            potential = obj.getInt("potential"),
            value = obj.optInt("value", obj.getInt("overall") * 1_500_000),
            salary = obj.optInt("salary", obj.getInt("overall") * 85_000),
            contract = obj.optInt("contract", 3),
            bat = batVal,
            power = powerVal,
            pace = paceVal,
            accuracy = accuracyVal,
            spin = spinVal,
            fielding = fieldingVal,
            mental = mentalVal
        )
    }

    private fun fallbackTeams() = mutableListOf(
        Team("Harbor Hawks", 120_000_000, "Balanced"),
        Team("Metro Kings", 145_000_000, "Aggressive"),
        Team("Desert Blazers", 105_000_000, "Youth"),
        Team("Capital Chargers", 130_000_000, "Stars"),
        Team("Coastal Cyclones", 95_000_000, "Value"),
        Team("Highland Strikers", 115_000_000, "Bowling")
    )

    private fun fallbackSquad() = mutableListOf(
        Player(1, "Arjun Mehta", 28, "India", "Opener", 86, 89, 129_000_000, 7_310_000, 3, bat = 88, power = 72, pace = 35, accuracy = 16, spin = 21, fielding = 72, mental = 75),
        Player(2, "Liam Carter", 31, "Australia", "Top-Order Batter", 84, 84, 126_000_000, 7_140_000, 2, bat = 84, power = 76, pace = 30, accuracy = 12, spin = 20, fielding = 74, mental = 78),
        Player(3, "Ravi Iyer", 25, "India", "Wicketkeeper", 82, 90, 123_000_000, 6_970_000, 3, bat = 81, power = 69, pace = 20, accuracy = 8, spin = 22, fielding = 85, mental = 76),
        Player(4, "Noah Bennett", 27, "England", "All-Rounder", 83, 87, 124_500_000, 7_055_000, 2, bat = 76, power = 78, pace = 68, accuracy = 70, spin = 19, fielding = 78, mental = 79),
        Player(5, "Kabir Shah", 24, "Pakistan", "All-Rounder", 80, 91, 120_000_000, 6_800_000, 4, bat = 74, power = 73, pace = 72, accuracy = 75, spin = 20, fielding = 76, mental = 74),
        Player(6, "Diego Silva", 30, "South Africa", "Middle-Order Batter", 79, 79, 118_500_000, 6_715_000, 2, bat = 80, power = 75, pace = 22, accuracy = 10, spin = 18, fielding = 70, mental = 73),
        Player(7, "Ethan Cole", 29, "New Zealand", "Fast Bowler", 82, 83, 123_000_000, 6_970_000, 3, bat = 45, power = 61, pace = 88, accuracy = 84, spin = 18, fielding = 73, mental = 77),
        Player(8, "Ishaan Rao", 26, "India", "Fast Bowler", 81, 88, 121_500_000, 6_885_000, 3, bat = 42, power = 58, pace = 86, accuracy = 82, spin = 17, fielding = 72, mental = 75),
        Player(9, "Zain Malik", 23, "Pakistan", "Spin Bowler", 78, 91, 117_000_000, 6_630_000, 4, bat = 48, power = 49, pace = 38, accuracy = 87, spin = 88, fielding = 74, mental = 76),
        Player(10, "Oliver Grant", 32, "England", "Fast Bowler", 77, 77, 115_500_000, 6_545_000, 1, bat = 35, power = 50, pace = 82, accuracy = 79, spin = 15, fielding = 68, mental = 72),
        Player(11, "Sanjay Patel", 22, "India", "Spin Bowler", 76, 92, 114_000_000, 6_460_000, 4, bat = 51, power = 52, pace = 34, accuracy = 85, spin = 86, fielding = 75, mental = 73),
        Player(12, "Marcus Lee", 26, "West Indies", "Finisher", 75, 84, 112_500_000, 6_375_000, 2, bat = 79, power = 85, pace = 26, accuracy = 14, spin = 20, fielding = 71, mental = 72),
        Player(13, "Aiden Brooks", 21, "Australia", "All-Rounder", 73, 94, 109_500_000, 6_205_000, 4, bat = 68, power = 72, pace = 67, accuracy = 64, spin = 23, fielding = 77, mental = 70),
        Player(14, "Farid Khan", 28, "Afghanistan", "Spin Bowler", 74, 79, 111_000_000, 6_290_000, 2, bat = 43, power = 54, pace = 28, accuracy = 89, spin = 90, fielding = 70, mental = 74),
        Player(15, "Jon Bell", 25, "England", "Wicketkeeper", 71, 82, 106_500_000, 6_035_000, 3, bat = 72, power = 70, pace = 15, accuracy = 8, spin = 25, fielding = 80, mental = 69)
    )

    private fun fallbackAuctionPool() = mutableListOf(
        Player(101, "Vikram Sethi", 26, "India", "Top-Order Batter", 85, 88, 127_500_000, 7_225_000, 3, bat = 87, power = 74, pace = 28, accuracy = 10, spin = 19, fielding = 73, mental = 77),
        Player(102, "Theo James", 24, "England", "Fast Bowler", 81, 91, 121_500_000, 6_885_000, 3, bat = 42, power = 58, pace = 87, accuracy = 84, spin = 20, fielding = 75, mental = 76),
        Player(103, "Milan Das", 20, "Bangladesh", "All-Rounder", 74, 95, 111_000_000, 6_290_000, 4, bat = 67, power = 70, pace = 65, accuracy = 69, spin = 18, fielding = 72, mental = 71),
        Player(104, "Carlos Mendez", 30, "South Africa", "Opener", 79, 80, 118_500_000, 6_715_000, 2, bat = 80, power = 80, pace = 25, accuracy = 9, spin = 21, fielding = 71, mental = 75)
    )

    private fun fallbackVenues() = listOf(
        Venue("v1", "Riverside Oval", "Harbor City", 35000, PitchType.BALANCED, 68),
        Venue("v2", "Metro Dome", "Metro City", 55000, PitchType.FLAT, 65),
        Venue("v3", "Highland Fortress", "Highland", 28000, PitchType.GREEN, 72),
        Venue("v4", "Desert Oasis Stadium", "Oasis City", 40000, PitchType.DRY, 70)
    )
}
