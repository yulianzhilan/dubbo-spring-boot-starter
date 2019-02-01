package com.alibaba.dubbo.spring.boot;

import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.dubbo.config.spring.ServiceBean;
import com.alibaba.dubbo.spring.boot.annotation.EnableDubboProviderConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * DubboProviderAutoConfiguration
 *
 * @author xionghui
 * @author 韩旺坤
 * @version 2.0.0
 * @since 1.0.0
 */
@Configuration
@ConditionalOnClass(Service.class)
@ConditionalOnBean(annotation = EnableDubboProviderConfiguration.class)
@AutoConfigureAfter(DubboAutoConfiguration.class)
@EnableConfigurationProperties(DubboProperties.class)
public class DubboProviderAutoConfiguration extends DubboCommonAutoConfiguration implements ApplicationContextAware {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private ApplicationContext applicationContext;

    private DubboProperties properties;

    @PostConstruct
    public void init() throws Exception {
        Map<String, Object> beanMap = this.applicationContext.getBeansWithAnnotation(Service.class);
        if (beanMap != null && beanMap.size() > 0) {
            this.initIdConfigMap(this.properties);
            for (Map.Entry<String, Object> entry : beanMap.entrySet()) {
                this.initProviderBean(entry.getKey(), entry.getValue());
            }
        }
    }

    private void initProviderBean(String beanName, Object bean) throws Exception {
        Service service = this.applicationContext.findAnnotationOnBean(beanName, Service.class);
        ServiceBean<Object> serviceConfig = new ServiceBean<Object>(service);
        if ((service.interfaceClass() == null || service.interfaceClass() == void.class)
                && (service.interfaceName() == null || "".equals(service.interfaceName()))) {
            Class<?>[] interfaces = bean.getClass().getInterfaces();
            if (interfaces.length > 0) {
                serviceConfig.setInterface(interfaces[0]);
            }
        }

        Environment environment = this.applicationContext.getEnvironment();
        String application = service.application();
        serviceConfig.setApplication(this.parseApplication(application, this.properties, environment,
                beanName, "application", application));
        String module = service.module();
        serviceConfig.setModule(
                this.parseModule(module, this.properties, environment, beanName, "module", module));
        String[] registries = service.registry();
        serviceConfig.setRegistries(
                this.parseRegistries(registries, this.properties, environment, beanName, "registry"));
        String[] protocols = service.protocol();
        serviceConfig.setProtocols(
                this.parseProtocols(protocols, this.properties, environment, beanName, "registry"));
        String monitor = service.monitor();
        serviceConfig.setMonitor(
                this.parseMonitor(monitor, this.properties, environment, beanName, "monitor", monitor));
        String provider = service.provider();
        serviceConfig.setProvider(
                this.parseProvider(provider, this.properties, environment, beanName, "provider", provider));

        serviceConfig.setApplicationContext(this.applicationContext);
        serviceConfig.afterPropertiesSet();
        serviceConfig.setRef(bean);
        serviceConfig.export();
        if (logger.isDebugEnabled()) {
            logger.debug("initial provider bean: " + beanName);
        }
    }

    @Override
    protected String buildErrorMsg(String... errors) {
        if (errors == null || errors.length != 3) {
            return super.buildErrorMsg(errors);
        }
        return new StringBuilder().append("beanName=").append(errors[0]).append(", ").append(errors[1])
                .append("=").append(errors[2]).append(" not found in multi configs").toString();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Autowired
    public void setProperties(DubboProperties properties) {
        this.properties = properties;
    }
}
