package org.sluck.arch.soawar.client;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.Resource;

/**
 * Created by sunxy on 2019/3/8 16:42.
 */
@Controller
public class TestCacheController {

    @Resource
    private ClientApplication clientApplication;

    @RequestMapping("/test")
    @ResponseBody
    public String testProsxy() {

        return clientApplication.serviceUrl("cglibproxytest");
    }

}
