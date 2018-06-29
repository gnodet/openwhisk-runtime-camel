package org.jboss.fuse.openwhisk.camel.core.function;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.direct.DirectComponent;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.language.simple.SimpleLanguage;
import org.apache.camel.model.FromDefinition;
import org.apache.camel.model.ProcessDefinition;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.SetHeaderDefinition;
import org.apache.camel.model.SplitDefinition;
import org.apache.camel.model.TransformDefinition;
import org.apache.camel.model.language.ExpressionDefinition;
import org.apache.camel.util.IntrospectionSupport;
import org.jboss.fuse.openwhisk.camel.api.CamelFunctionRouteBuilder;
import org.jboss.fuse.openwhisk.camel.core.support.FastCamelContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CamelFunction {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected final SimpleRegistry registry;

    protected final CamelContext camelContext;

    protected final ProducerTemplate camelTemplate;

    static {
        System.setProperty("CamelSimpleLRUCacheFactory", "true");
        new Thread() {
            @Override
            public void run() {
                IntrospectionSupport.cacheClass(FromDefinition.class);
                IntrospectionSupport.cacheClass(TransformDefinition.class);
                IntrospectionSupport.cacheClass(ExpressionDefinition.class);
                IntrospectionSupport.cacheClass(SetHeaderDefinition.class);
                IntrospectionSupport.cacheClass(SplitDefinition.class);
                IntrospectionSupport.cacheClass(RouteDefinition.class);
                IntrospectionSupport.cacheClass(ProcessDefinition.class);
            }
        }.start();
    }

    public CamelFunction() {
        registry = new SimpleRegistry();
        camelContext = createContext();
        camelTemplate = camelContext.createProducerTemplate();
        bind("function", new DirectComponent());
        bind("simple", new SimpleLanguage());
    }

    /**
     * Process a request
     */
    @SuppressWarnings("unchecked")
    public Map<String, ?> execute(Map<String, ?> request, Map<String, Object> env) {
        try {
            return camelTemplate.requestBodyAndHeaders(CamelFunctionRouteBuilder.INPUT_ENDPOINT_URI, request, env, Map.class);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("success", false);
            result.put("exception", e.getClass().getName());
            result.put("error", e.getMessage());
            log.error("Error during execution", e);
            return result;
        }
    }

    /**
     * Binds the given <code>name</code> to the <code>bean</code> object, so
     * that it can be looked up inside the CamelContext this command line tool
     * runs with.
     *
     * @param name the used name through which we do bind
     * @param bean the object to bind
     */
    public void bind(String name, Object bean) {
        registry.put(name, bean);
    }

    /**
     *
     * Gets or creates the {@link CamelContext} this main class is using.
     *
     * It just create a new CamelContextMap per call, please don't use it to access the camel context that will be ran by main.
     * If you want to setup the CamelContext please use MainListener to get the new created camel context.
     */
    public CamelContext getCamelContext() {
        return camelContext;
    }

    public void start() throws Exception {
        getCamelContext().start();
    }

    public void stop() throws Exception {
        getCamelContext().stop();
    }

    public void addRouteBuilder(RouteBuilder routeBuilder) throws Exception {
        if (routeBuilder instanceof CamelFunctionRouteBuilder) {
            ((CamelFunctionRouteBuilder) routeBuilder).setRegistry(registry);
        }
        routeBuilder.addRoutesToCamelContext(getCamelContext());
    }

    protected CamelContext createContext() {
//        CamelContext context = new DefaultCamelContext(registry);       // 458 / 491
        CamelContext context = new FastCamelContext(registry);          // 166 / 206
        return context;
    }

}
