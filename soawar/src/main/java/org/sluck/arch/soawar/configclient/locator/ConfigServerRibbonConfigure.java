package org.sluck.arch.soawar.configclient.locator;

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;


public class ConfigServerRibbonConfigure {

    @Value("${spring.cloud.config.discovery.serviceId:config_center}")
    private String configServer;

    @Bean
    public IClientConfig setConfig() {
        DefaultClientConfigImpl config = new DefaultClientConfigImpl();
        config.loadProperties(configServer);
        config.set(CommonClientConfigKey.MaxAutoRetries, 0); //默认0
        config.set(CommonClientConfigKey.MaxAutoRetriesNextServer, 1); // 默认1
        config.set(CommonClientConfigKey.MaxTotalConnections, 100);
        config.set(CommonClientConfigKey.MaxConnectionsPerHost, 20);
        config.set(CommonClientConfigKey.ConnectTimeout, 1000);
        config.set(CommonClientConfigKey.ReadTimeout, 3000);
        return config;
    }
}
