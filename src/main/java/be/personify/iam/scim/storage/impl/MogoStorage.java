package be.personify.iam.scim.storage.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import be.mogo.provisioning.connectors.ConnectorConnection;
import be.mogo.provisioning.connectors.ConnectorPool;
import be.personify.iam.model.provisioning.TargetSystem;
import be.personify.iam.scim.storage.ConfigurationException;
import be.personify.iam.scim.storage.Storage;
import be.personify.util.StringUtils;

public abstract class MogoStorage implements Storage {
	
	private static final Logger logger = LogManager.getLogger(MogoStorage.class);
	
	protected static final String ESCAPED_DOT = "\\.";
	
	
	protected void testConnection(TargetSystem targetSystem) {
		try {
			ConnectorConnection connection = ConnectorPool.getInstance().getConnectorForTargetSystem(targetSystem);
			if ( connection != null) {
				connection.getConnector().ping();
				connection.close();
				logger.info("successfully tested connection");
			}
			else {
				throw new ConfigurationException("can not lease connection");
			}
		}
		catch ( Exception e ) {
			throw new ConfigurationException("can not lease connection " + e.getMessage());
		}
	}
	
	
	
	
	
	
	protected Map<String, String> createDepthMapping(Map<String, String> m) {
		Map<String,String> mm = new HashMap<String,String>();
		for ( String key : m.keySet()) {
			String value = m.get(key);
			if ( value.contains(StringUtils.DOT)) {
				String[] parts = value.split(ESCAPED_DOT);
				if ( parts.length == 2) {
					mm.put(key, value);
				}
				else {
					throw new ConfigurationException("expression is limited to depth of 2 for mapping [" + key + "->" + value + "]");
				}
			}
		}
		return mm;
	}

}
