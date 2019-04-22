package org.sluck.arch.soawar.client;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by sunxy on 2019/3/26 9:20.
 */
public class HttpClientTest {

    public static void main(String[] args) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(3000) //设置从 pool 获取连接超时时间
                .setConnectTimeout(3000) //设置默认连接超时时间
                .setSocketTimeout(5000).build(); //设置默认读超时时间

        CloseableHttpClient httpClient = HttpClientBuilder.create()
                .setMaxConnTotal(5) //最大连接数
                .setMaxConnPerRoute(2) //每个 host 可用连接数
                .setDefaultRequestConfig(requestConfig) //设置 request 配置
                .evictExpiredConnections() //开启自动清理过期连接
                .evictIdleConnections(300, TimeUnit.SECONDS) //清理不活跃连接
                .build();

        HttpPost httpPost = new HttpPost("http://localhost:80/pay/requestPay");
        httpPost.addHeader("referer", "http://192.168.1.18");
        httpPost.addHeader("X-Real-IP", "192.168.2.18");
        httpPost.addHeader("X-Forwarded-For", "192.168.2.18");

        try {
            CloseableHttpResponse httpResponse = httpClient.execute(httpPost);
            System.out.println(httpResponse.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
