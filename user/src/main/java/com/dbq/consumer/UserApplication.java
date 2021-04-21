package com.dbq.consumer;

import com.dbq.common.base.BaseMapper;
import com.dbq.common.config.MybatisConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Import;

@EnableEurekaClient
@Import(MybatisConfiguration.class)
@EnableFeignClients("com.dbq.common.export.account")
@MapperScan(basePackages = "com.springcloud.consumer.mapper", markerInterface = BaseMapper.class)
@SpringBootApplication(scanBasePackages = {"com.dbq.common.export.account", "com.springcloud.consumer"})
public class UserApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }

}