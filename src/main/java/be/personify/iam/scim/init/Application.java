package  be.personify.iam.scim.init;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan( basePackages = { "be.personify.iam.scim"} )
@EnableAutoConfiguration()
public class Application {
		
    public static void main(String[] args) {
    	SpringApplication.run(Application.class, args);
    }
    
}
