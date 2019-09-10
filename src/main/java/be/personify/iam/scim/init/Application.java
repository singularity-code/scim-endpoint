package  be.personify.iam.scim.init;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.context.annotation.ComponentScan;

@EnableHypermediaSupport(type = EnableHypermediaSupport.HypermediaType.HAL)
@SpringBootApplication
@ComponentScan( basePackages = { "be.personify.iam"} )
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class, DataSourceTransactionManagerAutoConfiguration.class, HibernateJpaAutoConfiguration.class})
public class Application {
	
	
    public static void main(String[] args) {
    	ConfigurableApplicationContext app = SpringApplication.run(Application.class, args);
    	//ApplicationInitialization init = (ApplicationInitialization)app.getBean("initialization");
		//init.initialize();
    }
    
}
