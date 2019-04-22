package org.sluck.arch.stream.invokehander;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sluck.arch.stream.annotation.EventListener;
import org.sluck.arch.stream.converter.EventConverter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 对 {@link EventListener} 指定的方法进行封装的处理器
 * <p>
 * Created by sunxy on 2019/3/27 14:40.
 */
public class EventListenerMethodHandler {

    private final Class<?> beanType;

    private final Method method;//注解修饰的方法

    private final Object targetBean;//方法所在的 bean

    private final EventConverter eventConverter;

    private Boolean cachedCanConvert; //是否可以进行事件转换 (因为使用了反射，所以增加一个缓存）

    private Logger logger = LoggerFactory.getLogger(getClass());

    /**
     * 使用被注解修饰的方法和其所在的 spring bean 进行构造
     *
     * @param target
     * @param method
     */
    public EventListenerMethodHandler(Object target, Method method, EventConverter eventConverter) {
        Assert.notNull(target, "Bean is can't null");
        Assert.notNull(method, "Method is required");
        this.targetBean = target;
        this.beanType = ClassUtils.getUserClass(target);
        this.method = method;

        this.eventConverter = eventConverter;
    }

    /**
     * 使用参数执行指定的方法
     *
     * @param providedArgs
     * @return
     * @throws Exception
     */
    public Object invoke(Object... providedArgs) throws Exception {
        if (logger.isTraceEnabled()) {
            logger.trace("Arguments: " + Arrays.toString(providedArgs));
        }
        if (cachedCanConvert == null) {
            Class[] parmsTypes = method.getParameterTypes();
            if (parmsTypes.length == 0 || providedArgs == null || providedArgs.length == 0) {
                cachedCanConvert = false;
            } else {
                //只对第一个参数进行转换，也就是 eventListener 修饰的方法参数不能超过一个
                Class<?> sourceClass = providedArgs[0].getClass();
                Class<?> targetClass = parmsTypes[0];
                cachedCanConvert = eventConverter.canConvert(sourceClass, targetClass);
            }
        }

        if (cachedCanConvert) {
            Object event = null;
            try {
                event = eventConverter.toEvent(providedArgs[0]);
            } catch (Exception | Error e) {
                logger.error("事件转换出错", e);
                String text = (e.getMessage() != null ? e.getMessage() : "Illegal argument convert");
                throw new IllegalArgumentException(formatInvokeError(text, providedArgs), e);
            }
            return doInvoke(event);
        }

        //如果不能转换则使用原始参数
        return doInvoke(providedArgs);
    }

    private Object doInvoke(Object... args) throws Exception {
        try {
            return this.method.invoke(getBean(), args);
        } catch (IllegalArgumentException ex) {
            String text = (ex.getMessage() != null ? ex.getMessage() : "Illegal argument");
            throw new IllegalStateException(formatInvokeError(text, args), ex);
        } catch (InvocationTargetException ex) {
            // Unwrap for HandlerExceptionResolvers ...
            Throwable targetException = ex.getTargetException();
            if (targetException instanceof RuntimeException) {
                throw (RuntimeException) targetException;
            } else if (targetException instanceof Error) {
                throw (Error) targetException;
            } else if (targetException instanceof Exception) {
                throw (Exception) targetException;
            } else {
                throw new IllegalStateException(formatInvokeError("Invocation failure", args), targetException);
            }
        }
    }

    private Object getBean() {
        return this.targetBean;
    }

    private Class<?> getBeanType() {
        return this.beanType;
    }

    private Method getMethod() {
        return this.method;
    }

    private String formatInvokeError(String text, Object[] args) {
        String formattedArgs = IntStream.range(0, args.length)
                .mapToObj(i -> (args[i] != null ?
                        "[" + i + "] [type=" + args[i].getClass().getName() + "] [value=" + args[i] + "]" :
                        "[" + i + "] [null]"))
                .collect(Collectors.joining(",\n", " ", " "));

        return text + "\n" +
                "Endpoint [" + getBeanType().getName() + "]\n" +
                "Method [" + getMethod().toGenericString() + "] " +
                "with argument values:\n" + formattedArgs;
    }
}
