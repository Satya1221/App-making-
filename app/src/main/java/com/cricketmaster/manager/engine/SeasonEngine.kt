package com.cricketmaster.manager.engine

import com.cricketmaster.manager.model.*
import kotlin.random.Random

object PlayingXiValidator {
    fun validate(squad: List<Player>, xi: List<Int>, captain: Int, viceCaptain: Int, keeper: Int): String? = when {
        xi.size != 11 -> "Select exactly 11 players."
        xi.distinct().size != 11 -> "Playing XI contains duplicate players."
        xi.any { id -> squad.none { it.id == id } } -> "Every selected player must belong to your squad."
        captain !in xi || viceCaptain !in xi || keeper !in xi -> "Captain, vice-captain, and wicketkeeper must be in the XI."
        else -> null
    }
}

object TossEngine {
    fun toss(home: String, away: String, call: String, random: Random = Random.Default): Pair<String, String> {
        val winner = if (random.nextBoolean()) home else away
        val decision = call.uppercase().let { if (it == "BAT" || it == "FIELD") it else "BAT" }
        return winner to decision
    }
}

class SeasonEngine {

    /** Generates a complete double round-robin league schedule grouped into proper rounds/matchdays. */
    fun fixtures(teams: List<Team>): MutableList<Fixture> {
        val games = mutableListOf<Fixture>()
        if (teams.size < 2) return games

        val teamList = teams.map { it.name }.toMutableList()
        val numTeams = teamList.size
        // Ensure even number of teams for round-robin pairing
        if (numTeams % 2 != 0) {
            teamList.add("BYE")
        }
        val n = teamList.size
        val roundsInHalf = n - 1
        val matchesPerRound = n / 2

        var currentRound = 1

        // First Half (Home games)
        for (r in 0 until roundsInHalf) {
            for (m in 0 until matchesPerRound) {
                val home = teamList[(r + m) % (n - 1)]
                val away = if (m == 0) teamList[n - 1] else teamList[(r + n - 1 - m) % (n - 1)]

                if (home != "BYE" && away != "BYE") {
                    val venue = teams.find { it.name == home }?.homeVenue ?: "$home Stadium"
                    games.add(Fixture("$currentRound-$m", currentRound, home, away, venue))
                }
            }
            currentRound++
        }

        // Second Half (Reverse Away games)
        for (r in 0 until roundsInHalf) {
            for (m in 0 until matchesPerRound) {
                val away = teamList[(r + m) % (n - 1)]
                val home = if (m == 0) teamList[n - 1] else teamList[(r + n - 1 - m) % (n - 1)]

                if (home != "BYE" && away != "BYE") {
                    val venue = teams.find { it.name == home }?.homeVenue ?: "$home Stadium"
                    games.add(Fixture("$currentRound-$m", currentRound, home, away, venue))
                }
            }
            currentRound++
        }

        return games
    }

    fun applyResult(home: Team, away: Team, homeRuns: Int, awayRuns: Int, homeBalls: Int = 120, awayBalls: Int = 120) {
        home.played++
        away.played++

        home.runsScored += homeRuns
        home.runsConceded += awayRuns
        away.runsScored += awayRuns
        away.runsConceded += homeRuns

        home.ballsFaced += homeBalls
        home.ballsBowled += awayBalls
        away.ballsFaced += awayBalls
        away.ballsBowled += homeBalls

        if (homeRuns > awayRuns) {
            home.won++
            home.points += 2
            away.lost++
        } else if (awayRuns > homeRuns) {
            away.won++
            away.points += 2
            home.lost++
        } else {
            home.tied++
            away.tied++
            home.points += 1
            away.points += 1
        }

        home.nrr = calculateNrr(home)
        away.nrr = calculateNrr(away)
    }

    fun complete(fixture: Fixture, home: Team, away: Team, homeRuns: Int, awayRuns: Int, homeBalls: Int = 120, awayBalls: Int = 120): Boolean {
        if (fixture.status == "COMPLETED") return false
        applyResult(home, away, homeRuns, awayRuns, homeBalls, awayBalls)
        fixture.status = "COMPLETED"
        fixture.result = "${fixture.home} $homeRuns vs ${fixture.away} $awayRuns"
        return true
    }

    fun calculateNrr(team: Team): Double {
        if (team.ballsFaced == 0 || team.ballsBowled == 0) return 0.0
        val runRateFor = team.runsScored * 6.0 / team.ballsFaced
        val runRateAgainst = team.runsConceded * 6.0 / team.ballsBowled
        return runRateFor - runRateAgainst
    }

    fun startNewSeason(state: GameState, teams: List<Team>, squad: List<Player>) {
        state.season++
        teams.forEach { team ->
            team.points = 0
            team.played = 0
            team.won = 0
            team.lost = 0
            team.tied = 0
            team.nrr = 0.0
            team.runsScored = 0
            team.runsConceded = 0
            team.ballsFaced = 0
            team.ballsBowled = 0
        }

        squad.forEach { player ->
            player.contract = (player.contract - 1).coerceAtLeast(0)
            player.fitness = 95
            player.fatigue = 5
            player.injuryDaysRemaining = 0

            // Advance age
            player.age++

            // Progression based on age, potential ceiling, form, and match performance
            if (player.age in 18..26) {
                val growthBonus = if (player.form >= 75) 1 else 0
                val performanceBonus = if (player.matches > 5) 1 else 0
                val totalGrowth = Random.nextInt(1, 3) + growthBonus + performanceBonus
                player.overall = (player.overall + totalGrowth).coerceAtMost(player.potential).coerceAtMost(99)
            } else if (player.age >= 33) {
                val decline = Random.nextInt(1, 3) - (if (player.fitness > 90) 1 else 0)
                player.overall = (player.overall - decline.coerceAtLeast(1)).coerceAtLeast(50)
            }
        }

        state.fixtures = fixtures(teams)
        state.news.add(0, "Season ${state.season} has commenced! Fixtures regenerated, player age and contracts updated.")
    }
}
