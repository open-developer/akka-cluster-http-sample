package io.github.open_developer.akka.cluster.http.sample.frontend;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.dispatch.OnSuccess;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCode;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.util.Timeout;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.github.open_developer.akka.cluster.http.sample.job.JobMessage;
import io.github.open_developer.akka.cluster.http.sample.model.Order;
import scala.Function1;
import scala.concurrent.Await;
import scala.concurrent.ExecutionContext;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;
import scala.concurrent.duration.FiniteDuration;
import scala.runtime.BoxedUnit;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static akka.http.javadsl.server.Directives.*;
import static akka.pattern.Patterns.ask;

public class FrontendMain {

    public static void main(String[] args) {

        // Override the configuration of the port when specified as program argument
        final String port = args.length > 0 ? args[0] : "0";
        final Config config = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
                withFallback(ConfigFactory.parseString("akka.cluster.roles = [frontend]")).
                withFallback(ConfigFactory.load());

        ActorSystem system = ActorSystem.create("ClusterSystem", config);

        final ActorRef frontend = system.actorOf(
                Props.create(DispatchOrderFrontend.class), "frontend");


        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);

        FrontendMain main = new FrontendMain();

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = main.createRoute(system, frontend).flow(system, materializer);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow, ConnectHttp.toHost("localhost", 8090), materializer);


    }

    private Route createRoute(ActorSystem system, ActorRef frontend) {
        final Timeout timeout = new Timeout(Duration.create(10, TimeUnit.SECONDS));
//        final ExecutionContext ec = system.dispatcher();
//        ActorRef frontend = system.actorOf(
//                Props.create(DispatchOrderFrontend.class), "dispatchOrderfrontend");

        return route (
                path("dispatch", () ->
                        post(() ->
                                entity(Jackson.unmarshaller(Order.class), order -> {
                                    frontend.tell(new JobMessage.Job<Order>(order), ActorRef.noSender());
                                    return complete(StatusCodes.ACCEPTED, "request queued");
//                                    Future<Object> future =  ask(frontend, order, timeout);
//                                    try {
//                                        Route route = (Route) Await.result(future, timeout.duration());
//                                        return route;
//                                    } catch (Exception e) {
//                                        return failWith(e);
//                                    }
                                })
                        )

                )
        );
    }
}
