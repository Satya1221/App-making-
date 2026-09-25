package com.cricketmaster.manager.engine

import android.content.Context
import com.cricketmaster.manager.data.SampleData
import com.cricketmaster.manager.model.*
import org.json.JSONArray
import org.json.JSONObject

/** Owns the mutable campaign and persists enough state to resume a season after process death. */
class GameRepository(context: Context) {
    private val prefs = context.getSharedPreferences("cricket_manager_save", Context.MODE_PRIVATE)
    val squad = SampleData.squad()
    val auctionPool = SampleData.auctionPool()
    val teams = SampleData.teams
    val state = GameState(
        selectedXI = squad.take(11).map { it.id }.toMutableList(),
        news = mutableListOf("Welcome back, Manager. Your season begins today.", "Training facilities are ready for the squad.")
    )

    init { load(); if (state.fixtures.isEmpty()) state.fixtures = SeasonEngine().fixtures(teams) }

    fun save() {
        val root = JSONObject()
            .put("budget", state.budget).put("season", state.season).put("xi", JSONArray(state.selectedXI))
            .put("captain", state.captainId).put("viceCaptain", state.viceCaptainId).put("news", JSONArray(state.news))
            .put("auction", state.auctionIndex).put("auctionBid", state.auctionBid).put("history", JSONArray(state.auctionHistory))
            .put("tactic", state.tactic).put("signed", JSONArray(state.signedAuctionIds))
            .put("training", state.trainingLevel).put("stadium", state.stadiumLevel)
            .put("matchHistory", JSONArray(state.matchHistory.map(::resultJson)))
            .put("fixtures", JSONArray(state.fixtures.map { JSONObject().put("id",it.id).put("round",it.round).put("home",it.home).put("away",it.away).put("venue",it.venue).put("status",it.status).put("result",it.result) }))
            .also { root -> state.liveMatch?.let { root.put("liveMatch", liveJson(it)) } }
            .put("players", JSONArray(squad.map(::playerJson)))
            .put("teams", JSONArray(teams.map { JSONObject().put("name", it.name).put("budget", it.budget).put("points", it.points).put("played", it.played).put("won", it.won).put("lost", it.lost).put("nrr", it.nrr).put("tied",it.tied).put("rs",it.runsScored).put("rc",it.runsConceded).put("bf",it.ballsFaced).put("bb",it.ballsBowled) }))
        state.lastResult?.let { root.put("lastResult", resultJson(it)) }
        prefs.edit().putString("state", root.toString()).apply()
    }

    private fun load() {
        val raw = prefs.getString("state", null) ?: return
        runCatching {
            val root = JSONObject(raw)
            state.budget = root.getInt("budget"); state.season = root.getInt("season")
            state.selectedXI = intList(root.getJSONArray("xi")); state.captainId = root.getInt("captain")
            state.viceCaptainId = root.optInt("viceCaptain", state.viceCaptainId); state.news = stringList(root.getJSONArray("news"))
            state.auctionIndex = root.optInt("auction"); state.auctionBid = root.optInt("auctionBid")
            state.auctionHistory = root.optJSONArray("history")?.let(::stringList)?.toMutableList() ?: mutableListOf()
            state.tactic = root.optString("tactic", state.tactic); state.trainingLevel = root.optInt("training", 1); state.stadiumLevel = root.optInt("stadium", 1)
            state.signedAuctionIds = root.optJSONArray("signed")?.let(::intList)?.toMutableList() ?: mutableListOf()
            state.matchHistory = root.optJSONArray("matchHistory")?.let { rows -> List(rows.length()) { restoreResult(rows.getJSONObject(it)) }.toMutableList() } ?: mutableListOf(); state.fixtures=root.optJSONArray("fixtures")?.let{rows->List(rows.length()){i->rows.getJSONObject(i).let{Fixture(it.getString("id"),it.getInt("round"),it.getString("home"),it.getString("away"),it.getString("venue"),it.optString("status","SCHEDULED"),it.optString("result"))}}.toMutableList()}?:mutableListOf()
            state.signedAuctionIds.forEach { id -> auctionPool.find { it.id == id }?.let { if (squad.none { member -> member.id == id }) squad.add(it) } }
            root.optJSONArray("players")?.let { restorePlayers(it) }; root.optJSONArray("teams")?.let { restoreTeams(it) }
            root.optJSONObject("liveMatch")?.let { state.liveMatch = restoreLive(it) }
            root.optJSONObject("lastResult")?.let { state.lastResult = restoreResult(it) }
        }
    }

    private fun playerJson(player: Player) = JSONObject().put("id", player.id).put("form", player.form).put("fit", player.fitness).put("fat", player.fatigue).put("runs", player.runs).put("wk", player.wickets).put("m", player.matches).put("contract", player.contract).put("bf",player.ballsFaced).put("4",player.fours).put("6",player.sixes).put("50",player.fifties).put("100",player.hundreds).put("bb",player.ballsBowled).put("rc",player.runsConceded).put("confidence",player.confidence).put("catches",player.catches).put("ro",player.runOuts).put("st",player.stumpings)
    private fun restorePlayers(rows: JSONArray) { for (index in 0 until rows.length()) { val row = rows.getJSONObject(index); player(row.getInt("id"))?.apply { form=row.getInt("form"); fitness=row.getInt("fit"); fatigue=row.getInt("fat"); runs=row.getInt("runs"); wickets=row.getInt("wk"); matches=row.getInt("m"); contract=row.getInt("contract"); ballsFaced=row.optInt("bf");fours=row.optInt("4");sixes=row.optInt("6");fifties=row.optInt("50");hundreds=row.optInt("100");ballsBowled=row.optInt("bb");runsConceded=row.optInt("rc");confidence=row.optInt("confidence",70);catches=row.optInt("catches");runOuts=row.optInt("ro");stumpings=row.optInt("st") } } }
    private fun restoreTeams(rows: JSONArray) { for (index in 0 until rows.length()) { val row=rows.getJSONObject(index); teams.find { it.name==row.getString("name") }?.apply { budget=row.getInt("budget"); points=row.getInt("points"); played=row.getInt("played"); won=row.getInt("won"); lost=row.getInt("lost"); nrr=row.getDouble("nrr");tied=row.optInt("tied");runsScored=row.optInt("rs");runsConceded=row.optInt("rc");ballsFaced=row.optInt("bf");ballsBowled=row.optInt("bb") } } }
    private fun resultJson(result: MatchResult) = JSONObject().put("opponent",result.opponent).put("first",result.ourBatFirst).put("ourRuns",result.ourRuns).put("ourWickets",result.ourWickets).put("rivalRuns",result.rivalRuns).put("rivalWickets",result.rivalWickets).put("won",result.won).put("events",JSONArray(result.scoreEvents)).put("pom",result.playerOfMatch).also { root -> result.record?.let { root.put("record", recordJson(it)) } }
    private fun restoreResult(row: JSONObject) = MatchResult(row.getString("opponent"),row.getBoolean("first"),row.getInt("ourRuns"),row.getInt("ourWickets"),row.getInt("rivalRuns"),row.getInt("rivalWickets"),row.getBoolean("won"),stringList(row.getJSONArray("events")),row.getString("pom"),row.optJSONObject("record")?.let(::restoreRecord))
    private fun recordJson(record: MatchRecord) = JSONObject().put("home",record.setup.homeTeam).put("away",record.setup.awayTeam).put("venue",record.setup.venue).put("pitch",record.setup.pitch.name).put("weather",record.setup.weather.name).put("dew",record.setup.dew).put("toss",record.setup.tossWinner).put("decision",record.setup.tossDecision).put("winner",record.winner).put("margin",record.margin).put("pom",record.playerOfMatch).put("super",record.superOver).put("first",inningsJson(record.firstInnings)).put("second",inningsJson(record.secondInnings))
    private fun inningsJson(innings: InningsScorecard) = JSONObject().put("team",innings.battingTeam).put("runs",innings.runs).put("wk",innings.wickets).put("balls",innings.legalBalls).put("target",innings.target ?: -1).put("extras",innings.extras).put("bat",JSONArray(innings.batters.map { JSONObject().put("id",it.playerId).put("name",it.name).put("r",it.runs).put("b",it.balls).put("4",it.fours).put("6",it.sixes).put("out",it.dismissal) })).put("bowl",JSONArray(innings.bowlers.map { JSONObject().put("id",it.playerId).put("name",it.name).put("b",it.balls).put("r",it.runs).put("w",it.wickets).put("d",it.dots).put("m",it.maidens) })).put("deliveries",JSONArray(innings.deliveries.map { JSONObject().put("i",it.innings).put("o",it.over).put("b",it.ball).put("bat",it.batter).put("bowl",it.bowler).put("type",it.type).put("r",it.runs).put("e",it.extras).put("w",it.wicket).put("legal",it.legal) }))
    private fun restoreRecord(row: JSONObject): MatchRecord { val setup=MatchSetup(row.getString("home"),row.getString("away"),row.optString("venue"),PitchType.valueOf(row.optString("pitch","BALANCED")),Weather.valueOf(row.optString("weather","CLEAR")),row.optBoolean("dew"),row.optString("toss"),row.optString("decision","BAT"),homeXI=emptyList(),awayXI=emptyList(),homeCaptainId=0,awayCaptainId=0);return MatchRecord(setup,restoreInnings(row.getJSONObject("first")),restoreInnings(row.getJSONObject("second")),row.getString("winner"),row.getString("margin"),row.getString("pom"),row.optBoolean("super")) }
    private fun restoreInnings(row: JSONObject): InningsScorecard { val bat=row.getJSONArray("bat");val bowl=row.getJSONArray("bowl");val del=row.getJSONArray("deliveries");return InningsScorecard(row.getString("team"),row.getInt("runs"),row.getInt("wk"),row.getInt("balls"),row.optInt("target").takeIf{it>0},List(bat.length()){i->bat.getJSONObject(i).let{BatterLine(it.getInt("id"),it.getString("name"),it.getInt("r"),it.getInt("b"),it.getInt("4"),it.getInt("6"),it.getString("out"))}},List(bowl.length()){i->bowl.getJSONObject(i).let{BowlerLine(it.getInt("id"),it.getString("name"),it.getInt("b"),it.getInt("r"),it.getInt("w"),it.getInt("d"),it.getInt("m"))}},List(del.length()){i->del.getJSONObject(i).let{Delivery(it.getInt("i"),it.getInt("o"),it.getInt("b"),it.getString("bat"),it.getString("bowl"),it.getString("type"),it.getInt("r"),it.getInt("e"),if(it.isNull("w"))null else it.getString("w"),it.getBoolean("legal"))}},row.optInt("extras")) }
    private fun liveJson(live: LiveMatchState): JSONObject { val setup=live.setup;val current=InningsScorecard(live.current.battingTeam,live.current.runs,live.current.wickets,live.current.legalBalls,live.current.target,live.current.batters,live.current.bowlers.values.toList(),live.current.deliveries,live.current.extras);return JSONObject().put("homeIds",JSONArray(setup.homeXI.map{it.id})).put("awayIds",JSONArray(setup.awayXI.map{it.id})).put("home",setup.homeTeam).put("away",setup.awayTeam).put("toss",setup.tossWinner).put("decision",setup.tossDecision).put("inning",live.inningsNumber).put("current",inningsJson(current)).put("striker",live.current.striker).put("non",live.current.nonStriker).put("next",live.current.nextBatter).put("last",live.current.lastBowlerId).put("partnership",live.current.partnership).put("batTactic",live.battingApproach.name).put("fieldTactic",live.fieldingApproach.name).also{live.firstInnings?.let{first->it.put("first",inningsJson(first))}} }
    private fun restoreLive(row: JSONObject): LiveMatchState? = runCatching { val home=intList(row.getJSONArray("homeIds")).mapNotNull(::player);val awayIds=intList(row.getJSONArray("awayIds"));val away=awayIds.map { id -> player(id-20_000)?.copy(id=id,name="Metro ${player(id-20_000)?.name?.substringAfter(' ')}") }.filterNotNull();if(home.size!=11||away.size!=11)return null;val setup=MatchSetup(row.getString("home"),row.getString("away"),tossWinner=row.getString("toss"),tossDecision=row.getString("decision"),homeXI=home,awayXI=away,homeCaptainId=home.first().id,awayCaptainId=away.first().id);val score=restoreInnings(row.getJSONObject("current"));val bat=if(score.battingTeam==setup.homeTeam)home else away;val bowl=if(score.battingTeam==setup.homeTeam)away else home;val current=LiveInningsState(score.battingTeam,bat,bowl,score.target,score.batters.toMutableList(),score.bowlers.associateBy{it.playerId}.toMutableMap(),score.deliveries.toMutableList(),score.runs,score.wickets,score.legalBalls,score.extras,row.getInt("striker"),row.getInt("non"),row.getInt("next"),row.getInt("last"),row.getInt("partnership"));LiveMatchState(setup,row.getInt("inning"),current,row.optJSONObject("first")?.let(::restoreInnings),BattingApproach.valueOf(row.optString("batTactic","BALANCED")),BowlingApproach.valueOf(row.optString("fieldTactic","BALANCED"))) }.getOrNull()
    private fun intList(rows: JSONArray) = List(rows.length()) { rows.getInt(it) }.toMutableList()
    private fun stringList(rows: JSONArray) = List(rows.length()) { rows.getString(it) }.toMutableList()
    fun player(id: Int) = squad.find { it.id == id }
    fun reset() { prefs.edit().clear().apply() }
}
