# The Ports Framework

**Ports is a Java framework for inter-component communication within a JVM process.**

Core features:

* Very effective decoupling of components.
* Acts like a high-performance, *type-safe* enterprise service bus.
* Strong support for asynchronicity and parallelism.
* Declarative description of tests (in the manner of mocking frameworks).
* Fault injection.
* AOP-like features for monitoring, logging, or debugging.
* Built-in support for union types (``Either`` and ``Either3``),
  making error handling easier.

Ports requires at least Java 8.


## Maven setup for plain Ports

Add the following dependency to your POM(s):

```
<dependency>
   <groupId>org.timux.ports</groupId>
   <artifactId>ports-core</artifactId>
   <version>0.5.8</version>
</dependency>
```


## Maven setup for Ports with Vaadin and Spring

Add the following dependency to your POM(s):

```
<dependency>
   <groupId>org.timux.ports</groupId>
   <artifactId>ports-vaadinspring</artifactId>
   <version>0.5.8</version>
</dependency>
```

## License

Ports is released under the terms of the Apache License version 2.0.