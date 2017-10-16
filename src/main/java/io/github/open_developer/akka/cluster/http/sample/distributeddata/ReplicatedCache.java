package io.github.open_developer.akka.cluster.http.sample.distributeddata;

import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.cluster.Cluster;
import akka.cluster.ddata.*;
import scala.Option;
import scala.concurrent.duration.Duration;

import java.util.Optional;
import java.util.concurrent.TimeUnit;



public class ReplicatedCache extends AbstractActor {

    private final Replicator.WriteConsistency writeMajority = new Replicator.WriteMajority(Duration.create(3, TimeUnit.SECONDS));
    private final Replicator.ReadConsistency readMajority = new Replicator.ReadMajority(Duration.create(3, TimeUnit.SECONDS));
    private final ActorRef replicator = DistributedData.get(context().system()).replicator();
    private final Cluster node = Cluster.get(context().system());

    static class Request {
        public final String key;
        public final ActorRef replyTo;

        public Request(String key, ActorRef replyTo) {
            this.key = key;
            this.replyTo = replyTo;
        }
    }

    public static class PutInCache {
        public final String key;
        public final Object value;

        public PutInCache(String key, Object value) {
            this.key = key;
            this.value = value;
        }
    }

    public static class GetFromCache {
        public final String key;

        public GetFromCache(String key) {
            this.key = key;
        }
    }

    public static class Cached {
        public final String key;
        public final Optional<Object> value;

        public Cached(String key, Optional<Object> value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((key == null) ? 0 : key.hashCode());
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Cached other = (Cached) obj;
            if (key == null) {
                if (other.key != null)
                    return false;
            } else if (!key.equals(other.key))
                return false;
            if (value == null) {
                if (other.value != null)
                    return false;
            } else if (!value.equals(other.value))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return "Cached [key=" + key + ", value=" + value + "]";
        }

    }

    public static class Evict {
        public final String key;

        public Evict(String key) {
            this.key = key;
        }
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(PutInCache.class, cmd -> receivePutInCache(cmd.key, cmd.value))
                .match(Evict.class, cmd -> receiveEvict(cmd.key))
                .match(GetFromCache.class, cmd -> receiveGetFromCache(cmd.key))
                .match(Replicator.GetSuccess.class, g -> receiveGetSuccess((Replicator.GetSuccess<LWWMap<String, Object>>) g))
                .match(Replicator.NotFound.class, n -> receiveNotFound((Replicator.NotFound<LWWMap<String, Object>>) n))
                .match(Replicator.UpdateResponse.class, u -> {})
                .build();
    }


    private void receivePutInCache(String key, Object value) {
        Replicator.Update<LWWMap<String, Object>> update = new Replicator.Update<>(dataKey(key), LWWMap.create(), writeMajority,
                curr -> curr.put(node, key, value));
        replicator.tell(update, self());
    }

    private void receiveEvict(String key) {
        Replicator.Update<LWWMap<String, Object>> update = new Replicator.Update<>(dataKey(key), LWWMap.create(), writeMajority,
                curr -> curr.remove(node, key));
        replicator.tell(update, self());
    }

    private void receiveGetFromCache(String key) {
        Optional<Object> ctx = Optional.of(new Request(key, sender()));
        Replicator.Get<LWWMap<String, Object>> get = new Replicator.Get<>(dataKey(key), readMajority, ctx);
        replicator.tell(get, self());
    }

    private void receiveGetSuccess(Replicator.GetSuccess<LWWMap<String, Object>> g) {
        Request req = (Request) g.getRequest().get();
        Option<Object> valueOption = g.dataValue().get(req.key);
        Optional<Object> valueOptional = Optional.ofNullable(valueOption.isDefined() ? valueOption.get() : null);
        req.replyTo.tell(new Cached(req.key, valueOptional), self());
    }

    private void receiveNotFound(Replicator.NotFound<LWWMap<String, Object>> n) {
        Request req = (Request) n.getRequest().get();
        req.replyTo.tell(new Cached(req.key, Optional.empty()), self());
    }

    private Key<LWWMap<String, Object>> dataKey(String key) {
        return LWWMapKey.create("cache-" + Math.abs(key.hashCode()) % 1000);
    }
}
