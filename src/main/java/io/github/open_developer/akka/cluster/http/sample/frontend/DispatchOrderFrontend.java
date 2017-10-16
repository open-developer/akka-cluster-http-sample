package io.github.open_developer.akka.cluster.http.sample.frontend;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Terminated;
import akka.cluster.ClusterEvent;
import akka.cluster.Member;
import akka.cluster.MemberStatus;
import akka.http.javadsl.model.StatusCodes;
import io.github.open_developer.akka.cluster.http.sample.job.JobMessage;

import java.util.ArrayList;
import java.util.List;

import static akka.http.javadsl.server.Directives.complete;
import static io.github.open_developer.akka.cluster.http.sample.job.JobMessage.BACKEND_REGISTRATION;

public class DispatchOrderFrontend extends AbstractActor {

    List<ActorRef> backends = new ArrayList<>();
    int jobCounter=0;

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(JobMessage.Job.class, job -> backends.isEmpty(), job -> {
                    sender().tell(complete(StatusCodes.SERVICE_UNAVAILABLE, "Service unavailable, try again later") ,
                            sender());
                })
                .match(JobMessage.Job.class, job -> {
                    jobCounter++;
                    backends.get(jobCounter % backends.size())
                            .forward(job, getContext());
                    System.out.println(job.getJob().toString());
                })
                .matchEquals(BACKEND_REGISTRATION, message -> {
                    getContext().watch(sender());
                    backends.add(sender());
                })
                .match(Terminated.class, terminated -> {
                    backends.remove(terminated.getActor());
                })
                .build();
    }
}
