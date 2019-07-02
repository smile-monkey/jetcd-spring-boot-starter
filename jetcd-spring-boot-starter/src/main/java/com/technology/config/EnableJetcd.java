package com.technology.config;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(JetcdAutoConfiguration.class)  //采用导入方式，将JetcdAutoConfiguration托管。用户手动开启starter包的自动配置功能; 等价于spring.factories
public @interface EnableJetcd {

}
