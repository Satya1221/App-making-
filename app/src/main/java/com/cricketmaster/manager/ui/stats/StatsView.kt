package com.cricketmaster.manager.ui.stats

import android.content.Context
import android.widget.LinearLayout
import com.cricketmaster.manager.engine.GameRepository
import com.cricketmaster.manager.util.UIUtils
import com.cricketmaster.manager.util.UIUtils.format

class StatsView(
    private val context: Context,
    private val repo: GameRepository
) {

    fun build(parent: LinearLayout) {
        val squad = repo.squad

        UIUtils.card(
            context, parent,
            "SEASON & CAREER STATS LEADERBOARD",
            "Comprehensive individual performance metrics across all formats."
        )

        val topScorer = squad.maxByOrNull { it.runs }
        val topWicketTaker = squad.maxByOrNull { it.wickets }
        val highestSR = squad.filter { it.ballsFaced > 20 }.maxByOrNull { it.battingStrikeRate }
        val bestEcon = squad.filter { it.ballsBowled > 24 }.minByOrNull { it.bowlingEconomy }

        UIUtils.sectionHeader(context, parent, "League Leaders")

        UIUtils.card(
            context, parent,
            "🏏 MOST RUNS",
            topScorer?.let { "${it.name}: ${it.runs} runs (${it.innings} inn, Avg ${it.battingAverage.format()}, SR ${it.battingStrikeRate.format()})" } ?: "No data"
        )

        UIUtils.card(
            context, parent,
            "⚾ MOST WICKETS",
            topWicketTaker?.let { "${it.name}: ${it.wickets} wickets (${it.ballsBowled / 6} ov, Econ ${it.bowlingEconomy.format()})" } ?: "No data"
        )

        UIUtils.card(
            context, parent,
            "⚡ HIGHEST STRIKE RATE",
            highestSR?.let { "${it.name}: ${it.battingStrikeRate.format()} SR (${it.runs} runs)" } ?: "No data"
        )

        UIUtils.card(
            context, parent,
            "🎯 BEST ECONOMY RATE",
            bestEcon?.let { "${it.name}: ${it.bowlingEconomy.format()} Econ (${it.wickets} wkt)" } ?: "No data"
        )
    }
}
