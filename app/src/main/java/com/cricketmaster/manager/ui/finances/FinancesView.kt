package com.cricketmaster.manager.ui.finances

import android.content.Context
import android.widget.LinearLayout
import android.widget.Toast
import com.cricketmaster.manager.engine.GameRepository
import com.cricketmaster.manager.util.UIUtils

class FinancesView(
    private val context: Context,
    private val repo: GameRepository,
    private val onRefresh: () -> Unit
) {

    fun build(parent: LinearLayout) {
        val state = repo.state
        val totalSalaries = repo.squad.sumOf { it.salary }

        UIUtils.card(
            context, parent,
            "AVAILABLE PURSE: ₹${state.budget / 1_000_000}M",
            "Official Sponsor: ${state.sponsor}\nAnnual Player Wages: -₹${totalSalaries / 1_000_000}M/season"
        )

        UIUtils.sectionHeader(context, parent, "Facility Investments")

        UIUtils.card(
            context, parent,
            "STADIUM COMPLEX (LVL ${state.stadiumLevel})",
            "Higher seating capacity increases home match gate revenue."
        )
        UIUtils.button(context, parent, "UPGRADE STADIUM (₹8M)", false) {
            if (state.budget >= 8_000_000) {
                state.budget -= 8_000_000
                state.stadiumLevel++
                repo.save()
                toast("Stadium upgraded to level ${state.stadiumLevel}")
                onRefresh()
            } else {
                toast("Insufficient budget!")
            }
        }

        UIUtils.card(
            context, parent,
            "TRAINING GROUND (LVL ${state.trainingLevel})",
            "Accelerates player growth and stamina recovery."
        )
        UIUtils.button(context, parent, "UPGRADE TRAINING FACILITIES (₹5M)", false) {
            if (state.budget >= 5_000_000) {
                state.budget -= 5_000_000
                state.trainingLevel++
                repo.save()
                toast("Training facility upgraded to level ${state.trainingLevel}")
                onRefresh()
            } else {
                toast("Insufficient budget!")
            }
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}
