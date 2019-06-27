package org.sluck.arch.soawar.client;

import org.sluck.arch.soawar.ribbon.TestRibbonConfigure;
import org.springframework.cloud.netflix.ribbon.RibbonClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * Created by sunxy on 2019/3/8 16:42.
 */
@Controller
@RibbonClient(name = "TestService2", configuration = {TestRibbonConfigure.class})
public class TestCacheController {

    @Resource
    private ClientApplication clientApplication;

    @RequestMapping("/test")
    @ResponseBody
    public String testProsxy() {

        return clientApplication.serviceUrl("cglibproxytest");
    }

}
