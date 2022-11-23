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
package be.personify.iam.scim.authentication;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import be.personify.iam.scim.util.Constants;
import be.personify.iam.scim.util.PropertyFactory;
import be.personify.util.StringUtils;

public class AuthenticationUtils implements ApplicationContextAware {

	private static final Logger logger = LogManager.getLogger(AuthenticationUtils.class);

	private ApplicationContext applicationContext;

	@Value("${scim.authentication.implementation}")
	private String filterImplementation;

	@Autowired
	private PropertyFactory propertyFactory;

	private Map<String, Consumer> basicAuthUsers;
	private Map<String, Consumer> bearerAuthUsers;


	
	/**
	 * Gets the consumer list for a certain authentication type
	 * 
	 * @param authenticationType a
	 * @return map containing the consumers
	 */
	private Map<String, Consumer> getConsumerList(String authenticationType) {
		logger.info("initializing users of type {}", authenticationType);
		try {
			Map<String, Consumer> consumers = new HashMap<String, Consumer>();
			int count = 1;
			String user = null;
			List<String> roles = null;
			Consumer consumer = null;
			while ((user = propertyFactory.getProperty("scim.authentication.propertyfile.method." + authenticationType + ".user." + count)) != null) {
				String[] splitted = user.split(StringUtils.COLON);
				if ( splitted.length == 2) {
					consumer = new Consumer(splitted[0], splitted[1]);
				}
				else {
					throw new RuntimeException("consumer identifier has to be following format clientid:secret");
				}
				
				//roles
				String rolesString = propertyFactory.getProperty("scim.authentication.propertyfile.method." + authenticationType + ".user." + count + ".roles");
				logger.info("adding user {} with roles {}", user.split(StringUtils.COLON)[0], rolesString);
				roles = new ArrayList<String>();
				if (!StringUtils.isEmpty(rolesString)) {
					roles = Arrays.asList(rolesString.split(StringUtils.COMMA));
				}
				consumer.setRoles(roles);
				//tenant
				String tenantString = propertyFactory.getProperty("scim.authentication.propertyfile.method." + authenticationType + ".user." + count + ".tenant");
				consumer.setTenant(tenantString);
				
				consumers.put(splitted[0], consumer);
				count++;
				
			}
			logger.info("initializing auth users of type {} done : found {} users", authenticationType, consumers.size());
			return consumers;
		}
		catch (Exception e) {
			logger.error("initializing auth users of type {} ", authenticationType, e);
		}
		return null;
	}
	
	

	/**
	 * Gets the filter implementation
	 * 
	 * @return the filter
	 * @throws Exception
	 */
	public Filter getFilterImplementation() throws Exception {
		logger.info("trying to initialize authentication filter implementation of type {}", filterImplementation);
		Class<?> c = Class.forName(filterImplementation);
		Filter filter = (Filter) c.getDeclaredConstructor().newInstance();
		AutowireCapableBeanFactory factory = applicationContext.getAutowireCapableBeanFactory();
		factory.autowireBean(filter);
		factory.initializeBean(filter, "authenticationFilter");
		logger.info("successfully initialized {}", filterImplementation);
		return filter;
	}

	
	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	
	/**
	 * Gets a map with the basic auth consumers
	 * 
	 * @return a map containing the consumers of type basic auth
	 */
	public Map<String, Consumer> getBasicAuthConsumers() {
		if (basicAuthUsers == null) {
			basicAuthUsers = getConsumerList(Constants.BASIC.toLowerCase());
		}
		return basicAuthUsers;
	}

	/**
	 * Gets a map with the consumers of type bearer
	 * 
	 * @return a map containing the consumers of type bearer
	 */
	public Map<String, Consumer> getBearerAuthConsumers() {
		if (bearerAuthUsers == null) {
			bearerAuthUsers = getConsumerList(Constants.BEARER.toLowerCase());
		}
		return bearerAuthUsers;
	}
}
