package be.personify.iam.scim.storage;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PreDestroy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import be.personify.iam.scim.schema.Schema;

@Component
public class StorageImplementationFactory implements ApplicationContextAware {
	
	private static final Logger logger = LogManager.getLogger(StorageImplementationFactory.class);
	
	@Value("${scim.storage.implementation}")
	private String storageImplementation;
	
	private Map<String,Storage> storageMap = new HashMap<String, Storage>();
	
	private ApplicationContext applicationContext = null;
	
	
	/**
	 * Gets the storage implementation
	 * @param schema the schema
	 * @return the storage
	 */
	public synchronized Storage getStorageImplementation( Schema schema ) {
		String resourceType = schema.getName();
		Storage storage = storageMap.get(resourceType);
		if ( storage == null ) {
			logger.info("initializing storage for type {}", resourceType);
			try {
				Class<?> c = Class.forName(storageImplementation);
				storage = (Storage)c.newInstance();
				AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
				factory.autowireBean( storage );
				factory.initializeBean( storage, "storage" + resourceType );
				storage.initialize(resourceType);
				storageMap.put(resourceType, storage);
				logger.info("storage for type {} initialized", resourceType);
			}
			catch (Exception e) {
				logger.error("error initializing storage for type " + resourceType, e);
			}
		}
		return storage;
	}
	
	
	@Scheduled(fixedRateString = "${scim.storage.flushEvery}")
	public void flush() {
		for ( Storage storage : storageMap.values()) {
			storage.flush();
		}
	}
	
	
	@PreDestroy
    public void destroy() {
        flush();
    }


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
		
	}
	
	

}
