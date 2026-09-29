package com.cricketmaster.manager.model

/** Persistent player ratings. New fields have defaults so older campaigns remain loadable. */
data class Player(
    val id: Int, val name: String, val age: Int, val nationality: String, val role: String,
    var overall: Int, val potential: Int, var value: Int, var salary: Int, var contract: Int,
    var form: Int = 70, var fitness: Int = 92, var fatigue: Int = 8,
    val bat: Int, val power: Int, val pace: Int, val accuracy: Int, val spin: Int,
    val fielding: Int, val mental: Int, var runs: Int = 0, var wickets: Int = 0, var matches: Int = 0,
    val technique: Int = bat, val timing: Int = bat, val strikeRotation: Int = bat, val boundaryAbility: Int = power,
    val runningBetweenWickets: Int = fielding, val spinBatting: Int = bat, val paceBatting: Int = bat,
    val deathBatting: Int = power, val pressureHandling: Int = mental, val swing: Int = accuracy,
    val seam: Int = pace, val variation: Int = accuracy, val deathBowling: Int = accuracy,
    val powerplayBowling: Int = pace, val yorker: Int = accuracy, val slowerBall: Int = accuracy,
    val catching: Int = fielding, val throwing: Int = fielding, val groundFielding: Int = fielding,
    var confidence: Int = 70, val consistency: Int = mental, val experience: Int = overall,
    var ballsFaced: Int = 0, var fours: Int = 0, var sixes: Int = 0, var fifties: Int = 0, var hundreds: Int = 0,
    var ballsBowled: Int = 0, var runsConceded: Int = 0, var catches: Int = 0, var runOuts: Int = 0, var stumpings: Int = 0
) {
    val battingOverall get() = ((technique + timing + power + pressureHandling + paceBatting + spinBatting) / 6)
    val bowlingOverall get() = ((pace + accuracy + spin + variation + deathBowling + powerplayBowling) / 6)
}

data class Team(val name: String, var budget: Int, val strategy: String, var points: Int = 0, var played: Int = 0, var won: Int = 0, var lost: Int = 0, var nrr: Double = 0.0, var tied: Int = 0, var runsScored: Int = 0, var runsConceded: Int = 0, var ballsFaced: Int = 0, var ballsBowled: Int = 0)
data class Venue(val id: String, val name: String, val city: String, val capacity: Int, val pitch: PitchType, val boundarySize: Int)
enum class PitchType { FLAT, GREEN, DRY, BALANCED }
enum class Weather { CLEAR, OVERCAST, HUMID }
enum class MatchPhase { POWERPLAY, MIDDLE, DEATH }
enum class BattingApproach { DEFENSIVE, BALANCED, AGGRESSIVE, CHASE, DEATH_ATTACK }
enum class BowlingApproach { ATTACK, BALANCED, DEFENSIVE, YORKER, BOUNCER, SLOWER_BALL, SWING, SPIN_ATTACK }

data class MatchSetup(val homeTeam: String, val awayTeam: String, val venue: String = "Riverside Oval", val pitch: PitchType = PitchType.BALANCED, val weather: Weather = Weather.CLEAR, val dew: Boolean = false, val tossWinner: String = homeTeam, val tossDecision: String = "BAT", val overs: Int = 20, val homeXI: List<Player>, val awayXI: List<Player>, val homeCaptainId: Int, val awayCaptainId: Int, val homeKeeperId: Int? = null, val awayKeeperId: Int? = null)
data class BatterLine(val playerId: Int, val name: String, var runs: Int = 0, var balls: Int = 0, var fours: Int = 0, var sixes: Int = 0, var dismissal: String = "not out") { val strikeRate get() = if (balls == 0) 0.0 else runs * 100.0 / balls }
data class BowlerLine(val playerId: Int, val name: String, var balls: Int = 0, var runs: Int = 0, var wickets: Int = 0, var dots: Int = 0, var maidens: Int = 0) { val overs get() = "${balls / 6}.${balls % 6}"; val economy get() = if (balls == 0) 0.0 else runs * 6.0 / balls }
data class Delivery(val innings: Int, val over: Int, val ball: Int, val batter: String, val bowler: String, val type: String, val runs: Int, val extras: Int = 0, val wicket: String? = null, val legal: Boolean = true)
data class InningsScorecard(val battingTeam: String, var runs: Int, var wickets: Int, var legalBalls: Int, val target: Int? = null, val batters: List<BatterLine>, val bowlers: List<BowlerLine>, val deliveries: List<Delivery>, var extras: Int = 0) { val overs get() = "${legalBalls / 6}.${legalBalls % 6}"; val runRate get() = if (legalBalls == 0) 0.0 else runs * 6.0 / legalBalls }
data class MatchRecord(val setup: MatchSetup, val firstInnings: InningsScorecard, val secondInnings: InningsScorecard, val winner: String, val margin: String, val playerOfMatch: String, val superOver: Boolean = false)
data class BallEvent(val text: String, val runs: Int, val wicket: Boolean)
data class MatchResult(val opponent: String, val ourBatFirst: Boolean, val ourRuns: Int, val ourWickets: Int, val rivalRuns: Int, val rivalWickets: Int, val won: Boolean, val scoreEvents: List<String>, val playerOfMatch: String, val record: MatchRecord? = null)
data class Fixture(val id: String, val round: Int, val home: String, val away: String, val venue: String, var status: String = "SCHEDULED", var result: String = "")
data class LiveInningsState(val battingTeam: String, val battingXI: List<Player>, val bowlingXI: List<Player>, val target: Int? = null, val batters: MutableList<BatterLine> = battingXI.map { BatterLine(it.id, it.name) }.toMutableList(), val bowlers: MutableMap<Int, BowlerLine> = bowlingXI.associate { it.id to BowlerLine(it.id, it.name) }.toMutableMap(), val deliveries: MutableList<Delivery> = mutableListOf(), var runs: Int = 0, var wickets: Int = 0, var legalBalls: Int = 0, var extras: Int = 0, var striker: Int = 0, var nonStriker: Int = 1, var nextBatter: Int = 2, var lastBowlerId: Int = -1, var partnership: Int = 0)
data class LiveMatchState(val setup: MatchSetup, var inningsNumber: Int, var current: LiveInningsState, var firstInnings: InningsScorecard? = null, var battingApproach: BattingApproach = BattingApproach.BALANCED, var fieldingApproach: BowlingApproach = BowlingApproach.BALANCED, var completed: Boolean = false, var result: MatchResult? = null) {
    val phase get() = when { current.legalBalls < 36 -> MatchPhase.POWERPLAY; current.legalBalls < 90 -> MatchPhase.MIDDLE; else -> MatchPhase.DEATH }
    val overs get() = "${current.legalBalls / 6}.${current.legalBalls % 6}"
}

data class GameState(
    var season: Int = 1, var teamName: String = "Harbor Hawks", var budget: Int = 120_000_000,
    var selectedXI: MutableList<Int> = mutableListOf(), var captainId: Int = 1, var viceCaptainId: Int = 2, var wicketKeeperId: Int = 3,
    var news: MutableList<String> = mutableListOf(), var lastResult: MatchResult? = null,
    var sponsor: String = "Northstar Sports", var trainingLevel: Int = 1, var stadiumLevel: Int = 1,
    var auctionIndex: Int = 0, var auctionBid: Int = 0, var auctionHistory: MutableList<String> = mutableListOf(),
    var tactic: String = "Balanced", var signedAuctionIds: MutableList<Int> = mutableListOf(), var matchHistory: MutableList<MatchResult> = mutableListOf(), var liveMatch: LiveMatchState? = null, var fixtures: MutableList<Fixture> = mutableListOf(), var pendingToss: String = ""
)
