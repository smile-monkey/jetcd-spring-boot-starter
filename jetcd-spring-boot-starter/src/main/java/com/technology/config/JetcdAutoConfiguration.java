package com.technology.config;

import com.technology.service.JetcdDistributedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(JetcdDistributedLock.class)
@EnableConfigurationProperties(JetcdProperties.class)
public class JetcdAutoConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(JetcdDistributedLock.class);

    @Autowired
    private JetcdProperties properties;

    @Bean
    @ConditionalOnMissingBean
    JetcdDistributedLock jetcdDistributedLock() {
        return new JetcdDistributedLock(properties.getEndpoints().toArray(new String[]{}));
    }
}
