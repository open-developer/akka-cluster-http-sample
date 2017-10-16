package io.github.open_developer.akka.cluster.http.sample.job;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

public interface JobMessage {

    String BACKEND_REGISTRATION = "BackendRegistration";

    @Getter
    @AllArgsConstructor
    class Job<T> implements Serializable {
        private T job;
    }

    @Getter
    @AllArgsConstructor
    class JobFailed<T> implements Serializable {
        private String reason;
        private T job;
    }
}