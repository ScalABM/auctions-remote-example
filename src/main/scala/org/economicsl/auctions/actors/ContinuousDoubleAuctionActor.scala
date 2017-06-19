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
package org.economicsl.auctions.actors

import akka.actor.{Actor, ActorIdentity, ActorRef, Identify, ReceiveTimeout, Terminated}
import org.economicsl.auctions.quotes.{AskPriceQuoteRequest, BidPriceQuoteRequest, SpreadQuoteRequest}
import org.economicsl.auctions.singleunit.pricing.{MidPointPricingPolicy, PricingPolicy}
import org.economicsl.auctions.singleunit.orders.{AskOrder, BidOrder}
import org.economicsl.auctions.singleunit.twosided.OpenBidDoubleAuction
import org.economicsl.core.Tradable

import scala.concurrent.duration._
import scala.util.{Failure, Success}


class ContinuousDoubleAuctionActor[T <: Tradable](pricingPolicy: PricingPolicy[T], tickSize: Long, path: String)
    extends Actor {

  requestSettlementService()

  def requestSettlementService(): Unit = {
    val settlementService = context.actorSelection(path)
    settlementService ! Identify()
    context.system.scheduler.scheduleOnce(3.seconds, self, ReceiveTimeout)(context.system.dispatcher)
  }

  def identifying: Receive = {
    case order: AskOrder[T] =>
      auction.insert(order) match {
        case Success(updated) =>
          auction = updated
        case Failure(ex) => ???
      }
    case order: BidOrder[T] =>
      auction.insert(order) match {
        case Success(updated) =>
          auction = updated
        case Failure(ex) => ???
      }
    case ActorIdentity(_, Some(settlementService)) =>
      context.watch(settlementService)
      context.become(active(settlementService))
    case ActorIdentity(_, None) =>
      println(s"Settlement service not available at $path")
    case ReceiveTimeout => requestSettlementService()
  }

  def active(settlementService: ActorRef): Receive = {
    case order: AskOrder[T] =>
      auction.insert(order) match {  // clearing on receipt of order!
        case Success(updated) =>
          val result = updated.clear
          result.fills.foreach{ fills => settlementService ! fills }
          auction = result.residual
        case Failure(ex) => ???
      }
    case order: BidOrder[T] =>
      auction.insert(order) match {  // clearing on receipt of order!
        case Success(updated) =>
          val result = updated.clear
          result.fills.foreach{ fills => settlementService ! fills }
          auction = result.residual
        case Failure(ex) => ???
      }
    case request: AskPriceQuoteRequest[T] =>
      sender() ! auction.receive(request)
    case request: BidPriceQuoteRequest[T] =>
      sender() ! auction.receive(request)
    case request: SpreadQuoteRequest[T] =>
      sender() ! auction.receive(request)
    case Terminated(`settlementService`) =>  // auction service will attempt to re-connect to the settlement service!
      println(s"Settlement Service at ${settlementService.path} terminated!")
      context.become(identifying)
      requestSettlementService()
  }

  def receive: Receive = identifying

  /* Double auction using this pricing rule is not incentive compatible for either buyer or seller! */
  private[this] var auction = OpenBidDoubleAuction.withDiscriminatoryPricing(new MidPointPricingPolicy[T], tickSize)

}

