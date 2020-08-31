package be.personify.iam.scim.init;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import be.personify.iam.scim.util.BasicAuthenticationFilter;

@Configuration
@EnableScheduling
@EnableAsync
public class SpringConfig{

	@Bean
	public FilterRegistrationBean<BasicAuthenticationFilter> authenticationFilters(){
	    FilterRegistrationBean<BasicAuthenticationFilter> registrationBean = new FilterRegistrationBean<>();     
	    BasicAuthenticationFilter authenticationFilter = new BasicAuthenticationFilter();
	    registrationBean.setFilter(authenticationFilter);
	    return registrationBean;    
	}
    
    
}
