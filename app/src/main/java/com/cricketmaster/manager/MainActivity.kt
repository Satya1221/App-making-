package com.cricketmaster.manager

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import com.cricketmaster.manager.engine.GameRepository
import com.cricketmaster.manager.model.MatchResult
import com.cricketmaster.manager.model.Player
import com.cricketmaster.manager.ui.auction.AuctionView
import com.cricketmaster.manager.ui.dev.DeveloperModeView
import com.cricketmaster.manager.ui.finances.FinancesView
import com.cricketmaster.manager.ui.home.HomeDashboardView
import com.cricketmaster.manager.ui.league.LeagueView
import com.cricketmaster.manager.ui.live.LiveMatchView
import com.cricketmaster.manager.ui.scorecard.ScorecardView
import com.cricketmaster.manager.ui.settings.SettingsView
import com.cricketmaster.manager.ui.squad.PlayerDetailView
import com.cricketmaster.manager.ui.squad.SquadView
import com.cricketmaster.manager.ui.stats.StatsView
import com.cricketmaster.manager.ui.tactics.TacticsView
import com.cricketmaster.manager.ui.training.TrainingView
import com.cricketmaster.manager.util.UIUtils

class MainActivity : Activity() {

    private lateinit var repo: GameRepository
    private var currentTab = "HOME"
    private var selectedPlayer: Player? = null
    private var activeScorecard: MatchResult? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = GameRepository(this)
        renderUI()
    }

    override fun onPause() {
        super.onPause()
        repo.save()
    }

    private fun renderUI() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(UIUtils.bg)
        }

        // Top Header
        val topHeader = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UIUtils.dp(this@MainActivity, 16), UIUtils.dp(this@MainActivity, 14), UIUtils.dp(this@MainActivity, 16), UIUtils.dp(this@MainActivity, 8))
        }
        topHeader.addView(UIUtils.tv(this, "CRICKET MASTER MANAGER", 22f, UIUtils.textPrimary, true))
        topHeader.addView(UIUtils.tv(this, "Season ${repo.state.season} • ${repo.state.teamName}", 12.5f, UIUtils.gold))
        root.addView(topHeader)

        // Scrollable Body Content
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val body = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(UIUtils.dp(this@MainActivity, 16), 0, UIUtils.dp(this@MainActivity, 16), UIUtils.dp(this@MainActivity, 16))
        }

        when {
            activeScorecard != null -> {
                ScorecardView(this, activeScorecard!!) {
                    activeScorecard = null
                    renderUI()
                }.build(body)
            }
            selectedPlayer != null -> {
                PlayerDetailView(this, repo, selectedPlayer!!) {
                    selectedPlayer = null
                    renderUI()
                }.build(body)
            }
            else -> {
                when (currentTab) {
                    "HOME" -> HomeDashboardView(this, repo).build(body) { tab ->
                        currentTab = tab
                        renderUI()
                    }
                    "SQUAD" -> SquadView(this, repo, { player ->
                        selectedPlayer = player
                        renderUI()
                    }, { renderUI() }).build(body)
                    "TACTICS" -> TacticsView(this, repo) { renderUI() }.build(body)
                    "LIVE" -> LiveMatchView(this, repo, { tab ->
                        currentTab = tab
                        renderUI()
                    }, { result ->
                        activeScorecard = result
                        renderUI()
                    }, { renderUI() }).build(body)
                    "LEAGUE" -> LeagueView(this, repo).build(body)
                    "AUCTION" -> AuctionView(this, repo) { renderUI() }.build(body)
                    "TRAINING" -> TrainingView(this, repo) { renderUI() }.build(body)
                    "FINANCES" -> FinancesView(this, repo) { renderUI() }.build(body)
                    "STATS" -> StatsView(this, repo).build(body)
                    "SETTINGS" -> SettingsView(this, repo, {
                        currentTab = "DEV"
                        renderUI()
                    }, { renderUI() }).build(body)
                    "DEV" -> DeveloperModeView(this, repo) {
                        currentTab = "SETTINGS"
                        renderUI()
                    }.build(body)
                }
            }
        }

        scroll.addView(body)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))

        // Bottom Navigation Bar
        root.addView(buildNavigationBar())

        setContentView(root)
    }

    private fun buildNavigationBar(): HorizontalScrollView {
        val hscroll = HorizontalScrollView(this).apply {
            setBackgroundColor(UIUtils.surface)
            isHorizontalScrollBarEnabled = false
        }

        val navContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(UIUtils.dp(this@MainActivity, 6), UIUtils.dp(this@MainActivity, 6), UIUtils.dp(this@MainActivity, 6), UIUtils.dp(this@MainActivity, 6))
        }

        val tabs = listOf(
            "HOME" to "🏠",
            "LIVE" to "🏏",
            "SQUAD" to "♟",
            "TACTICS" to "⚙",
            "LEAGUE" to "🏆",
            "AUCTION" to "🔨",
            "TRAINING" to "🏋",
            "FINANCES" to "💰",
            "STATS" to "📊",
            "SETTINGS" to "⚙"
        )

        tabs.forEach { (tabKey, icon) ->
            val isActive = currentTab == tabKey && selectedPlayer == null && activeScorecard == null
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(UIUtils.dp(this@MainActivity, 12), UIUtils.dp(this@MainActivity, 4), UIUtils.dp(this@MainActivity, 12), UIUtils.dp(this@MainActivity, 4))
                background = UIUtils.bgDrawable(this@MainActivity, if (isActive) UIUtils.surface2 else UIUtils.surface, 8)
                setOnClickListener {
                    currentTab = tabKey
                    selectedPlayer = null
                    activeScorecard = null
                    renderUI()
                }
            }

            item.addView(UIUtils.tv(this, icon, 16f, if (isActive) UIUtils.gold else UIUtils.muted, true).apply { gravity = Gravity.CENTER })
            item.addView(UIUtils.tv(this, tabKey, 9.5f, if (isActive) UIUtils.gold else UIUtils.muted, true).apply { gravity = Gravity.CENTER })

            navContainer.addView(item)
        }

        hscroll.addView(navContainer)
        return hscroll
    }
}
