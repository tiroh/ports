# Ports Framework

The Ports Framework enables decoupling of components by employing a purely
component-based design paradigm. Using Ports, component dependencies are
reduced to a minimum (and thus also the need for dependency injection).


## Maven setup for plain Ports

Add the following dependency to your POM(s):

```
<dependency>
   <groupId>org.timux.ports</groupId>
   <artifactId>ports-library</artifactId>
   <version>0.4.0</version>
</dependency>
```


## Maven setup for Ports with Vaadin and Spring

Add the following dependency to your POM(s):

```
<dependency>
   <groupId>org.timux.ports</groupId>
   <artifactId>ports-vaadinspring</artifactId>
   <version>0.4.0</version>
</dependency>
```

The Ports Framework requires at least Java 8. Add the following entry to your
Super POM to make sure the compiler is set up correctly:

```
<properties>
    <maven.compiler.source>1.8</maven.compiler.source>
    <maven.compiler.target>1.8</maven.compiler.target>
</properties>
```

## License

Ports is released under the terms of the Apache License version 2.0.