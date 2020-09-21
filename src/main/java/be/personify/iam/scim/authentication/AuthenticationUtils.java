package be.personify.iam.scim.authentication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.StringUtils;

import be.personify.iam.scim.util.Constants;
import be.personify.iam.scim.util.PropertyFactory;

public class AuthenticationUtils implements ApplicationContextAware {
	
	
	private static final Logger logger = LogManager.getLogger(AuthenticationUtils.class);
	
	private ApplicationContext applicationContext;
	
	@Value("${scim.authentication.implementation}")
	private String filterImplementation;
	
	@Autowired
	private PropertyFactory propertyFactory;
	
	
	private Map<String,List<String>> basicAuthUsers;
	private Map<String,List<String>> bearerAuthUsers;
	

	
	/**
	 * Gets the userlist for a certain authentication type
	 * @param authenticationType authenticationtype ( basic or bearer )
	 * @return a map containing the user+secret as a key and a list of roles as value
	 */
	private Map<String,List<String>> getUserList(String authenticationType){
		logger.info("initializing users of type {}", authenticationType);
		try {
			Map<String,List<String>> users = new HashMap<String,List<String>>();
			int count = 1;
			String user = null;
			List<String> roles = null;
			while ( (user = propertyFactory.getProperty("scim.authentication.propertyfile.method." + authenticationType + ".user." + count)) != null) {
				String rolesString = propertyFactory.getProperty("scim.authentication.propertyfile.method." + authenticationType + ".user." + count + ".roles");
				logger.info("adding user {} with roles {}", user.split(Constants.COLON)[0], rolesString);
				roles = new ArrayList<String>();
				if ( !StringUtils.isEmpty(rolesString)) {
					roles = Arrays.asList(rolesString.split(Constants.COMMA));
				}
				users.put(user,roles);
				count++;
			}
			logger.info("initializing auth users of type {} done : found {} users", authenticationType, users.size());
			return users;
		}
		catch( Exception e ) {
			logger.error("initializing auth users of type {} ", authenticationType, e);
		}
		return null;
	}
	
	
	
	
	public Filter getFilterImplementation() throws Exception {
		logger.info("trying to initialize authentication filter implementation of type {}", filterImplementation);
		Class<?> c = Class.forName(filterImplementation);
		Filter filter = (Filter)c.newInstance();
		AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
		factory.autowireBean( filter );
		factory.initializeBean( filter, "authenticationFilter");
		logger.info("successfully initialized {}", filterImplementation);
		return filter;
	}
	
	

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
	
	
	
	public Map<String,List<String>> getBasicAuthUsers() {
		if ( basicAuthUsers == null) {
			basicAuthUsers = getUserList(Constants.BASIC.toLowerCase());
		}
		return basicAuthUsers;
	}
	
	public Map<String,List<String>> getBearerAuthUsers() {
		if ( bearerAuthUsers == null) {
			bearerAuthUsers = getUserList(Constants.BEARER.toLowerCase());
		}
		return bearerAuthUsers;
	}
	

}
