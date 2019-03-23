package org.sluck.arch.turbine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.hystrix.dashboard.EnableHystrixDashboard;
import org.springframework.cloud.netflix.turbine.EnableTurbine;

/**
 * Created by sunxy on 2019/3/18 14:09.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableTurbine
@EnableHystrixDashboard
public class TurbineApplication {

    public static void main(String[] args) {
        args = new String[1];
        args[0] = "--spring.profiles.active=turbine";
        SpringApplication.run(TurbineApplication.class, args);
    }
}
