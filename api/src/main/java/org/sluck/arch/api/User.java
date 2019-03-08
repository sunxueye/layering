package org.sluck.arch.api;

import java.io.Serializable;

/**
 * Created by sunxy on 2019/2/21 21:37.
 */
public class User implements Serializable {

    private String name;

    private int age;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }
}
