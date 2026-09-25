package com.cricketmaster.manager.engine

import com.cricketmaster.manager.model.*
import kotlin.random.Random

object PlayingXiValidator {
    fun validate(squad: List<Player>, xi: List<Int>, captain: Int, viceCaptain: Int, keeper: Int): String? = when {
        xi.size != 11 -> "Select exactly 11 players."
        xi.distinct().size != 11 -> "Playing XI contains duplicate players."
        xi.any { id -> squad.none { it.id == id } } -> "Every selected player must belong to your squad."
        captain !in xi || viceCaptain !in xi || keeper !in xi -> "Captain, vice-captain and wicketkeeper must be in the XI."
        else -> null
    }
}

object TossEngine {
    fun toss(home: String, away: String, call: String, random: Random = Random.Default): Pair<String, String> {
        val winner = if (random.nextBoolean()) home else away
        return winner to call.uppercase().let { if (it == "BAT" || it == "FIELD") it else "BAT" }
    }
}

class SeasonEngine {
    fun fixtures(teams: List<Team>): MutableList<Fixture> { var round=1;val games=mutableListOf<Fixture>();for(i in teams.indices)for(j in i+1 until teams.size){games+=Fixture("$round-${i}-${j}",round++,teams[i].name,teams[j].name,"${teams[i].name} Ground");games+=Fixture("$round-${j}-${i}",round++,teams[j].name,teams[i].name,"${teams[j].name} Ground")};return games }
    fun applyResult(home: Team, away: Team, homeRuns: Int, awayRuns: Int, homeBalls: Int = 120, awayBalls: Int = 120) { home.played++;away.played++;home.runsScored+=homeRuns;home.runsConceded+=awayRuns;away.runsScored+=awayRuns;away.runsConceded+=homeRuns;home.ballsFaced+=homeBalls;home.ballsBowled+=awayBalls;away.ballsFaced+=awayBalls;away.ballsBowled+=homeBalls;if(homeRuns>awayRuns){home.won++;home.points+=2;away.lost++}else if(awayRuns>homeRuns){away.won++;away.points+=2;home.lost++}else{home.tied++;away.tied++;home.points++;away.points++};home.nrr=nrr(home);away.nrr=nrr(away) }
    fun complete(fixture: Fixture, home: Team, away: Team, homeRuns: Int, awayRuns: Int, homeBalls: Int = 120, awayBalls: Int = 120): Boolean { if(fixture.status=="COMPLETED")return false;applyResult(home,away,homeRuns,awayRuns,homeBalls,awayBalls);fixture.status="COMPLETED";fixture.result="${fixture.home} $homeRuns vs ${fixture.away} $awayRuns";return true }
    private fun nrr(team: Team)=if(team.ballsFaced==0||team.ballsBowled==0)0.0 else team.runsScored*6.0/team.ballsFaced-team.runsConceded*6.0/team.ballsBowled
}
