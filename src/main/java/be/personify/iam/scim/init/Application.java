package  be.personify.iam.scim.init;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main spring-boot application for the JAVA SCIM server implementation
 * @author wouter
 *
 */
@SpringBootApplication
@ComponentScan( basePackages = { "be.personify.iam.scim"} )
@EnableAutoConfiguration(exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class})
public class Application {
		
    public static void main(String[] args) {
    	SpringApplication.run(Application.class, args);
    }
    
}
