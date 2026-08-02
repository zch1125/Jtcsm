package com.jtcsm.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.sql.init.SqlInitializationAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.net.InetAddress;

/**
 * API 网关启动类
 * <p>
 * 统一入口，负责路由分发、JWT 鉴权、CORS、限流等跨切面关注点。
 * 网关使用 WebFlux（Reactive），因此排除 WebMVC、DataSource 等不相关的自动配置。
 * </p>
 */
@SpringBootApplication(exclude = {
    WebMvcAutoConfiguration.class,
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    SqlInitializationAutoConfiguration.class
})
@ComponentScan(basePackages = "com.jtcsm",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.REGEX,
        pattern = "com\\.jtcsm\\.common\\.(exception|interceptor)\\..*"
    )
)
public class JtcsmGatewayApplication {

    private static final Logger log = LoggerFactory.getLogger(JtcsmGatewayApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(JtcsmGatewayApplication.class, args);
    }

    @Bean
    public CommandLineRunner startupLog(Environment env) {
        return args -> {
            String port = env.getProperty("server.port", "8080");
            String host = InetAddress.getLocalHost().getHostAddress();
            String contextPath = env.getProperty("server.servlet.context-path", "");
            log.info("JTCSM-GATEWAY 启动完成 → http://{}:{}{}", host, port, contextPath);
        };
    }
}
