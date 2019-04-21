package org.sluck.arch.soawar.configclient.locator;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.config.client.ConfigClientProperties;
import org.springframework.cloud.config.client.ConfigServicePropertySourceLocator;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

//@Configuration
//@RibbonClient(name = "clientRibbon", configuration = {ConfigServerRibbonConfigure.class})
public class CustomConfigServiceBootstrapConfiguration {

    @Value("${spring.cloud.config.discovery.serviceId:config_center}")
    private String configServer;

    @Resource(name = "cientRestTemplate")
    private RestTemplate restTemplate;

    @Bean
    public ConfigServicePropertySourceLocator configServicePropertySourceLocator(ConfigClientProperties clientProperties) {
        ConfigServicePropertySourceLocator configServicePropertySourceLocator =  new ConfigServicePropertySourceLocator(clientProperties);
        configServicePropertySourceLocator.setRestTemplate(customRestTemplate(clientProperties));
        return configServicePropertySourceLocator;
    }

    private RestTemplate customRestTemplate(ConfigClientProperties clientProperties) {
        return restTemplate;
    }

    @LoadBalanced
    @Bean(name = "cientRestTemplate")
    public RestTemplate restTemplate(ConfigClientProperties clientProperties) {
        RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();

        return restTemplateBuilder
                .setReadTimeout(Duration.ofMillis((long) clientProperties.getRequestReadTimeout())) //会覆盖  ClientHttpRequestFactory 里的配置
                .setConnectTimeout(Duration.ofMillis(2000)) //会覆盖  ClientHttpRequestFactory 里的配置
                .requestFactory(this::clientHttpRequestFactory)
                .build();

    }

    /**
     * 配置一个默认的 spring rest 需要的 httpFactory， 用于设置一些 http client pool 的一些基本配置
     *
     * @return
     */
    private ClientHttpRequestFactory clientHttpRequestFactory() {
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory();
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(1000) //设置从 pool 获取连接超时时间
                .setConnectTimeout(1000) //设置默认连接超时时间
                .setSocketTimeout(2000).build(); //设置默认读超时时间

        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setMaxConnTotal(5) //最大连接数
                .setMaxConnPerRoute(2) //每个 host 可用连接数
                .setDefaultRequestConfig(requestConfig) //设置 request 配置
                .evictExpiredConnections() //开启自动清理过期连接
                .evictIdleConnections(300, TimeUnit.SECONDS) //清理不活跃连接
                .build();

        requestFactory.setHttpClient(httpClient);
        return requestFactory;
    }
}
