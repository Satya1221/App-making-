package com.cricketmaster.manager

import android.app.*
import android.os.Bundle
import android.graphics.Color
import android.graphics.Typeface
import android.view.*
import android.widget.*
import com.cricketmaster.manager.engine.*
import com.cricketmaster.manager.model.*

class MainActivity: Activity() {
    private val bg=Color.rgb(7,19,29); private val panel=Color.rgb(16,38,53); private val accent=Color.rgb(255,183,3); private lateinit var repo:GameRepository; private val match=MatchEngine(); private val auctionEngine=AuctionEngine()
    override fun onCreate(b:Bundle?){super.onCreate(b); repo=GameRepository(this); home()}
    override fun onPause(){super.onPause();repo.save()}
    private fun root(title:String):LinearLayout { val r=LinearLayout(this);r.orientation=LinearLayout.VERTICAL;r.setBackgroundColor(bg); r.setPadding(18,20,18,8); val h=TextView(this);h.text=title;h.setTextColor(Color.WHITE);h.textSize=25f;h.setTypeface(null,Typeface.BOLD);r.addView(h); return r }
    private fun scroll(title:String, draw:(LinearLayout)->Unit){val r=root(title);val s=ScrollView(this);val c=LinearLayout(this);c.orientation=LinearLayout.VERTICAL;s.addView(c);draw(c);r.addView(s,LinearLayout.LayoutParams(-1,0,1f));r.addView(nav());setContentView(r)}
    private fun text(s:String,size:Float=15f,color:Int=Color.LTGRAY)=TextView(this).apply{text=s;textSize=size;setTextColor(color);setPadding(6,5,6,5)}
    private fun card(c:LinearLayout,title:String,body:String, action:(()->Unit)?=null){val b=LinearLayout(this);b.orientation=LinearLayout.VERTICAL;b.setPadding(14,10,14,10);b.setBackgroundColor(panel); val lp=LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,8,0,3);c.addView(b,lp);b.addView(text(title,17f,Color.WHITE).apply{setTypeface(null,Typeface.BOLD)});b.addView(text(body));if(action!=null)b.setOnClickListener{action()}}
    private fun button(c:LinearLayout,label:String,run:()->Unit){c.addView(Button(this).apply{text=label;setOnClickListener{run()}},LinearLayout.LayoutParams(-1,-2).apply{setMargins(0,4,0,4)})}
    private fun nav():HorizontalScrollView { val s=HorizontalScrollView(this);val l=LinearLayout(this); listOf("HOME" to ::home,"SQUAD" to ::squad,"AUCTION" to ::auction,"MATCHES" to ::matches,"TACTICS" to ::tactics,"FINANCES" to ::finances,"STANDINGS" to ::standings,"MORE" to ::more).forEach{(n,f)->l.addView(Button(this).apply{text=n;setTextColor(accent);setOnClickListener{f()}},LinearLayout.LayoutParams(-2,-2))};s.addView(l);return s }
    private fun home()=scroll("CRICKET MASTER  •  SEASON ${repo.state.season}"){c->
        card(c,repo.state.teamName,"● Overall ${repo.squad.map{it.overall}.average().toInt()}   • League position #${repo.teams.sortedByDescending{it.points}.indexOfFirst{it.name==repo.state.teamName}+1}\nAvailable purse  ₹${repo.state.budget/1_000_000}M   • Squad ${repo.squad.size}/25")
        card(c,"NEXT FIXTURE","Harbor Hawks vs Metro Kings\nT20 League • Riverside Oval • Clear skies",::matches)
        card(c,"LAST RESULT",repo.state.lastResult?.let{"vs ${it.opponent}: ${if(it.won)"WON" else "LOST"} • ${it.ourRuns}/${it.ourWickets}"} ?: "No match played yet")
        card(c,"FORM GUIDE","${repo.squad.take(5).joinToString("  "){it.name.substringBefore(" ")+" "+it.form}}")
        text("QUICK ACTIONS",14f,accent); button(c,"▶ PLAY NEXT MATCH",::matches);button(c,"Manage playing XI",::squad);button(c,"Auction room",::auction);button(c,"Set tactics",::tactics)
        text("CLUB NEWS",14f,accent);repo.state.news.take(4).forEach{card(c,"UPDATE",it)} }
    private fun squad()=scroll("SQUAD  •  ${repo.state.selectedXI.size}/11 SELECTED"){c->
        val roles=repo.squad.groupingBy{it.role}.eachCount();card(c,"SQUAD BALANCE",roles.entries.joinToString(" • "){"${it.key}: ${it.value}"}+if(roles["Fast bowler"]?:0<3)"\n⚠ Add fast bowling depth." else "\n✓ Squad is balanced.")
        repo.squad.sortedByDescending{it.overall}.forEach { p-> card(c,"${if(p.id in repo.state.selectedXI) "✓ " else ""}${p.name}  ${p.overall}","${p.role} • ${p.age} • ${p.nationality}\nForm ${p.form}  Fitness ${p.fitness}  Contract ${p.contract}y • ₹${p.value/1_000_000}M") { player(p) } }
        button(c,"Auto-pick strongest XI"){repo.state.selectedXI=repo.squad.sortedByDescending{it.overall}.take(11).map{it.id}.toMutableList();toast("Strongest XI selected");squad()}
    }
    private fun player(p:Player)=scroll(p.name.uppercase()){c->
        card(c,"${p.role}  •  OVERALL ${p.overall}","${p.nationality} • Age ${p.age} • Potential ${p.potential}\nForm ${p.form}/100   Fitness ${p.fitness}/100   Fatigue ${p.fatigue}/100")
        card(c,"BATTING","Technique ${p.bat}   Power ${p.power}   Timing ${p.bat-2}\nAgainst pace ${p.bat}   Against spin ${p.bat-3}   Finishing ${p.power}")
        card(c,"BOWLING","Pace ${p.pace}   Accuracy ${p.accuracy}   Spin ${p.spin}\nVariations ${p.accuracy+2}   Economy ${9-(p.accuracy/20.0)}")
        card(c,"FIELDING & MENTAL","Catching ${p.fielding} • Reflexes ${p.fielding-2} • Throwing ${p.fielding+1}\nComposure ${p.mental} • Consistency ${p.mental-3} • Leadership ${p.mental-5}")
        card(c,"CAREER","Matches ${p.matches} • Runs ${p.runs} • Wickets ${p.wickets}\nMarket value ₹${p.value/1_000_000}M • Salary ₹${p.salary/1000}K • ${p.contract} years remaining")
        button(c,if(p.id in repo.state.selectedXI)"Remove from playing XI" else "Add to playing XI"){if(p.id in repo.state.selectedXI)repo.state.selectedXI.remove(p.id) else if(repo.state.selectedXI.size<11)repo.state.selectedXI.add(p.id) else toast("Select only 11 players");squad()};button(c,"Set as captain"){repo.state.captainId=p.id;toast("${p.name} is captain")};button(c,"Set as vice captain"){repo.state.viceCaptainId=p.id;toast("${p.name} is vice captain")};if(p.role.contains("Wicketkeeper",true))button(c,"Set as wicketkeeper"){if(p.id !in repo.state.selectedXI)toast("Wicketkeeper must be in XI")else{repo.state.wicketKeeperId=p.id;repo.save();toast("${p.name} selected as wicketkeeper")}};button(c,"Training: improve form"){p.form=(p.form+3).coerceAtMost(99);p.fatigue+=2;repo.state.news.add(0,"${p.name} completed an intensive training session.");toast("Form improved")};button(c,"Renew contract (+1 year)"){p.contract++;repo.state.budget-=p.salary;toast("Contract renewed")}
    }
    private fun auction()=scroll("PLAYER AUCTION"){c->
        val p=repo.auctionPool.getOrNull(repo.state.auctionIndex);if(p==null){card(c,"AUCTION COMPLETE","All listed players have been processed. Your purchases are now in the squad.");button(c,"Return home",::home);return@scroll}
        val base=p.value*7/10; val bid=if(repo.state.auctionBid==0)base else repo.state.auctionBid
        card(c,"ON THE BLOCK: ${p.name}","${p.role} • ${p.nationality} • Age ${p.age}\nOverall ${p.overall} • Potential ${p.potential}\nBase price ₹${base/1_000_000}M  • Current bid ₹${bid/1_000_000}M\nYour purse ₹${repo.state.budget/1_000_000}M")
        button(c,"Raise bid by ₹1M"){if(repo.state.budget>=bid+1_000_000){repo.state.auctionBid=bid+1_000_000;auction()}else toast("Insufficient purse")}
        button(c,"Close bidding / AI decision"){ val ai=auctionEngine.aiBid(p,bid,repo.teams.filter{it.name!=repo.state.teamName});if(ai==null || bid>=ai.second){repo.state.budget-=bid;repo.squad.add(p);repo.state.signedAuctionIds.add(p.id);repo.state.auctionHistory.add("SIGNED ${p.name} for ₹${bid/1_000_000}M");repo.state.news.add(0,"Auction win: ${p.name} joins Harbor Hawks for ₹${bid/1_000_000}M.");toast("You signed ${p.name}")}else {ai.first.budget-=ai.second;repo.state.auctionHistory.add("${ai.first.name} signed ${p.name} for ₹${ai.second/1_000_000}M");toast("${ai.first.name} outbid you")};repo.state.auctionIndex++;repo.state.auctionBid=0;repo.save();auction()}
        text("BID HISTORY",14f,accent);repo.state.auctionHistory.asReversed().forEach{card(c,"AUCTION",it)}
    }
    private fun matches()=scroll("MATCH CENTRE"){c->
        repo.state.liveMatch?.takeIf{!it.completed}?.let{live->card(c,"LIVE MATCH IN PROGRESS","${live.current.battingTeam} ${live.current.runs}/${live.current.wickets} (${live.overs}) • ${live.phase}",::liveMatch);button(c,"OPEN LIVE MATCH",::liveMatch);return@scroll}
        card(c,"LEAGUE MATCHDAY", "Harbor Hawks vs Metro Kings\nRiverside Oval • Hard pitch • Clear weather\nTactic: ${repo.state.tactic}")
        text("SEASON FIXTURES",14f,accent);repo.state.fixtures.take(8).forEach{f->card(c,"Round ${f.round}: ${f.home} vs ${f.away}","${f.venue} • ${f.status}${if(f.result.isBlank())"" else "\n${f.result}"}")}
        button(c,"MATCH SETUP & TOSS",::matchSetup)
        button(c,"SIMULATE NEXT MATCH"){if(repo.state.selectedXI.size!=11){toast("Select exactly 11 players first");return@button}; val r=match.simulate(repo.state.selectedXI.mapNotNull{repo.player(it)},"Metro Kings",repo.state.tactic);repo.state.lastResult=r;repo.state.matchHistory.add(0,r);val us=repo.teams[0];val rival=repo.teams.first{it.name=="Metro Kings"};us.played++;rival.played++;if(r.won){us.won++;us.points+=2;rival.lost++}else {us.lost++;rival.won++;rival.points+=2};us.nrr+=((r.ourRuns-r.rivalRuns)/20.0);rival.nrr=-us.nrr;repo.state.news.add(0,"${if(r.won)"Victory" else "Defeat"}: Hawks ${r.ourRuns}/${r.ourWickets} vs Metro Kings ${r.rivalRuns}/${r.rivalWickets}.");repo.save();scorecard(r)}
        repo.state.lastResult?.let{card(c,"LATEST SCORE","Hawks ${it.ourRuns}/${it.ourWickets} • Metro Kings ${it.rivalRuns}/${it.rivalWickets}", {scorecard(it)})};repo.state.matchHistory.take(5).forEachIndexed{i,r->card(c,"MATCH HISTORY #${i+1}","vs ${r.opponent}: Hawks ${r.ourRuns}/${r.ourWickets} • ${if(r.won)"Won" else "Lost"}",{scorecard(r)})}
    }
    private fun matchSetup()=scroll("MATCH SETUP"){c->
        val error=PlayingXiValidator.validate(repo.squad,repo.state.selectedXI,repo.state.captainId,repo.state.viceCaptainId,repo.state.wicketKeeperId);card(c,"PLAYING XI",if(error==null)"11 selected • Captain ${repo.player(repo.state.captainId)?.name} • Vice captain ${repo.player(repo.state.viceCaptainId)?.name} • Wicketkeeper ${repo.player(repo.state.wicketKeeperId)?.name}" else "⚠ $error");button(c,"Open squad selection",::squad);card(c,"TOSS","Choose a decision after the coin toss. Toss outcome is saved before the first ball.");button(c,"TOSS & BAT"){startLiveFromToss("BAT")};button(c,"TOSS & FIELD"){startLiveFromToss("FIELD")}
    }
    private fun startLiveFromToss(decision:String){val error=PlayingXiValidator.validate(repo.squad,repo.state.selectedXI,repo.state.captainId,repo.state.viceCaptainId,repo.state.wicketKeeperId);if(error!=null){toast(error);return};val xi=repo.state.selectedXI.mapNotNull{repo.player(it)};val opponent=xi.map{it.copy(id=it.id+20_000,name="Metro ${it.name.substringAfter(' ')}")};val toss=TossEngine.toss("Harbor Hawks","Metro Kings",decision);val setup=MatchSetup("Harbor Hawks","Metro Kings",homeXI=xi,awayXI=opponent,homeCaptainId=repo.state.captainId,awayCaptainId=opponent.first().id,homeKeeperId=repo.state.wicketKeeperId,awayKeeperId=opponent.first().id,tossWinner=toss.first,tossDecision=toss.second);repo.state.pendingToss="${toss.first} won the toss and chose to ${toss.second.lowercase()}.";repo.state.liveMatch=LiveMatchEngine().start(setup);repo.save();liveMatch()}
    private fun liveMatch()=scroll("LIVE MATCH CENTRE"){c->
        val live=repo.state.liveMatch?:run{matches();return@scroll};val inn=live.current;val striker=inn.battingXI[inn.striker];val non=inn.battingXI[inn.nonStriker];val bowler=inn.bowlingXI.firstOrNull{it.id==inn.lastBowlerId}?:inn.bowlingXI.first()
        card(c,"T20 LEAGUE • ${live.setup.venue}","${inn.battingTeam}  ${inn.runs}/${inn.wickets} (${live.overs}) • ${live.phase}\nTarget ${inn.target?:"—"} • CRR ${(if(inn.legalBalls==0)0.0 else inn.runs*6.0/inn.legalBalls).format()} • RRR ${inn.target?.let{((it-inn.runs)*6.0/(120-inn.legalBalls).coerceAtLeast(1)).format()}?:"—"}\nPartnership ${inn.partnership}")
        card(c,"AT THE CREASE","${striker.name}  ${inn.batters[inn.striker].runs} (${inn.batters[inn.striker].balls}) • Confidence ${striker.confidence} • Fatigue ${striker.fatigue}\n${non.name}  ${inn.batters[inn.nonStriker].runs} (${inn.batters[inn.nonStriker].balls})\nBowler: ${bowler.name} • ${inn.bowlers[bowler.id]?.overs}-${inn.bowlers[bowler.id]?.runs}-${inn.bowlers[bowler.id]?.wickets}")
        card(c,"LAST 12 DELIVERIES",inn.deliveries.takeLast(12).joinToString("\n"){"${it.over}.${it.ball} ${it.bowler} to ${it.batter}: ${it.wicket?:if(it.extras>0)it.type else it.runs}"}.ifBlank{"Awaiting first ball"})
        button(c,"NEXT BALL"){stepLive(1)};button(c,"NEXT OVER"){LiveMatchEngine().nextOver(live);afterLiveStep()};button(c,"AUTO SIMULATE"){LiveMatchEngine().autoSimulate(live);afterLiveStep()};button(c,"PAUSE / SAVE"){repo.save();toast("Live match saved")}
        text("BATTING MENTALITY",14f,accent);listOf(BattingApproach.DEFENSIVE,BattingApproach.BALANCED,BattingApproach.AGGRESSIVE,BattingApproach.DEATH_ATTACK).forEach{a->button(c,a.name.replace('_',' ')){LiveMatchEngine().setBatting(live,a);liveMatch()}}
        text("FIELDING & BOWLING",14f,accent);listOf(BowlingApproach.DEFENSIVE,BowlingApproach.BALANCED,BowlingApproach.ATTACK,BowlingApproach.YORKER,BowlingApproach.SPIN_ATTACK).forEach{a->button(c,a.name.replace('_',' ')){LiveMatchEngine().setFielding(live,a);liveMatch()}}
        text("AVAILABLE BOWLERS",14f,accent);inn.bowlingXI.filter{(inn.bowlers[it.id]?.balls?:24)<24}.forEach{p->button(c,"${p.name}: ${inn.bowlers[p.id]?.overs} (${4-(inn.bowlers[p.id]?.balls?:24)/6} overs left)"){if(!LiveMatchEngine().selectBowler(live,p.id))toast("Choose a bowler who did not bowl the previous over") else toast("${p.name} selected")}}
    }
    private fun stepLive(count:Int){val live=repo.state.liveMatch?:return;repeat(count){if(!live.completed)LiveMatchEngine().nextBall(live)};afterLiveStep()}
    private fun afterLiveStep(){val live=repo.state.liveMatch?:return;if(live.completed){live.result?.let{r->val fixture=repo.state.fixtures.firstOrNull{it.home=="Harbor Hawks"&&it.away==r.opponent&&it.status=="SCHEDULED"};if(fixture!=null){SeasonEngine().complete(fixture,repo.teams.first{it.name==fixture.home},repo.teams.first{it.name==fixture.away},r.ourRuns,r.rivalRuns)};repo.state.lastResult=r;repo.state.matchHistory.add(0,r);repo.state.liveMatch=null;repo.save();scorecard(r)}}else{repo.save();liveMatch()}}
    private fun scorecard(r:MatchResult)=scroll("MATCH SCORECARD"){c->
        card(c,if(r.won)"HAWKS WIN!" else "METRO KINGS WIN", "Harbor Hawks ${r.ourRuns}/${r.ourWickets}  vs  Metro Kings ${r.rivalRuns}/${r.rivalWickets}\nPlayer of the Match: ${r.playerOfMatch}\nPitch: hard • Weather: clear")
        card(c,"LIVE PITCH", "┏━━━━━━━━━━━━━━━┓\n       ◉  BATTER\n     ●   ▮▮   ●\n       BOWLER\n┗━━━━━━━━━━━━━━━┛\nFinal run rate ${(r.ourRuns/20.0).format()}")
        val detail=r.record;card(c,"HAWKS BATTING",detail?.let{d->val innings=if(r.ourBatFirst)d.firstInnings else d.secondInnings;innings.batters.filter{it.balls>0||it.dismissal!="not out"}.joinToString("\n"){"${it.name}  ${it.runs} (${it.balls})  4s ${it.fours}  6s ${it.sixes}  ${it.dismissal}"}}?:"Detailed scorecard available for newly played matches.")
        card(c,"BOWLING FIGURES",detail?.let{d->val innings=if(r.ourBatFirst)d.secondInnings else d.firstInnings;innings.bowlers.filter{it.balls>0}.joinToString("\n"){"${it.name}  ${it.overs}-${it.runs}-${it.wickets}  Econ ${it.economy.format()}  Dots ${it.dots}"}+"\nExtras ${innings.extras}"}?:"Detailed bowling figures available for newly played matches.")
        card(c,"BALL-BY-BALL",r.scoreEvents.joinToString("\n"));button(c,"Back to match centre",::matches)
    }
    private fun tactics()=scroll("TACTICS"){c->
        card(c,"ACTIVE PLAN", "${repo.state.tactic}\nTactics influence scoring probability and match simulations.")
        listOf("Balanced","Aggressive","Defensive","Powerplay Attack","Spin Attack","Death Overs Attack","Yorker Bowling","Containment").forEach { t->button(c,t){repo.state.tactic=t;repo.save();toast("Tactics switched to $t");tactics()} }
    }
    private fun finances()=scroll("CLUB FINANCES"){c->
        val salaries=repo.squad.sumOf{it.salary};card(c,"AVAILABLE BUDGET","₹${repo.state.budget/1_000_000}M\nSponsor: ${repo.state.sponsor} • Season income ₹24M")
        card(c,"SEASON REPORT","Starting budget ₹120M\nPlayer salaries -₹${salaries/1_000_000}M\nAuction spending -₹${repo.state.auctionHistory.size*8}M\nMatch revenue +₹8M • Sponsorship +₹24M")
        button(c,"Upgrade training facility (₹5M)"){if(repo.state.budget>=5_000_000){repo.state.budget-=5_000_000;repo.state.trainingLevel++;repo.squad.forEach{it.form=(it.form+1).coerceAtMost(99)};toast("Training level ${repo.state.trainingLevel}")}else toast("Insufficient budget")};button(c,"Upgrade stadium seating (₹8M)"){if(repo.state.budget>=8_000_000){repo.state.budget-=8_000_000;repo.state.stadiumLevel++;toast("Stadium level ${repo.state.stadiumLevel}")}else toast("Insufficient budget")}
    }
    private fun standings()=scroll("LEAGUE TABLE"){c->
        repo.teams.sortedWith(compareByDescending<Team>{it.points}.thenByDescending{it.nrr}).forEachIndexed{i,t->card(c,"${i+1}. ${t.name}","Played ${t.played}  Won ${t.won}  Lost ${t.lost}  NRR ${t.nrr.format()}   PTS ${t.points}")};card(c,"SEASON FORMAT","Double round robin league • Top four reach playoffs • Final decides the champion.")
    }
    private fun more()=scroll("CLUB OPERATIONS"){c->
        card(c,"SEASON ${repo.state.season}","Sponsor ${repo.state.sponsor} • Training ${repo.state.trainingLevel} • Stadium ${repo.state.stadiumLevel}")
        button(c,"Advance to new season"){repo.state.season++;repo.teams.forEach{it.points=0;it.played=0;it.won=0;it.lost=0};repo.squad.forEach{it.contract=(it.contract-1).coerceAtLeast(0);it.fitness=95;it.fatigue=5};repo.state.news.add(0,"Season ${repo.state.season} has begun. Contracts and form have been updated.");toast("New season started");home()};button(c,"Save game now"){repo.save();toast("Game saved locally")};button(c,"Reset saved game"){repo.reset();toast("Saved data cleared. Restart app to begin again.")}
    }
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_SHORT).show()
    private fun Double.format()="%.2f".format(this)
}
