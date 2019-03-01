package org.sluck.arch.soawar.ribbon;

import com.netflix.client.config.CommonClientConfigKey;
import com.netflix.client.config.DefaultClientConfigImpl;
import com.netflix.client.config.IClientConfig;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Bean;

/**
 * Created by sunxy on 2019/2/21 17:44.
 */
public class TestRibbonConfigure {

    @Bean
    public IClientConfig setConfig() {
        DefaultClientConfigImpl config = new DefaultClientConfigImpl();
        config.loadProperties("TestService");
        config.set(CommonClientConfigKey.MaxAutoRetries, 1); //默认0
        config.set(CommonClientConfigKey.MaxAutoRetriesNextServer, 2); // 默认1
        config.set(CommonClientConfigKey.MaxTotalConnections, 100);
        config.set(CommonClientConfigKey.MaxConnectionsPerHost, 20);
        config.set(CommonClientConfigKey.ConnectTimeout, 1000);
        config.set(CommonClientConfigKey.ReadTimeout, 3000);
        return config;
    }
}
