package org.economicsl.remote.actors

import akka.actor.{ActorIdentity, ActorRef, Identify, Terminated}
import org.economicsl.auctions.actors.StackableActor


/** Mixin trait providing access to a remote settlement service actor. */
trait RemoteSettlementServiceProvider
    extends StackableActor {

  def settlementServicePath: String

  @scala.throws[Exception](classOf[Exception])
  override def preStart(): Unit = {
    super.preStart()
    identifySettlementService(settlementServicePath)
  }

  override def receive: Receive = {
    case ActorIdentity("settlementService", maybeActorRef) =>
      maybeActorRef match {
        case Some(actorRef) =>
          context.watch(actorRef)
          settlementService = Some(actorRef)
        case None =>
          ???  // todo what should happen in this case? Log as a warning? Then retry? Could be that settlement actor has not yet started?
      }
    case Terminated(actorRef) if settlementService.contains(actorRef) =>
      context.unwatch(actorRef)
      identifySettlementService(settlementServicePath)
      // todo probably also want to log this as a warning!
    case message =>
      super.receive(message)
  }

  /* By default we initialize this value to `None`... */
  protected var settlementService: Option[ActorRef] = None

  protected def identifySettlementService(path: String): Unit = {
    val actorSelection = context.actorSelection(path)
    actorSelection ! Identify("settlementService")
  }

}
