package com.alibaba.dubbo.spring.boot.annotation;

import java.lang.annotation.*;

/**
 * 启动dubbo服务端
 *
 * @author scott
 * @date 2019-02-01
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EnableDubboProviderConfiguration {
}
