package be.personify.iam.scim.init;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import be.personify.iam.scim.storage.Storage;
import be.personify.iam.scim.util.AuthenticationFilter;
import be.personify.iam.scim.util.CryptUtils;
import be.personify.iam.scim.util.TokenUtils;

@Configuration
@EnableScheduling
@EnableAsync
public class SpringConfig{

	@Bean
	public FilterRegistrationBean<AuthenticationFilter> authenticationFilters(){
	    FilterRegistrationBean<AuthenticationFilter> registrationBean = new FilterRegistrationBean<>();     
	    AuthenticationFilter authenticationFilter = new AuthenticationFilter();
	    authenticationFilter.setTokenUtils(tokenUtils());
	    registrationBean.setFilter(authenticationFilter);
	    return registrationBean;    
	}
	
	
	@Bean
	public CryptUtils cryptUtils(){
		return new CryptUtils();
	}
	
	@Bean
	public TokenUtils tokenUtils(){
		return new TokenUtils();
	}
	
}
