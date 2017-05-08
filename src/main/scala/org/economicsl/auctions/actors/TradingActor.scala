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

import akka.actor.{Actor, ActorIdentity, ActorRef, Identify, ReceiveTimeout, Terminated}
import org.economicsl.auctions.singleunit.{LimitAskOrder, LimitBidOrder}
import org.economicsl.auctions.{Price, Tradable}

import scala.concurrent.duration._
import scala.util.Random


/** This actor will be replaced by a TradingActor which submits orders to the AuctionActor. */
class TradingActor(uuid: UUID, path: String) extends Actor {

  requestAuctionService()

  def requestAuctionService(): Unit = {
    context.actorSelection(path) ! Identify()
  }

  def identifying: Receive = {
    case ActorIdentity(_, Some(auctionService)) =>
      context.watch(auctionService)
      context.become(active(auctionService))
      context.system.scheduler.schedule(1.second, 1.second) {
        val limit = Price(Random.nextInt(10000))
        val tradable = new Tradable {
          def tick = 1
        }
        if (Random.nextFloat() < 0.5) {
          self ! SendAskOrder(limit, tradable)
        } else {
          self ! SendBidOrder(limit, tradable)
        }

      } (context.system.dispatcher)
    case ActorIdentity(_, None) => println(s"Remote auction not available at $path")
    case ReceiveTimeout => requestAuctionService()
  }

  def active(auctionService: ActorRef): Receive = {
    case SendAskOrder(limit, tradable) =>
      auctionService ! LimitAskOrder(uuid, limit, tradable)
    case SendBidOrder(limit, tradable) =>
      auctionService ! LimitBidOrder(uuid, limit, tradable)
    case Terminated(`auctionService`) =>
      println("Auction terminated!")
      requestAuctionService()
      context.become(identifying)
  }

  def receive: Receive = identifying

  case class SendAskOrder[T <: Tradable](limit: Price, tradable: T)

  case class SendBidOrder[T <: Tradable](limit: Price, tradable: T)

}
