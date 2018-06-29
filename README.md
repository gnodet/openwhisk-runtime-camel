# Openwhisk-Runtime-Camel

This project contains a Runtime for Apache OpenWhisk that can be used to efficiently run Camel routes.

The corresponding Docker image is build automatically and is available at
  https://hub.docker.com/r/gnodet/openwhisk-camel/

To deploy a route in an OpenWhisk / OpenShift environment, build your route jar following the [camel-openwhisk-example] example and deploy it using the following command

```
wsk action create camelFunction camel-openwhisk-example/target/camel-openwhisk-example-0.1.0-SNAPSHOT.jar --docker gnodet/openwhisk-runtime-camel --main org.jboss.fuse.openwhisk.camel.example.SimpleCamelFunction
```

You can then invoke the route using

```
wsk action invoke --blocking camelFunction -p message 'foo@bar@baz'
```