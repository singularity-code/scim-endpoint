package be.personify.iam.scim.util;

import java.util.Arrays;
import java.util.Properties;
import java.util.stream.StreamSupport;

import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;

public class PropertiesUtil {
	
	
	
	public static Properties getPropertiesFromEnv(Environment environment) {
		Properties props = new Properties();
		MutablePropertySources propSrcs = ((AbstractEnvironment)environment).getPropertySources();
		StreamSupport.stream(propSrcs.spliterator(), false)
		        .filter(ps -> ps instanceof EnumerablePropertySource)
		        .map(ps -> ((EnumerablePropertySource) ps).getPropertyNames())
		        .flatMap(Arrays::<String>stream)
		        .forEach(propName -> props.setProperty(propName, environment.getProperty(propName)));
		return props;
	}
	

}
