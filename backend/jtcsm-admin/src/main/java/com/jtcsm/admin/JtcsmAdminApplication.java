package com.jtcsm.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.InetAddress;

/**
 * JTCSM 管理后台启动类
 * <p>
 * 依赖 common 模块提供通用工具类和统一响应
 * </p>
 */
@SpringBootApplication(scanBasePackages = {"com.jtcsm.admin", "com.jtcsm.common"})
public class JtcsmAdminApplication {

    private static final Logger log = LoggerFactory.getLogger(JtcsmAdminApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(JtcsmAdminApplication.class, args);
    }

    @Bean
    public CommandLineRunner startupLog(Environment env) {
        return args -> {
            String port = env.getProperty("server.port", "8080");
            String host = InetAddress.getLocalHost().getHostAddress();
            String contextPath = env.getProperty("server.servlet.context-path", "");
            log.info("JTCSM-ADMIN 启动完成 → http://{}:{}{}", host, port, contextPath);
        };
    }
}
