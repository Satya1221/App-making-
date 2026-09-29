package com.cricketmaster.manager

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import com.cricketmaster.manager.engine.GameRepository
import com.cricketmaster.manager.model.Player
import com.cricketmaster.manager.model.Team
import kotlin.math.roundToInt

class PremiumMainActivity : Activity() {
    private lateinit var repo: GameRepository
    private val bg = Color.rgb(10, 13, 20)
    private val surface = Color.rgb(23, 27, 38)
    private val surface2 = Color.rgb(30, 35, 48)
    private val border = Color.rgb(49, 57, 76)
    private val gold = Color.rgb(239, 190, 67)
    private val text = Color.rgb(235, 238, 246)
    private val muted = Color.rgb(151, 159, 177)
    private val green = Color.rgb(63, 205, 125)
    private val red = Color.rgb(226, 78, 91)
    private var active = "HOME"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = GameRepository(this)
        renderHome()
    }

    override fun onPause() { super.onPause(); repo.save() }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).roundToInt()
    private fun bgDrawable(color: Int, radius: Int = 16, stroke: Int = 0, strokeColor: Int = border): GradientDrawable = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
        if (stroke > 0) setStroke(dp(stroke), strokeColor)
    }

    private fun tv(value: String, size: Float = 15f, color: Int = text, bold: Boolean = false): TextView = TextView(this).apply {
        this.text = value; textSize = size; setTextColor(color); setPadding(dp(4), dp(3), dp(4), dp(3))
        if (bold) setTypeface(Typeface.DEFAULT, Typeface.BOLD)
    }

    private fun root(title: String, subtitle: String = ""): LinearLayout {
        val r = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        val top = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(10)) }
        top.addView(tv(title, 26f, text, true))
        if (subtitle.isNotBlank()) top.addView(tv(subtitle, 13f, muted))
        r.addView(top)
        return r
    }

    private fun show(title: String, subtitle: String = "", draw: (LinearLayout) -> Unit) {
        val r = root(title, subtitle)
        val scroll = ScrollView(this).apply { isFillViewport = true }
        val body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(18), 0, dp(18), dp(14)) }
        draw(body); scroll.addView(body)
        r.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        r.addView(bottomNav())
        setContentView(r)
    }

    private fun section(parent: LinearLayout, title: String, right: String = "") {
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(16), 0, dp(7)) }
        row.addView(tv(title.uppercase(), 12f, muted, true), LinearLayout.LayoutParams(0, -2, 1f))
        if (right.isNotBlank()) row.addView(tv(right.uppercase(), 11f, gold, true))
        parent.addView(row)
    }

    private fun card(parent: LinearLayout, title: String, body: String, action: (() -> Unit)? = null) {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(12)); background = bgDrawable(surface, 15, 1)
            if (action != null) { isClickable = true; setOnClickListener { action() } }
        }
        c.addView(tv(title, 16f, text, true)); c.addView(tv(body, 13.5f, muted))
        val lp = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(5), 0, dp(5)) }
        parent.addView(c, lp)
    }

    private fun pill(parent: LinearLayout, label: String, value: String, color: Int = gold) {
        val p = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(11), dp(8), dp(11), dp(8)); background = bgDrawable(surface2, 10, 1) }
        p.addView(tv(label.uppercase(), 10f, muted, true)); p.addView(tv(value, 16f, color, true))
        parent.addView(p, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(3), 0, dp(3), 0) })
    }

    private fun action(parent: LinearLayout, label: String, primary: Boolean = true, run: () -> Unit) {
        val b = TextView(this).apply {
            text = label; textSize = 14f; gravity = Gravity.CENTER; setTypeface(Typeface.DEFAULT, Typeface.BOLD); setPadding(dp(12), dp(12), dp(12), dp(12)); setOnClickListener { run() }
            background = bgDrawable(if (primary) gold else surface, 13, 1, if (primary) gold else border); setTextColor(if (primary) Color.BLACK else text)
        }
        parent.addView(b, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(5), 0, dp(5)) })
    }

    private fun bottomNav(): LinearLayout {
        val nav = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER; setPadding(dp(8), dp(6), dp(8), dp(7)); background = bgDrawable(Color.rgb(14,17,25), 0, 1) }
        val items = listOf("HOME" to "⌂", "TEAM" to "♟", "LEAGUE" to "♜", "WORLD" to "◎", "PROFILE" to "♙", "HQ" to "▦")
        items.forEach { (name, icon) ->
            val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(4), dp(3), dp(4), dp(2)) }
            col.addView(tv(icon, 21f, if (active == name) gold else muted, true).apply { gravity = Gravity.CENTER })
            col.addView(tv(name.replace("LEAGUE", "LEAGUE"), 9f, if (active == name) gold else muted, true).apply { gravity = Gravity.CENTER })
            col.setOnClickListener { active = name; when (name) { "HOME" -> renderHome(); "TEAM" -> renderTeam(); "LEAGUE" -> renderLeague(); "WORLD" -> renderWorld(); "PROFILE" -> renderProfile(); "HQ" -> renderHQ() } }
            nav.addView(col, LinearLayout.LayoutParams(0, -2, 1f))
        }
        return nav
    }

    private fun renderHome() = show("${repo.state.teamName}", "Season ${repo.state.season}  •  Management hub") { c ->
        val team = repo.teams.firstOrNull { it.name == repo.state.teamName }
        val hero = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(15), dp(16), dp(15)); background = bgDrawable(surface, 17, 1) }
        hero.addView(tv("CLUB OVERVIEW", 11f, gold, true)); hero.addView(tv(repo.state.teamName, 24f, text, true))
        hero.addView(tv("T20  •  ${repo.state.sponsor}", 13f, muted))
        val stats = LinearLayout(this).apply { setPadding(0, dp(10), 0, 0) }
        pill(stats, "Rating", "${team?.points ?: 0} PTS", gold); pill(stats, "Budget", "₹${repo.state.budget / 1_000_000}M", green); pill(stats, "Squad", "${repo.squad.size}/25", text)
        hero.addView(stats); c.addView(hero)

        section(c, "Next fixture", "Calendar")
        val fixture = repo.state.fixtures.firstOrNull { it.status == "SCHEDULED" }
        card(c, "MATCHDAY", fixture?.let { "${it.home}  vs  ${it.away}\n${it.venue}  •  Round ${it.round}" } ?: "No scheduled fixture yet")

        section(c, "Today")
        val training = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        pill(training, "Form", "${repo.squad.map { it.form }.average().roundToInt()} / 100", gold)
        pill(training, "Fitness", "${repo.squad.map { it.fitness }.average().roundToInt()}%", green)
        pill(training, "Tactic", repo.state.tactic, text)
        c.addView(training)

        section(c, "Quick actions")
        action(c, "PLAY / SIMULATE NEXT MATCH") { active = "TEAM"; renderTeam() }
        action(c, "MANAGE SQUAD", false) { active = "TEAM"; renderTeam() }
        action(c, "OPEN FULL MANAGEMENT", false) { startActivity(android.content.Intent(this, MainActivity::class.java)) }

        section(c, "Club news")
        repo.state.news.take(3).forEach { card(c, "UPDATE", it) }
    }

    private fun renderTeam() = show("${repo.state.teamName}", "Squad  •  ${repo.squad.size} players") { c ->
        val team = repo.teams.firstOrNull { it.name == repo.state.teamName }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(dp(14), dp(12), dp(14), dp(12)); background = bgDrawable(surface, 15, 1) }
        pill(header, "BAT", "${repo.squad.filter { it.bat >= it.pace }.map { it.overall }.average().roundToInt()}", gold)
        pill(header, "BOWL", "${repo.squad.map { it.pace.coerceAtLeast(it.accuracy) }.average().roundToInt()}", green)
        pill(header, "PTS", "${team?.points ?: 0}", text)
        c.addView(header)
        section(c, "Playing XI", "${repo.state.selectedXI.size}/11")
        repo.squad.sortedByDescending { it.overall }.forEachIndexed { i, p ->
            playerRow(c, p, i + 1)
        }
        action(c, "AUTO-PICK STRONGEST XI") {
            repo.state.selectedXI = repo.squad.sortedByDescending { it.overall }.take(11).map { it.id }.toMutableList(); repo.save(); renderTeam()
        }
        action(c, "OPEN DETAILED MANAGEMENT", false) { startActivity(android.content.Intent(this, MainActivity::class.java)) }
    }

    private fun playerRow(parent: LinearLayout, p: Player, number: Int) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(10), dp(10), dp(10), dp(10)); background = bgDrawable(surface, 13, 1) }
        val badge = tv(number.toString(), 12f, gold, true).apply { gravity = Gravity.CENTER; background = bgDrawable(Color.rgb(52,43,24), 9, 1, Color.rgb(92,76,37)) }
        row.addView(badge, LinearLayout.LayoutParams(dp(35), dp(35)))
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), 0, 0, 0) }
        info.addView(tv(p.name, 15f, text, true)); info.addView(tv("${p.role}  •  ${p.age} yrs  •  ${p.nationality}", 11.5f, muted))
        row.addView(info, LinearLayout.LayoutParams(0, -2, 1f))
        val rating = tv(p.overall.toString(), 17f, if (p.form >= 70) green else gold, true).apply { gravity = Gravity.CENTER }
        row.addView(rating, LinearLayout.LayoutParams(dp(45), -2))
        row.setOnClickListener { playerDetails(p) }
        parent.addView(row, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(4), 0, dp(4)) })
    }

    private fun playerDetails(p: Player) = show(p.name, "${p.role}  •  ${p.age} yrs  •  ${p.nationality}") { c ->
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        pill(top, "FORM", p.form.toString(), gold); pill(top, "MORALE", "${(p.confidence / 10).coerceIn(1,10)}/10", text); pill(top, "FITNESS", "${p.fitness}%", green); c.addView(top)
        section(c, "Batting")
        statGrid(c, listOf("BAT" to p.battingOverall, "POWER" to p.power, "TIMING" to p.timing, "TECH" to p.technique, "PACE BAT" to p.paceBatting, "SPIN BAT" to p.spinBatting))
        section(c, "Bowling")
        statGrid(c, listOf("BOWL" to p.bowlingOverall, "PACE" to p.pace, "ACCURACY" to p.accuracy, "SPIN" to p.spin, "VARIATION" to p.variation, "DEATH" to p.deathBowling))
        section(c, "Fielding & mental")
        statGrid(c, listOf("CATCH" to p.catching, "THROW" to p.throwing, "FIELD" to p.groundFielding, "MENTAL" to p.mental, "CONSIST." to p.consistency, "EXP" to p.experience))
        section(c, "Career")
        card(c, "RECORD", "Matches ${p.matches}  •  Runs ${p.runs}  •  Wickets ${p.wickets}\nValue ₹${p.value / 1_000_000}M  •  Salary ₹${p.salary / 1000}K  •  Contract ${p.contract} years")
    }

    private fun statGrid(parent: LinearLayout, stats: List<Pair<String, Int>>) {
        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        stats.chunked(3).forEach { group ->
            val row = LinearLayout(this)
            group.forEach { (label, value) ->
                val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(9), dp(8), dp(9), dp(8)); background = bgDrawable(surface2, 9, 1) }
                box.addView(tv(label, 9.5f, muted, true)); box.addView(tv(value.toString(), 16f, if (value >= 80) green else text, true))
                row.addView(box, LinearLayout.LayoutParams(0, -2, 1f).apply { setMargins(dp(3), dp(3), dp(3), dp(3)) })
            }
            grid.addView(row)
        }
        parent.addView(grid)
    }

    private fun renderLeague() = show("League", "Season ${repo.state.season}  •  Premier T20") { c ->
        card(c, "LEAGUE & PLAYOFFS", "Every team plays the league schedule. Top four progress to the playoffs.")
        section(c, "Standings", "PTS / NRR")
        repo.teams.sortedWith(compareByDescending<Team> { it.points }.thenByDescending { it.nrr }).forEachIndexed { i, t ->
            val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(10), dp(10), dp(10), dp(10)); background = bgDrawable(if (t.name == repo.state.teamName) Color.rgb(48,45,35) else surface, 10, 1) }
            row.addView(tv("${i + 1}", 13f, if (t.name == repo.state.teamName) gold else muted, true), LinearLayout.LayoutParams(dp(30), -2))
            row.addView(tv(t.name, 15f, if (t.name == repo.state.teamName) gold else text, true), LinearLayout.LayoutParams(0, -2, 1f))
            row.addView(tv("${t.played}  ${t.won}  ${t.lost}", 11f, muted))
            row.addView(tv("${t.points}", 15f, text, true), LinearLayout.LayoutParams(dp(38), -2))
            row.addView(tv("${if (t.nrr >= 0) "+" else ""}${"%.2f".format(t.nrr)}", 11f, green, true), LinearLayout.LayoutParams(dp(55), -2))
            c.addView(row, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(3), 0, dp(3)) })
        }
        section(c, "Season leaders")
        val runs = repo.squad.maxByOrNull { it.runs }; val wickets = repo.squad.maxByOrNull { it.wickets }
        card(c, "MOST RUNS", runs?.let { "${it.name}  •  ${it.runs} runs" } ?: "No runs recorded yet")
        card(c, "MOST WICKETS", wickets?.let { "${it.name}  •  ${it.wickets} wickets" } ?: "No wickets recorded yet")
    }

    private fun renderWorld() = show("World", "International cricket  •  T20I") { c ->
        val tabs = LinearLayout(this)
        listOf("PLAYERS", "NATIONS", "SEARCH").forEach { label ->
            val t = tv(label, 12f, if (label == "PLAYERS") gold else muted, true).apply { gravity = Gravity.CENTER; background = bgDrawable(if (label == "PLAYERS") Color.rgb(52,47,32) else surface, 10, 1) }
            tabs.addView(t, LinearLayout.LayoutParams(0, dp(43), 1f).apply { setMargins(dp(2), 0, dp(2), 0) })
        }
        c.addView(tabs)
        section(c, "Rankings")
        val ranked = repo.squad.sortedByDescending { it.overall }.take(10)
        if (ranked.isEmpty()) card(c, "NO RANKINGS YET", "Play or simulate international matches to populate rankings.")
        else ranked.forEachIndexed { i, p -> card(c, "#${i + 1}  ${p.name}", "${p.nationality}  •  ${p.role}\nOverall ${p.overall}  •  Form ${p.form}") }
    }

    private fun renderProfile() = show("Profile", "Career and manager record") { c ->
        val profile = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(16), dp(16), dp(16)); background = bgDrawable(surface, 17, 1) }
        profile.addView(tv("HEAD COACH", 11f, gold, true)); profile.addView(tv("Manager", 24f, text, true)); profile.addView(tv(repo.state.teamName, 15f, red, true)); profile.addView(tv("Season ${repo.state.season}", 13f, muted)); c.addView(profile)
        val wins = repo.teams.firstOrNull { it.name == repo.state.teamName }?.won ?: 0
        section(c, "Lifetime stats")
        statGrid(c, listOf("SEASONS" to repo.state.season, "MATCHES" to repo.state.matchHistory.size, "WINS" to wins, "LOSSES" to (repo.state.matchHistory.size - wins).coerceAtLeast(0), "TITLES" to 0, "PLAYOFF" to 0))
        section(c, "Career journey")
        card(c, "SEASON ${repo.state.season}", if (repo.state.matchHistory.isEmpty()) "No silverware yet — your story starts here." else "${repo.state.matchHistory.size} matches recorded. Keep building the campaign.")
        section(c, "Records")
        val best = repo.squad.maxByOrNull { it.runs }; card(c, "PLAYER RECORDS", best?.let { "Leading scorer: ${it.name} • ${it.runs} runs" } ?: "Player records will appear as the season progresses.")
    }

    private fun renderHQ() = show("Club HQ", "Facilities  •  Finances  •  Academy") { c ->
        val head = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(16), dp(14), dp(16), dp(14)); background = bgDrawable(surface, 17, 1) }
        head.addView(tv(repo.state.teamName, 22f, text, true)); head.addView(tv("Club-owned headquarters", 13f, muted)); head.addView(tv("₹${repo.state.budget / 1_000_000}M CLUB CASH", 18f, gold, true)); c.addView(head)
        section(c, "Facilities")
        facility(c, "Stadium", repo.state.stadiumLevel, "More matchday ticket potential", 90, repo.state.stadiumLevel < 6)
        facility(c, "Training Complex", repo.state.trainingLevel, "Stronger first-team development", 67, repo.state.trainingLevel < 6)
        facility(c, "Medical Centre", repo.state.trainingLevel, "Better fitness and injury support", 64, repo.state.trainingLevel < 6)
        facility(c, "Youth Academy", repo.state.trainingLevel, "Deeper youth scouting and development", 64, repo.state.trainingLevel < 6)
        facility(c, "Commercial Centre", repo.state.stadiumLevel, "Stronger merchandise and brand reach", 58, repo.state.stadiumLevel < 6)
        section(c, "Board")
        card(c, "RESULTS", "Steady  •  ${repo.teams.firstOrNull { it.name == repo.state.teamName }?.points ?: 0} points")
        card(c, "FANS", "Steady  •  Squad quality ${repo.squad.map { it.overall }.average().roundToInt()}")
        card(c, "FINANCE", "Steady  •  Sponsor ${repo.state.sponsor}")
        action(c, "OPEN FINANCE & UPGRADES", false) { startActivity(android.content.Intent(this, MainActivity::class.java)) }
    }

    private fun facility(parent: LinearLayout, name: String, level: Int, desc: String, cost: Int, canUpgrade: Boolean) {
        val c = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(12), dp(14), dp(12)); background = bgDrawable(surface, 15, 1) }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        top.addView(tv(name, 17f, text, true), LinearLayout.LayoutParams(0, -2, 1f)); top.addView(tv("L$level", 13f, gold, true)); c.addView(top)
        c.addView(tv(desc, 12.5f, muted)); c.addView(tv("NEXT LEVEL   ${level.coerceAtMost(5)} → ${(level + 1).coerceAtMost(6)}    •    BUILD COST  ${cost} CF", 11f, muted, true))
        if (canUpgrade) {
            val b = TextView(this).apply { text = "START UPGRADE"; gravity = Gravity.CENTER; setTextColor(Color.BLACK); textSize = 13f; setTypeface(Typeface.DEFAULT, Typeface.BOLD); setPadding(dp(10), dp(10), dp(10), dp(10)); background = bgDrawable(gold, 11) }
            b.setOnClickListener { toast("Upgrade queued — use full management for final confirmation") }
            c.addView(b, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(8), 0, 0) })
        }
        parent.addView(c, LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, dp(5), 0, dp(5)) })
    }

    private fun toast(s: String) = Toast.makeText(this, s, Toast.LENGTH_SHORT).show()
}
