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
            "Higher facility levels increase training gains and reduce recovery time for tired players."
        )

        UIUtils.sectionHeader(context, parent, "Team Drills")

        UIUtils.button(context, parent, "🏏 INTENSIVE BATTING DRILL", true) {
            repo.squad.forEach { p ->
                if (p.role.contains("Batter") || p.role == "Opener" || p.role == "Finisher") {
                    p.form = (p.form + 4).coerceAtMost(99)
                }
                p.fatigue = (p.fatigue + 3).coerceAtMost(100)
            }
            state.news.add(0, "Batting coach led an intensive power-hitting workshop.")
            repo.save()
            toast("Batting form improved for key batters!")
            onRefresh()
        }

        UIUtils.button(context, parent, "⚾ BOWLING ACCURACY WORKSHOP", true) {
            repo.squad.forEach { p ->
                if (p.role.contains("Bowler")) {
                    p.form = (p.form + 4).coerceAtMost(99)
                }
                p.fatigue = (p.fatigue + 3).coerceAtMost(100)
            }
            state.news.add(0, "Bowling coach executed death-overs line & length drills.")
            repo.save()
            toast("Bowling confidence boosted!")
            onRefresh()
        }

        UIUtils.button(context, parent, "🏋 FITNESS & RECOVERY SESSION", false) {
            repo.squad.forEach { p ->
                p.fatigue = (p.fatigue - 15).coerceAtLeast(0)
                p.fitness = (p.fitness + 5).coerceAtMost(100)
            }
            state.news.add(0, "Physio conducted team recovery and fitness session.")
            repo.save()
            toast("Squad fatigue reduced!")
            onRefresh()
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}
