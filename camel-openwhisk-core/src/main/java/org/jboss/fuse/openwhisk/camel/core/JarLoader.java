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

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.Map;

import org.apache.camel.builder.RouteBuilder;
import org.jboss.fuse.openwhisk.camel.core.function.CamelFunction;

public class JarLoader extends URLClassLoader {

    private final Class<?> mainClass;
    private final CamelFunction function;

    public JarLoader(Path jarPath, String entrypoint) throws Exception {
        super(new URL[] { jarPath.toUri().toURL() });

        this.mainClass = loadClass(entrypoint);
        Object instance = mainClass.getDeclaredConstructor().newInstance();
        RouteBuilder rb = RouteBuilder.class.cast(instance);
        this.function = new CamelFunction();
        this.function.addRouteBuilder(rb);
        this.function.start();
    }

    public Map<String, ?> invokeMain(Map<String, ?> arg, Map<String, Object> env) {
        return function.execute(arg, env);
    }

}
