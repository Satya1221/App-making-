package com.cricketmaster.manager.engine

import com.cricketmaster.manager.model.*
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/** Incremental, delivery-at-a-time T20 controller used by the interactive Match Centre. */
class LiveMatchEngine(private val random: Random = Random.Default) {
    fun start(setup: MatchSetup, approach: BattingApproach = BattingApproach.BALANCED): LiveMatchState {
        val homeFirst = (setup.tossWinner == setup.homeTeam) == (setup.tossDecision == "BAT")
        val bat = if (homeFirst) setup.homeXI else setup.awayXI; val bowl = if (homeFirst) setup.awayXI else setup.homeXI
        return LiveMatchState(setup, 1, LiveInningsState(if(homeFirst) setup.homeTeam else setup.awayTeam, bat, bowl), battingApproach = approach)
    }
    fun nextBall(match: LiveMatchState): Delivery? {
        if (match.completed || inningsFinished(match)) return finishOrChangeInnings(match)
        val inn=match.current; val phase=match.phase; val bowler=selectBowler(inn, phase, match.setup, match.fieldingApproach)
        val batter=inn.battingXI[inn.striker]; val delivery=ball(match.inningsNumber, inn.legalBalls/6, inn.legalBalls%6+1,batter,bowler,phase,match.battingApproach,match.fieldingApproach,match.setup)
        val figure=inn.bowlers.getValue(bowler.id); inn.deliveries.add(delivery.copy(batter=batter.name,bowler=bowler.name));inn.runs+=delivery.runs;inn.extras+=delivery.extras;inn.partnership+=delivery.runs;figure.runs+=delivery.runs
        if(delivery.legal){inn.legalBalls++;figure.balls++;if(delivery.runs==0&&delivery.wicket==null)figure.dots++}
        if(delivery.wicket!=null){inn.wickets++;inn.partnership=0;inn.batters[inn.striker].dismissal=delivery.wicket;figure.wickets++;if(inn.wickets<10&&inn.nextBatter<inn.battingXI.size){inn.striker=inn.nextBatter;inn.nextBatter++}}
        else { val line=inn.batters[inn.striker];line.runs+=delivery.runs-delivery.extras;if(delivery.legal)line.balls++;if(delivery.runs-delivery.extras==4)line.fours++;if(delivery.runs-delivery.extras==6)line.sixes++;if(delivery.runs%2==1)swap(inn) }
        if(delivery.legal&&inn.legalBalls%6==0){if(figure.runs==0)figure.maidens++;swap(inn);inn.lastBowlerId=bowler.id}
        if(inningsFinished(match)) finishOrChangeInnings(match)
        return delivery
    }
    fun nextOver(match: LiveMatchState): List<Delivery> { val start=match.current.legalBalls/6;val out=mutableListOf<Delivery>();while(!match.completed&&match.current.legalBalls/6==start)nextBall(match)?.let(out::add);return out }
    fun autoSimulate(match: LiveMatchState): List<Delivery> { val out=mutableListOf<Delivery>();while(!match.completed)nextBall(match)?.let(out::add);return out }
    fun setBatting(match:LiveMatchState, approach:BattingApproach){match.battingApproach=approach}
    fun setFielding(match:LiveMatchState, approach:BowlingApproach){match.fieldingApproach=approach}
    fun selectBowler(match:LiveMatchState, playerId:Int):Boolean { val line=match.current.bowlers[playerId]?:return false;if(line.balls>=24||playerId==match.current.lastBowlerId)return false;match.current.lastBowlerId=playerId;return true }
    private fun finishOrChangeInnings(match:LiveMatchState):Delivery? { val inn=match.current;if(match.inningsNumber==1){match.firstInnings=toScorecard(inn);val bat=inn.bowlingXI;val bowl=inn.battingXI;match.inningsNumber=2;match.current=LiveInningsState(batTeam(match.setup,bat),bat,bowl,inn.runs+1);return null};val first=match.firstInnings!!;val second=toScorecard(inn);val winner=if(second.runs>=second.target!!)second.battingTeam else first.battingTeam;val record=MatchRecord(match.setup,first,second,winner,if(winner==second.battingTeam)"${10-second.wickets} wickets" else "${first.runs-second.runs} runs",(first.batters+second.batters).maxByOrNull{it.runs+it.strikeRate/10}?.name?:"Team effort");StatisticsEngine.apply(record,match.setup.homeXI);match.result=MatchResult(match.setup.awayTeam,first.battingTeam==match.setup.homeTeam,if(first.battingTeam==match.setup.homeTeam)first.runs else second.runs,if(first.battingTeam==match.setup.homeTeam)first.wickets else second.wickets,if(first.battingTeam==match.setup.homeTeam)second.runs else first.runs,if(first.battingTeam==match.setup.homeTeam)second.wickets else first.wickets,winner==match.setup.homeTeam,(first.deliveries+second.deliveries).map{"${it.over}.${it.ball} ${it.bowler} to ${it.batter}: ${it.wicket?:it.runs}"},record.playerOfMatch,record);match.completed=true;return null }
    private fun batTeam(setup:MatchSetup, players:List<Player>)=if(players.first().id==setup.homeXI.first().id)setup.homeTeam else setup.awayTeam
    private fun inningsFinished(match:LiveMatchState):Boolean { val innings=match.current;val target=innings.target;return innings.legalBalls>=match.setup.overs*6||innings.wickets>=10||(target!=null&&innings.runs>=target) }
    private fun toScorecard(i:LiveInningsState)=InningsScorecard(i.battingTeam,i.runs,i.wickets,i.legalBalls,i.target,i.batters,i.bowlers.values.toList(),i.deliveries,i.extras)
    private fun swap(i:LiveInningsState){val t=i.striker;i.striker=i.nonStriker;i.nonStriker=t}
    private fun selectBowler(i:LiveInningsState,phase:MatchPhase,setup:MatchSetup,field:BowlingApproach)=i.bowlingXI.filter{ i.bowlers.getValue(it.id).balls<24&&it.id!=i.lastBowlerId }.maxByOrNull{p->p.bowlingOverall+when(phase){MatchPhase.POWERPLAY->p.powerplayBowling;MatchPhase.MIDDLE->if(setup.pitch==PitchType.DRY)p.spin else p.accuracy;MatchPhase.DEATH->p.deathBowling+p.yorker}+if(field==BowlingApproach.SPIN_ATTACK)p.spin/2 else 0}?:i.bowlingXI.first{ i.bowlers.getValue(it.id).balls<24 }
    private fun ball(n:Int,o:Int,b:Int,bat:Player,bowl:Player,phase:MatchPhase,approach:BattingApproach,field:BowlingApproach,setup:MatchSetup):Delivery { val intent=when(approach){BattingApproach.DEFENSIVE->-8;BattingApproach.AGGRESSIVE->8;BattingApproach.DEATH_ATTACK->13;BattingApproach.CHASE->4;else->0};val fieldEffect=when(field){BowlingApproach.DEFENSIVE->-5;BowlingApproach.ATTACK->2;BowlingApproach.YORKER->-4;BowlingApproach.BOUNCER->1;BowlingApproach.SPIN_ATTACK->-2;else->0};val edge=(bat.battingOverall+bat.form/8+intent-bowl.bowlingOverall-bowl.fatigue/3+fieldEffect+if(setup.pitch==PitchType.FLAT)6 else 0).coerceIn(-35,35);if(random.nextDouble()<bound(.02+(55-bowl.accuracy)/700.0))return Delivery(n,o+1,b,"","","wide",1,1,legal=false);if(random.nextDouble()<bound(.008+(50-bowl.accuracy)/1400.0))return Delivery(n,o+1,b,"","","no ball",1,1,legal=false);if(random.nextDouble()<bound(.045-edge/900.0+if(field==BowlingApproach.ATTACK).01 else 0.0))return Delivery(n,o+1,b,"","","wicket",0,0,listOf("bowled","caught","lbw","run out","stumped").random(random));val six=bound(.02+(bat.power-bowl.deathBowling)/700.0+if(phase==MatchPhase.DEATH).03 else 0.0);val four=bound(.10+edge/450.0+if(phase==MatchPhase.DEATH).04 else 0.0);val roll=random.nextDouble();val runs=when{roll<six->6;roll<six+four->4;roll<.52+edge/1300.0->1;roll<.72->2;roll<.77->3;else->0};return Delivery(n,o+1,b,"","",phase.name,runs) }
    private fun bound(v:Double)=min(.35,max(.001,v))
}
