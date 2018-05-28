# Remote auctions application

Example application demonstrating key ideas for "Economy Of Things" (EoT) use cases. This example demonstrates both 
the remote deployment of auction and settlement services that can be discovered by trading actors. The reote application 
consists of three main components.

* `TradingSystem` listens on port 2552 and starts potentially several `AuctionParticipantActor` instances that generate 
`SingleUnitAskOrder` and `SingleUnitBidOrder` instances and send them to the auction service for processing. In 
practice, I would anticipate some IoT-enabled device to have a JVM process with a `TradingSystem` with a single 
`AuctionParticipantActor` instance.  TODO: use `dccker` and `docker-compose` to demonstrate how the system would work 
with `N` separate `TradingSystem` instances each running inside its own JVM process in a separate container.
* `AuctionSystem` listens on port 2553 and starts an `AuctionActor` instance that provides services for matching 
individual buyers and sellers at specific prices and quantities. The auction service generates streams of `SpotContract` 
instances which are sent to the settlement service for further processing.
* `SettlementSystem` listens on port 2554 and logs the received streams of `SpotContract` instances. At some point the 
`SettlementSystem` might interact with a `Blockchain` as part of the settlement service.

Each of the actor systems has its own configuration file located in the `resources` directory. The `TradingSystem` uses 
the `trading.conf`; `AuctionSystem` uses the `auction.conf`; and `SettlementSystem` uses the `settlement.conf`. All 
three configuration files share basic settings via `common.conf`. The `common.conf` enables remoting by installing the
`RemoteActorRefProvider` and chooses the default remote transport. Note that when deploying the services on multiple 
machines you will need to change the default IP address with the real IP addresses.

The `SettlementActor` does not really illustrate anything exciting: it simple logs out each of the `Fill` instances it 
receives. The `RemoteAuctionServiceActor` takes a `String` path as constructor parameter. This is the full path, 
including the remote address of the settlement service. 

"akka.tcp://SettlementSystem@127.0.0.1:2554/user/settlement"

Observe how the actor system name of the path matches the remote settlement system’s name, as do IP address and port 
number. As always, top-level actors are always created below the "/user" guardian, which supervises them.

Upon creation the `RemoteAuctionServiceActor` instance sends an `Identify` message to the actor selection of the path. 
The remote `LoggingSettlementActor` actor will reply with an `ActorIdentity` message containing its `ActorRef`. Note 
that `Identify` is a built-in message that all Akka `Actor` instances understand, and automatically reply to, with a 
`ActorIdentity`. If the `RemoteAuctionServiceActor` is unable to identify the remote settlement service, it will 
retry after some scheduled timeout duration.

Once the `RemoteAuctionServiceActor` has the `ActorRef` of the remote settlement service it can monitor it. The 
remote settlement system might be shutdown and later started up again, in which case the `RemoteAuctionServiceActor` 
would receive the `Terminated` message which prompts it to retry the identification process in orde to establish a 
connection to the new remote settlement system.

### Running the remote applications
Let's run each of the actor systems remotely in separate JVM processes. Start the settlement system by opening up a
new terminal window and running the following.

```bash
sbt "run-main RemoteSettlementServiceApp"
```

This should generate some generic logging cruft followed by a message indicating that the settlement service has been 
activated.  The settlement service is the core service on top of which everything else depends.  If the service is down 
or otherwise unreachable then the auction service can not operate and the participants cannot settle transactions.

Once the settlement system has started, start the auction system by opening up a new terminal window and running the following.

```bash
sbt "run-main RemoteAuctionServiceApp"
```
Again, this should generate some logging cruft followed by a message indicating that the auction service is operational. 
Finally, start the trading system by opening up a new terminal window and running thw following.

```bash
sbt "run-main org.economicsl.auctions.remote.RemoteAuctionExampleApp Trading"
```

Once the trading system is operational, you should be able to see `SpotContract` instances being logged to the terminal window 
handling the remote settlement service.