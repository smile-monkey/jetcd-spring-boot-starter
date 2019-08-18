package com.technology;

import com.technology.config.EnableJetcd;
import com.technology.service.JetcdDistributedLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;


@SpringBootApplication
@EnableJetcd
public class MainApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }

    @Autowired
    private JetcdDistributedLock jetcdDistributedLock;
    @Autowired
    private Environment environment;

    public void run(String... strings) throws Exception {
        // TODO yml文件注入有问题!
        System.out.println(environment.getProperty("etcd.config.enabled"));
        System.out.println(environment.getProperty("etcd.config.endpoints"));
        System.out.println(jetcdDistributedLock);
    }
}
