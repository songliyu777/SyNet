package com.synet.cache.autoconfigure;

import java.util.Objects;

public class TestBean {

    private String name;

    public TestBean() {
    }

    public TestBean(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return "TestBean{" +
                "name='" + name + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestBean bean = (TestBean) o;
        return Objects.equals(name, bean.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
