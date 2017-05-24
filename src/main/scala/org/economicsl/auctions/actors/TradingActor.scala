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

import java.util.UUID

import akka.actor.{Actor, ActorIdentity, ActorLogging, ActorRef, Identify, ReceiveTimeout, Terminated}
import org.economicsl.auctions.quotes._
import org.economicsl.auctions.singleunit.orders.{LimitAskOrder, LimitBidOrder}
import org.economicsl.auctions.{Price, Tradable}

import scala.concurrent.duration._
import scala.util.Random


/** This actor will be replaced by a TradingActor which submits orders to the AuctionActor. */
class TradingActor[T <: Tradable](uuid: UUID, path: String, tradable: T) extends Actor with ActorLogging {

  requestAuctionService()

  def requestAuctionService(): Unit = {
    context.actorSelection(path) ! Identify()
  }

  def identifying: Receive = {
    case ActorIdentity(_, Some(auctionService)) =>
      context.watch(auctionService)
      context.become(active(auctionService))

      // send orders every once an a while...
      context.system.scheduler.schedule(1.second, 1.second) {
        val limit = Price(Random.nextInt(10000))
        if (Random.nextFloat() < 0.5) {
          self ! SendAskOrder(limit)
        } else {
          self ! SendBidOrder(limit)
        }

      } (context.system.dispatcher)

      // request price quotes every once and while...
      context.system.scheduler.schedule(2.second, 1.second) {
        val threshold = Random.nextFloat()
        if (threshold < 0.33) {
          self ! RequestAskPriceQuote
        } else if (threshold < 0.66) {
          self ! RequestBidPriceQuote
        } else {
          self ! RequestSpreadQuote
        }

      } (context.system.dispatcher)

    case ActorIdentity(_, None) => println(s"Remote auction not available at $path")
    case ReceiveTimeout => requestAuctionService()
  }

  def active(auctionService: ActorRef): Receive = {
    // trading actor responds to "instructions"
    case SendAskOrder(limit) =>
      auctionService ! LimitAskOrder(uuid, limit, tradable)
    case SendBidOrder(limit) =>
      auctionService ! LimitBidOrder(uuid, limit, tradable)
    case RequestAskPriceQuote =>
      auctionService ! AskPriceQuoteRequest()
    case RequestBidPriceQuote =>
      auctionService ! BidPriceQuoteRequest()
    case RequestSpreadQuote =>
      auctionService ! SpreadQuoteRequest()

    // TODO: how to respond differently to the differnt types of quotes?
    case Some(AskPriceQuote(quote)) => log.info(quote.toString)
    case quote: Option[BidPriceQuote] => log.info(quote.toString)
    case quote: Option[SpreadQuote] => log.info(quote.toString)

    case Terminated(`auctionService`) =>
      println("Auction terminated!")
      requestAuctionService()
      context.become(identifying)
  }

  def receive: Receive = identifying

  case class SendAskOrder(limit: Price)

  case class SendBidOrder(limit: Price)

  case object RequestAskPriceQuote

  case object RequestBidPriceQuote

  case object RequestSpreadQuote

}
