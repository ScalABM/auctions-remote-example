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

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import org.economicsl.auctions.AuctionProtocol
import org.economicsl.auctions.singleunit.OpenBidAuction
import org.economicsl.auctions.singleunit.pricing.MidPointQuotePricingPolicy
import org.economicsl.core.securities.Stock


object RemoteAuctionServiceApp extends App {

  /** Key piece of information that both the AuctionActor and AuctionParticipantActors must know! */
  val settlementServicePath = "akka://SettlementSystem@127.0.0.1:2554/user/settlement"

  startRemoteAuctionSystem()

  case class AppleStock() extends Stock {
    val ticker: String = "APPL"
  }

  /** Starts the remote auction system. */
  def startRemoteAuctionSystem(): Unit = {
    val auctionSystem = ActorSystem("AuctionSystem", ConfigFactory.load("auction"))
    val pricingPolicy = MidPointQuotePricingPolicy[AppleStock]()
    val tradable: AppleStock = AppleStock()
    val protocol = AuctionProtocol(tradable)
    val auction = OpenBidAuction.withDiscriminatoryClearingPolicy(pricingPolicy, protocol)
    val auctionProps = RemoteAuctionServiceActor.props[AppleStock](auction, settlementServicePath)
    auctionSystem.actorOf(auctionProps, "auction")
    println("Started AuctionSystem - waiting for orders!")
  }

}
