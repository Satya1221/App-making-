package com.cricketmaster.manager.engine

import com.cricketmaster.manager.model.*
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** Facade retained for the existing UI; delegates every delivery to the T20 simulator. */
class MatchEngine(private val random: Random = Random.Default) {
    fun simulate(xi: List<Player>, opponent: String, aggressive: Boolean): MatchResult {
        return simulate(xi, opponent, if (aggressive) "Aggressive" else "Balanced")
    }
    fun simulate(xi: List<Player>, opponent: String, tactic: String): MatchResult {
        val opponentXI = xi.map { it.copy(id = it.id + 10_000, name = "${opponent.split(' ').first()} ${it.name.substringAfter(' ')}", form = (it.form - 3).coerceIn(40, 90)) }
        val setup = MatchSetup("Harbor Hawks", opponent, homeXI = xi, awayXI = opponentXI, homeCaptainId = xi.first().id, awayCaptainId = opponentXI.first().id, pitch = listOf(PitchType.FLAT, PitchType.GREEN, PitchType.DRY, PitchType.BALANCED).random(random), weather = listOf(Weather.CLEAR, Weather.OVERCAST, Weather.HUMID).random(random), dew = random.nextBoolean(), tossWinner = if (random.nextBoolean()) "Harbor Hawks" else opponent, tossDecision = if (random.nextBoolean()) "BAT" else "FIELD")
        val approach = when (tactic) { "Defensive" -> BattingApproach.DEFENSIVE; "Powerplay Attack", "Aggressive" -> BattingApproach.AGGRESSIVE; "Death Overs Attack" -> BattingApproach.DEATH_ATTACK; else -> BattingApproach.BALANCED }
        return MatchSimulator(random).play(setup, approach)
    }
}

class MatchSimulator(private val random: Random) {
    fun play(setup: MatchSetup, homeApproach: BattingApproach): MatchResult {
        val homeBatsFirst = (setup.tossWinner == setup.homeTeam) == (setup.tossDecision == "BAT")
        val firstTeam = if (homeBatsFirst) setup.homeTeam else setup.awayTeam
        val firstXI = if (homeBatsFirst) setup.homeXI else setup.awayXI
        val firstBowlers = if (homeBatsFirst) setup.awayXI else setup.homeXI
        val first = InningsSimulator(setup, random).play(1, firstTeam, firstXI, firstBowlers, null, if (homeBatsFirst) homeApproach else BattingApproach.BALANCED)
        val secondTeam = if (homeBatsFirst) setup.awayTeam else setup.homeTeam
        val secondXI = if (homeBatsFirst) setup.awayXI else setup.homeXI
        val secondBowlers = if (homeBatsFirst) setup.homeXI else setup.awayXI
        val secondApproach = if (homeBatsFirst) BattingApproach.CHASE else homeApproach
        var second = InningsSimulator(setup, random).play(2, secondTeam, secondXI, secondBowlers, first.runs + 1, secondApproach)
        var superOver = false
        var winner: String
        var margin: String
        if (first.runs == second.runs) {
            superOver = true
            val homeSO = InningsSimulator(setup.copy(overs = 1), random).play(3, setup.homeTeam, setup.homeXI.take(3), setup.awayXI, null, BattingApproach.DEATH_ATTACK, wicketLimit = 2)
            val awaySO = InningsSimulator(setup.copy(overs = 1), random).play(4, setup.awayTeam, setup.awayXI.take(3), setup.homeXI, homeSO.runs + 1, BattingApproach.DEATH_ATTACK, wicketLimit = 2)
            if (homeSO.runs == awaySO.runs) { // controlled tie-breaker avoids an endless match
                winner = if (random.nextBoolean()) setup.homeTeam else setup.awayTeam; margin = "Super Over tie-break"
            } else { winner = if (homeSO.runs > awaySO.runs) setup.homeTeam else setup.awayTeam; margin = "${kotlin.math.abs(homeSO.runs - awaySO.runs)} runs (Super Over)" }
        } else if (second.runs >= (second.target ?: Int.MAX_VALUE)) {
            winner = second.battingTeam; margin = "${10 - second.wickets} wickets (${setup.overs * 6 - second.legalBalls} balls remaining)"
        } else { winner = first.battingTeam; margin = "${first.runs - second.runs} runs" }
        val record = MatchRecord(setup, first, second, winner, margin, playerOfMatch(first, second), superOver)
        StatisticsEngine.apply(record, setup.homeXI)
        val home = if (homeBatsFirst) first else second; val away = if (homeBatsFirst) second else first
        return MatchResult(setup.awayTeam, homeBatsFirst, home.runs, home.wickets, away.runs, away.wickets, winner == setup.homeTeam, (first.deliveries + second.deliveries).map { eventText(it) }, record.playerOfMatch, record)
    }
    private fun playerOfMatch(a: InningsScorecard, b: InningsScorecard): String = (a.batters + b.batters).maxByOrNull { it.runs + it.strikeRate / 10 }?.name ?: "Team effort"
    private fun eventText(d: Delivery) = "${d.over}.${d.ball} ${d.bowler} to ${d.batter}: ${d.wicket ?: if (d.extras > 0) "${d.type} +${d.runs}" else d.runs}"
}

private class InningsSimulator(private val setup: MatchSetup, private val random: Random) {
    fun play(number: Int, team: String, batting: List<Player>, bowling: List<Player>, target: Int?, approach: BattingApproach, wicketLimit: Int = 10): InningsScorecard {
        val batters = batting.map { BatterLine(it.id, it.name) }; val figures = bowling.associate { it.id to BowlerLine(it.id, it.name) }.toMutableMap(); val deliveries = mutableListOf<Delivery>()
        var score = 0; var wickets = 0; var legalBalls = 0; var extras = 0; var striker = 0; var nonStriker = 1; var next = 2; var lastBowler = -1
        while (legalBalls < setup.overs * 6 && wickets < wicketLimit && next <= batting.size) {
            val over = legalBalls / 6; val phase = when { over < 6 -> MatchPhase.POWERPLAY; over < 15 -> MatchPhase.MIDDLE; else -> MatchPhase.DEATH }
            val bowler = BowlingAI.choose(bowling, figures, lastBowler, phase, setup); if (legalBalls % 6 == 0) lastBowler = bowler.id
            val figure = figures.getValue(bowler.id); val batter = batting[striker]; val line = batters[striker]
            val decision = DeliveryEngine(random).deliver(number, over, legalBalls % 6 + 1, batter, bowler, phase, approachFor(approach, target, score, legalBalls), setup, target)
            deliveries += decision.copy(batter = batter.name, bowler = bowler.name); score += decision.runs; extras += decision.extras; figure.runs += decision.runs
            if (decision.legal) { legalBalls++; figure.balls++; if (decision.runs == 0 && decision.wicket == null) figure.dots++ }
            if (decision.wicket != null) { wickets++; line.dismissal = decision.wicket; figure.wickets++; if (wickets < wicketLimit && next < batting.size) { striker = next; next++ } }
            else { line.runs += decision.runs - decision.extras; if (decision.legal) line.balls++; if (decision.runs % 2 == 1) { val t=striker; striker=nonStriker; nonStriker=t }; if (decision.runs - decision.extras == 4) line.fours++; if (decision.runs - decision.extras == 6) line.sixes++ }
            if (legalBalls % 6 == 0 && decision.legal) { val t=striker; striker=nonStriker; nonStriker=t; if (figure.runs == 0) figure.maidens++ }
            if (target != null && score >= target) break
        }
        return InningsScorecard(team, score, wickets, legalBalls, target, batters, figures.values.toList(), deliveries, extras)
    }
    private fun approachFor(base: BattingApproach, target: Int?, score: Int, balls: Int): BattingApproach { if (target == null) return base; val remaining = max(1, setup.overs * 6 - balls); val required = (target - score) * 6.0 / remaining; return when { balls >= 90 -> BattingApproach.DEATH_ATTACK; required > 10.5 -> BattingApproach.AGGRESSIVE; required > 8.5 -> BattingApproach.CHASE; else -> base } }
}

private object BowlingAI {
    fun choose(players: List<Player>, figures: Map<Int, BowlerLine>, last: Int, phase: MatchPhase, setup: MatchSetup): Player = players.filter { figures.getValue(it.id).balls < 24 && it.id != last }.maxByOrNull { p -> p.bowlingOverall + when (phase) { MatchPhase.POWERPLAY -> p.powerplayBowling; MatchPhase.DEATH -> p.deathBowling + p.yorker; MatchPhase.MIDDLE -> if (setup.pitch == PitchType.DRY) p.spin else p.accuracy } / 3 - p.fatigue / 3 } ?: players.first { figures.getValue(it.id).balls < 24 }
}

private class DeliveryEngine(private val random: Random) {
    fun deliver(innings:Int, over:Int, ball:Int, batter:Player, bowler:Player, phase:MatchPhase, approach:BattingApproach, setup:MatchSetup, target:Int?): Delivery {
        val pitchBat = when(setup.pitch){PitchType.FLAT->7;PitchType.GREEN->-6;PitchType.DRY->-3;PitchType.BALANCED->0}; val weatherBowl=if(setup.weather==Weather.OVERCAST)4 else 0
        val dewBat=if(setup.dew && innings==2)3 else 0; val phaseRisk=when(phase){MatchPhase.POWERPLAY->2;MatchPhase.MIDDLE->-2;MatchPhase.DEATH->7}; val intent=when(approach){BattingApproach.DEFENSIVE->-7;BattingApproach.BALANCED->0;BattingApproach.CHASE->4;BattingApproach.AGGRESSIVE->8;BattingApproach.DEATH_ATTACK->12}
        val bat = batter.battingOverall + batter.form/8 + batter.confidence/12 - batter.fatigue/3 + pitchBat + dewBat + intent
        val bowl = bowler.bowlingOverall + bowler.form/10 - bowler.fatigue/3 + weatherBowl + if(setup.pitch==PitchType.DRY) bowler.spin/8 else 0
        val edge = (bat - bowl + phaseRisk).coerceIn(-35,35); val wide = random.nextDouble() < bounded(.025 + (55-bowler.accuracy)/700.0); if(wide) return Delivery(innings,over+1,ball,"","","wide",1,1,null,false)
        val noBall = random.nextDouble() < bounded(.008 + (50-bowler.accuracy)/1300.0); if(noBall) return Delivery(innings,over+1,ball,"","","no ball",1,1,null,false)
        val wicketChance=bounded(.045 - edge/900.0 + if(phase==MatchPhase.DEATH).012 else 0.0); if(random.nextDouble()<wicketChance){val type=listOf("bowled","caught","lbw","run out","stumped","hit wicket").random(random);return Delivery(innings,over+1,ball,"","","wicket",0,0,type,true)}
        val boundary=bounded(.105 + edge/450.0 + if(phase==MatchPhase.DEATH).045 else 0.0); val six=bounded(.025 + (batter.power-bowler.deathBowling)/700.0 + if(approach==BattingApproach.DEATH_ATTACK).04 else 0.0)
        val roll=random.nextDouble(); val runs=when{roll<six->6;roll<six+boundary->4;roll<.52+edge/1200.0->1;roll<.72+edge/1500.0->2;roll<.77->3;else->0};return Delivery(innings,over+1,ball,"","","${phase.name.lowercase()} ball",runs)
    }
    private fun bounded(value:Double)=min(.35,max(.001,value))
}

object StatisticsEngine {
    fun apply(record: MatchRecord, homePlayers: List<Player>) { val lines = listOf(record.firstInnings, record.secondInnings); homePlayers.forEach { player -> player.matches++; lines.forEach { innings -> innings.batters.find { it.playerId==player.id }?.let { b -> player.runs+=b.runs;player.ballsFaced+=b.balls;player.fours+=b.fours;player.sixes+=b.sixes;if(b.runs>=100)player.hundreds++ else if(b.runs>=50)player.fifties++;player.form=(player.form + if(b.runs>=30)2 else -1).coerceIn(0,100) }; innings.bowlers.find { it.playerId==player.id }?.let { bowl -> player.wickets+=bowl.wickets;player.ballsBowled+=bowl.balls;player.runsConceded+=bowl.runs;player.fatigue=(player.fatigue+bowl.balls/4).coerceAtMost(100);player.form=(player.form + bowl.wickets*2).coerceIn(0,100) } }; player.confidence=(player.confidence + if(record.winner==record.setup.homeTeam)1 else -1).coerceIn(20,100) } }
}
