package com.alibaba.dubbo.spring.boot.annotation;

import java.lang.annotation.*;

/**
 * Enable Dubbo (for provider or consumer) for spring boot application
 * 启用dubbo服务端&&消费端
 *
 * @author xionghui
 * @version 1.0.0
 * @since 1.0.0
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@EnableDubboConsumerConfiguration
@EnableDubboProviderConfiguration
@Documented
public @interface EnableDubboConfiguration {

}
