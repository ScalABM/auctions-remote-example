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

import akka.actor.{ActorIdentity, ActorRef, Props}
import org.economicsl.auctions.{AuctionParticipant, AuctionProtocol}
import org.economicsl.auctions.actors.{AuctionParticipantActor, PoissonOrderIssuingSchedule}
import org.economicsl.core.{Price, Tradable}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Random


class RemoteAuctionParticipantActor[P <: AuctionParticipant[P]](
  protected var participant: P,
  val lambda: Double,
  val prng: Random,
  val timeUnit: TimeUnit,
  val auctionServicePath: String,
  val settlementServicePath: String)
    extends AuctionParticipantActor[P]
    with PoissonOrderIssuingSchedule[P]
    with RemoteAuctionServiceProvider
    with RemoteSettlementServiceProvider {

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    super.preStart()
    identifyAuctionService(auctionServicePath)
  }

  // use the actor's default dispatcher as the execution context
  val executionContext: ExecutionContext = context.dispatcher


  override def receive: Receive = {
    case message =>
      log.info(message.toString)
      super.receive(message)
  }

  protected var auctions: Map[AuctionProtocol[Tradable], ActorRef] = Map.empty[AuctionProtocol[Tradable], ActorRef]

}


object RemoteAuctionParticipantActor {

  def props[P <: AuctionParticipant[P]](participant: P, lambda: Double, prng: Random, timeUnit: TimeUnit, auctionServicePath: String, settlementServicePath: String): Props = {
    Props(new RemoteAuctionParticipantActor[P](participant, lambda, prng, timeUnit, auctionServicePath, settlementServicePath))
  }

}