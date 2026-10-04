package com.cricketmaster.manager.ui.home

import android.content.Context
import android.widget.LinearLayout
import com.cricketmaster.manager.engine.GameRepository
import com.cricketmaster.manager.util.UIUtils

class HomeDashboardView(private val context: Context, private val repo: GameRepository) {

    fun build(parent: LinearLayout, onNavigate: (String) -> Unit) {
        val state = repo.state
        val team = repo.teams.firstOrNull { it.name == state.teamName }
        val squadAvgOverall = if (repo.squad.isNotEmpty()) repo.squad.map { it.overall }.average().toInt() else 0

        // Hero Card
        val hero = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UIUtils.dp(context, 16), UIUtils.dp(context, 15), UIUtils.dp(context, 16), UIUtils.dp(context, 15))
            background = UIUtils.bgDrawable(context, UIUtils.surface, 16, 1)
        }
        hero.addView(UIUtils.tv(context, "SEASON ${state.season} MANAGEMENT HUB", 11f, UIUtils.gold, true))
        hero.addView(UIUtils.tv(context, state.teamName, 24f, UIUtils.textPrimary, true))
        hero.addView(UIUtils.tv(context, "Premier T20 • Sponsor: ${state.sponsor}", 13f, UIUtils.muted))

        val statsRow = LinearLayout(context).apply { setPadding(0, UIUtils.dp(context, 10), 0, 0) }
        val leaguePos = repo.teams.sortedByDescending { it.points }.indexOfFirst { it.name == state.teamName } + 1
        UIUtils.pill(context, statsRow, "Rating", "$squadAvgOverall OVR", UIUtils.gold)
        UIUtils.pill(context, statsRow, "Position", "#$leaguePos", UIUtils.blue)
        UIUtils.pill(context, statsRow, "Purse", "₹${state.budget / 1_000_000}M", UIUtils.green)
        UIUtils.pill(context, statsRow, "Squad", "${repo.squad.size}/25", UIUtils.textPrimary)
        hero.addView(statsRow)
        parent.addView(hero)

        // Contextual Manager Alerts
        UIUtils.sectionHeader(context, parent, "Manager Alerts")
        var alertsShown = 0

        val lowForm = repo.squad.filter { it.form < 60 }
        if (lowForm.isNotEmpty()) {
            UIUtils.card(context, parent, "⚠ LOW FORM ALERT", "${lowForm.first().name} and ${lowForm.size - 1} others are struggling for form.") {
                onNavigate("SQUAD")
            }
            alertsShown++
        }

        val injured = repo.squad.filter { it.isInjured }
        if (injured.isNotEmpty()) {
            UIUtils.card(context, parent, "🚑 INJURY REPORT", "${injured.first().name} is out injured for ${injured.first().injuryDaysRemaining} days.") {
                onNavigate("SQUAD")
            }
            alertsShown++
        }

        val nextFixture = state.fixtures.firstOrNull { it.status == "SCHEDULED" }
        if (nextFixture != null) {
            UIUtils.card(context, parent, "NEXT MATCHDAY", "Round ${nextFixture.round}: ${nextFixture.home} vs ${nextFixture.away}\nVenue: ${nextFixture.venue}") {
                onNavigate("LIVE")
            }
            alertsShown++
        }

        if (alertsShown == 0) {
            UIUtils.card(context, parent, "✓ ALL SYSTEMS GO", "Your squad is in top shape and ready for the next fixture.")
        }

        // Quick Actions
        UIUtils.sectionHeader(context, parent, "Quick Actions")
        UIUtils.button(context, parent, "▶ PLAY / SIMULATE NEXT MATCH", true) {
            onNavigate("LIVE")
        }
        UIUtils.button(context, parent, "♟ MANAGE SQUAD & PLAYING XI", false) {
            onNavigate("SQUAD")
        }
        UIUtils.button(context, parent, "⚙ SET MATCH TACTICS", false) {
            onNavigate("TACTICS")
        }

        // Latest Club News
        UIUtils.sectionHeader(context, parent, "Club News Feed")
        state.news.take(4).forEach { item ->
            UIUtils.card(context, parent, "NEWS UPDATE", item)
        }
    }
}
