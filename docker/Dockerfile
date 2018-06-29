#
# Licensed to the Apache Software Foundation (ASF) under one or more contributor
# license agreements.  See the NOTICE file distributed with this work for additional
# information regarding copyright ownership.  The ASF licenses this file to you
# under the Apache License, Version 2.0 (the # "License"); you may not use this
# file except in compliance with the License.  You may obtain a copy of the License
# at:
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software distributed
# under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
# CONDITIONS OF ANY KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations under the License.
#

FROM adoptopenjdk/openjdk8-openj9:x86_64-alpine-jdk8u162-b12_openj9-0.8.0-slim

ARG CAMEL_OPENWHISK_CORE_VERSION="0.1.0-e540b50"

RUN mkdir /opt/camelAction \
    && cd /opt/camelAction \
	&& curl -L -o camelAction.jar https://raw.githubusercontent.com/gnodet/openwhisk-runtime-camel/m2-repo/org/jboss/fuse/openwhisk/camel-openwhisk-core/$CAMEL_OPENWHISK_CORE_VERSION/camel-openwhisk-core-$CAMEL_OPENWHISK_CORE_VERSION.jar \
	&& mkdir javaSharedCache

RUN /opt/java/openjdk/bin/java -Dfile.encoding=UTF-8 -Xshareclasses:cacheDir=/opt/camelAction/javaSharedCache -Xquickstart -cp /opt/camelAction/camelAction.jar org.jboss.fuse.openwhisk.camel.core.Proxy test

CMD ["/opt/java/openjdk/bin/java", "-Dfile.encoding=UTF-8", "-Xshareclasses:cacheDir=/opt/camelAction/javaSharedCache,readonly", "-Xquickstart", "-cp", "/opt/camelAction/camelAction.jar", "org.jboss.fuse.openwhisk.camel.core.Proxy"]
