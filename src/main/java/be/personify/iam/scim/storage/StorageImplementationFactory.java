package be.personify.iam.scim.storage;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class StorageImplementationFactory {
	
	@Value("${scim.storage.implementation:be.mogo.iam.scim.storage.MemoryStorageImpl}")
	private String storageImplementation;
	
	private Storage storage = null;
	
	
	public Storage getStorageImplementation() {
		if ( storage == null ) {
			try {
				Class<?> c = Class.forName(storageImplementation);
				storage = (Storage)c.newInstance();
				storage.initialize();
			}
			catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
			catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return storage;
		
		
	}
	
	
	@Scheduled(fixedRateString = "${scim.storage.memory.flushToFileEvery}")
	public void flush() {
		if ( storage != null) {
        	storage.flush();
        }
	}
	
	
	
	
	
	@PreDestroy
    public void destroy() {
        if ( storage != null) {
        	storage.flush();
        }
    }

}
