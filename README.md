dubbo-spring-boot-starter
===================================

[源码来自于](https://github.com/alibaba/dubbo-spring-boot-starter/)

支持jdk版本为1.8或者1.8+

### 如何发布dubbo服务
* 添加依赖:

```gradle  
    //指定dubbo版本 
    implementation("com.alibaba:dubbo:2.6.5")
    //指定dubbo-starter版本
    implementation("com.alibaba.spring.boot:dubbo-spring-boot-starter:2.1.2-SNAPSHOT")
```

* 在application.properties添加dubbo的相关配置信息，样例配置如下:

```properties
    spring.application.name=dubbo-spring-boot-starter
    spring.dubbo.server=true
    spring.dubbo.registry=N/A
```

注：这个配置只针对服务提供端，消费端不用指定协议，它自己会根据服务端的地址信息和@Reference注解去解析协议

* 接下来在Spring Boot Application的上添加`@EnableDubboConfiguration`，表示要开启dubbo功能. (dubbo provider服务可以使用或者不使用web容器)

```java
@SpringBootApplication
@EnableDubboConfiguration
public class DubboProviderLauncher {
  //...
}
```

* 编写你的dubbo服务，只需要添加要发布的服务实现上添加`@Service`（import com.alibaba.dubbo.config.annotation.Service）注解，其中interfaceClass是要发布服务的接口.

```java
@Service(interfaceClass = IHelloService.class)
@Component
public class HelloServiceImpl implements IHelloService {
  //...
}
```

* 启动你的Spring Boot应用，观察控制台，可以看到dubbo启动相关信息.


### 如何消费Dubbo服务

* 添加依赖:

```xml
    <dependency>
        <groupId>com.alibaba.spring.boot</groupId>
        <artifactId>dubbo-spring-boot-starter</artifactId>
        <version>2.0.0</version>
    </dependency>
```

* 在application.properties添加dubbo的相关配置信息，样例配置如下:

```properties
spring.application.name=dubbo-spring-boot-starter
```

* 开启`@EnableDubboConfiguration`

```java
@SpringBootApplication
@EnableDubboConfiguration
public class DubboConsumerLauncher {
  //...
}
```

* 通过`@Reference`注入需要使用的interface.

```java
@Component
public class HelloConsumer {
  @Reference(url = "dubbo://127.0.0.1:20880")
  private IHelloService iHelloService;
  
}
```

### 参考文档

* dubbo: http://dubbo.io
* spring-boot: http://projects.spring.io/spring-boot
* dubbo-spring-boot-project: https://github.com/dubbo/dubbo-spring-boot-project
