package be.personify.iam.scim.init;

import javax.servlet.Filter;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import be.personify.iam.scim.authentication.AuthenticationUtils;
import be.personify.iam.scim.util.CryptUtils;
import be.personify.iam.scim.util.PropertyFactory;
import be.personify.iam.scim.util.TokenUtils;

@Configuration
@EnableScheduling
@EnableAsync
public class SpringConfig{

	@Bean
	public FilterRegistrationBean<Filter> authenticationFilters(){
	    FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();     
	    //PropertyFileAuthenticationFilter authenticationFilter = new PropertyFileAuthenticationFilter();
	    Filter filter;
		try {
			filter = authenticationUtils().getFilterImplementation();
			registrationBean.setFilter(filter);
		    return registrationBean;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	        
	}
	
	
	@Bean
	public CryptUtils cryptUtils(){
		return new CryptUtils();
	}
	
	@Bean
	public TokenUtils tokenUtils(){
		return new TokenUtils();
	}
	
	
	@Bean
	public AuthenticationUtils authenticationUtils(){
		return new AuthenticationUtils();
	}
	
	
	@Bean
	public PropertyFactory propertyFactgory(){
		return new PropertyFactory();
	}
	
	
	
	
	
}
