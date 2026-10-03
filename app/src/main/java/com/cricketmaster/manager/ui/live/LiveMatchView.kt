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
    private val onNavigate: (String) -> Unit,
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

        // Pick the next scheduled match involving the user's team
        val nextFixture = state.fixtures.firstOrNull { it.status == "SCHEDULED" && (it.home == state.teamName || it.away == state.teamName) }
            ?: state.fixtures.firstOrNull { it.status == "SCHEDULED" }

        val homeName = nextFixture?.home ?: state.teamName
        val awayName = nextFixture?.away ?: "Metro Kings"
        val opponentName = if (homeName == state.teamName) awayName else homeName
        val venueName = nextFixture?.venue ?: "Riverside Oval"

        UIUtils.card(
            context, parent,
            "MATCHDAY: $homeName vs $awayName",
            "Round ${nextFixture?.round ?: 1} • Venue: $venueName\nTactic: ${state.tactic}"
        )

        if (validation != null) {
            UIUtils.card(context, parent, "⚠ SQUAD SELECTION ERROR", validation)
            UIUtils.button(context, parent, "GO TO SQUAD SELECTION", true) {
                onNavigate("SQUAD")
            }
            return
        }

        UIUtils.sectionHeader(context, parent, "Match Options")

        UIUtils.button(context, parent, "🪙 COIN TOSS & BAT FIRST", true) {
            startLiveMatch(nextFixture, homeName, awayName, opponentName, "BAT")
        }

        UIUtils.button(context, parent, "🪙 COIN TOSS & FIELD FIRST", true) {
            startLiveMatch(nextFixture, homeName, awayName, opponentName, "FIELD")
        }

        UIUtils.button(context, parent, "⚡ INSTANT SIMULATE MATCH", false) {
            val xi = state.selectedXI.mapNotNull { repo.player(it) }
            val result = MatchEngine().simulate(xi, opponentName, state.tactic)

            state.lastResult = result
            state.matchHistory.add(0, result)

            val homeTeam = repo.teams.firstOrNull { it.name == homeName } ?: repo.teams.first()
            val awayTeam = repo.teams.firstOrNull { it.name == awayName } ?: repo.teams.last()

            if (nextFixture != null) {
                SeasonEngine().complete(nextFixture, homeTeam, awayTeam, result.ourRuns, result.rivalRuns)
                simulateOtherFixturesForRound(nextFixture.round)
            } else {
                SeasonEngine().applyResult(homeTeam, awayTeam, result.ourRuns, result.rivalRuns)
            }

            repo.save()
            onOpenScorecard(result)
        }
    }

    private fun startLiveMatch(nextFixture: Fixture?, homeName: String, awayName: String, opponentName: String, decision: String) {
        val state = repo.state
        val xi = state.selectedXI.mapNotNull { repo.player(it) }
        val isUserHome = homeName == state.teamName

        val opponentXI = xi.mapIndexed { index, p ->
            p.copy(id = 20_000 + (index * 10) + (p.id % 10), name = "${opponentName.split(' ').first()} ${p.name.substringAfter(' ')}")
        }

        val toss = TossEngine.toss(homeName, awayName, decision)
        val setup = MatchSetup(
            homeTeam = homeName,
            awayTeam = awayName,
            homeXI = if (isUserHome) xi else opponentXI,
            awayXI = if (isUserHome) opponentXI else xi,
            homeCaptainId = if (isUserHome) state.captainId else opponentXI.first().id,
            awayCaptainId = if (isUserHome) opponentXI.first().id else state.captainId,
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

                val nextFixture = repo.state.fixtures.firstOrNull { it.status == "SCHEDULED" && (it.home == repo.state.teamName || it.away == repo.state.teamName) }
                if (nextFixture != null) {
                    val homeTeam = repo.teams.firstOrNull { it.name == nextFixture.home } ?: repo.teams.first()
                    val awayTeam = repo.teams.firstOrNull { it.name == nextFixture.away } ?: repo.teams.last()
                    SeasonEngine().complete(nextFixture, homeTeam, awayTeam, r.ourRuns, r.rivalRuns)
                    simulateOtherFixturesForRound(nextFixture.round)
                }

                repo.state.liveMatch = null
                repo.save()
                onOpenScorecard(r)
            }
        } else {
            if (live.current.legalBalls % 6 == 0) {
                repo.save()
            }
            onRefresh()
        }
    }

    private fun simulateOtherFixturesForRound(round: Int) {
        val otherFixtures = repo.state.fixtures.filter { it.round == round && it.status == "SCHEDULED" && it.home != repo.state.teamName && it.away != repo.state.teamName }
        otherFixtures.forEach { fixture ->
            val home = repo.teams.find { it.name == fixture.home }
            val away = repo.teams.find { it.name == fixture.away }
            if (home != null && away != null) {
                val homeRuns = (130..190).random()
                val awayRuns = (120..195).random()
                SeasonEngine().complete(fixture, home, away, homeRuns, awayRuns)
            }
        }
    }
}
