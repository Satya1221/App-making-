package com.cricketmaster.manager.ui.dev

import android.content.Context
import android.widget.LinearLayout
import android.widget.Toast
import com.cricketmaster.manager.engine.GameRepository
import com.cricketmaster.manager.engine.SeasonEngine
import com.cricketmaster.manager.util.UIUtils

class DeveloperModeView(
    private val context: Context,
    private val repo: GameRepository,
    private val onBack: () -> Unit
) {

    fun build(parent: LinearLayout) {
        val state = repo.state

        UIUtils.card(
            context, parent,
            "🛠 DEVELOPER / DEBUG CONSOLE",
            "Full sandbox administrative controls for testing game systems and match states."
        )

        UIUtils.sectionHeader(context, parent, "Sandbox Administrative Cheats")

        UIUtils.button(context, parent, "💰 ADD ₹50,000,000 TO PURSE", true) {
            state.budget += 50_000_000
            repo.save()
            toast("₹50M added to budget")
        }

        UIUtils.button(context, parent, "🚑 HEAL SQUAD & RESTORE FATIGUE", true) {
            repo.squad.forEach { p ->
                p.injuryDaysRemaining = 0
                p.injuryType = null
                p.fatigue = 0
                p.fitness = 100
                p.form = 95
                p.morale = 95
            }
            repo.save()
            toast("All squad players fully healed & rested!")
        }

        UIUtils.button(context, parent, "⚡ BOOST ALL PLAYER OVERALLS (+5 OVR)", false) {
            repo.squad.forEach { p ->
                p.overall = (p.overall + 5).coerceAtMost(99)
            }
            repo.save()
            toast("Squad overall ratings increased!")
        }

        UIUtils.button(context, parent, "⏩ ADVANCE TO NEXT SEASON INSTANTLY", false) {
            SeasonEngine().startNewSeason(state, repo.teams, repo.squad)
            repo.save()
            toast("Season advanced!")
        }

        UIUtils.button(context, parent, "◀ BACK TO SETTINGS", false) {
            onBack()
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}
