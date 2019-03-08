package org.sluck.arch.soawar.client;

import org.sluck.arch.api.TestClient;
import org.sluck.arch.api.TestValues;
import org.sluck.arch.api.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

/**
 * Created by sunxy on 2019/3/4 21:20.
 */
@FeignClient("TestService")
public interface TestClientService extends TestClient {

    @Override
    @RequestMapping(method = RequestMethod.GET, value = "/user")
    User getStores(TestValues testValues);
}
