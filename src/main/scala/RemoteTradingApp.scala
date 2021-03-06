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
import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.economicsl.auctions.singleunit.participants.SingleUnitAuctionParticipant
import org.economicsl.core.{Price, Tradable}

import scala.util.Random


object RemoteTradingApp extends App {

  /** Key piece of information that both the AuctionActor and AuctionParticipantActors must know! */
  val settlementServicePath = "akka://SettlementSystem@127.0.0.1:2554/user/settlement"

  startRemoteTradingSystem()

  /** Starts the remote trading system. */
  def startRemoteTradingSystem(): Unit = {
    val tradingSystem = ActorSystem("TradingSystem", ConfigFactory.load("trading"))
    val auctionServicePath = "akka://AuctionSystem@127.0.0.1:2553/user/auction"
    for (i <- 1 to 10) yield {
      val issuer = UUID.randomUUID()
      val prng = new Random()
      val randomInitialValuation = Price(prng.nextInt(200))
      val valuations: Map[Tradable, Price] = Map.empty.withDefaultValue(randomInitialValuation)  // todo need better mechanism for initializing valuations!
      val participant: SingleUnitAuctionParticipant = TestSingleUnitAuctionParticipant(issuer, valuations)
      val lambda = 0.5
      val timeUnit = TimeUnit.SECONDS
      val participantProps = RemoteAuctionParticipantActor.props(participant, lambda, prng, timeUnit, auctionServicePath, settlementServicePath)
      tradingSystem.actorOf(participantProps, issuer.toString)
    }
    println("Started TradingSystem!")
  }

}
