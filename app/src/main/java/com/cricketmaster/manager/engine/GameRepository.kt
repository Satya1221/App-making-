package com.cricketmaster.manager.engine

import android.content.Context
import com.cricketmaster.manager.data.RosterLoader
import com.cricketmaster.manager.model.*
import org.json.JSONArray
import org.json.JSONObject

/** Owns campaign state and provides save/load with schema versioning and safe fallback defaults. */
class GameRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("cricket_manager_save", Context.MODE_PRIVATE)

    var squad: MutableList<Player> = mutableListOf()
    var auctionPool: MutableList<Player> = mutableListOf()
    var teams: MutableList<Team> = mutableListOf()
    var venues: List<Venue> = listOf()
    var state: GameState = GameState()

    init {
        val loadedData = RosterLoader.loadRoster(context)
        squad = loadedData.squad
        auctionPool = loadedData.auctionPool
        teams = loadedData.teams
        venues = loadedData.venues

        state = GameState(
            selectedXI = squad.take(11).map { it.id }.toMutableList(),
            news = mutableListOf("Welcome back, Manager. Your campaign begins today.", "Training facilities are ready for the squad.")
        )

        load()
        if (state.fixtures.isEmpty()) {
            state.fixtures = SeasonEngine().fixtures(teams)
        }
    }

    fun save() {
        val root = JSONObject()
            .put("version", 2)
            .put("budget", state.budget)
            .put("season", state.season)
            .put("teamName", state.teamName)
            .put("xi", JSONArray(state.selectedXI))
            .put("captain", state.captainId)
            .put("viceCaptain", state.viceCaptainId)
            .put("wicketKeeper", state.wicketKeeperId)
            .put("news", JSONArray(state.news))
            .put("sponsor", state.sponsor)
            .put("tactic", state.tactic)
            .put("trainingLevel", state.trainingLevel)
            .put("stadiumLevel", state.stadiumLevel)
            .put("medicalLevel", state.medicalLevel)
            .put("academyLevel", state.academyLevel)
            .put("devMode", state.devModeEnabled)
            .put("auctionIndex", state.auctionIndex)
            .put("auctionBid", state.auctionBid)
            .put("auctionHistory", JSONArray(state.auctionHistory))
            .put("signedAuctionIds", JSONArray(state.signedAuctionIds))
            .put("matchHistory", JSONArray(state.matchHistory.take(20).map(::resultJson)))
            .put("fixtures", JSONArray(state.fixtures.map { f ->
                JSONObject().put("id", f.id).put("round", f.round).put("home", f.home).put("away", f.away).put("venue", f.venue).put("status", f.status).put("result", f.result)
            }))
            .put("players", JSONArray(squad.map(::playerJson)))
            .put("teams", JSONArray(teams.map { t ->
                JSONObject().put("name", t.name).put("budget", t.budget).put("strategy", t.strategy).put("points", t.points).put("played", t.played).put("won", t.won).put("lost", t.lost).put("tied", t.tied).put("nrr", t.nrr).put("rs", t.runsScored).put("rc", t.runsConceded).put("bf", t.ballsFaced).put("bb", t.ballsBowled)
            }))

        state.lastResult?.let { root.put("lastResult", resultJson(it)) }
        state.liveMatch?.let { root.put("liveMatch", liveJson(it)) }

        prefs.edit().putString("state", root.toString()).apply()
    }

    private fun load() {
        val raw = prefs.getString("state", null) ?: return
        runCatching {
            val root = JSONObject(raw)
            state.budget = root.optInt("budget", state.budget)
            state.season = root.optInt("season", state.season)
            state.teamName = root.optString("teamName", state.teamName)
            state.captainId = root.optInt("captain", state.captainId)
            state.viceCaptainId = root.optInt("viceCaptain", state.viceCaptainId)
            state.wicketKeeperId = root.optInt("wicketKeeper", state.wicketKeeperId)
            state.sponsor = root.optString("sponsor", state.sponsor)
            state.tactic = root.optString("tactic", state.tactic)
            state.trainingLevel = root.optInt("trainingLevel", 1)
            state.stadiumLevel = root.optInt("stadiumLevel", 1)
            state.medicalLevel = root.optInt("medicalLevel", 1)
            state.academyLevel = root.optInt("academyLevel", 1)
            state.devModeEnabled = root.optBoolean("devMode", false)
            state.auctionIndex = root.optInt("auctionIndex", 0)
            state.auctionBid = root.optInt("auctionBid", 0)

            root.optJSONArray("xi")?.let { state.selectedXI = intList(it).toMutableList() }
            root.optJSONArray("news")?.let { state.news = stringList(it).toMutableList() }
            root.optJSONArray("auctionHistory")?.let { state.auctionHistory = stringList(it).toMutableList() }
            root.optJSONArray("signedAuctionIds")?.let { state.signedAuctionIds = intList(it).toMutableList() }

            root.optJSONArray("players")?.let { restorePlayers(it) }
            root.optJSONArray("teams")?.let { restoreTeams(it) }

            // Ensure signed players are present in squad
            state.signedAuctionIds.forEach { id ->
                auctionPool.find { it.id == id }?.let { p ->
                    if (squad.none { member -> member.id == id }) squad.add(p)
                }
            }

            state.matchHistory = root.optJSONArray("matchHistory")?.let { rows ->
                List(rows.length()) { restoreResult(rows.getJSONObject(it)) }.toMutableList()
            } ?: mutableListOf()

            state.fixtures = root.optJSONArray("fixtures")?.let { rows ->
                List(rows.length()) { i ->
                    val f = rows.getJSONObject(i)
                    Fixture(f.getString("id"), f.getInt("round"), f.getString("home"), f.getString("away"), f.getString("venue"), f.optString("status", "SCHEDULED"), f.optString("result", ""))
                }.toMutableList()
            } ?: mutableListOf()

            root.optJSONObject("lastResult")?.let { state.lastResult = restoreResult(it) }
            root.optJSONObject("liveMatch")?.let { state.liveMatch = restoreLive(it) }
        }
    }

    private fun playerJson(player: Player) = JSONObject().apply {
        put("id", player.id)
        put("form", player.form)
        put("fitness", player.fitness)
        put("fatigue", player.fatigue)
        put("morale", player.morale)
        put("confidence", player.confidence)
        put("injuryDays", player.injuryDaysRemaining)
        put("contract", player.contract)
        put("salary", player.salary)
        put("overall", player.overall)
        put("runs", player.runs)
        put("wickets", player.wickets)
        put("matches", player.matches)
        put("innings", player.innings)
        put("ballsFaced", player.ballsFaced)
        put("fours", player.fours)
        put("sixes", player.sixes)
        put("fifties", player.fifties)
        put("hundreds", player.hundreds)
        put("highestScore", player.highestScore)
        put("notOuts", player.notOuts)
        put("ballsBowled", player.ballsBowled)
        put("runsConceded", player.runsConceded)
        put("maidens", player.maidens)
        put("catches", player.catches)
        put("runOuts", player.runOuts)
        put("stumpings", player.stumpings)
    }

    private fun restorePlayers(rows: JSONArray) {
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            player(row.getInt("id"))?.apply {
                form = row.optInt("form", form)
                fitness = row.optInt("fitness", fitness)
                fatigue = row.optInt("fatigue", fatigue)
                morale = row.optInt("morale", morale)
                confidence = row.optInt("confidence", confidence)
                injuryDaysRemaining = row.optInt("injuryDays", 0)
                contract = row.optInt("contract", contract)
                salary = row.optInt("salary", salary)
                overall = row.optInt("overall", overall)
                runs = row.optInt("runs", runs)
                wickets = row.optInt("wickets", wickets)
                matches = row.optInt("matches", matches)
                innings = row.optInt("innings", innings)
                ballsFaced = row.optInt("ballsFaced", ballsFaced)
                fours = row.optInt("fours", fours)
                sixes = row.optInt("sixes", sixes)
                fifties = row.optInt("fifties", fifties)
                hundreds = row.optInt("hundreds", hundreds)
                highestScore = row.optInt("highestScore", highestScore)
                notOuts = row.optInt("notOuts", notOuts)
                ballsBowled = row.optInt("ballsBowled", ballsBowled)
                runsConceded = row.optInt("runsConceded", runsConceded)
                maidens = row.optInt("maidens", maidens)
                catches = row.optInt("catches", catches)
                runOuts = row.optInt("runOuts", runOuts)
                stumpings = row.optInt("stumpings", stumpings)
            }
        }
    }

    private fun restoreTeams(rows: JSONArray) {
        for (i in 0 until rows.length()) {
            val row = rows.getJSONObject(i)
            teams.find { it.name == row.getString("name") }?.apply {
                budget = row.optInt("budget", budget)
                points = row.optInt("points", points)
                played = row.optInt("played", played)
                won = row.optInt("won", won)
                lost = row.optInt("lost", lost)
                tied = row.optInt("tied", tied)
                nrr = row.optDouble("nrr", nrr)
                runsScored = row.optInt("rs", runsScored)
                runsConceded = row.optInt("rc", runsConceded)
                ballsFaced = row.optInt("bf", ballsFaced)
                ballsBowled = row.optInt("bb", ballsBowled)
            }
        }
    }

    private fun resultJson(result: MatchResult) = JSONObject().apply {
        put("opponent", result.opponent)
        put("first", result.ourBatFirst)
        put("ourRuns", result.ourRuns)
        put("ourWickets", result.ourWickets)
        put("rivalRuns", result.rivalRuns)
        put("rivalWickets", result.rivalWickets)
        put("won", result.won)
        put("events", JSONArray(result.scoreEvents.take(50)))
        put("pom", result.playerOfMatch)
        result.record?.let { put("record", recordJson(it)) }
    }

    private fun restoreResult(row: JSONObject) = MatchResult(
        row.getString("opponent"),
        row.getBoolean("first"),
        row.getInt("ourRuns"),
        row.getInt("ourWickets"),
        row.getInt("rivalRuns"),
        row.getInt("rivalWickets"),
        row.getBoolean("won"),
        stringList(row.getJSONArray("events")),
        row.getString("pom"),
        row.optJSONObject("record")?.let(::restoreRecord)
    )

    private fun recordJson(record: MatchRecord) = JSONObject().apply {
        put("home", record.setup.homeTeam)
        put("away", record.setup.awayTeam)
        put("venue", record.setup.venue)
        put("pitch", record.setup.pitch.name)
        put("weather", record.setup.weather.name)
        put("dew", record.setup.dew)
        put("toss", record.setup.tossWinner)
        put("decision", record.setup.tossDecision)
        put("winner", record.winner)
        put("margin", record.margin)
        put("pom", record.playerOfMatch)
        put("super", record.superOver)
        put("first", inningsJson(record.firstInnings))
        put("second", inningsJson(record.secondInnings))
    }

    private fun restoreRecord(row: JSONObject): MatchRecord {
        val setup = MatchSetup(
            row.getString("home"),
            row.getString("away"),
            row.optString("venue"),
            PitchType.valueOf(row.optString("pitch", "BALANCED")),
            Weather.valueOf(row.optString("weather", "CLEAR")),
            row.optBoolean("dew"),
            row.optString("toss"),
            row.optString("decision", "BAT"),
            homeXI = emptyList(),
            awayXI = emptyList(),
            homeCaptainId = 0,
            awayCaptainId = 0
        )
        return MatchRecord(
            setup,
            restoreInnings(row.getJSONObject("first")),
            restoreInnings(row.getJSONObject("second")),
            row.getString("winner"),
            row.getString("margin"),
            row.getString("pom"),
            row.optBoolean("super")
        )
    }

    private fun inningsJson(innings: InningsScorecard) = JSONObject().apply {
        put("team", innings.battingTeam)
        put("runs", innings.runs)
        put("wk", innings.wickets)
        put("balls", innings.legalBalls)
        put("target", innings.target ?: -1)
        put("extras", innings.extras)
        put("bat", JSONArray(innings.batters.map {
            JSONObject().put("id", it.playerId).put("name", it.name).put("r", it.runs).put("b", it.balls).put("4", it.fours).put("6", it.sixes).put("out", it.dismissal)
        }))
        put("bowl", JSONArray(innings.bowlers.map {
            JSONObject().put("id", it.playerId).put("name", it.name).put("b", it.balls).put("r", it.runs).put("w", it.wickets).put("d", it.dots).put("m", it.maidens)
        }))
    }

    private fun restoreInnings(row: JSONObject): InningsScorecard {
        val bat = row.getJSONArray("bat")
        val bowl = row.getJSONArray("bowl")
        return InningsScorecard(
            row.getString("team"),
            row.getInt("runs"),
            row.getInt("wk"),
            row.getInt("balls"),
            row.optInt("target").takeIf { it > 0 },
            List(bat.length()) { i ->
                bat.getJSONObject(i).let {
                    BatterLine(it.getInt("id"), it.getString("name"), it.getInt("r"), it.getInt("b"), it.getInt("4"), it.getInt("6"), it.getString("out"))
                }
            },
            List(bowl.length()) { i ->
                bowl.getJSONObject(i).let {
                    BowlerLine(it.getInt("id"), it.getString("name"), it.getInt("b"), it.getInt("r"), it.getInt("w"), it.getInt("d"), it.getInt("m"))
                }
            },
            emptyList(),
            row.optInt("extras")
        )
    }

    private fun liveJson(live: LiveMatchState): JSONObject {
        val setup = live.setup
        val current = InningsScorecard(
            live.current.battingTeam, live.current.runs, live.current.wickets, live.current.legalBalls,
            live.current.target, live.current.batters, live.current.bowlers.values.toList(), live.current.deliveries, live.current.extras
        )
        return JSONObject().apply {
            put("homeIds", JSONArray(setup.homeXI.map { it.id }))
            put("awayIds", JSONArray(setup.awayXI.map { it.id }))
            put("home", setup.homeTeam)
            put("away", setup.awayTeam)
            put("toss", setup.tossWinner)
            put("decision", setup.tossDecision)
            put("inning", live.inningsNumber)
            put("current", inningsJson(current))
            put("striker", live.current.striker)
            put("non", live.current.nonStriker)
            put("next", live.current.nextBatter)
            put("last", live.current.lastBowlerId)
            put("partnership", live.current.partnership)
            put("batTactic", live.battingApproach.name)
            put("fieldTactic", live.fieldingApproach.name)
            live.firstInnings?.let { put("first", inningsJson(it)) }
        }
    }

    private fun restoreLive(row: JSONObject): LiveMatchState? = runCatching {
        val home = intList(row.getJSONArray("homeIds")).mapNotNull(::player)
        val awayIds = intList(row.getJSONArray("awayIds"))
        val away = awayIds.map { id ->
            player(id - 20_000)?.copy(id = id, name = "Metro ${player(id - 20_000)?.name?.substringAfter(' ')}")
        }.filterNotNull()
        if (home.size != 11 || away.size != 11) return null

        val setup = MatchSetup(
            row.getString("home"), row.getString("away"),
            tossWinner = row.getString("toss"), tossDecision = row.getString("decision"),
            homeXI = home, awayXI = away, homeCaptainId = home.first().id, awayCaptainId = away.first().id
        )
        val score = restoreInnings(row.getJSONObject("current"))
        val bat = if (score.battingTeam == setup.homeTeam) home else away
        val bowl = if (score.battingTeam == setup.homeTeam) away else home

        val current = LiveInningsState(
            score.battingTeam, bat, bowl, score.target,
            score.batters.toMutableList(),
            score.bowlers.associateBy { it.playerId }.toMutableMap(),
            mutableListOf(), score.runs, score.wickets, score.legalBalls, score.extras,
            row.getInt("striker"), row.getInt("non"), row.getInt("next"), row.getInt("last"), row.getInt("partnership")
        )

        LiveMatchState(
            setup, row.getInt("inning"), current,
            row.optJSONObject("first")?.let(::restoreInnings),
            BattingApproach.valueOf(row.optString("batTactic", "BALANCED")),
            BowlingApproach.valueOf(row.optString("fieldTactic", "BALANCED"))
        )
    }.getOrNull()

    private fun intList(rows: JSONArray) = List(rows.length()) { rows.getInt(it) }
    private fun stringList(rows: JSONArray) = List(rows.length()) { rows.getString(it) }

    fun player(id: Int) = squad.find { it.id == id } ?: auctionPool.find { it.id == id }

    fun reset() {
        prefs.edit().clear().apply()
        val loadedData = RosterLoader.loadRoster(context)
        squad = loadedData.squad
        auctionPool = loadedData.auctionPool
        teams = loadedData.teams
        venues = loadedData.venues
        state = GameState(
            selectedXI = squad.take(11).map { it.id }.toMutableList(),
            news = mutableListOf("Game state reset to default. Welcome to your new campaign!", "Squad facilities cleared.")
        )
        state.fixtures = SeasonEngine().fixtures(teams)
    }
}
