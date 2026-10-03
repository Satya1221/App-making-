package com.cricketmaster.manager.engine

import com.cricketmaster.manager.model.*
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** Unified Delivery Engine calculating ball outcomes based on player ratings, pitch, weather, phase, and tactics. */
class DeliveryEngine(private val random: Random = Random.Default) {

    fun deliver(
        innings: Int,
        over: Int,
        ball: Int,
        batter: Player,
        bowler: Player,
        phase: MatchPhase,
        battingApproach: BattingApproach,
        bowlingApproach: BowlingApproach,
        setup: MatchSetup
    ): Delivery {
        val pitchBatModifier = when (setup.pitch) {
            PitchType.FLAT -> 6
            PitchType.GREEN -> -6
            PitchType.DRY -> -3
            PitchType.BALANCED -> 0
        }
        val weatherBowlModifier = if (setup.weather == Weather.OVERCAST) 4 else 0
        val dewBatModifier = if (setup.dew && innings == 2) 4 else 0

        val intent = when (battingApproach) {
            BattingApproach.DEFENSIVE -> -8
            BattingApproach.BALANCED -> 0
            BattingApproach.CHASE -> 4
            BattingApproach.AGGRESSIVE -> 8
            BattingApproach.DEATH_ATTACK -> 13
        }

        val fieldEffect = when (bowlingApproach) {
            BowlingApproach.DEFENSIVE -> -5
            BowlingApproach.ATTACK -> 2
            BowlingApproach.YORKER -> -4
            BowlingApproach.BOUNCER -> 1
            BowlingApproach.SPIN_ATTACK -> -2
            else -> 0
        }

        val phaseRisk = when (phase) {
            MatchPhase.POWERPLAY -> 2
            MatchPhase.MIDDLE -> -2
            MatchPhase.DEATH -> 6
        }

        val batPower = batter.battingOverall + (batter.form / 8) + (batter.confidence / 12) - (batter.fatigue / 3) + pitchBatModifier + dewBatModifier + intent
        val bowlPower = bowler.bowlingOverall + (bowler.form / 10) - (bowler.fatigue / 3) + weatherBowlModifier + fieldEffect + if (setup.pitch == PitchType.DRY) (bowler.spin / 8) else 0

        val edge = (batPower - bowlPower + phaseRisk).coerceIn(-35, 35)

        // Extras check
        val wideChance = bound(0.02 + (60 - bowler.accuracy) / 700.0)
        if (random.nextDouble() < wideChance) {
            return Delivery(innings, over, ball, batter.name, bowler.name, "wide", 1, extras = 1, wicket = null, legal = false)
        }

        val noBallChance = bound(0.008 + (55 - bowler.accuracy) / 1400.0)
        if (random.nextDouble() < noBallChance) {
            return Delivery(innings, over, ball, batter.name, bowler.name, "no ball", 1, extras = 1, wicket = null, legal = false)
        }

        // Wicket check
        val wicketChance = bound(0.045 - (edge / 900.0) + if (phase == MatchPhase.DEATH) 0.012 else 0.0 + if (bowlingApproach == BowlingApproach.ATTACK) 0.01 else 0.0)
        if (random.nextDouble() < wicketChance) {
            val dismissalTypes = listOf("bowled", "caught", "lbw", "run out", "stumped")
            val dismissal = dismissalTypes.random(random)
            return Delivery(innings, over, ball, batter.name, bowler.name, "wicket", 0, extras = 0, wicket = dismissal, legal = true)
        }

        // Boundary and Run check
        val sixChance = bound(0.02 + (batter.power - bowler.deathBowling) / 700.0 + if (battingApproach == BattingApproach.DEATH_ATTACK) 0.04 else 0.0)
        val fourChance = bound(0.10 + (edge / 450.0) + if (phase == MatchPhase.DEATH) 0.04 else 0.0)

        val roll = random.nextDouble()
        val runs = when {
            roll < sixChance -> 6
            roll < sixChance + fourChance -> 4
            roll < 0.52 + (edge / 1300.0) -> 1
            roll < 0.72 + (edge / 1500.0) -> 2
            roll < 0.77 -> 3
            else -> 0
        }

        return Delivery(innings, over, ball, batter.name, bowler.name, "${phase.name.lowercase()} ball", runs, extras = 0, wicket = null, legal = true)
    }

    private fun bound(value: Double) = min(0.35, max(0.001, value))
}

/** Robust Bowling AI that guaranteed safe bowler selection without throwing NoSuchElementException. */
object BowlingAI {
    fun choose(
        players: List<Player>,
        figures: Map<Int, BowlerLine>,
        lastBowlerId: Int,
        phase: MatchPhase,
        setup: MatchSetup,
        bowlingApproach: BowlingApproach = BowlingApproach.BALANCED
    ): Player {
        if (players.isEmpty()) throw IllegalArgumentException("Player list cannot be empty for bowler selection")

        // Filter players who haven't exceeded 4 overs (24 legal balls) and aren't the last bowler
        val eligible = players.filter { (figures[it.id]?.balls ?: 0) < 24 && it.id != lastBowlerId }

        if (eligible.isNotEmpty()) {
            return eligible.maxByOrNull { p ->
                val phaseSkill = when (phase) {
                    MatchPhase.POWERPLAY -> p.powerplayBowling
                    MatchPhase.MIDDLE -> if (setup.pitch == PitchType.DRY) p.spin else p.accuracy
                    MatchPhase.DEATH -> p.deathBowling + p.yorker
                }
                val tacticalBonus = if (bowlingApproach == BowlingApproach.SPIN_ATTACK) p.spin / 2 else 0
                p.bowlingOverall + (phaseSkill / 3) + tacticalBonus - (p.fatigue / 3)
            } ?: eligible.first()
        }

        // Fallback 1: Any bowler under 24 balls (even if they were the last bowler, in extreme cases)
        val anyUnderLimit = players.filter { (figures[it.id]?.balls ?: 0) < 24 }
        if (anyUnderLimit.isNotEmpty()) {
            return anyUnderLimit.maxByOrNull { it.bowlingOverall } ?: anyUnderLimit.first()
        }

        // Fallback 2: Any bowler in squad if all limits are somehow hit
        return players.first()
    }
}

/** Batch Match Simulator used for instant background match resolution. */
class MatchEngine(private val random: Random = Random.Default) {

    fun simulate(xi: List<Player>, opponentName: String, tactic: String): MatchResult {
        val opponentXI = xi.mapIndexed { index, p ->
            p.copy(
                id = p.id + 10_000 + index,
                name = "${opponentName.split(' ').first()} ${p.name.substringAfter(' ')}",
                form = (p.form - 3).coerceIn(40, 90)
            )
        }

        val setup = MatchSetup(
            homeTeam = "Harbor Hawks",
            awayTeam = opponentName,
            homeXI = xi,
            awayXI = opponentXI,
            homeCaptainId = xi.first().id,
            awayCaptainId = opponentXI.first().id,
            pitch = listOf(PitchType.FLAT, PitchType.GREEN, PitchType.DRY, PitchType.BALANCED).random(random),
            weather = listOf(Weather.CLEAR, Weather.OVERCAST, Weather.HUMID).random(random),
            dew = random.nextBoolean(),
            tossWinner = if (random.nextBoolean()) "Harbor Hawks" else opponentName,
            tossDecision = if (random.nextBoolean()) "BAT" else "FIELD"
        )

        val approach = when (tactic) {
            "Defensive" -> BattingApproach.DEFENSIVE
            "Powerplay Attack", "Aggressive" -> BattingApproach.AGGRESSIVE
            "Death Overs Attack" -> BattingApproach.DEATH_ATTACK
            else -> BattingApproach.BALANCED
        }

        return MatchSimulator(random).play(setup, approach)
    }
}

class MatchSimulator(private val random: Random = Random.Default) {

    fun play(setup: MatchSetup, homeApproach: BattingApproach): MatchResult {
        val homeBatsFirst = (setup.tossWinner == setup.homeTeam) == (setup.tossDecision == "BAT")

        val firstTeam = if (homeBatsFirst) setup.homeTeam else setup.awayTeam
        val firstXI = if (homeBatsFirst) setup.homeXI else setup.awayXI
        val firstBowlers = if (homeBatsFirst) setup.awayXI else setup.homeXI

        val firstInnings = InningsSimulator(setup, random).play(
            number = 1,
            team = firstTeam,
            batting = firstXI,
            bowling = firstBowlers,
            target = null,
            approach = if (homeBatsFirst) homeApproach else BattingApproach.BALANCED
        )

        val secondTeam = if (homeBatsFirst) setup.awayTeam else setup.homeTeam
        val secondXI = if (homeBatsFirst) setup.awayXI else setup.homeXI
        val secondBowlers = if (homeBatsFirst) setup.homeXI else setup.awayXI
        val secondApproach = if (homeBatsFirst) BattingApproach.CHASE else homeApproach

        val secondInnings = InningsSimulator(setup, random).play(
            number = 2,
            team = secondTeam,
            batting = secondXI,
            bowling = secondBowlers,
            target = firstInnings.runs + 1,
            approach = secondApproach
        )

        var superOver = false
        val winner: String
        val margin: String

        if (firstInnings.runs == secondInnings.runs) {
            superOver = true
            winner = if (random.nextBoolean()) setup.homeTeam else setup.awayTeam
            margin = "Super Over tie-break"
        } else if (secondInnings.runs >= (secondInnings.target ?: Int.MAX_VALUE)) {
            winner = secondInnings.battingTeam
            margin = "${10 - secondInnings.wickets} wickets"
        } else {
            winner = firstInnings.battingTeam
            margin = "${firstInnings.runs - secondInnings.runs} runs"
        }

        val pom = playerOfMatch(firstInnings, secondInnings)
        val record = MatchRecord(setup, firstInnings, secondInnings, winner, margin, pom, superOver)

        StatisticsEngine.apply(record, setup.homeXI, setup.awayXI)

        val homeScorecard = if (homeBatsFirst) firstInnings else secondInnings
        val awayScorecard = if (homeBatsFirst) secondInnings else firstInnings

        return MatchResult(
            opponent = setup.awayTeam,
            ourBatFirst = homeBatsFirst,
            ourRuns = homeScorecard.runs,
            ourWickets = homeScorecard.wickets,
            rivalRuns = awayScorecard.runs,
            rivalWickets = awayScorecard.wickets,
            won = winner == setup.homeTeam,
            scoreEvents = (firstInnings.deliveries + secondInnings.deliveries).map { eventText(it) },
            playerOfMatch = pom,
            record = record
        )
    }

    private fun playerOfMatch(a: InningsScorecard, b: InningsScorecard): String {
        val topBat = (a.batters + b.batters).maxByOrNull { it.runs + (it.fours * 2) + (it.sixes * 3) }
        val topBowl = (a.bowlers + b.bowlers).maxByOrNull { (it.wickets * 25) - (it.runs / 2) }

        val batPts = (topBat?.runs ?: 0)
        val bowlPts = ((topBowl?.wickets ?: 0) * 25)

        return if (bowlPts > batPts && topBowl != null) topBowl.name else topBat?.name ?: "Team effort"
    }

    private fun eventText(d: Delivery) = "${d.over}.${d.ball} ${d.bowler} to ${d.batter}: ${d.wicket ?: if (d.extras > 0) "${d.type} +${d.runs}" else d.runs}"
}

private class InningsSimulator(private val setup: MatchSetup, private val random: Random) {

    fun play(
        number: Int,
        team: String,
        batting: List<Player>,
        bowling: List<Player>,
        target: Int?,
        approach: BattingApproach
    ): InningsScorecard {
        val batters = batting.map { BatterLine(it.id, it.name) }.toMutableList()
        val figures = bowling.associate { it.id to BowlerLine(it.id, it.name) }.toMutableMap()
        val deliveries = mutableListOf<Delivery>()

        var score = 0
        var wickets = 0
        var legalBalls = 0
        var extras = 0
        var striker = 0
        var nonStriker = 1
        var nextBatter = 2
        var lastBowlerId = -1

        val deliveryEngine = DeliveryEngine(random)

        while (legalBalls < setup.overs * 6 && wickets < 10 && striker < batting.size) {
            val over = legalBalls / 6
            val ballInOver = (legalBalls % 6) + 1
            val phase = when {
                over < 6 -> MatchPhase.POWERPLAY
                over < 15 -> MatchPhase.MIDDLE
                else -> MatchPhase.DEATH
            }

            val bowler = BowlingAI.choose(bowling, figures, lastBowlerId, phase, setup)
            val figure = figures.getValue(bowler.id)
            val batter = batting[striker]
            val line = batters[striker]

            val currentApproach = approachFor(approach, target, score, legalBalls)
            val decision = deliveryEngine.deliver(number, over, ballInOver, batter, bowler, phase, currentApproach, BowlingApproach.BALANCED, setup)

            deliveries.add(decision)
            score += decision.runs
            extras += decision.extras
            figure.runs += decision.runs

            if (decision.legal) {
                legalBalls++
                figure.balls++
                if (decision.runs == 0 && decision.wicket == null) figure.dots++
            }

            if (decision.wicket != null) {
                wickets++
                line.dismissal = decision.wicket
                figure.wickets++
                if (wickets < 10 && nextBatter < batting.size) {
                    striker = nextBatter
                    nextBatter++
                }
            } else {
                line.runs += (decision.runs - decision.extras)
                if (decision.legal) line.balls++
                if (decision.runs - decision.extras == 4) line.fours++
                if (decision.runs - decision.extras == 6) line.sixes++

                if (decision.runs % 2 == 1) {
                    val temp = striker
                    striker = nonStriker
                    nonStriker = temp
                }
            }

            if (decision.legal && legalBalls % 6 == 0) {
                val temp = striker
                striker = nonStriker
                nonStriker = temp
                lastBowlerId = bowler.id
                if (figure.runs == 0) figure.maidens++
            }

            if (target != null && score >= target) break
        }

        return InningsScorecard(team, score, wickets, legalBalls, target, batters, figures.values.toList(), deliveries, extras)
    }

    private fun approachFor(base: BattingApproach, target: Int?, score: Int, balls: Int): BattingApproach {
        if (target == null) return base
        val remaining = max(1, setup.overs * 6 - balls)
        val required = (target - score) * 6.0 / remaining
        return when {
            balls >= 90 -> BattingApproach.DEATH_ATTACK
            required > 10.5 -> BattingApproach.AGGRESSIVE
            required > 8.5 -> BattingApproach.CHASE
            else -> base
        }
    }
}

object StatisticsEngine {
    fun apply(record: MatchRecord, homePlayers: List<Player>, awayPlayers: List<Player> = emptyList()) {
        val allPlayers = homePlayers + awayPlayers
        val lines = listOf(record.firstInnings, record.secondInnings)

        allPlayers.forEach { player ->
            player.matches++

            lines.forEach { innings ->
                innings.batters.find { it.playerId == player.id }?.let { b ->
                    if (b.balls > 0 || b.dismissal != "not out") player.innings++
                    player.runs += b.runs
                    player.ballsFaced += b.balls
                    player.fours += b.fours
                    player.sixes += b.sixes

                    if (b.runs > player.highestScore) player.highestScore = b.runs
                    if (b.dismissal == "not out" && b.balls > 0) player.notOuts++

                    if (b.runs >= 100) player.hundreds++
                    else if (b.runs >= 50) player.fifties++

                    player.form = (player.form + if (b.runs >= 30) 3 else -1).coerceIn(20, 99)
                }

                innings.bowlers.find { it.playerId == player.id }?.let { bowl ->
                    player.wickets += bowl.wickets
                    player.ballsBowled += bowl.balls
                    player.runsConceded += bowl.runs
                    player.maidens += bowl.maidens

                    if (bowl.wickets > player.bestBowlingWickets) {
                        player.bestBowlingWickets = bowl.wickets
                        player.bestBowlingRuns = bowl.runs
                    }

                    player.fatigue = (player.fatigue + (bowl.balls / 4)).coerceAtMost(100)
                    player.form = (player.form + (bowl.wickets * 2)).coerceIn(20, 99)
                }
            }

            player.confidence = (player.confidence + if (record.winner == record.setup.homeTeam) 2 else -1).coerceIn(20, 100)
        }
    }
}
