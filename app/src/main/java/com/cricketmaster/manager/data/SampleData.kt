package com.cricketmaster.manager.data

import com.cricketmaster.manager.model.*

object SampleData {
    val teams = mutableListOf(
        Team("Harbor Hawks",120_000_000,"Balanced"), Team("Metro Kings",145_000_000,"Aggressive"), Team("Desert Blazers",105_000_000,"Youth"),
        Team("Capital Chargers",130_000_000,"Stars"), Team("Coastal Cyclones",95_000_000,"Value"), Team("Highland Strikers",115_000_000,"Bowling"))
    fun squad() = mutableListOf(
        p(1,"Arjun Mehta",28,"India","Batter",86,89,88,72,35,16,21), p(2,"Liam Carter",31,"Australia","Batter",84,84,84,76,30,12,20),
        p(3,"Ravi Iyer",25,"India","Wicketkeeper",82,90,81,69,20,8,22), p(4,"Noah Bennett",27,"England","All-rounder",83,87,76,78,68,70,19),
        p(5,"Kabir Shah",24,"Pakistan","All-rounder",80,91,74,73,72,75,20), p(6,"Diego Silva",30,"South Africa","Batter",79,79,80,75,22,10,18),
        p(7,"Ethan Cole",29,"New Zealand","Fast bowler",82,83,45,61,88,84,18), p(8,"Ishaan Rao",26,"India","Fast bowler",81,88,42,58,86,82,17),
        p(9,"Zain Malik",23,"Pakistan","Spinner",78,91,48,49,38,87,21), p(10,"Oliver Grant",32,"England","Fast bowler",77,77,35,50,82,79,15),
        p(11,"Sanjay Patel",22,"India","Spinner",76,92,51,52,34,85,22), p(12,"Marcus Lee",26,"West Indies","Batter",75,84,79,77,26,14,20),
        p(13,"Aiden Brooks",21,"Australia","All-rounder",73,94,68,72,67,64,23), p(14,"Farid Khan",28,"Afghanistan","Spinner",74,79,43,54,28,89,18),
        p(15,"Jon Bell",25,"England","Wicketkeeper",71,82,72,70,15,8,25))
    fun auctionPool() = mutableListOf(
        p(101,"Vikram Sethi",26,"India","Batter",85,88,87,74,28,10,19), p(102,"Theo James",24,"England","Fast bowler",81,91,42,58,87,84,20),
        p(103,"Milan Das",20,"Bangladesh","All-rounder",74,95,67,70,65,69,18), p(104,"Carlos Mendez",30,"South Africa","Batter",79,80,80,80,25,9,21))
    private fun p(id:Int,n:String,a:Int,nat:String,r:String,o:Int,p:Int,b:Int,pow:Int,pa:Int,ac:Int,sp:Int): Player =
        Player(id,n,a,nat,r,o,p,o*1_500_000,o*85_000,3,bat=b,power=pow,pace=pa,accuracy=ac,spin=sp,fielding=70+(id%18),mental=72+(id%15))
}
