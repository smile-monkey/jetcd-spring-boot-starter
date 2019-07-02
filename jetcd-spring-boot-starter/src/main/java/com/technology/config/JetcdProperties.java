package com.technology.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("etcd.config")
public class JetcdProperties {
    private String endpoints;

    private boolean enabled;

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(String endpoints) {
        this.endpoints = endpoints;
    }
}
