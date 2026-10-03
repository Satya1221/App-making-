package com.cricketmaster.manager.engine

import com.cricketmaster.manager.model.Player
import com.cricketmaster.manager.model.Team

/** AI Auction Engine with manager personas, purse checks, role needs, and dynamic bidding ceilings. */
class AuctionEngine {

    fun aiBid(player: Player, currentBid: Int, teams: List<Team>): Pair<Team, Int>? {
        val eligibleTeams = teams.filter { it.budget >= currentBid + 1_000_000 }
        if (eligibleTeams.isEmpty()) return null

        val candidate = eligibleTeams.maxByOrNull { team ->
            val strategyValue = when (team.strategy) {
                "Stars" -> player.overall * 1.30
                "Youth" -> player.potential * 1.25
                "Bowling" -> if (player.role.contains("Bowler", true) || player.role == "Spin Bowler") player.overall * 1.28 else player.overall.toDouble()
                "Aggressive" -> player.overall * 1.18
                "Value" -> if (player.salary < 6_500_000) player.overall * 1.20 else player.overall.toDouble()
                else -> player.overall.toDouble()
            }
            strategyValue + (team.budget / 10_000_000.0)
        } ?: return null

        val multiplier = when (candidate.strategy) {
            "Aggressive" -> 1.25
            "Stars" -> 1.20
            "Youth" -> 1.15
            "Bowling" -> 1.12
            else -> 1.05
        }

        val maxBidCeiling = (player.value * multiplier).toInt().coerceAtMost(candidate.budget - 5_000_000)
        if (maxBidCeiling <= currentBid) return null

        val incrementalStep = 1_000_000 + (player.potential - 70).coerceAtLeast(0) * 50_000
        val nextBid = minOf(maxBidCeiling, currentBid + incrementalStep)

        return candidate to nextBid
    }
}
