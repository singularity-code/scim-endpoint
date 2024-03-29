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
package be.personify.iam.scim.rest;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.text.StringSubstitutor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import be.personify.iam.scim.schema.SchemaReader;
import be.personify.iam.scim.util.Constants;
import be.personify.iam.scim.util.PropertiesUtil;
import be.personify.util.io.IOUtils;

/**
 * Discovery controller for the SCIM server
 *
 * @author wouter
 */
@RestController
public class DiscoveryController extends Controller {

	private static final Logger logger = LogManager.getLogger(DiscoveryController.class);

	private Map<String, Object> serviceProviderConfig = null;
	private List<Object> resourceTypes = null;
	private List<Object> schemas = null;

	//@Autowired
	//private SchemaReader schemaReader;
	
	@Autowired
	private ResourceLoader resourceLoader;
	
	@Value("${scim.schemas.location}")
	private String schemasLocation;
	
	@Value("${scim.resourceTypes.location}")
	private String resourceTypesLocation;
	
	@Value("${scim.serviceProvider.location}")
	private String serviceProviderLocation;
	
	//@Autowired
	//private Environment env;
	
	


	@GetMapping(path = "/scim/v2/ServiceProviderConfig", produces = { "application/scim+json", "application/json" })
	public ResponseEntity<?> getServiceProviderConfig(HttpServletRequest request, HttpServletResponse response) {

		long start = System.currentTimeMillis();
		ResponseEntity<?> result = null;
		try {
			if (serviceProviderConfig == null) {
				Resource resource = resourceLoader.getResource(serviceProviderLocation);
				String content = new String(IOUtils.readFileAsBytes(resource.getInputStream()));
				content = StringSubstitutor.replace(content, PropertiesUtil.getPropertiesFromEnv(env));
				serviceProviderConfig = Constants.objectMapper.readValue(content, Map.class);
			}
			result = new ResponseEntity<Map<String, Object>>(serviceProviderConfig, HttpStatus.OK);
		}
		catch (IOException e) {
			logger.error("can not read service provider config", e);
			result = new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		logger.info("ServiceProviderConfig fetched in {} ms", (System.currentTimeMillis() - start));

		return result;
	}

	
	@GetMapping(path = "/scim/v2/ResourceTypes", produces = { "application/scim+json", "application/json" })
	public ResponseEntity<?> getResourceTypes(HttpServletRequest request, HttpServletResponse response) {

		long start = System.currentTimeMillis();
		ResponseEntity<?> result = null;
		try {
			if (resourceTypes == null) {
				Resource resource = resourceLoader.getResource(resourceTypesLocation);
				String content = new String(IOUtils.readFileAsBytes(resource.getInputStream()));
				content = StringSubstitutor.replace(content, PropertiesUtil.getPropertiesFromEnv(env));
				resourceTypes = Constants.objectMapper.readValue(content, List.class);
			}
			result = new ResponseEntity<List<Object>>(resourceTypes, HttpStatus.OK);
		}
		catch (IOException e) {
			logger.error("can not read resource types", e);
			result = new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		logger.info("resource types fetched in {} ms", (System.currentTimeMillis() - start));

		return result;
	}
	
	

	@GetMapping(path = "/scim/v2/Schemas", produces = { "application/scim+json", "application/json" })
	public ResponseEntity<?> getSchemas(HttpServletRequest request, HttpServletResponse response) {

		long start = System.currentTimeMillis();
		ResponseEntity<?> result = null;
		try {
			if (schemas == null) {
				schemas = schemaReader.getSchemas();
			}
			result = new ResponseEntity<List<Object>>(schemas, HttpStatus.OK);
		} 
		catch (Exception e) {
			logger.error("can not read schemas", e);
			result = new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
		}
		logger.info("schemas fetched in {} ms", (System.currentTimeMillis() - start));

		return result;
	}
	
	
	
	
	
	
	
	
	
}
