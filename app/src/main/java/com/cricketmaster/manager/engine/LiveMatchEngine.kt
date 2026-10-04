package com.cricketmaster.manager.engine

import com.cricketmaster.manager.model.*
import kotlin.random.Random

/** Delivery-by-delivery interactive T20 live match controller. */
class LiveMatchEngine(private val random: Random = Random.Default) {

    private val deliveryEngine = DeliveryEngine(random)

    fun start(setup: MatchSetup, approach: BattingApproach = BattingApproach.BALANCED): LiveMatchState {
        val homeFirst = (setup.tossWinner == setup.homeTeam) == (setup.tossDecision == "BAT")
        val bat = if (homeFirst) setup.homeXI.toMutableList() else setup.awayXI.toMutableList()
        val bowl = if (homeFirst) setup.awayXI.toMutableList() else setup.homeXI.toMutableList()

        return LiveMatchState(
            setup = setup,
            inningsNumber = 1,
            current = LiveInningsState(
                battingTeam = if (homeFirst) setup.homeTeam else setup.awayTeam,
                battingXI = bat,
                bowlingXI = bowl
            ),
            battingApproach = approach
        )
    }

    /** Substitutes an active player with an Impact Sub player during the match (IPL rules: 1 per team). */
    fun substituteImpactPlayer(
        match: LiveMatchState,
        teamName: String,
        playerOutId: Int,
        subInPlayer: Player
    ): Boolean {
        if (match.completed) return false

        val isHome = teamName == match.setup.homeTeam
        if (isHome && match.homeImpactSubUsed) return false
        if (!isHome && match.awayImpactSubUsed) return false

        val inn = match.current

        // Check if team is currently batting or bowling
        if (inn.battingTeam == teamName) {
            val outIndex = inn.battingXI.indexOfFirst { it.id == playerOutId }
            if (outIndex == -1) return false

            // Do not substitute currently active batters on the pitch unless un-batted
            val isStriker = outIndex == inn.striker
            val isNonStriker = outIndex == inn.nonStriker
            if (isStriker || isNonStriker) return false

            inn.battingXI[outIndex] = subInPlayer
            inn.batters[outIndex] = BatterLine(subInPlayer.id, subInPlayer.name)
        } else {
            val outIndex = inn.bowlingXI.indexOfFirst { it.id == playerOutId }
            if (outIndex == -1) return false

            val currentBowlerId = if (inn.lastBowlerId != -1) inn.lastBowlerId else -1
            if (playerOutId == currentBowlerId && (inn.legalBalls % 6 != 0)) return false // cannot swap mid-over

            inn.bowlingXI[outIndex] = subInPlayer
            if (!inn.bowlers.containsKey(subInPlayer.id)) {
                inn.bowlers[subInPlayer.id] = BowlerLine(subInPlayer.id, subInPlayer.name)
            }
        }

        if (isHome) match.homeImpactSubUsed = true else match.awayImpactSubUsed = true
        return true
    }

    fun nextBall(match: LiveMatchState): Delivery? {
        if (match.completed || inningsFinished(match)) {
            return finishOrChangeInnings(match)
        }

        val inn = match.current
        val phase = match.phase

        val bowler = selectBowlerInternal(inn, phase, match.setup, match.fieldingApproach)
        val batter = inn.battingXI[inn.striker]

        val delivery = deliveryEngine.deliver(
            innings = match.inningsNumber,
            over = inn.legalBalls / 6,
            ball = (inn.legalBalls % 6) + 1,
            batter = batter,
            bowler = bowler,
            phase = phase,
            battingApproach = match.battingApproach,
            bowlingApproach = match.fieldingApproach,
            setup = match.setup
        )

        val figure = inn.bowlers.getValue(bowler.id)
        inn.deliveries.add(delivery)
        inn.runs += delivery.runs
        inn.extras += delivery.extras
        inn.partnership += delivery.runs
        figure.runs += delivery.runs

        if (delivery.legal) {
            inn.legalBalls++
            figure.balls++
            if (delivery.runs == 0 && delivery.wicket == null) figure.dots++
        }

        if (delivery.wicket != null) {
            inn.wickets++
            inn.partnership = 0
            inn.batters[inn.striker].dismissal = delivery.wicket
            figure.wickets++

            if (inn.wickets < 10 && inn.nextBatter < inn.battingXI.size) {
                inn.striker = inn.nextBatter
                inn.nextBatter++
            }
        } else {
            val line = inn.batters[inn.striker]
            line.runs += (delivery.runs - delivery.extras)
            if (delivery.legal) line.balls++
            if (delivery.runs - delivery.extras == 4) line.fours++
            if (delivery.runs - delivery.extras == 6) line.sixes++

            if (delivery.runs % 2 == 1) {
                swapStrike(inn)
            }
        }

        if (delivery.legal && inn.legalBalls % 6 == 0) {
            if (figure.runs == 0) figure.maidens++
            swapStrike(inn)
            inn.lastBowlerId = bowler.id
        }

        if (inningsFinished(match)) {
            finishOrChangeInnings(match)
        }

        return delivery
    }

    fun nextOver(match: LiveMatchState): List<Delivery> {
        val startOver = match.current.legalBalls / 6
        val deliveries = mutableListOf<Delivery>()
        while (!match.completed && match.current.legalBalls / 6 == startOver) {
            nextBall(match)?.let { deliveries.add(it) }
        }
        return deliveries
    }

    fun autoSimulate(match: LiveMatchState): List<Delivery> {
        val deliveries = mutableListOf<Delivery>()
        while (!match.completed) {
            nextBall(match)?.let { deliveries.add(it) }
        }
        return deliveries
    }

    fun setBatting(match: LiveMatchState, approach: BattingApproach) {
        match.battingApproach = approach
    }

    fun setFielding(match: LiveMatchState, approach: BowlingApproach) {
        match.fieldingApproach = approach
    }

    fun selectBowler(match: LiveMatchState, playerId: Int): Boolean {
        val line = match.current.bowlers[playerId] ?: return false
        if (line.balls >= 24 || playerId == match.current.lastBowlerId) return false
        match.current.lastBowlerId = playerId
        return true
    }

    private fun selectBowlerInternal(
        inn: LiveInningsState,
        phase: MatchPhase,
        setup: MatchSetup,
        fieldingApproach: BowlingApproach
    ): Player {
        return BowlingAI.choose(inn.bowlingXI, inn.bowlers, inn.lastBowlerId, phase, setup, fieldingApproach)
    }

    private fun finishOrChangeInnings(match: LiveMatchState): Delivery? {
        val inn = match.current

        if (match.inningsNumber == 1) {
            match.firstInnings = toScorecard(inn)
            val bat = inn.bowlingXI
            val bowl = inn.battingXI

            match.inningsNumber = 2
            match.current = LiveInningsState(
                battingTeam = batTeam(match.setup, bat),
                battingXI = bat,
                bowlingXI = bowl,
                target = inn.runs + 1
            )
            return null
        }

        val first = match.firstInnings!!
        val second = toScorecard(inn)

        val winner = if (second.runs >= (second.target ?: Int.MAX_VALUE)) second.battingTeam else first.battingTeam
        val margin = if (winner == second.battingTeam) "${10 - second.wickets} wickets" else "${first.runs - second.runs} runs"

        val pom = (first.batters + second.batters).maxByOrNull { it.runs + (it.fours * 2) + (it.sixes * 3) }?.name ?: "Team effort"

        val record = MatchRecord(
            setup = match.setup,
            firstInnings = first,
            secondInnings = second,
            winner = winner,
            margin = margin,
            playerOfMatch = pom
        )

        StatisticsEngine.apply(record, match.setup.homeXI, match.setup.awayXI)

        val homeFirst = first.battingTeam == match.setup.homeTeam
        val homeScore = if (homeFirst) first else second
        val awayScore = if (homeFirst) second else first

        match.result = MatchResult(
            opponent = match.setup.awayTeam,
            ourBatFirst = homeFirst,
            ourRuns = homeScore.runs,
            ourWickets = homeScore.wickets,
            rivalRuns = awayScore.runs,
            rivalWickets = awayScore.wickets,
            won = winner == match.setup.homeTeam,
            scoreEvents = (first.deliveries + second.deliveries).map { "${it.over}.${it.ball} ${it.bowler} to ${it.batter}: ${it.wicket ?: if (it.extras > 0) "${it.type} +${it.runs}" else it.runs}" },
            playerOfMatch = pom,
            record = record
        )

        match.completed = true
        return null
    }

    private fun batTeam(setup: MatchSetup, players: List<Player>) =
        if (players.first().id == setup.homeXI.first().id) setup.homeTeam else setup.awayTeam

    private fun inningsFinished(match: LiveMatchState): Boolean {
        val innings = match.current
        val target = innings.target
        return innings.legalBalls >= match.setup.overs * 6 || innings.wickets >= 10 || (target != null && innings.runs >= target)
    }

    private fun toScorecard(i: LiveInningsState) = InningsScorecard(
        battingTeam = i.battingTeam,
        runs = i.runs,
        wickets = i.wickets,
        legalBalls = i.legalBalls,
        target = i.target,
        batters = i.batters,
        bowlers = i.bowlers.values.toList(),
        deliveries = i.deliveries,
        extras = i.extras
    )

    private fun swapStrike(i: LiveInningsState) {
        val temp = i.striker
        i.striker = i.nonStriker
        i.nonStriker = temp
    }
}
