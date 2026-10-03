package com.cricketmaster.manager.ui.live

import android.content.Context
import android.widget.LinearLayout
import android.widget.Toast
import com.cricketmaster.manager.engine.*
import com.cricketmaster.manager.model.*
import com.cricketmaster.manager.util.UIUtils
import com.cricketmaster.manager.util.UIUtils.format

class LiveMatchView(
    private val context: Context,
    private val repo: GameRepository,
    private val onOpenScorecard: (MatchResult) -> Unit,
    private val onRefresh: () -> Unit
) {

    private val liveMatchEngine = LiveMatchEngine()

    fun build(parent: LinearLayout) {
        val state = repo.state

        val live = state.liveMatch
        if (live == null) {
            buildPreMatchSetup(parent)
            return
        }

        if (live.completed) {
            live.result?.let { r ->
                onOpenScorecard(r)
            }
            return
        }

        buildLiveMatchCentre(parent, live)
    }

    private fun buildPreMatchSetup(parent: LinearLayout) {
        val state = repo.state
        val squad = repo.squad
        val validation = PlayingXiValidator.validate(squad, state.selectedXI, state.captainId, state.viceCaptainId, state.wicketKeeperId)

        UIUtils.card(
            context, parent,
            "NEXT FIXTURE: HARBOR HAWKS vs METRO KINGS",
            "Venue: Riverside Oval • Pitch: Balanced • Weather: Clear\nTactic: ${state.tactic}"
        )

        if (validation != null) {
            UIUtils.card(context, parent, "⚠ SQUAD SELECTION ERROR", validation)
            UIUtils.button(context, parent, "GO TO SQUAD SELECTION", true) {
                // Navigate handled in container
            }
            return
        }

        UIUtils.sectionHeader(context, parent, "Match Options")

        UIUtils.button(context, parent, "🪙 COIN TOSS & BAT FIRST", true) {
            startLiveMatch("BAT")
        }

        UIUtils.button(context, parent, "🪙 COIN TOSS & FIELD FIRST", true) {
            startLiveMatch("FIELD")
        }

        UIUtils.button(context, parent, "⚡ INSTANT SIMULATE MATCH", false) {
            val xi = state.selectedXI.mapNotNull { repo.player(it) }
            val result = MatchEngine().simulate(xi, "Metro Kings", state.tactic)

            state.lastResult = result
            state.matchHistory.add(0, result)

            val homeTeam = repo.teams.first { it.name == "Harbor Hawks" }
            val awayTeam = repo.teams.first { it.name == "Metro Kings" }
            SeasonEngine().applyResult(homeTeam, awayTeam, result.ourRuns, result.rivalRuns)

            repo.save()
            onOpenScorecard(result)
        }
    }

    private fun startLiveMatch(decision: String) {
        val state = repo.state
        val xi = state.selectedXI.mapNotNull { repo.player(it) }
        val opponent = xi.mapIndexed { index, p ->
            p.copy(id = p.id + 20_000 + index, name = "Metro ${p.name.substringAfter(' ')}")
        }

        val toss = TossEngine.toss("Harbor Hawks", "Metro Kings", decision)
        val setup = MatchSetup(
            homeTeam = "Harbor Hawks",
            awayTeam = "Metro Kings",
            homeXI = xi,
            awayXI = opponent,
            homeCaptainId = state.captainId,
            awayCaptainId = opponent.first().id,
            tossWinner = toss.first,
            tossDecision = toss.second
        )

        state.liveMatch = liveMatchEngine.start(setup)
        repo.save()
        onRefresh()
    }

    private fun buildLiveMatchCentre(parent: LinearLayout, live: LiveMatchState) {
        val inn = live.current
        val striker = inn.battingXI.getOrNull(inn.striker)
        val nonStriker = inn.battingXI.getOrNull(inn.nonStriker)
        val bowler = inn.bowlingXI.find { it.id == inn.lastBowlerId } ?: inn.bowlingXI.first()

        // Match Header
        val crr = if (inn.legalBalls == 0) 0.0 else inn.runs * 6.0 / inn.legalBalls
        val targetText = inn.target?.let { "Target: $it | RRR: ${((it - inn.runs) * 6.0 / (120 - inn.legalBalls).coerceAtLeast(1)).format()}" } ?: "Target: —"

        UIUtils.card(
            context, parent,
            "${inn.battingTeam} ${inn.runs}/${inn.wickets} (${live.overs} ov) • ${live.phase.name}",
            "CRR: ${crr.format()} | $targetText\nPartnership: ${inn.partnership} runs"
        )

        // Crease Info
        val strikerLine = inn.batters.getOrNull(inn.striker)
        val nonStrikerLine = inn.batters.getOrNull(inn.nonStriker)
        val bowlerLine = inn.bowlers[bowler.id]

        UIUtils.card(
            context, parent,
            "AT THE CREASE",
            "🏏 ${striker?.name ?: "Batter"}*: ${strikerLine?.runs ?: 0} (${strikerLine?.balls ?: 0}b)\n" +
                    "🏏 ${nonStriker?.name ?: "Batter"}: ${nonStrikerLine?.runs ?: 0} (${nonStrikerLine?.balls ?: 0}b)\n" +
                    "⚾ Bowler: ${bowler.name} • ${bowlerLine?.overs ?: "0.0"}-${bowlerLine?.runs ?: 0}-${bowlerLine?.wickets ?: 0}"
        )

        // Ball by Ball Log
        val recentDeliveries = inn.deliveries.takeLast(6).joinToString("\n") { d ->
            "${d.over}.${d.ball} ${d.bowler} to ${d.batter}: ${d.wicket ?: if (d.extras > 0) "${d.type} +${d.runs}" else "${d.runs} runs"}"
        }.ifBlank { "Awaiting first ball of the innings..." }

        UIUtils.card(context, parent, "THIS OVER COMMENTARY", recentDeliveries)

        // Controls
        UIUtils.sectionHeader(context, parent, "Simulation Controls")

        UIUtils.button(context, parent, "▶ BOWL NEXT BALL", true) {
            liveMatchEngine.nextBall(live)
            checkMatchEnd(live)
        }

        UIUtils.button(context, parent, "⏩ SIMULATE OVER", true) {
            liveMatchEngine.nextOver(live)
            checkMatchEnd(live)
        }

        UIUtils.button(context, parent, "⚡ AUTO-SIMULATE INNINGS", false) {
            liveMatchEngine.autoSimulate(live)
            checkMatchEnd(live)
        }

        // Tactics Selector
        UIUtils.sectionHeader(context, parent, "Batting Approach")
        val approaches = listOf(
            BattingApproach.DEFENSIVE to "Defensive",
            BattingApproach.BALANCED to "Balanced",
            BattingApproach.AGGRESSIVE to "Aggressive",
            BattingApproach.DEATH_ATTACK to "Death Attack"
        )

        approaches.forEach { (app, label) ->
            val isCurrent = live.battingApproach == app
            UIUtils.button(context, parent, "${if (isCurrent) "✓ " else ""}$label", isCurrent) {
                liveMatchEngine.setBatting(live, app)
                onRefresh()
            }
        }
    }

    private fun checkMatchEnd(live: LiveMatchState) {
        if (live.completed) {
            live.result?.let { r ->
                repo.state.lastResult = r
                repo.state.matchHistory.add(0, r)
                repo.state.liveMatch = null
                repo.save()
                onOpenScorecard(r)
            }
        } else {
            repo.save()
            onRefresh()
        }
    }
}
