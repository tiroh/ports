/*
 * Copyright 2018-2021 Tim Rohlfs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.timux.ports;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The main utility class for Ports's IoC functionality. Use this class to configure
 * your component scan path and to start your application.
 *
 * @author Tim Rohlfs
 * @since 0.7.0
 */
public class PortsApplication {

    private static final List<String> componentScanPackages = new ArrayList<>();

    private static final Logger logger = LoggerFactory.getLogger(PortsApplication.class);

    /**
     * Adds the provided packages to the component scan path. You can call this method multiple
     * times without overriding previous settings.
     */
    public synchronized static void componentScan(String... packages) {
        componentScanPackages.addAll(Arrays.asList(packages));
    }

    /**
     * Starts Ports's IoC container which in turn boots the application. Before calling this method,
     * you should configure your component scan path using the {@link #componentScan} method.
     */
    public synchronized static void start(String... args) {
        ClasspathScanner.scan(componentScanPackages);
        PortsIoc.instantiateStaticComponents();
    }

    public static <T> T getOrMakeInstance(Class<T> componentClass) {
        return PortsIoc.getOrMakeInstance(componentClass);
    }

    /**
     * Shuts the IoC container down. This does not terminate any running threads, it only clears all internal
     * data structures. This method should only be used in testing.
     */
    public synchronized static void shutdown() {
        componentScanPackages.clear();
        PortsIoc.clear();
        ClasspathScanner.clear();
        Ports.releaseProtocols();
        DomainManager.release();
    }
}
