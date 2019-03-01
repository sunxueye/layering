package org.sluck.arch.soawar.client;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sluck.arch.soawar.ribbon.TestRibbonConfigure;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.commons.httpclient.ApacheHttpClientConnectionManagerFactory;
import org.springframework.cloud.netflix.eureka.EurekaDiscoveryClient;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.time.Duration;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
//@EnableEurekaClient
//@EnableDiscoveryClient
@RestController
@EnableFeignClients
@RibbonClient(name = "TestService", configuration = {TestRibbonConfigure.class})
public class ClientApplication {

    //@Bean
    //public HttpClientConnectionManager connectionManager(
    //        ApacheHttpClientConnectionManagerFactory connectionManagerFactory,
    //        RegistryBuilder registryBuilder) {
    //    final HttpClientConnectionManager connectionManager = connectionManagerFactory
    //            .newConnectionManager(true, 200,
    //                    50,
    //                    900,TimeUnit.SECONDS, registryBuilder);
    //    connectionManager.se
    //    this.connectionManagerTimer.schedule(new TimerTask() {
    //        @Override
    //        public void run() {
    //            connectionManager.closeExpiredConnections();
    //        }
    //    }, 30000, httpClientProperties.getConnectionTimerRepeat());
    //    return connectionManager;
    //}

    @LoadBalanced
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {

        return restTemplateBuilder
                .setReadTimeout(Duration.ofMillis(5000))
                .setConnectTimeout(Duration.ofMillis(2000))
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
                .setSocketTimeout(1000).build(); //设置默认读超时时间

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

    @Resource
    private EurekaDiscoveryClient discoveryClient;

    @Autowired
    private RestTemplate restTemplate;

    @Resource
    private TestClient testClient;

    private Logger logger = LoggerFactory.getLogger(getClass());

    @RequestMapping("/hello")
    public void serviceUrl() {
        //List<ServiceInstance> list = discoveryClient.getInstances("TestService");
        //list.stream().forEach(info -> {
        //    logger.info(info.getUri().toASCIIString());
        //});
        User user = testClient.getStores();
        System.out.println(user.getName());
    }

    @RequestMapping("/hello2")
    public String testService() {
        logger.info("---------------------- 准备开始远程调用 ----------------------");
        String res = restTemplate.getForEntity("http://TestService/hello", String.class).getBody();
        logger.info("---------------------- 请求远程调用返回结果 --------------------:" + res);
        return res;
    }

    public static void main(String[] args) {
        args = new String[1];
        args[0] = "--spring.profiles.active=client";
        SpringApplication.run(ClientApplication.class, args);
    }

}

