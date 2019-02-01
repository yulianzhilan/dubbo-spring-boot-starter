package com.alibaba.dubbo.spring.boot;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.spring.ReferenceBean;
import com.alibaba.dubbo.spring.boot.annotation.EnableDubboConsumerConfiguration;
import com.alibaba.dubbo.spring.boot.bean.ClassIdBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DubboConsumerAutoConfiguration
 *
 * @author xionghui
 * @version 2.0.0
 * @since 1.0.0
 */
@Configuration
@ConditionalOnClass(Reference.class)
@ConditionalOnBean(annotation = EnableDubboConsumerConfiguration.class)
@EnableConfigurationProperties(DubboProperties.class)
@AutoConfigureAfter(DubboAutoConfiguration.class)
public class DubboConsumerAutoConfiguration extends DubboCommonAutoConfiguration implements ApplicationContextAware {
    private static final Map<ClassIdBean, Object> DUBBO_REFERENCES_MAP =
            new ConcurrentHashMap<ClassIdBean, Object>();

    private Logger logger = LoggerFactory.getLogger(getClass());

    private ApplicationContext applicationContext;

    private DubboProperties properties;

    public static Object getDubboReference(ClassIdBean classIdBean) {
        return DUBBO_REFERENCES_MAP.get(classIdBean);
    }

    /**
     * spring bean post processor
     * 对bean的初始化进行人工干预
     */
    @Bean
    @Order
    public BeanPostProcessor beanPostProcessor() {
        return new BeanPostProcessor() {

            /**
             * 初始化之前，将有{@link com.alibaba.dubbo.config.annotation.Reference}注解的属性，手动注入代理类
             *
             * @param bean
             * @param beanName
             * @return
             * @throws BeansException
             */
            @Override
            public Object postProcessBeforeInitialization(Object bean, String beanName)
                    throws BeansException {
                Class<?> objClz;
                if (AopUtils.isAopProxy(bean)) {
                    objClz = AopUtils.getTargetClass(bean);
                } else {
                    objClz = bean.getClass();
                }

                try {
                    List<Field> fieldList = new ArrayList<>();
                    handleSuperClass(objClz, fieldList);
                    for (Field field : fieldList) {
                        Reference reference = field.getAnnotation(Reference.class);
                        DubboConsumerAutoConfiguration.this
                                .initIdConfigMap(DubboConsumerAutoConfiguration.this.properties);
                        ReferenceBean<?> referenceBean =
                                DubboConsumerAutoConfiguration.this.getConsumerBean(beanName, field, reference);
                        Class<?> interfaceClass = referenceBean.getInterfaceClass();
                        String group = referenceBean.getGroup();
                        String version = referenceBean.getVersion();
                        ClassIdBean classIdBean = new ClassIdBean(interfaceClass, group, version);
                        Object dubboReference =
                                DubboConsumerAutoConfiguration.DUBBO_REFERENCES_MAP.get(classIdBean);
                        if (dubboReference == null) {
                            synchronized (this) {
                                // double check
                                dubboReference =
                                        DubboConsumerAutoConfiguration.DUBBO_REFERENCES_MAP.get(classIdBean);
                                if (dubboReference == null) {
                                    referenceBean.afterPropertiesSet();
                                    // dubboReference should not be null, otherwise it will cause
                                    // NullPointerException
                                    dubboReference = referenceBean.getObject();
                                    DubboConsumerAutoConfiguration.DUBBO_REFERENCES_MAP.put(classIdBean,
                                            dubboReference);
                                }
                            }
                        }
                        field.setAccessible(true);
                        field.set(bean, dubboReference);
                        if (logger.isDebugEnabled()) {
                            logger.debug("initial consumer bean: " + beanName + " -> " + field.getName());
                        }
                    }
                } catch (Exception e) {
                    throw new BeanCreationException(beanName, e);
                }
                return bean;
            }

            /**
             * 处理父类引用问题
             *   abstract class @Reference {@see NullPointException}
             *   info: objClz.getDeclaredFields() can only get all the field for current class, but cannot access the super class
             *   option 1: try to get superClass's field and set proxy bean at one of the Sons;
             *   option 2: handle the abstract superClass reference at end of bean initialization;
             * @author scott
             * @date 2019-02-01
             */
            private void handleSuperClass(Class target, List<Field> fields) {
                if (target != null) {
                    Class superclass = target.getSuperclass();
                    if (superclass != null && !superclass.getSimpleName().equals(Object.class.getSimpleName())) {
                        Field[] declaredFields = superclass.getDeclaredFields();
                        if (declaredFields.length > 0) {
                            for (Field field : declaredFields) {
                                Reference reference = field.getAnnotation(Reference.class);
                                if (reference != null && !fields.contains(field)) {
                                    fields.add(field);
                                }
                            }
                        }
                        handleSuperClass(superclass, fields);
                    }
                }
            }

            /**
             * 初始化之后不做任何处理
             *
             * @param bean
             * @param beanName
             * @return
             * @throws BeansException
             */
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName)
                    throws BeansException {
                return bean;
            }
        };
    }

    /**
     * init consumer bean
     *
     * @param reference reference
     * @return ReferenceBean<T>
     * @throws BeansException BeansException
     */
    private <T> ReferenceBean<T> getConsumerBean(String beanName, Field field, Reference reference)
            throws BeansException {
        ReferenceBean<T> referenceBean = new ReferenceBean<T>(reference);
        if ((reference.interfaceClass() == null || reference.interfaceClass() == void.class)
                && (reference.interfaceName() == null || "".equals(reference.interfaceName()))) {
            referenceBean.setInterface(field.getType());
        }

        Environment environment = this.applicationContext.getEnvironment();
        String application = reference.application();
        referenceBean.setApplication(this.parseApplication(application, this.properties, environment,
                beanName, field.getName(), "application", application));
        String module = reference.module();
        referenceBean.setModule(this.parseModule(module, this.properties, environment, beanName,
                field.getName(), "module", module));
        String[] registries = reference.registry();
        referenceBean.setRegistries(this.parseRegistries(registries, this.properties, environment,
                beanName, field.getName(), "registry"));
        String monitor = reference.monitor();
        referenceBean.setMonitor(this.parseMonitor(monitor, this.properties, environment, beanName,
                field.getName(), "monitor", monitor));
        String consumer = reference.consumer();
        referenceBean.setConsumer(this.parseConsumer(consumer, this.properties, environment, beanName,
                field.getName(), "consumer", consumer));

        referenceBean.setApplicationContext(DubboConsumerAutoConfiguration.this.applicationContext);
        return referenceBean;
    }

    @Override
    protected String buildErrorMsg(String... errors) {
        if (errors == null || errors.length != 4) {
            return super.buildErrorMsg(errors);
        }
        return new StringBuilder().append("beanName=").append(errors[0]).append(", field=")
                .append(errors[1]).append(", ").append(errors[2]).append("=").append(errors[3])
                .append(" not found in multi configs").toString();
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
