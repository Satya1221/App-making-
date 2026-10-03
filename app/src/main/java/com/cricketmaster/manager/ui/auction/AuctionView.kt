package com.cricketmaster.manager.ui.auction

import android.content.Context
import android.widget.LinearLayout
import android.widget.Toast
import com.cricketmaster.manager.engine.AuctionEngine
import com.cricketmaster.manager.engine.GameRepository
import com.cricketmaster.manager.util.UIUtils

class AuctionView(
    private val context: Context,
    private val repo: GameRepository,
    private val onRefresh: () -> Unit
) {

    private val auctionEngine = AuctionEngine()

    fun build(parent: LinearLayout) {
        val state = repo.state
        val pool = repo.auctionPool

        if (state.auctionIndex >= pool.size) {
            UIUtils.card(
                context, parent,
                "AUCTION CONCLUDED",
                "All listed marquee players have been processed. Signed players are now available in your squad roster."
            )

            UIUtils.sectionHeader(context, parent, "Signed Auction Players")
            state.signedAuctionIds.forEach { id ->
                val player = repo.player(id)
                if (player != null) {
                    UIUtils.card(context, parent, "SIGNED: ${player.name}", "${player.role} • OVR ${player.overall}")
                }
            }
            return
        }

        val player = pool[state.auctionIndex]
        val basePrice = player.value * 7 / 10
        val currentBid = if (state.auctionBid == 0) basePrice else state.auctionBid

        UIUtils.card(
            context, parent,
            "LOT #${state.auctionIndex + 1}: ${player.name}",
            "${player.role} • ${player.nationality} • Age ${player.age}\n" +
                    "Overall: ${player.overall} | Potential: ${player.potential}\n" +
                    "Base Price: ₹${basePrice / 1_000_000}M | Current Bid: ₹${currentBid / 1_000_000}M\n" +
                    "Your Available Purse: ₹${state.budget / 1_000_000}M"
        )

        UIUtils.sectionHeader(context, parent, "Bidding Controls")

        UIUtils.button(context, parent, "💰 RAISE BID (+₹1M)", true) {
            val nextBid = currentBid + 1_000_000
            if (state.budget >= nextBid) {
                state.auctionBid = nextBid
                toast("Bid raised to ₹${nextBid / 1_000_000}M")
                onRefresh()
            } else {
                toast("Insufficient budget!")
            }
        }

        UIUtils.button(context, parent, "🔨 CLOSE BIDDING & RESOLVE LOT", false) {
            val otherTeams = repo.teams.filter { it.name != state.teamName }
            val aiResponse = auctionEngine.aiBid(player, currentBid, otherTeams)

            if (aiResponse == null || currentBid >= aiResponse.second) {
                // User wins bid
                state.budget -= currentBid
                repo.squad.add(player)
                state.signedAuctionIds.add(player.id)
                state.auctionHistory.add("SIGNED ${player.name} for ₹${currentBid / 1_000_000}M")
                state.news.add(0, "Auction Win: ${player.name} joins ${state.teamName} for ₹${currentBid / 1_000_000}M.")
                toast("Congratulations! You signed ${player.name}")
            } else {
                // AI outbids
                val winnerTeam = aiResponse.first
                val winBid = aiResponse.second
                winnerTeam.budget -= winBid
                state.auctionHistory.add("${winnerTeam.name} signed ${player.name} for ₹${winBid / 1_000_000}M")
                toast("${winnerTeam.name} outbid you with ₹${winBid / 1_000_000}M")
            }

            state.auctionIndex++
            state.auctionBid = 0
            repo.save()
            onRefresh()
        }

        UIUtils.sectionHeader(context, parent, "Auction History Log")
        state.auctionHistory.asReversed().take(6).forEach { log ->
            UIUtils.card(context, parent, "TRANSACTION", log)
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}
