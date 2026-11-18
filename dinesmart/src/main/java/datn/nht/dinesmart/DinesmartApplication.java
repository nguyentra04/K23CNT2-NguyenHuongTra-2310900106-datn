package datn.nht.dinesmart;


import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class DinesmartApplication {

    public static void main(String[] args) {
        SpringApplication.run(DinesmartApplication.class, args);
    }

    /** Đồng bộ múi giờ JVM với SQL Server (SYSDATETIME() là giờ local của máy chủ DB). */
    @PostConstruct
    void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }
}