package com.cricketmaster.manager.ui.squad

import android.content.Context
import android.widget.LinearLayout
import com.cricketmaster.manager.engine.GameRepository
import com.cricketmaster.manager.engine.PlayingXiValidator
import com.cricketmaster.manager.model.Player
import com.cricketmaster.manager.util.UIUtils

class SquadView(
    private val context: Context,
    private val repo: GameRepository,
    private val onSelectPlayer: (Player) -> Unit,
    private val onRefresh: () -> Unit
) {

    fun build(parent: LinearLayout) {
        val state = repo.state
        val squad = repo.squad

        // XI Status Header
        val validationError = PlayingXiValidator.validate(squad, state.selectedXI, state.captainId, state.viceCaptainId, state.wicketKeeperId)
        val statusText = if (validationError == null) {
            "✓ Playing XI is valid and match-ready."
        } else {
            "⚠ $validationError"
        }

        UIUtils.card(context, parent, "PLAYING XI SELECTION (${state.selectedXI.size}/11)", statusText)

        UIUtils.button(context, parent, "⚡ AUTO-PICK STRONGEST XI", true) {
            state.selectedXI = squad.sortedByDescending { it.overall }.take(11).map { it.id }.toMutableList()
            repo.save()
            onRefresh()
        }

        UIUtils.sectionHeader(context, parent, "Full Squad Roster", "${squad.size} Players")

        squad.sortedByDescending { it.overall }.forEach { p ->
            val isXI = p.id in state.selectedXI
            val isCaptain = p.id == state.captainId
            val isViceCaptain = p.id == state.viceCaptainId
            val isKeeper = p.id == state.wicketKeeperId

            val badgePrefix = buildString {
                if (isXI) append("[XI] ")
                if (isCaptain) append("(C) ")
                if (isViceCaptain) append("(VC) ")
                if (isKeeper) append("(WK) ")
            }

            val body = "${p.role} • ${p.nationality} • Age ${p.age}\n" +
                    "Form ${p.form}/100 • Fitness ${p.fitness}% • Contract ${p.contract}y\n" +
                    "Value ₹${p.value / 1_000_000}M • Salary ₹${p.salary / 1_000}K"

            UIUtils.card(context, parent, "$badgePrefix${p.name}  ★ ${p.overall}", body) {
                onSelectPlayer(p)
            }
        }
    }
}
