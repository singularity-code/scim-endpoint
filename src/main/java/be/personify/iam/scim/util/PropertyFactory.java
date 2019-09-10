package be.personify.iam.scim.util;

import java.io.InputStream;
import java.util.Properties;

public class PropertyFactory {
	
	private static PropertyFactory _instance = null; 
	
	private Properties properties;
	
	public static synchronized PropertyFactory getInstance() {
		if ( _instance == null ) {
			_instance = new PropertyFactory();
			try {
				_instance.initialize();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
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
	
	
	

}
