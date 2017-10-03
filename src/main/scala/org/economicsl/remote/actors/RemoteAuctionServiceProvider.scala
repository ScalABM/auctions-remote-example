package org.economicsl.remote.actors

import akka.actor.{ActorIdentity, ActorRef, Identify, Terminated}
import org.economicsl.auctions.actors.AuctionActor.RegisterAuctionParticipant
import org.economicsl.auctions.actors.StackableActor


trait RemoteAuctionServiceProvider
  extends StackableActor {

  def auctionServicePath: String

  override def receive: Receive = {
    case message @ ActorIdentity("auctionService", maybeActorRef) =>
      maybeActorRef match {
        case Some(actorRef) =>
          context.watch(actorRef)
          auctionService = Some(actorRef)
          auctionService.foreach(auctionRef => auctionRef ! RegisterAuctionParticipant(self))
        case None =>
          ???  // todo what should happen in this case? Log as a warning? Then retry? Could be that auction actor has not yet started?
      }
      super.receive(message)
    case Terminated(actorRef) if auctionService.contains(actorRef) =>
      context.unwatch(actorRef)
      identifyAuctionService(auctionServicePath)
    // todo probably also want to log this as a warning!
    case message =>
      super.receive(message)
  }

  protected var auctionService: Option[ActorRef] = None

  protected def identifyAuctionService(path: String): Unit = {
    val actorSelection = context.actorSelection(path)
    actorSelection ! Identify("auctionService")
  }

}
