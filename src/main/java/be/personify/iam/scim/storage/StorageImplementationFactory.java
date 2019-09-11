package be.personify.iam.scim.storage;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PreDestroy;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StorageImplementationFactory {
	
	private static final Logger logger = LogManager.getLogger(StorageImplementationFactory.class);
	
	@Value("${scim.storage.implementation:be.mogo.iam.scim.storage.MemoryStorageImpl}")
	private String storageImplementation;
	
	private Map<String,Storage> storageMap = new HashMap<String, Storage>();
	
	
	/**
	 * returns the storage implementation
	 * @param resourceType
	 * @return
	 */
	public Storage getStorageImplementation( String resourceType ) {
		Storage storage = storageMap.get(resourceType);
		if ( storage == null ) {
			logger.info("initializing storage for type {}", resourceType);
			try {
				Class<?> c = Class.forName(storageImplementation);
				storage = (Storage)c.newInstance();
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
	
	
	@Scheduled(fixedRateString = "${scim.storage.memory.flushToFileEvery}")
	public void flush() {
		for ( Storage storage : storageMap.values()) {
			storage.flush();
		}
	}
	
	
	@PreDestroy
    public void destroy() {
        flush();
    }
	
	

}
