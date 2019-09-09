package be.mogo.iam.scim.util;

import java.io.FileInputStream;
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
		String rootPath = Thread.currentThread().getContextClassLoader().getResource(Constants.EMPTY).getPath();
		String appConfigPath = rootPath + "application.properties";
		properties = new Properties();
		properties.load(new FileInputStream(appConfigPath));
	}
	
	
	
	public String getProperty( String key ) {
		return properties.getProperty(key);
	}
	
	
	

}
