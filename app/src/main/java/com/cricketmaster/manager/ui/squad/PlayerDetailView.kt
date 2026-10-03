package com.cricketmaster.manager.ui.squad

import android.content.Context
import android.widget.LinearLayout
import android.widget.Toast
import com.cricketmaster.manager.engine.GameRepository
import com.cricketmaster.manager.model.Player
import com.cricketmaster.manager.util.UIUtils

class PlayerDetailView(
    private val context: Context,
    private val repo: GameRepository,
    private val player: Player,
    private val onBack: () -> Unit
) {

    fun build(parent: LinearLayout) {
        val state = repo.state

        // Overview Card
        UIUtils.card(
            context, parent,
            "${player.name} (${player.role})",
            "${player.nationality} • Age ${player.age} • Overall ${player.overall} • Potential ${player.potential}\n" +
                    "Form ${player.form}/100 • Fitness ${player.fitness}% • Morale ${player.morale}/100"
        )

        // Actions
        UIUtils.sectionHeader(context, parent, "Squad Actions")

        val inXI = player.id in state.selectedXI
        UIUtils.button(context, parent, if (inXI) "REMOVE FROM PLAYING XI" else "ADD TO PLAYING XI", !inXI) {
            if (inXI) {
                state.selectedXI.remove(player.id)
                toast("${player.name} removed from XI")
            } else if (state.selectedXI.size < 11) {
                state.selectedXI.add(player.id)
                toast("${player.name} added to XI")
            } else {
                toast("Select maximum 11 players")
            }
            repo.save()
            onBack()
        }

        UIUtils.button(context, parent, "SET AS CAPTAIN", false) {
            state.captainId = player.id
            repo.save()
            toast("${player.name} appointed Captain")
            onBack()
        }

        UIUtils.button(context, parent, "SET AS VICE CAPTAIN", false) {
            state.viceCaptainId = player.id
            repo.save()
            toast("${player.name} appointed Vice Captain")
            onBack()
        }

        UIUtils.button(context, parent, "RENEW CONTRACT (+1 YEAR)", false) {
            player.contract++
            state.budget -= player.salary
            repo.save()
            toast("Contract renewed for 1 year")
            onBack()
        }

        UIUtils.button(context, parent, "◀ BACK TO SQUAD", false) {
            onBack()
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}
