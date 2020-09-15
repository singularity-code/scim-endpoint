package be.personify.iam.scim.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class PropertyFactory {
	
	private static PropertyFactory _instance = null; 
	
	private static final Logger logger = LogManager.getLogger(PropertyFactory.class);
	
	private Properties properties;
	
	public static synchronized PropertyFactory getInstance() {
		if ( _instance == null ) {
			_instance = new PropertyFactory();
			try {
				_instance.initialize();
			} catch (Exception e) {
				logger.error("can not instanciate property factory", e);
				_instance = null;
			}
		}
		return _instance;
	}
	

	private void initialize() throws Exception {
		InputStream is = PropertyFactory.class.getResourceAsStream("/application.properties");
		properties = new Properties();
		properties.load(is);
	}
	
	
	
	public String getProperty( String key ) {
		return properties.getProperty(key);
	}
	
	
	public List<String> getPropertyKeysStartingWith( String s ){
		List<String> keys = new ArrayList<String>();
		for ( Object k : properties.keySet()) {
			String kk = (String)k;
			if ( kk.startsWith(s)){
				keys.add(kk);
			}
		}
		return keys;
	}
	
	
	

}
