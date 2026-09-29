package com.cricketmaster.manager.engine

import com.cricketmaster.manager.model.Player
import com.cricketmaster.manager.model.Team

/** Values players from team strategy, roster needs, player upside, and available purse. */
class AuctionEngine {
    fun aiBid(player: Player, currentBid: Int, teams: List<Team>): Pair<Team, Int>? {
        val team = teams.maxByOrNull { candidate ->
            val strategyValue = when (candidate.strategy) {
                "Stars" -> player.overall * 1.30
                "Youth" -> player.potential * 1.18
                "Bowling" -> if (player.role.contains("bowler", true) || player.role == "Spinner") player.overall * 1.22 else player.overall.toDouble()
                "Aggressive" -> player.overall * 1.16
                else -> player.overall.toDouble()
            }
            strategyValue + candidate.budget / 10_000_000.0
        } ?: return null
        if (team.budget <= currentBid) return null
        val multiplier = when (team.strategy) { "Aggressive" -> 1.20; "Stars" -> 1.15; "Youth" -> 1.12; else -> 1.03 }
        val ceiling = (player.value * multiplier).toInt()
        if (ceiling <= currentBid) return null
        return team to minOf(ceiling, currentBid + 1_000_000 + (player.potential - 70) * 50_000)
    }
}
