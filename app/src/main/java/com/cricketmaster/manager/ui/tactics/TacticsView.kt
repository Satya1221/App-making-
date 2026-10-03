package com.cricketmaster.manager.ui.tactics

import android.content.Context
import android.widget.LinearLayout
import android.widget.Toast
import com.cricketmaster.manager.engine.GameRepository
import com.cricketmaster.manager.util.UIUtils

class TacticsView(
    private val context: Context,
    private val repo: GameRepository,
    private val onRefresh: () -> Unit
) {

    fun build(parent: LinearLayout) {
        val state = repo.state

        UIUtils.card(
            context, parent,
            "ACTIVE MATCH PLAN: ${state.tactic.uppercase()}",
            "Tactics directly modify scoring probabilities, risk levels, and bowling variations during match simulations."
        )

        UIUtils.sectionHeader(context, parent, "Select Tactical Mindset")

        val options = listOf(
            "Balanced" to "Standard risk-reward balance across powerplay, middle, and death overs.",
            "Aggressive" to "Higher boundary probability with increased wicket risk.",
            "Defensive" to "Focus on wicket preservation and reducing dots.",
            "Powerplay Attack" to "Maximal aggression in the opening 6 overs.",
            "Death Overs Attack" to "Ultra-aggressive hitting in overs 16–20.",
            "Spin Attack" to "Exploit spin-friendly conditions in middle overs.",
            "Yorker Bowling" to "Target toes at the death to limit boundaries.",
            "Containment" to "Defensive field placings and economical lines."
        )

        options.forEach { (name, desc) ->
            val isSelected = state.tactic == name
            UIUtils.card(
                context, parent,
                "${if (isSelected) "✓ " else ""}$name",
                desc
            ) {
                state.tactic = name
                repo.save()
                Toast.makeText(context, "Tactics set to $name", Toast.LENGTH_SHORT).show()
                onRefresh()
            }
        }
    }
}
