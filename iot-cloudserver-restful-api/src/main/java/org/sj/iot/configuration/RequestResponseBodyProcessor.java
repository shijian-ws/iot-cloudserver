package org.sj.iot.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.HandlerMethodReturnValueHandler;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestResponseBodyMethodProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * JSON解析处理器
 *
 * @author shijian
 * @email shijianws@163.com
 * @date 2018-01-01
 */
@Component
public class RequestResponseBodyProcessor implements HandlerMethodArgumentResolver, HandlerMethodReturnValueHandler, ApplicationListener<ContextRefreshedEvent> {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestResponseBodyProcessor.class);

    private static final boolean isSpringBoot; // 是否Spring Boot环境

    static {
        Class<?> springApplication = null;
        try {
            springApplication = Class.forName("org.springframework.boot.SpringApplication");
        } catch (Exception e) {
        }
        if (springApplication != null) {
            isSpringBoot = true;
        } else {
            isSpringBoot = false;
        }
    }

    /**
     * 请求体JSON处理器
     */
    private RequestResponseBodyMethodProcessor argumentResolver;
    /**
     * 响应对象JSON处理器
     */
    private RequestResponseBodyMethodProcessor returnValueHandler;

    /**
     * 容器实例化完成触发
     */
    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        ApplicationContext context = event.getApplicationContext();
        if (isSpringBoot && context.getParent() != null) {
            // 非 Spring Boot 主容器, 可能是 management 容器
            return;
        } else if (!isSpringBoot && context.getParent() == null) {
            // 非 Spring MVC 容器, 可能是 Spring 主容器
            return;
        }
        RequestMappingHandlerAdapter handlerAdapter = context.getBean(RequestMappingHandlerAdapter.class);
        argumentResolver = processeResolver(this, handlerAdapter::getArgumentResolvers, handlerAdapter::setArgumentResolvers); // 替换参数解析器
        returnValueHandler = processeResolver(this, handlerAdapter::getReturnValueHandlers, handlerAdapter::setReturnValueHandlers); // 替换响应结果处理器
    }

    /**
     * 替换解析器
     */
    private static <T> RequestResponseBodyMethodProcessor processeResolver(T newResolver, Supplier<List<T>> getter, Consumer<List<T>> setter) {
        RequestResponseBodyMethodProcessor processor = null;
        List<T> oldList = getter.get();
        List<T> newList = new ArrayList<>();
        for (T resolver : oldList) {
            if (resolver instanceof RequestResponseBodyMethodProcessor) {
                // 请求体, 响应体处理器
                processor = (RequestResponseBodyMethodProcessor) resolver;
                newList.add(0, newResolver);
                continue;
            }
            newList.add(resolver);
        }
        setter.accept(newList);
        return processor;
    }

    /**
     * 是否能处理指定类型参数
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if (argumentResolver != null) {
            return argumentResolver.supportsParameter(parameter);
        }
        return false;
    }

    /**
     * 处理参数
     */
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        return argumentResolver.resolveArgument(parameter, mavContainer, webRequest, binderFactory);
    }

    /**
     * 是否能处理指定返回值类型
     */
    @Override
    public boolean supportsReturnType(MethodParameter returnType) {
        if (returnValueHandler != null) {
            return returnValueHandler.supportsReturnType(returnType);
        }
        return false;
    }

    /**
     * 处理指定返回值
     */
    @Override
    public void handleReturnValue(Object returnValue, MethodParameter returnType, ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {
        returnValueHandler.handleReturnValue(returnValue, returnType, mavContainer, webRequest);
    }
}