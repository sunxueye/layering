package org.sluck.arch.api;

/**
 * Created by sunxy on 2019/2/21 21:39.
 */

public interface TestClient {

    User getStores(TestValues testValues);

    //@RequestMapping(method = RequestMethod.POST, value = "/stores/{storeId}", consumes = "application/json")
    //Store update(@PathVariable("storeId") Long storeId, Store store);
}
