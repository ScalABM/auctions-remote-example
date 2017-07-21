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

import akka.actor.{Actor, ActorIdentity, ActorRef, Identify, Terminated}

import scala.concurrent.duration.FiniteDuration


/** This should be a mixin trait that layers remoting behavior on top of AuctionParticipant behavior from
  * esl-auctions.
  *
  * @author davidrpugh
  * @since 0.2.0
  */
trait AuctionRegistryParticipant
    extends Actor {

  import AuctionRegistry._

  /** Path to the `AuctionRegistry` (could be local or remote). */
  def auctionRegistryPath: String

  /** Duration that this `Actor` should wait for `AuctionRegistry` to be available before shutting down. */
  def auctionRegistryTimeout: FiniteDuration

  /** Attempt to identify the location of the `AuctionRegistry` (which could be local or remote!).
    *
    * @return
    */
  override def receive: Receive = {
    case message @ ActorIdentity("auctionRegistry", Some(actorRef)) =>
      context.watch(actorRef)
      auctionRegistry = Some(actorRef)
    case Terminated(actorRef) if auctionRegistry.contains(actorRef) =>
      context.unwatch(actorRef)
      auctionRegistry = None
    case message =>
      super.receive(message)
  }

  /** Upon receipt of a `RegisterAuction` message from the `AuctionRegistry` containing the `auctionRefs` for each of
    * the currently available auctions, the `AuctionParticipant` will send a `RegisterAuctionParticipant` message to
    * each of these `auctionRefs` in order to obtain the respective `AuctionProtocol` instances. These `AuctionProtocol`
    * instances should contain all relevant information about the structure and rules of a particular auction necessary
    * for the `AuctionParticipant` to generate and submit valid orders.
    */
  def registeringAuctions: Receive = {
    case RegisterAuction(auctionRefs) =>
      auctionRefs.foreach{ auctionRef =>
        context.watch(auctionRef)
        auctionRef ! RegisterAuctionParticipant(self)
      }
    case Terminated(actorRef) if auctions.contains(actorRef) =>
      context.unwatch(actorRef)
      auctions = auctions - actorRef
    case protocol : AuctionProtocol =>
      auctions = auctions + (sender() -> protocol)
  }

  /* This will eventually come from the AuctionParticipant. */
  protected var auctions: Map[AuctionRef, AuctionProtocol] = Map.empty

  /* Reference for auction registry (initialized to `None`) */
  protected var auctionRegistry: Option[ActorRef] = None

  protected def identify(path: String, messageId: Any): Unit = {
    context.actorSelection(path) ! Identify(messageId)
  }

}