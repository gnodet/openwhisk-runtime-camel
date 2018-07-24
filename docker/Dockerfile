FROM openjdk:10-jdk

ARG CAMEL_OPENWHISK_CORE_VERSION="0.2.0"

RUN mkdir /opt/camelAction \
    && cd /opt/camelAction \
    && curl -L -o camelAction.jar https://raw.githubusercontent.com/gnodet/openwhisk-runtime-camel/m2-repo/org/jboss/fuse/openwhisk/camel-openwhisk-core/$CAMEL_OPENWHISK_CORE_VERSION/camel-openwhisk-core-$CAMEL_OPENWHISK_CORE_VERSION.jar 
	
RUN java -XX:+UseAppCDS -XX:DumpLoadedClassList=/opt/camelAction/classes.lst -cp /opt/camelAction/camelAction.jar org.jboss.fuse.openwhisk.camel.core.CamelRuntime test

RUN java -XX:+UseAppCDS -Xshare:dump -XX:SharedClassListFile=/opt/camelAction/classes.lst -XX:SharedArchiveFile=/opt/camelAction/app-cds.jsa -cp /opt/camelAction/camelAction.jar org.jboss.fuse.openwhisk.camel.core.CamelRuntime test

CMD ["java", "-Dfile.encoding=UTF-8", "-XX:+UseAppCDS", "-Xshare:on", "-XX:SharedArchiveFile=/opt/camelAction/app-cds.jsa", "-cp", "/opt/camelAction/camelAction.jar", "org.jboss.fuse.openwhisk.camel.core.CamelRuntime"]
