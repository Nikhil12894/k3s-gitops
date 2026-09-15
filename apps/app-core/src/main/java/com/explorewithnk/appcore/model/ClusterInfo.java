package com.explorewithnk.appcore.model;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.Map;

@RegisterForReflection
public class ClusterInfo {

    public String appName;
    public String version;
    public String runtime;
    public boolean isNative;
    public Map<String, Object> services;

    public ClusterInfo() {
    }

    public ClusterInfo(String appName, String version, String runtime, boolean isNative, Map<String, Object> services) {
        this.appName = appName;
        this.version = version;
        this.runtime = runtime;
        this.isNative = isNative;
        this.services = services;
    }
}
