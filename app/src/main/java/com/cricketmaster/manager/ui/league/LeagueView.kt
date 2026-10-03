package com.cricketmaster.manager.ui.league

import android.content.Context
import android.widget.LinearLayout
import com.cricketmaster.manager.engine.GameRepository
import com.cricketmaster.manager.model.Team
import com.cricketmaster.manager.util.UIUtils
import com.cricketmaster.manager.util.UIUtils.format

class LeagueView(
    private val context: Context,
    private val repo: GameRepository
) {

    fun build(parent: LinearLayout) {
        val teams = repo.teams
        val sortedTeams = teams.sortedWith(compareByDescending<Team> { it.points }.thenByDescending { it.nrr })

        UIUtils.card(
            context, parent,
            "PREMIER T20 LEAGUE STANDINGS",
            "Top 4 teams qualify for the end-of-season championship playoffs."
        )

        UIUtils.sectionHeader(context, parent, "League Table")

        sortedTeams.forEachIndexed { index, team ->
            val rank = index + 1
            val isUs = team.name == repo.state.teamName
            val prefix = if (isUs) "★ " else "#$rank "

            val nrrStr = if (team.nrr >= 0) "+${team.nrr.format()}" else team.nrr.format()
            val statsText = "P: ${team.played} | W: ${team.won} | L: ${team.lost} | T: ${team.tied}\n" +
                    "PTS: ${team.points} | NRR: $nrrStr"

            UIUtils.card(
                context, parent,
                "$prefix${team.name}",
                statsText
            )
        }

        UIUtils.sectionHeader(context, parent, "Season Schedule")
        repo.state.fixtures.take(10).forEach { f ->
            val resultText = if (f.status == "COMPLETED") "\nResult: ${f.result}" else ""
            UIUtils.card(
                context, parent,
                "Round ${f.round}: ${f.home} vs ${f.away}",
                "Venue: ${f.venue} • Status: ${f.status}$resultText"
            )
        }
    }
}
