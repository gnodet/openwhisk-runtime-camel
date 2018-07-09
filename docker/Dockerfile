FROM adoptopenjdk/openjdk8-openj9:x86_64-alpine-jdk8u162-b12_openj9-0.8.0-slim

ARG CAMEL_OPENWHISK_CORE_VERSION="0.1.0-e540b50"

RUN adduser -D -H myuser

RUN mkdir /opt/camelAction \
    && cd /opt/camelAction \
    && curl -L -o camelAction.jar https://raw.githubusercontent.com/gnodet/openwhisk-runtime-camel/m2-repo/org/jboss/fuse/openwhisk/camel-openwhisk-core/$CAMEL_OPENWHISK_CORE_VERSION/camel-openwhisk-core-$CAMEL_OPENWHISK_CORE_VERSION.jar \
    && mkdir javaSharedCache \
    && chown -R myuser /opt/camelAction

USER myuser

RUN /opt/java/openjdk/bin/java -Dfile.encoding=UTF-8 -Xshareclasses:cacheDir=/opt/camelAction/javaSharedCache -Xquickstart -cp /opt/camelAction/camelAction.jar org.jboss.fuse.openwhisk.camel.core.Proxy test

CMD ["/opt/java/openjdk/bin/java", "-Dfile.encoding=UTF-8", "-Xshareclasses:cacheDir=/opt/camelAction/javaSharedCache,readonly", "-Xquickstart", "-cp", "/opt/camelAction/camelAction.jar", "org.jboss.fuse.openwhisk.camel.core.Proxy"]
