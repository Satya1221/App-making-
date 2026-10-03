package com.cricketmaster.manager.ui.scorecard

import android.content.Context
import android.widget.LinearLayout
import com.cricketmaster.manager.model.MatchResult
import com.cricketmaster.manager.util.UIUtils
import com.cricketmaster.manager.util.UIUtils.format

class ScorecardView(
    private val context: Context,
    private val matchResult: MatchResult,
    private val onBack: () -> Unit
) {

    fun build(parent: LinearLayout) {
        val record = matchResult.record

        val titleText = if (matchResult.won) "VICTORY! HAWKS WIN" else "DEFEAT"
        val subtitle = "Harbor Hawks ${matchResult.ourRuns}/${matchResult.ourWickets} vs ${matchResult.opponent} ${matchResult.rivalRuns}/${matchResult.rivalWickets}\n" +
                "Player of the Match: ${matchResult.playerOfMatch}"

        UIUtils.card(context, parent, titleText, subtitle)

        if (record != null) {
            val firstInnings = record.firstInnings
            val secondInnings = record.secondInnings

            // First Innings Batting
            UIUtils.sectionHeader(context, parent, "${firstInnings.battingTeam} Batting Scorecard")
            val bat1Text = firstInnings.batters.filter { it.balls > 0 || it.dismissal != "not out" }.joinToString("\n") { b ->
                "${b.name.padEnd(16)} ${b.runs.toString().padStart(3)} (${b.balls.toString().padStart(3)}b) ${b.fours}x4 ${b.sixes}x6  ${b.dismissal}"
            }.ifBlank { "No batters." }
            UIUtils.card(context, parent, "BATTING", bat1Text)

            // First Innings Bowling
            UIUtils.sectionHeader(context, parent, "Bowling Figures")
            val bowl1Text = firstInnings.bowlers.filter { it.balls > 0 }.joinToString("\n") { bowl ->
                "${bowl.name.padEnd(16)} ${bowl.overs.padStart(4)} ov  ${bowl.runs.toString().padStart(3)}r  ${bowl.wickets}w  Econ ${bowl.economy.format()}"
            }.ifBlank { "No bowling data." }
            UIUtils.card(context, parent, "BOWLING", bowl1Text)

            // Second Innings Batting
            UIUtils.sectionHeader(context, parent, "${secondInnings.battingTeam} Batting Scorecard")
            val bat2Text = secondInnings.batters.filter { it.balls > 0 || it.dismissal != "not out" }.joinToString("\n") { b ->
                "${b.name.padEnd(16)} ${b.runs.toString().padStart(3)} (${b.balls.toString().padStart(3)}b) ${b.fours}x4 ${b.sixes}x6  ${b.dismissal}"
            }.ifBlank { "No batters." }
            UIUtils.card(context, parent, "BATTING", bat2Text)

            // Second Innings Bowling
            UIUtils.sectionHeader(context, parent, "Bowling Figures")
            val bowl2Text = secondInnings.bowlers.filter { it.balls > 0 }.joinToString("\n") { bowl ->
                "${bowl.name.padEnd(16)} ${bowl.overs.padStart(4)} ov  ${bowl.runs.toString().padStart(3)}r  ${bowl.wickets}w  Econ ${bowl.economy.format()}"
            }.ifBlank { "No bowling data." }
            UIUtils.card(context, parent, "BOWLING", bowl2Text)
        }

        // Commentary / Events
        UIUtils.sectionHeader(context, parent, "Ball-By-Ball Highlights")
        val highlights = matchResult.scoreEvents.filter { it.contains("wicket") || it.contains("6") || it.contains("4") }.take(15).joinToString("\n").ifBlank { "Solid controlled innings." }
        UIUtils.card(context, parent, "KEY MOMENTS", highlights)

        UIUtils.button(context, parent, "◀ BACK TO MATCH CENTRE", true) {
            onBack()
        }
    }
}
