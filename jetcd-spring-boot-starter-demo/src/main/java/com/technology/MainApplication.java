package com.technology;
import com.technology.service.JetcdDistributedLock;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.Resource;


@SpringBootApplication
public class MainApplication implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(MainApplication.class, args);
    }

    @Resource
    private JetcdDistributedLock jetcdDistributedLock;
    public void run(String... strings) throws Exception {
        System.out.println(jetcdDistributedLock);
    }
}
