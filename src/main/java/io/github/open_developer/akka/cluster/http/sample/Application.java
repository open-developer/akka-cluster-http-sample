package io.github.open_developer.akka.cluster.http.sample;

import io.github.open_developer.akka.cluster.http.sample.backend.BackendMain;
import io.github.open_developer.akka.cluster.http.sample.frontend.FrontendMain;

public class Application {

    public static void main(String[] args) throws Exception {
        BackendMain.main(new String[]{"2551"});
        BackendMain.main(new String[]{"2552"});
        BackendMain.main(new String[0]);
        FrontendMain.main(new String[0]);
        Thread.sleep(200000);
//        BackendMain.main(new String[0]);
    }
}
