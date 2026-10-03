package com.cricketmaster.manager

import com.cricketmaster.manager.engine.*
import com.cricketmaster.manager.model.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class GameEngineTests {

    private fun samplePlayer(id: Int, name: String, role: String, overall: Int) = Player(
        id = id,
        name = name,
        age = 25,
        nationality = "India",
        role = role,
        overall = overall,
        potential = overall + 5,
        value = 100_000_000,
        salary = 5_000_000,
        contract = 3,
        bat = if (role.contains("Batter")) overall else 40,
        power = if (role.contains("Batter")) overall else 40,
        pace = if (role.contains("Fast")) overall else 20,
        accuracy = if (role.contains("Bowler")) overall else 20,
        spin = if (role.contains("Spin")) overall else 20,
        fielding = 70,
        mental = 75
    )

    private fun sampleSquad(): List<Player> = listOf(
        samplePlayer(1, "Batter 1", "Opener", 85),
        samplePlayer(2, "Batter 2", "Opener", 84),
        samplePlayer(3, "Batter 3", "Top-Order Batter", 82),
        samplePlayer(4, "Batter 4", "Middle-Order Batter", 80),
        samplePlayer(5, "Keeper", "Wicketkeeper", 81),
        samplePlayer(6, "AllRounder 1", "All-Rounder", 79),
        samplePlayer(7, "AllRounder 2", "All-Rounder", 78),
        samplePlayer(8, "Bowler 1", "Fast Bowler", 83),
        samplePlayer(9, "Bowler 2", "Fast Bowler", 82),
        samplePlayer(10, "Bowler 3", "Spin Bowler", 80),
        samplePlayer(11, "Bowler 4", "Spin Bowler", 77),
        samplePlayer(12, "Reserve 1", "Middle-Order Batter", 72)
    )

    @Test
    fun testPlayingXIValidator() {
        val squad = sampleSquad()
        val validXI = squad.take(11).map { it.id }

        // Valid setup
        assertNull(PlayingXiValidator.validate(squad, validXI, captain = 1, viceCaptain = 2, keeper = 5))

        // Invalid count
        assertNotNull(PlayingXiValidator.validate(squad, validXI.take(10), captain = 1, viceCaptain = 2, keeper = 5))

        // Captain not in XI
        assertNotNull(PlayingXiValidator.validate(squad, validXI, captain = 12, viceCaptain = 2, keeper = 5))
    }

    @Test
    fun testBowlingAIChooseSafe() {
        val squad = sampleSquad()
        val figures = squad.associate { it.id to BowlerLine(it.id, it.name) }
        val setup = MatchSetup("Home", "Away", homeXI = squad.take(11), awayXI = squad.take(11), homeCaptainId = 1, awayCaptainId = 1)

        val bowler = BowlingAI.choose(squad, figures, lastBowlerId = 1, phase = MatchPhase.POWERPLAY, setup = setup)
        assertNotNull(bowler)
        assertNotEquals(1, bowler.id)
    }

    @Test
    fun testMatchSimulation() {
        val xi = sampleSquad().take(11)
        val matchEngine = MatchEngine(Random(123))
        val result = matchEngine.simulate(xi, "Metro Kings", "Balanced")

        assertNotNull(result)
        assertTrue(result.ourRuns >= 0)
        assertTrue(result.rivalRuns >= 0)
        assertNotNull(result.record)
    }

    @Test
    fun testLiveMatchEngine() {
        val xi = sampleSquad().take(11)
        val setup = MatchSetup(
            homeTeam = "Harbor Hawks",
            awayTeam = "Metro Kings",
            homeXI = xi,
            awayXI = xi.map { it.copy(id = it.id + 100) },
            homeCaptainId = 1,
            awayCaptainId = 101
        )

        val liveEngine = LiveMatchEngine(Random(456))
        val liveState = liveEngine.start(setup)

        assertFalse(liveState.completed)
        assertEquals(1, liveState.inningsNumber)

        val delivery = liveEngine.nextBall(liveState)
        assertNotNull(delivery)
        assertTrue(liveState.current.runs >= 0)

        liveEngine.autoSimulate(liveState)
        assertTrue(liveState.completed)
        assertNotNull(liveState.result)
    }

    @Test
    fun testSeasonEngineNrrCalculation() {
        val seasonEngine = SeasonEngine()
        val home = Team("Harbor Hawks", 100_000_000)
        val away = Team("Metro Kings", 100_000_000)

        seasonEngine.applyResult(home, away, homeRuns = 180, awayRuns = 150, homeBalls = 120, awayBalls = 120)

        assertEquals(1, home.won)
        assertEquals(1, away.lost)
        assertEquals(2, home.points)
        assertEquals(0, away.points)

        assertTrue(home.nrr > 0.0)
        assertTrue(away.nrr < 0.0)
    }

    @Test
    fun testAuctionEngineAIBid() {
        val auctionEngine = AuctionEngine()
        val player = samplePlayer(101, "Auction Star", "Opener", 88)
        val teams = listOf(
            Team("Metro Kings", 150_000_000, strategy = "Stars"),
            Team("Desert Blazers", 90_000_000, strategy = "Youth")
        )

        val bidResult = auctionEngine.aiBid(player, currentBid = 50_000_000, teams = teams)
        assertNotNull(bidResult)
        assertEquals("Metro Kings", bidResult!!.first.name)
        assertTrue(bidResult.second > 50_000_000)
    }
}
