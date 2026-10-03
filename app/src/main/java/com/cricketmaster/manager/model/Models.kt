package com.cricketmaster.manager.model

/** Core Player Model with full ratings, contracts, career stats, fatigue, morale, and injuries. */
data class Player(
    val id: Int,
    val name: String,
    val age: Int,
    val nationality: String,
    val role: String, // "Opener", "Top-Order Batter", "Middle-Order Batter", "Finisher", "Wicketkeeper", "All-Rounder", "Fast Bowler", "Spin Bowler"
    var overall: Int,
    val potential: Int,
    var value: Int,
    var salary: Int,
    var contract: Int, // years remaining
    var form: Int = 70, // 0 - 100
    var fitness: Int = 95, // 0 - 100
    var fatigue: Int = 5, // 0 - 100
    var morale: Int = 80, // 0 - 100
    var confidence: Int = 75, // 0 - 100
    var injuryDaysRemaining: Int = 0,
    var injuryType: String? = null,

    // Batting Skills
    val bat: Int = 70,
    val power: Int = 70,
    val technique: Int = bat,
    val timing: Int = bat,
    val strikeRotation: Int = bat,
    val paceBatting: Int = bat,
    val spinBatting: Int = bat,
    val deathBatting: Int = power,

    // Bowling Skills
    val pace: Int = 50,
    val accuracy: Int = 50,
    val spin: Int = 30,
    val swing: Int = accuracy,
    val seam: Int = pace,
    val variation: Int = accuracy,
    val powerplayBowling: Int = pace,
    val deathBowling: Int = accuracy,
    val yorker: Int = accuracy,
    val slowerBall: Int = accuracy,

    // Fielding & Mental
    val fielding: Int = 70,
    val catching: Int = fielding,
    val throwing: Int = fielding,
    val groundFielding: Int = fielding,
    val mental: Int = 70,
    val pressureHandling: Int = mental,
    val consistency: Int = mental,
    val experience: Int = overall,

    // Career Batting Stats
    var matches: Int = 0,
    var innings: Int = 0,
    var runs: Int = 0,
    var ballsFaced: Int = 0,
    var fours: Int = 0,
    var sixes: Int = 0,
    var fifties: Int = 0,
    var hundreds: Int = 0,
    var highestScore: Int = 0,
    var notOuts: Int = 0,

    // Career Bowling Stats
    var ballsBowled: Int = 0,
    var runsConceded: Int = 0,
    var wickets: Int = 0,
    var maidens: Int = 0,
    var bestBowlingWickets: Int = 0,
    var bestBowlingRuns: Int = 0,

    // Career Fielding Stats
    var catches: Int = 0,
    var runOuts: Int = 0,
    var stumpings: Int = 0
) {
    val battingOverall get() = ((technique + timing + power + pressureHandling + paceBatting + spinBatting) / 6)
    val bowlingOverall get() = ((pace + accuracy + spin + variation + deathBowling + powerplayBowling) / 6)
    val battingAverage get() = if (innings - notOuts <= 0) runs.toDouble() else runs.toDouble() / (innings - notOuts)
    val battingStrikeRate get() = if (ballsFaced == 0) 0.0 else runs * 100.0 / ballsFaced
    val bowlingEconomy get() = if (ballsBowled == 0) 0.0 else runsConceded * 6.0 / ballsBowled
    val bowlingAverage get() = if (wickets == 0) 0.0 else runsConceded.toDouble() / wickets
    val isInjured get() = injuryDaysRemaining > 0
}

/** Team model with strategy, finance, and league stats. */
data class Team(
    val name: String,
    var budget: Int,
    val strategy: String = "Balanced", // "Aggressive", "Youth", "Stars", "Value", "Bowling"
    var points: Int = 0,
    var played: Int = 0,
    var won: Int = 0,
    var lost: Int = 0,
    var tied: Int = 0,
    var nrr: Double = 0.0,
    var runsScored: Int = 0,
    var runsConceded: Int = 0,
    var ballsFaced: Int = 0,
    var ballsBowled: Int = 0,
    val homeVenue: String = "$name Stadium"
)

/** Coaching staff model. */
data class Staff(
    val id: Int,
    val name: String,
    val role: String, // "Head Coach", "Batting Coach", "Bowling Coach", "Fitness Coach", "Physio"
    val rating: Int, // 1 - 100
    val salary: Int
)

/** Stadium / Venue details. */
data class Venue(
    val id: String,
    val name: String,
    val city: String,
    val capacity: Int,
    val pitch: PitchType = PitchType.BALANCED,
    val boundarySize: Int = 70
)

enum class PitchType { FLAT, GREEN, DRY, BALANCED }
enum class Weather { CLEAR, OVERCAST, HUMID }
enum class MatchPhase { POWERPLAY, MIDDLE, DEATH }
enum class BattingApproach { DEFENSIVE, BALANCED, AGGRESSIVE, CHASE, DEATH_ATTACK }
enum class BowlingApproach { ATTACK, BALANCED, DEFENSIVE, YORKER, BOUNCER, SLOWER_BALL, SWING, SPIN_ATTACK }

data class MatchSetup(
    val homeTeam: String,
    val awayTeam: String,
    val venue: String = "Riverside Oval",
    val pitch: PitchType = PitchType.BALANCED,
    val weather: Weather = Weather.CLEAR,
    val dew: Boolean = false,
    val tossWinner: String = homeTeam,
    val tossDecision: String = "BAT",
    val overs: Int = 20,
    val homeXI: List<Player>,
    val awayXI: List<Player>,
    val homeCaptainId: Int,
    val awayCaptainId: Int,
    val homeKeeperId: Int? = null,
    val awayKeeperId: Int? = null
)

data class BatterLine(
    val playerId: Int,
    val name: String,
    var runs: Int = 0,
    var balls: Int = 0,
    var fours: Int = 0,
    var sixes: Int = 0,
    var dismissal: String = "not out"
) {
    val strikeRate get() = if (balls == 0) 0.0 else runs * 100.0 / balls
}

data class BowlerLine(
    val playerId: Int,
    val name: String,
    var balls: Int = 0,
    var runs: Int = 0,
    var wickets: Int = 0,
    var dots: Int = 0,
    var maidens: Int = 0
) {
    val overs get() = "${balls / 6}.${balls % 6}"
    val economy get() = if (balls == 0) 0.0 else runs * 6.0 / balls
}

data class Delivery(
    val innings: Int,
    val over: Int,
    val ball: Int,
    val batter: String,
    val bowler: String,
    val type: String,
    val runs: Int,
    val extras: Int = 0,
    val wicket: String? = null,
    val legal: Boolean = true
)

data class InningsScorecard(
    val battingTeam: String,
    var runs: Int,
    var wickets: Int,
    var legalBalls: Int,
    val target: Int? = null,
    val batters: List<BatterLine>,
    val bowlers: List<BowlerLine>,
    val deliveries: List<Delivery>,
    var extras: Int = 0
) {
    val overs get() = "${legalBalls / 6}.${legalBalls % 6}"
    val runRate get() = if (legalBalls == 0) 0.0 else runs * 6.0 / legalBalls
}

data class MatchRecord(
    val setup: MatchSetup,
    val firstInnings: InningsScorecard,
    val secondInnings: InningsScorecard,
    val winner: String,
    val margin: String,
    val playerOfMatch: String,
    val superOver: Boolean = false
)

data class MatchResult(
    val opponent: String,
    val ourBatFirst: Boolean,
    val ourRuns: Int,
    val ourWickets: Int,
    val rivalRuns: Int,
    val rivalWickets: Int,
    val won: Boolean,
    val scoreEvents: List<String>,
    val playerOfMatch: String,
    val record: MatchRecord? = null
)

data class Fixture(
    val id: String,
    val round: Int,
    val home: String,
    val away: String,
    val venue: String,
    var status: String = "SCHEDULED", // "SCHEDULED", "COMPLETED"
    var result: String = ""
)

data class LiveInningsState(
    val battingTeam: String,
    val battingXI: List<Player>,
    val bowlingXI: List<Player>,
    val target: Int? = null,
    val batters: MutableList<BatterLine> = battingXI.map { BatterLine(it.id, it.name) }.toMutableList(),
    val bowlers: MutableMap<Int, BowlerLine> = bowlingXI.associate { it.id to BowlerLine(it.id, it.name) }.toMutableMap(),
    val deliveries: MutableList<Delivery> = mutableListOf(),
    var runs: Int = 0,
    var wickets: Int = 0,
    var legalBalls: Int = 0,
    var extras: Int = 0,
    var striker: Int = 0,
    var nonStriker: Int = 1,
    var nextBatter: Int = 2,
    var lastBowlerId: Int = -1,
    var partnership: Int = 0
)

data class LiveMatchState(
    val setup: MatchSetup,
    var inningsNumber: Int,
    var current: LiveInningsState,
    var firstInnings: InningsScorecard? = null,
    var battingApproach: BattingApproach = BattingApproach.BALANCED,
    var fieldingApproach: BowlingApproach = BowlingApproach.BALANCED,
    var completed: Boolean = false,
    var result: MatchResult? = null
) {
    val phase get() = when {
        current.legalBalls < 36 -> MatchPhase.POWERPLAY
        current.legalBalls < 90 -> MatchPhase.MIDDLE
        else -> MatchPhase.DEATH
    }
    val overs get() = "${current.legalBalls / 6}.${current.legalBalls % 6}"
}

data class NotificationEvent(
    val title: String,
    val message: String,
    val type: String = "GENERAL", // "INJURY", "FINANCE", "AUCTION", "MATCH", "TRAINING"
    val timestamp: Long = System.currentTimeMillis()
)

data class FinancialRecord(
    val description: String,
    val amount: Int, // Positive for income, negative for expense
    val category: String = "GENERAL"
)

data class GameState(
    var season: Int = 1,
    var teamName: String = "Harbor Hawks",
    var budget: Int = 120_000_000,
    var selectedXI: MutableList<Int> = mutableListOf(),
    var captainId: Int = 1,
    var viceCaptainId: Int = 2,
    var wicketKeeperId: Int = 3,
    var news: MutableList<String> = mutableListOf(),
    var notifications: MutableList<NotificationEvent> = mutableListOf(),
    var lastResult: MatchResult? = null,
    var sponsor: String = "Northstar Sports",
    var trainingLevel: Int = 1,
    var stadiumLevel: Int = 1,
    var medicalLevel: Int = 1,
    var academyLevel: Int = 1,
    var auctionIndex: Int = 0,
    var auctionBid: Int = 0,
    var auctionHistory: MutableList<String> = mutableListOf(),
    var tactic: String = "Balanced",
    var signedAuctionIds: MutableList<Int> = mutableListOf(),
    var matchHistory: MutableList<MatchResult> = mutableListOf(),
    var liveMatch: LiveMatchState? = null,
    var fixtures: MutableList<Fixture> = mutableListOf(),
    var pendingToss: String = "",
    var devModeEnabled: Boolean = false
)
