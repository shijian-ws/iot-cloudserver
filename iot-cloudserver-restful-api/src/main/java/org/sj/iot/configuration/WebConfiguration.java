package org.sj.iot.configuration;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.web.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * web配置
 *
 * @author shijian
 * @email shijianws@163.com
 * @date 2018-01-01
 */
@Configuration
@AutoConfigureBefore(WebMvcAutoConfiguration.class)
@RestControllerAdvice
public class WebConfiguration {
    /*@Configuration
    public class InterceptorConfigurer extends WebMvcConfigurerAdapter {
        @Autowired
        private HandlerInterceptor handlerInterceptor;

        @Override
        public void addInterceptors(InterceptorRegistry registry) {
            // 注册拦截器
            registry.addInterceptor(handlerInterceptor);
        }
    }*/

    @Autowired(required = false)
    private List<ErrorViewResolver> errorViewResolvers;

    private ServerProperties serverProperties;

    public WebConfiguration(ServerProperties serverProperties) {
        this.serverProperties = serverProperties;
    }

    /**
     * http status 异常处理器, 拦截response.sendError();
     */
    @Bean
    public BasicErrorController basicErrorController(ErrorAttributes attributes) {
        final Logger LOGGER = LoggerFactory.getLogger(WebConfiguration.class);
        return new BasicErrorController(attributes, serverProperties.getError(), errorViewResolvers) {
            @Override
            public ModelAndView errorHtml(HttpServletRequest request, HttpServletResponse response) {
                Map<String, Object> errorAttributes = new HashMap<>(super.getErrorAttributes(request, false));
                LOGGER.error("出现HTTP错误: {}", errorAttributes);
                String message = (String) errorAttributes.get("message");
                int status = (Integer) errorAttributes.get("status");
                try {
                    // 将 http 异常转换 应用异常
                    WebConfiguration.this.exceptionHandler(request, response, null);
                } catch (Exception e) {
                    LOGGER.error("{}", e.getMessage());
                }
                return null;
            }
        };
    }

    /**
     * 应用异常处理器
     */
    @ExceptionHandler
    public void exceptionHandler(HttpServletRequest request, HttpServletResponse response, Exception ex) throws Exception {
        
    }
}
