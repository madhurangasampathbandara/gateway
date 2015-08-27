package org.wso2.carbon.mediation.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.spi.ExecutorServiceManager;
import org.apache.camel.spi.ThreadPoolProfile;

/**
 *
 */

public class Test {
    public static void main(String[] args) throws Exception {
        CamelContext context = new DefaultCamelContext();
        context.disableJMX();
/*
        ExecutorServiceManager manager = context.getExecutorServiceManager();
        ThreadPoolProfile defaultProfile = manager.getDefaultThreadPoolProfile();
        defaultProfile.setPoolSize(500);
        defaultProfile.setMaxPoolSize(1000);
        defaultProfile.setMaxQueueSize(5000);
        defaultProfile.setDefaultProfile(true);
*/

        context.addRoutes(new RouteBuilder() {
            public void configure() {

                from("wso2-gw:http://204.13.85.2:9090/service")
                        .choice()
                            .when(header("routeId").regex("r1"))
                                .to("wso2-gw:http://204.13.85.5:5050/services/echo")
                            .when(header("routeId").regex("r2"))
                                .to("wso2-gw:http://204.13.85.5:6060/services/echo")
                            .otherwise()
                                .to("wso2-gw:http://204.13.85.5:7070/services/echo");

                from("wso2-gw:http://localhost:9090/service")
                        .choice()
                            .when(header("routeId").regex("r1"))
                                .to("wso2-gw:http://localhost:8080/services/echo")
                            .when(header("routeId").regex("r2"))
                                .to("wso2-gw:http://localhost:6060/services/echo")
                            .otherwise()
                                .to("wso2-gw:http://localhost:7070/services/echo");

            }

        });
        context.start();
        while (true) {
            Thread.sleep(100000);
        }
    }
}
