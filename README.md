# Ports Framework

The Ports Framework enables decoupling of components by employing a purely
component-based design paradigm. Using Ports, component dependencies are
reduced to a minimum (and thus also the need for dependency injection).


## Maven setup

The `ports-demo` project demonstrates how to setup and use the Ports Framework.
It implements the steps described below.

Add the following dependency to your POM(s):

```
<dependency>
   <groupId>org.timux.ports</groupId>
   <artifactId>ports-library</artifactId>
   <version>0.3.2</version>
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

Then, you may add the following manifest entry to your JAR file:

```
Premain-Class: org.timux.ports.agent.Agent
```

This is optional, yet recommended. If you choose not to do it,
Ports will use a fallback that is less efficient. (However, the performance
penalty is so small that it would only be noticable in relatively rare corner
cases that involve very large numbers of consecutive port calls.)

For example, if you use the `maven-assembly-plugin` in order to build a JAR
with all dependencies, the complete plugin setup could look like this:

```
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-assembly-plugin</artifactId>
    <version>3.1.0</version>

    <configuration>
        <descriptorRefs>
            <descriptorRef>jar-with-dependencies</descriptorRef>
        </descriptorRefs>
        <archive>
            <manifest>
                <mainClass>... (your main class here) ...</mainClass>
            </manifest>
            <manifestEntries>
                <Premain-Class>org.timux.ports.agent.Agent</Premain-Class>
            </manifestEntries>
        </archive>
    </configuration>

    <executions>
        <execution>
            <id>make-assembly</id>
            <phase>package</phase>
            <goals>
                <goal>single</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

With this setup, your application is ready to use the Ports Framework.


## IDE setup

The following setup is optional, yet recommended. If you choose not to do it,
Ports will use a fallback that is less efficient. (However, the performance
penalty is so small that it would only be noticable in relatively rare corner
cases that involve very large numbers of consecutive port calls.)

You probably also want to setup your IDE so that you can run and debug
your application from there. For this, add the following entry to the JVM
options (and replace `HOME_DIR` with your user home directory):

```
-javaagent:HOME_DIR/.m2/repository/org/timux/ports/ports-agent/0.3.2/ports-agent-0.3.2-jar-with-dependencies.jar
``` 


## License

Ports is released under the terms of the Apache License version 2.0.