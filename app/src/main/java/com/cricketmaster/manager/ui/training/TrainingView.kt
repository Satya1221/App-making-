package com.cricketmaster.manager.ui.training

import android.content.Context
import android.widget.LinearLayout
import android.widget.Toast
import com.cricketmaster.manager.engine.GameRepository
import com.cricketmaster.manager.util.UIUtils

class TrainingView(
    private val context: Context,
    private val repo: GameRepository,
    private val onRefresh: () -> Unit
) {

    fun build(parent: LinearLayout) {
        val state = repo.state

        UIUtils.card(
            context, parent,
            "TRAINING COMPLEX LEVEL ${state.trainingLevel}",
            "Higher facility levels increase training gains and reduce fatigue penalty."
        )

        UIUtils.sectionHeader(context, parent, "Team Drills & Costs")

        UIUtils.button(context, parent, "🏏 BATTING INTENSIVE DRILL (Cost: ₹1M)", true) {
            if (state.budget < 1_000_000) {
                toast("Insufficient budget for drill!")
                return@button
            }
            state.budget -= 1_000_000
            repo.squad.forEach { p ->
                if (p.role.contains("Batter") || p.role == "Opener" || p.role == "Finisher") {
                    p.form = (p.form + 5).coerceAtMost(99)
                    p.confidence = (p.confidence + 3).coerceAtMost(100)
                }
                p.fatigue = (p.fatigue + 4 - (state.trainingLevel / 2)).coerceIn(0, 100)
            }
            state.news.add(0, "Batting coach led an intensive power-hitting workshop (-₹1M).")
            repo.save()
            toast("Batting form and confidence improved!")
            onRefresh()
        }

        UIUtils.button(context, parent, "⚾ BOWLING ACCURACY WORKSHOP (Cost: ₹1M)", true) {
            if (state.budget < 1_000_000) {
                toast("Insufficient budget for drill!")
                return@button
            }
            state.budget -= 1_000_000
            repo.squad.forEach { p ->
                if (p.role.contains("Bowler")) {
                    p.form = (p.form + 5).coerceAtMost(99)
                    p.confidence = (p.confidence + 3).coerceAtMost(100)
                }
                p.fatigue = (p.fatigue + 4 - (state.trainingLevel / 2)).coerceIn(0, 100)
            }
            state.news.add(0, "Bowling coach executed death-overs line & length drills (-₹1M).")
            repo.save()
            toast("Bowling form and confidence boosted!")
            onRefresh()
        }

        UIUtils.button(context, parent, "🏋 TEAM FITNESS & PHYSIO RECOVERY (Cost: ₹500K)", false) {
            if (state.budget < 500_000) {
                toast("Insufficient budget for physio session!")
                return@button
            }
            state.budget -= 500_000
            repo.squad.forEach { p ->
                p.fatigue = (p.fatigue - 15).coerceAtLeast(0)
                p.fitness = (p.fitness + 8).coerceAtMost(100)
            }
            state.news.add(0, "Physio conducted team recovery and fitness session (-₹500K).")
            repo.save()
            toast("Squad fatigue reduced and fitness restored!")
            onRefresh()
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}
