package com.cricketmaster.manager.ui.settings

import android.content.Context
import android.widget.LinearLayout
import android.widget.Toast
import com.cricketmaster.manager.engine.GameRepository
import com.cricketmaster.manager.engine.SeasonEngine
import com.cricketmaster.manager.util.UIUtils

class SettingsView(
    private val context: Context,
    private val repo: GameRepository,
    private val onOpenDevMode: () -> Unit,
    private val onRefresh: () -> Unit
) {

    fun build(parent: LinearLayout) {
        val state = repo.state

        UIUtils.card(
            context, parent,
            "CRICKET MASTER MANAGER v1.0.0",
            "Season ${state.season} • Club: ${state.teamName}"
        )

        UIUtils.sectionHeader(context, parent, "Campaign Operations")

        UIUtils.button(context, parent, "⏩ ADVANCE TO NEXT SEASON", true) {
            SeasonEngine().startNewSeason(state, repo.teams, repo.squad)
            repo.save()
            toast("New season initialized!")
            onRefresh()
        }

        UIUtils.button(context, parent, "💾 SAVE CAMPAIGN NOW", false) {
            repo.save()
            toast("Campaign saved locally")
        }

        UIUtils.button(context, parent, "🛠 DEVELOPER / DEBUG TOOLS", false) {
            state.devModeEnabled = true
            repo.save()
            onOpenDevMode()
        }

        UIUtils.button(context, parent, "⚠️ RESET ENTIRE SAVE DATA", false) {
            repo.reset()
            toast("Save data reset!")
            onRefresh()
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}
