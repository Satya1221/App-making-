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

    fun fixtures(teams: List<Team>): MutableList<Fixture> {
        var round = 1
        val games = mutableListOf<Fixture>()
        for (i in teams.indices) {
            for (j in i + 1 until teams.size) {
                games += Fixture("$round-${i}-${j}", round++, teams[i].name, teams[j].name, teams[i].homeVenue)
                games += Fixture("$round-${j}-${i}", round++, teams[j].name, teams[i].name, teams[j].homeVenue)
            }
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

            // Age progression and skill potential development / decline
            if (player.age < 26) {
                if (player.overall < player.potential) {
                    player.overall += Random.nextInt(1, 3)
                }
            } else if (player.age > 33) {
                player.overall = (player.overall - Random.nextInt(1, 3)).coerceAtLeast(50)
            }
        }

        state.fixtures = fixtures(teams)
        state.news.add(0, "Season ${state.season} has commenced! Fixtures regenerated, player age and contracts updated.")
    }
}
