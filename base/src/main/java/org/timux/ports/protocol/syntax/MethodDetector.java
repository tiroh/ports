package org.timux.ports.protocol.syntax;

import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.InvocationHandler;

import java.lang.reflect.Method;
import java.util.function.BiFunction;

class MethodDetector implements InvocationHandler {

    private Method method;

    @Override
    public Object invoke(Object o, Method method, Object[] objects) throws Throwable {
        this.method = method;
        return null;
    }

    static <T, I, O> Method getMethod(Class<T> clazz, BiFunction<T, I, O> inPort) {
        MethodDetector methodDetector = new MethodDetector();
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(clazz);
        enhancer.setCallback(methodDetector);
        T p = (T) enhancer.create();

        inPort.apply(p, null);

        return methodDetector.method;
    }
}
