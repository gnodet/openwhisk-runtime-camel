/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.fuse.openwhisk.camel.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.jboss.fuse.openwhisk.camel.api.CamelFunctionRouteBuilder;
import org.jboss.fuse.openwhisk.camel.core.function.CamelFunction;
import org.jboss.fuse.openwhisk.camel.core.json.JsonReader;
import org.jboss.fuse.openwhisk.camel.core.json.JsonWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CamelRuntime {
    private HttpServer server;

    private JarLoader loader = null;

    private Logger log = LoggerFactory.getLogger(CamelRuntime.class);

    public CamelRuntime(int port) throws IOException {
        this.server = HttpServer.create(new InetSocketAddress(port), -1);

        this.server.createContext("/init", new InitHandler());
        this.server.createContext("/run", new RunHandler());
        this.server.setExecutor(null); // creates a default executor
    }

    public void setExecutor(Executor executor) {
        server.setExecutor(executor);
    }

    public void start() {
        server.start();
        log.info("Server started on {}", server.getAddress());
    }

    private static void writeResponse(HttpExchange t, int code, String content) throws IOException {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        t.sendResponseHeaders(code, bytes.length);
        OutputStream os = t.getResponseBody();
        os.write(bytes);
        os.close();
    }

    private static void writeError(HttpExchange t, String errorMessage) throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter.write(sw, Collections.singletonMap("error", errorMessage));
        writeResponse(t, 502, sw.toString());
    }

    private class InitHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            log.info("Initialize");
            if (loader != null) {
                CamelRuntime.writeError(t, "Cannot initialize the action more than once.");
                log.error("Error during initialization: Cannot initialize the action more than once.");
                return;
            }

            try {
                InputStream is = t.getRequestBody();
                Reader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                ExtJsonReader parser = new ExtJsonReader(r);
                Map<String, ?> inputObject = (Map) parser.parse();

                Map<String, ?> message = (Map) inputObject.get("value");
                String mainClass = (String) message.get("main");
                Path jarPath = (Path) message.get("code");

                // Start up the custom classloader. This also checks that the
                // main method exists.
                loader = new JarLoader(jarPath, mainClass);

                CamelRuntime.writeResponse(t, 200, "{ \"OK\": true }");
                log.info("Initialization finished.");
            } catch (Exception e) {
                log.error("Error during initialization", e);
                CamelRuntime.writeError(t, "An error has occurred (see logs for details): " + e);
            }
        }
    }

    private class RunHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            log.info("Running");
            if (loader == null) {
                CamelRuntime.writeError(t, "Cannot invoke an uninitialized action.");
                return;
            }

            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            SecurityManager sm = System.getSecurityManager();

            try {
                InputStream is = t.getRequestBody();
                Reader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                Map<String, Object> ie = (Map) JsonReader.read(r);
                Map<String, ?> inputObject = (Map) ie.remove("value");

                Thread.currentThread().setContextClassLoader(loader);
                System.setSecurityManager(new WhiskSecurityManager());

                // User code starts running here.
                Map<String, ?> output = loader.invokeMain(inputObject, ie);
                // User code finished running here.

                if(output == null) {
                    throw new NullPointerException("The action returned null");
                }

                StringWriter sw = new StringWriter();
                JsonWriter.write(sw, output);
                CamelRuntime.writeResponse(t, 200, sw.toString());
                log.info("Run finished");
            } catch (Exception e) {
                log.error("Error during run", e);
                CamelRuntime.writeError(t, "An error has occurred (see logs for details): " + e);
            } finally {
                System.setSecurityManager(sm);
                Thread.currentThread().setContextClassLoader(cl);

                System.err.println("XXX_THE_END_OF_A_WHISK_ACTIVATION_XXX");
                System.err.flush();
            }
        }
    }

    private static class ExtJsonReader extends JsonReader {

        public ExtJsonReader(Reader reader) {
            super(reader);
        }

        @Override
        public Object parse() throws IOException {
            return super.parse();
        }

        @Override
        protected Object readValue() throws IOException {
            if (current == '\"' && stack.size() == 4) {
                Iterator<Object> it = stack.iterator();
                if (Objects.equals("code", it.next())) {
                    Object v = it.next();
                    if (v instanceof Map) {
                        if (((Map) v).get("binary") == Boolean.TRUE) {
                            if (Objects.equals("value", it.next())) {
                                return readBinary();
                            }
                        }
                    }
                }
            }
            return super.readValue();
        }

        private Object readBinary() throws IOException {
            Path path = Files.createTempFile("useraction-", ".jar");
            read();
            InputStream in = Base64.getDecoder().wrap(new InputStream() {
                @Override
                public int read() throws IOException {
                    int c = current;
                    if (c != '\"') {
                        ExtJsonReader.this.read();
                    } else {
                        c = -1;
                    }
                    return c;
                }
            });
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
            read();
            return path;
        }
    }

    public static void main(String args[]) throws Exception {
        if (args.length == 1 && "test".equals(args[0])) {
            Executor executor = Executors.newCachedThreadPool(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread th = new Thread(r);
                    th.setDaemon(true);
                    return th;
                }
            });
            CamelRuntime camelRuntime = new CamelRuntime(8080);
            camelRuntime.start();
            CamelFunction function = new CamelFunction();
            function.addRouteBuilder(new CamelFunctionRouteBuilder() {
                @Override
                public void configure() {
                    from().setBody(constant(Collections.<String, Object>emptyMap()));
                }
            });
            function.start();
            function.execute(Collections.<String, Object>emptyMap(), Collections.<String, Object>emptyMap());
            System.out.println("OK !");
            System.exit(0);
        } else {
            CamelRuntime camelRuntime = new CamelRuntime(8080);
            camelRuntime.start();
        }
    }
}
