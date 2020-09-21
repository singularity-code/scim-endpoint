package be.personify.iam.scim.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

public class PropertyFactory {
	
	@Autowired
	private Environment env;
	
	private static final Logger logger = LogManager.getLogger(PropertyFactory.class);
	
	
	
	public String getProperty( String key ) {
		return env.getProperty(key);
	}
	
	
	public String resolvePlaceHolder( String text ) {
		return env.resolvePlaceholders(text);
	}
	
	
	

}
