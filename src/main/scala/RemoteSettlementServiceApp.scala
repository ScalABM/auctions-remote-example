/*
Copyright (c) 2017 KAPSARC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import org.economicsl.auctions.actors.LoggingSettlementActor


object RemoteSettlementServiceApp extends App {

  /** Key piece of information that both the AuctionActor and AuctionParticipantActors must know! */
  val settlementServicePath = "akka://SettlementSystem@127.0.0.1:2554/user/settlement"

  startRemoteSettlementSystem()

  /** Starts the remote settlement system.
    *
    * @note Remote settlement system could reside on same JVM as the auction system; however, generally many auction
    *       systems might share a common settlement system. In these case it probably makes more sense for a settlement
    *       system to be on a separate JVM.
    */
  def startRemoteSettlementSystem(): Unit = {
    val settlementSystem = ActorSystem("SettlementSystem", ConfigFactory.load("settlement"))
    settlementSystem.actorOf(Props[LoggingSettlementActor], "settlement")
    println("Started SettlementSystem - waiting for contracts!")
  }

}
