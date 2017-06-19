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
package org.economicsl.auctions.remote

import java.util.UUID

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import org.economicsl.auctions.actors.{ContinuousDoubleAuctionActor, SettlementActor, TradingActor}
import org.economicsl.auctions.singleunit.pricing.MidPointPricingPolicy
import org.economicsl.core.securities.Stock


object LookupApplication {

  def main(args: Array[String]): Unit = {
    if (args.isEmpty || args.head == "Trading")
      startRemoteTradingSystem()
    if (args.isEmpty || args.head == "Auction")
      startRemoteAuctionSystem()
    if (args.isEmpty || args.head == "Settlement")
      startRemoteSettlementSystem()
  }

  case class AppleStock() extends Stock {
    val ticker: String = "APPL"
  }

  val tradable: AppleStock = AppleStock()

  /** Starts the remote trading system. */
  def startRemoteTradingSystem(): Unit = {
    val tradingSystem = ActorSystem("TradingSystem", ConfigFactory.load("trading"))
    val auctionService = "akka://AuctionSystem@127.0.0.1:2553/user/auction"
    for (i <- 1 to 10) yield {
      val issuer = UUID.randomUUID()
      tradingSystem.actorOf(Props(classOf[TradingActor[AppleStock]], issuer, auctionService, tradable), issuer.toString)
    }
    println("Started TradingSystem!")
  }

  /** Starts the remote auction system. */
  def startRemoteAuctionSystem(): Unit = {
    val auctionSystem = ActorSystem("AuctionSystem", ConfigFactory.load("auction"))
    val pricingPolicy = new MidPointPricingPolicy[AppleStock]()
    val tickSize = 1L
    val settlementService = "akka://SettlementSystem@127.0.0.1:2554/user/settlement"
    val auctionProps = Props(classOf[ContinuousDoubleAuctionActor[AppleStock]], pricingPolicy, tickSize, settlementService)
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
    settlementSystem.actorOf(Props[SettlementActor], "settlement")
    println("Started SettlementSystem - waiting for fills!")
  }

}
