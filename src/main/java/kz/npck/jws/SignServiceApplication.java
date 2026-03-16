package kz.npck.jws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SignServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SignServiceApplication.class, args);
    }

}
