package org.sluck.arch.soawar.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Created by sunxy on 2019/2/21 21:39.
 */
@FeignClient("TestService")
public interface TestClient {

    @RequestMapping(method = RequestMethod.GET, value = "/user")
    User getStores();

    //@RequestMapping(method = RequestMethod.POST, value = "/stores/{storeId}", consumes = "application/json")
    //Store update(@PathVariable("storeId") Long storeId, Store store);
}
