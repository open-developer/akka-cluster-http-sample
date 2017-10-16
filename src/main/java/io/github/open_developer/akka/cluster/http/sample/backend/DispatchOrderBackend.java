package io.github.open_developer.akka.cluster.http.sample.backend;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import io.github.open_developer.akka.cluster.http.sample.distributeddata.ReplicatedCache;
import io.github.open_developer.akka.cluster.http.sample.frontend.DispatchOrderFrontend;
import io.github.open_developer.akka.cluster.http.sample.job.JobMessage;
import io.github.open_developer.akka.cluster.http.sample.model.Order;

import static io.github.open_developer.akka.cluster.http.sample.job.JobMessage.BACKEND_REGISTRATION;

public class DispatchOrderBackend extends AbstractActor {

    Cluster cluster = Cluster.get(getContext().system());
    LoggingAdapter log = Logging.getLogger(getContext().getSystem(), self());
    ActorRef replicatedCache = getContext().getSystem().actorOf(
            Props.create(ReplicatedCache.class), "cache");

    //subscribe to cluster changes, MemberUp
    @Override
    public void preStart() {
        cluster.subscribe(self(), ClusterEvent.MemberUp.class);
    }

    //re-subscribe when restart
    @Override
    public void postStop() {
        cluster.unsubscribe(self());
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(JobMessage.Job.class, job -> {
                    Order order = (Order) job.getJob();
                    replicatedCache.tell(new ReplicatedCache.PutInCache(String.valueOf(order.getOrderId()), order), self());
                })
                .match(ClusterEvent.CurrentClusterState.class, state -> {
                    for (Member member : state.getMembers()) {
                        if (member.status().equals(MemberStatus.up())) {
                            register(member);
                        }
                    }
                })
                .match(ClusterEvent.MemberUp.class, mUp -> {
                    register(mUp.member());
                })
                .build();
    }

    void register(Member member) {
        if (member.hasRole("frontend"))
            getContext().actorSelection(member.address() + "/user/frontend").tell(
                    BACKEND_REGISTRATION, self());
    }
}
