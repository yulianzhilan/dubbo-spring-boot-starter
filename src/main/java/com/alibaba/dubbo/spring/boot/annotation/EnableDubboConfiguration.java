package com.alibaba.dubbo.spring.boot.annotation;

import java.lang.annotation.*;

/**
 * Enable Dubbo (for provider or consumer) for spring boot application
 *
 * @author xionghui
 * @version 1.0.0
 * @since 1.0.0
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnableDubboConfiguration {

}
