package org.jboss.fuse.openwhisk.camel.api;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.model.RouteDefinition;

public abstract class CamelFunctionRouteBuilder extends RouteBuilder {

    public static final String INPUT_ENDPOINT_URI = "function:input";

    protected SimpleRegistry registry;

    public void setRegistry(SimpleRegistry registry) {
        this.registry = registry;
    }

    public void bind(String name, Object bean) {
        registry.put(name, bean);
    }

    public RouteDefinition from() {
        return from(INPUT_ENDPOINT_URI);
    }

}
