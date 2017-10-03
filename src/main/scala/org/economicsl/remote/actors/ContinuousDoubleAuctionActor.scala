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
package org.economicsl.remote.actors

import akka.actor.{ActorIdentity, Props}
import org.economicsl.auctions.actors.{AuctionActor, BidderActivityClearingSchedule, BidderActivityQuotingSchedule}
import org.economicsl.auctions.singleunit.OpenBidAuction
import org.economicsl.core.Tradable


/**
  *
  * @param auction
  * @param settlementServicePath
  * @tparam T
  * @author davidrpugh
  * @since 0.1.0
  */
class ContinuousDoubleAuctionActor[T <: Tradable](
  protected var auction: OpenBidAuction[T],
  val settlementServicePath: String)
    extends AuctionActor[T, OpenBidAuction[T]]
    with BidderActivityClearingSchedule[T, OpenBidAuction[T]]
    with BidderActivityQuotingSchedule[T]
    with RemoteSettlementServiceProvider {

  override def receive: Receive = {
    case message =>
      log.info(message.toString)
      super.receive(message)
  }

}


object ContinuousDoubleAuctionActor {

  def props[T <: Tradable](auction: OpenBidAuction[T], settlementServicePath: String): Props = {
    Props(new ContinuousDoubleAuctionActor[T](auction, settlementServicePath))
  }

}

