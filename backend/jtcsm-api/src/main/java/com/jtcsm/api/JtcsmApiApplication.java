package com.jtcsm.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.InetAddress;

/**
 * 今天吃什么 API 启动类
 * <p>
 * 依赖 common 模块提供通用工具类、统一响应结果 RESTful 风格
 * </p>
*/
@SpringBootApplication(scanBasePackages = {"com.jtcsm.api", "com.jtcsm.common"})
public class JtcsmApiApplication {

    private static final Logger log = LoggerFactory.getLogger(JtcsmApiApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(JtcsmApiApplication.class, args);
    }

    @Bean
    public CommandLineRunner startupLog(Environment env) {
        return args -> {
            String port = env.getProperty("server.port", "8080");
            String host = InetAddress.getLocalHost().getHostAddress();
            String contextPath = env.getProperty("server.servlet.context-path", "");
            log.info("JTCSM-API 启动完成 → http://{}:{}{}", host, port, contextPath);
        };
    }
}
