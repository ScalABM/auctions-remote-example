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
package org.economicsl.remote

import java.util.UUID
import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import org.economicsl.auctions.AuctionProtocol
import org.economicsl.auctions.actors.LoggingSettlementActor
import org.economicsl.auctions.singleunit.OpenBidAuction
import org.economicsl.auctions.singleunit.participants.SingleUnitAuctionParticipant
import org.economicsl.auctions.singleunit.pricing.MidPointPricingPolicy
import org.economicsl.core.{Price, Tradable}
import org.economicsl.core.securities.Stock
import org.economicsl.remote.actors.{ContinuousDoubleAuctionActor, RemoteAuctionParticipantActor}

import scala.util.Random


object ExampleRemoteApplication extends App {

  /** Key piece of information that both the AuctionActor and AuctionParticipantActors must know! */
  val settlementServicePath = "akka://SettlementSystem@127.0.0.1:2554/user/settlement"

  if (args.isEmpty || args.head == "Settlement")
    startRemoteSettlementSystem()
  if (args.isEmpty || args.head == "Auction")
    startRemoteAuctionSystem()
  if (args.isEmpty || args.head == "Trading")
    startRemoteTradingSystem()

  case class AppleStock() extends Stock {
    val ticker: String = "APPL"
  }

  /** Starts the remote trading system. */
  def startRemoteTradingSystem(): Unit = {
    val tradingSystem = ActorSystem("TradingSystem", ConfigFactory.load("trading"))
    val auctionServicePath = "akka://AuctionSystem@127.0.0.1:2553/user/auction"
    val initialPrices: Map[Tradable, Price] = Map.empty.withDefaultValue(Price(100))  // todo this information should be passed from the auction to the participant upon registration!
    for (i <- 1 to 10) yield {
      val issuer = UUID.randomUUID()
      val prng = new Random()
      val randomInitialValuation = Price(prng.nextInt(200))
      val valuations: Map[Tradable, Price] = Map.empty.withDefaultValue(randomInitialValuation)  // todo need better mechanism for initializing valuations!
      val participant: SingleUnitAuctionParticipant = TestSingleUnitAuctionParticipant(issuer, initialPrices, valuations)
      val lambda = 0.5
      val timeUnit = TimeUnit.SECONDS
      val participantProps = RemoteAuctionParticipantActor.props(participant, lambda, prng, timeUnit, auctionServicePath, settlementServicePath)
      tradingSystem.actorOf(participantProps, issuer.toString)
    }
    println("Started TradingSystem!")
  }

  /** Starts the remote auction system. */
  def startRemoteAuctionSystem(): Unit = {
    val auctionSystem = ActorSystem("AuctionSystem", ConfigFactory.load("auction"))
    val pricingPolicy = new MidPointPricingPolicy[AppleStock]()
    val tradable: AppleStock = AppleStock()
    val protocol = AuctionProtocol(tradable)
    val auction = OpenBidAuction.withDiscriminatoryClearingPolicy(pricingPolicy, protocol)
    val auctionProps = ContinuousDoubleAuctionActor.props[AppleStock](auction, settlementServicePath)
    auctionSystem.actorOf(auctionProps, "auction")
    println("Started AuctionSystem - waiting for orders!")
  }

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
