# Ports Framework

The Ports Framework enables decoupling of components by employing a purely
component-based design paradigm. Using Ports, component dependencies are
reduced to a minimum (and thus also the need for dependency injection).


## Maven setup for plain Ports

The Ports Framework requires at least Java 8.

Add the following dependency to your POM(s):

```
<dependency>
   <groupId>org.timux.ports</groupId>
   <artifactId>ports-core</artifactId>
   <version>0.4.1</version>
</dependency>
```


## Maven setup for Ports with Vaadin and Spring

Add the following dependency to your POM(s):

```
<dependency>
   <groupId>org.timux.ports</groupId>
   <artifactId>ports-vaadinspring</artifactId>
   <version>0.4.1</version>
</dependency>
```

## License

Ports is released under the terms of the Apache License version 2.0.