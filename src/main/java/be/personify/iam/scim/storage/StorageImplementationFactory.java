/*
*     Copyright 2019-2022 Wouter Van der Beken @ https://personify.be
*
* Generated software by personify.be

* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*          http://www.apache.org/licenses/LICENSE-2.0
*
 * Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package be.personify.iam.scim.storage;

import be.personify.iam.scim.schema.Schema;
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

@Component
public class StorageImplementationFactory implements ApplicationContextAware {

	private static final Logger logger = LogManager.getLogger(StorageImplementationFactory.class);

	@Value("${scim.storage.implementation}")
	private String storageImplementation;

	private Map<String, Storage> storageMap = new HashMap<String, Storage>();

	private ApplicationContext applicationContext = null;

	/**
	 * Gets the storage implementation
	 *
	 * @param schema the schema
	 * @return the storage
	 */
	public synchronized Storage getStorageImplementation(Schema schema) {
		String resourceType = schema.getName();
		Storage storage = storageMap.get(resourceType);
		if (storage == null) {
			logger.info("using environment variable [scim.storage.implementation]");
			logger.info("initializing storage for type {} with implementation {}", resourceType, storageImplementation);
			try {
				Class<?> c = Class.forName(storageImplementation);
				storage = (Storage) c.getDeclaredConstructor().newInstance();
				AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
				factory.autowireBean(storage);
				factory.initializeBean(storage, "storage" + resourceType);
				storage.initialize(resourceType);
				storageMap.put(resourceType, storage);
				logger.info("storage for type {} initialized", resourceType);
				logger.info("is this storage tenant enabled : {}", storage.tenantCompatible());
			}
			catch (ClassNotFoundException cnfe) {
				logger.error("error initializing storage implementation " + storageImplementation, cnfe);
				throw new ConfigurationException("the storage implementation class [" + storageImplementation + "] is not found");
			} 
			catch (Exception e) {
				logger.error("error initializing storage for type " + resourceType, e);
				throw new ConfigurationException("error configuring [" + storageImplementation + "] " + e.getMessage());
			}
		}
		return storage;
	}

	@Scheduled(fixedRateString = "${scim.storage.flushEvery}")
	public void flush() {
		for (Storage storage : storageMap.values()) {
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
