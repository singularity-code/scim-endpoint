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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.util.UriComponentsBuilder;

import be.personify.iam.scim.authentication.CurrentConsumer;
import be.personify.iam.scim.schema.Schema;
import be.personify.iam.scim.schema.SchemaAttribute;
import be.personify.iam.scim.schema.SchemaException;
import be.personify.iam.scim.schema.SchemaReader;
import be.personify.iam.scim.schema.SchemaResourceType;
import be.personify.iam.scim.storage.ConfigurationException;
import be.personify.iam.scim.storage.ConstraintViolationException;
import be.personify.iam.scim.storage.DataException;
import be.personify.iam.scim.storage.Storage;
import be.personify.iam.scim.storage.StorageImplementationFactory;
import be.personify.iam.scim.util.Constants;
import be.personify.iam.scim.util.PropertyFactory;
import be.personify.iam.scim.util.ScimErrorType;
import be.personify.util.SearchCriteria;
import be.personify.util.SearchCriteriaUtil;
import be.personify.util.SearchCriterium;
import be.personify.util.SearchOperation;
import be.personify.util.StringUtils;
import be.personify.util.exception.InvalidFilterException;
import be.personify.util.scim.PatchOperation;
import be.personify.util.scim.ScimMutability;

/**
 * Main controller class for the SCIM operations
 *
 * @author vanderw
 */
public class Controller {

	private static final String SORT_ORDER = "SortOrder";
	private static final String SORT_BY = "SortBy";

	private static final String SCHEMA_VALIDATION = "schema validation : ";

	private static final Logger logger = LogManager.getLogger(Controller.class);

	@Value("${scim.allowIdOnCreate:true}")
	private boolean allowIdOnCreate;
	
	@Value("${scim.returnGroupsOnUserGet:true}")
	private boolean returnGroupsOnUserGet;
	
	@Value("${scim.returnGroupsOnUserSearch:true}")
	private boolean returnGroupsOnUserSearch;
	
	@Value("${scim.returnGroupsOnUser.max:100}")
	private int returnGroupsMax;
	
	@Value("${scim.returnGroupsOnUser.includedFields:id,displayName}")
	private String returnGroupsIncludedFields;
	
	@Autowired
	private PropertyFactory propertyFactory;
	
	private Map<String,String> defaultSort = new HashMap<String,String>();
	
	private List<String> returnGroupsIncludedFieldsArray = null;

	@Autowired
	private StorageImplementationFactory storageImplementationFactory;
	
	private SearchCriteriaUtil searchCriteriaUtil = new SearchCriteriaUtil();

	@Autowired
	protected SchemaReader schemaReader;
	
	@Autowired
	private PatchUtils patchUtils;
	
	@Autowired
	protected Environment env;

	
	//POST
	protected ResponseEntity<Map<String, Object>> post(Map<String, Object> entity, HttpServletRequest request, HttpServletResponse response, SchemaResourceType resourceType, String attributes, String excludedAttributes) {
		long start = System.currentTimeMillis();
		try {
			
			Schema schema = resourceType.getSchemaObject();
			// validate
			schemaReader.validate(resourceType, entity, true, request.getMethod());
			
			// prepare
			String id = createId(entity);
			entity.put(Constants.ID, id);
			String location = UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request)).build().toUriString() + StringUtils.SLASH + id;
			createMeta(new Date(), id, entity, schema.getName(), location);
			response.addHeader(Constants.HEADER_LOCATION, location);

			// store and return
			storageImplementationFactory.getStorageImplementation(schema).create(id, entity, CurrentConsumer.getCurrent());
			logger.info("resource of type {} with id {} created in {} ms", schema.getName(), id, (System.currentTimeMillis() - start));
			return new ResponseEntity<>(filterAttributes(schema, entity, getListFromString(attributes), excludedAttributes), HttpStatus.CREATED);

		}
		catch (SchemaException e) {
			logger.error("invalid schema in {} ms : {}", (System.currentTimeMillis() - start), e.getMessage());
			return showError(400, SCHEMA_VALIDATION + e.getMessage(), ScimErrorType.invalidSyntax);
		}
		catch (ConstraintViolationException e) {
			logger.error("constraint violation in {} ms : {}", (System.currentTimeMillis() - start), e.getMessage());
			return showError(409, SCHEMA_VALIDATION + e.getMessage(), ScimErrorType.uniqueness);
		}
		catch (DataException d ){
			return showError(HttpStatus.INTERNAL_SERVER_ERROR.value(), d.getMessage());
		}
		catch (ConfigurationException e) {
			return showError(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
		}
	}

	
	
	//PUT
	protected ResponseEntity<Map<String, Object>> put(String id, Map<String, Object> entity, HttpServletRequest request, HttpServletResponse response, SchemaResourceType resourceType, String attributes, String excludedAttributes) {
		long start = System.currentTimeMillis();
		try {
			Schema mainSchema = resourceType.getSchemaObject();
			// validate
			logger.debug("schema {} ", mainSchema);
			schemaReader.validate(resourceType, entity, true, request.getMethod());
			if (!entity.get(Constants.ID).equals(id)) {
				return showError(400, "id [" + entity.get(Constants.ID)	+ "] given in the data does not match the one in the url [" + id + "]");
			}
			;
			Map<String, Object> existingEntity = storageImplementationFactory.getStorageImplementation(mainSchema).get(id,CurrentConsumer.getCurrent());
			if (existingEntity != null) {
				entity.put(Constants.KEY_META, existingEntity.get(Constants.KEY_META));
			} 
			else {
				return showError(404, "resource of type " + mainSchema.getName() + " with id " + id + " can not be updated");
			}

			// prepare
			String location = UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request)).build().toUriString();
			createMeta(new Date(), id, entity, mainSchema.getName(), location);
			response.addHeader(Constants.HEADER_LOCATION, location);

			// store
			storageImplementationFactory.getStorageImplementation(mainSchema).update(id, entity,CurrentConsumer.getCurrent());

			logger.info("resource of type {} with id {} updated in {} ms", mainSchema.getName(), id, (System.currentTimeMillis() - start));
			return new ResponseEntity<>(filterAttributes(mainSchema, entity, getListFromString(attributes), excludedAttributes), HttpStatus.OK);

		} 
		catch (SchemaException e) {
			logger.error("invalid schema in {} ms : {}", (System.currentTimeMillis() - start), e.getMessage());
			return showError(400, SCHEMA_VALIDATION + e.getMessage(), ScimErrorType.invalidSyntax);
		} 
		catch (ConstraintViolationException e) {
			logger.error("constraint violation", e);
			return showError(409, SCHEMA_VALIDATION + e.getMessage(), ScimErrorType.uniqueness);
		}
		catch (DataException | ConfigurationException e) {
			return showError(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
		}
	}

	
	
	//PATCH
	@SuppressWarnings("unchecked")
	protected ResponseEntity<Map<String, Object>> patch(String id, Map<String, Object> patchRequest, HttpServletRequest request, HttpServletResponse response, SchemaResourceType resourceType, String attributes, String excludedAttributes) {

		long start = System.currentTimeMillis();
		try {
			Schema mainSchema = resourceType.getSchemaObject();
			
			// validate
			schemaReader.validate(resourceType, patchRequest, false, request.getMethod());
			if (!patchRequest.containsKey(Constants.KEY_OPERATIONS)) {
				return showError(400, "no operations present");
			}

			String location = UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request)).build().toUriString();

			Map<String, Object> existingEntity = storageImplementationFactory.getStorageImplementation(mainSchema).get(id, CurrentConsumer.getCurrent());
			if (existingEntity != null) {
				patchRequest.put(Constants.KEY_META, existingEntity.get(Constants.KEY_META));
			} 
			else {
				return showError(404, "resource of type " + mainSchema.getName() + " with id " + id + " can not be updated");
			}

			response.addHeader(Constants.HEADER_LOCATION, location);

			List<Map<String, Object>> operations = (List<Map<String, Object>>) patchRequest.get(Constants.KEY_OPERATIONS);
			
			PatchOperation opType = null;
			String path = null;
			Object value = null;

			for (Map<String, Object> operation : operations) {
				if (!operation.containsKey(Constants.KEY_OP)) {
					return showError(404, "Invalid Operation : " + operation);
				}

				opType = PatchOperation.valueOf(((String)operation.get(Constants.KEY_OP)).toLowerCase());
				if ( opType == null ) {
					return showError(404, "Invalid Operation" + operation);
				}
				path = (String) operation.get(Constants.KEY_PATH);
				value = operation.get(Constants.KEY_VALUE);
				
				if ( !canPerformAction(mainSchema,opType,path)){
					return showError(403, "operation " + opType + " is not allowed for path " + path);
				}
				
				patchUtils.patchEntity( existingEntity, opType, path, value, mainSchema);

			}
			Map <String, Object> extensions = (Map <String, Object>) existingEntity.get("urn:ietf:params:scim:schemas:extension:enterprise:2.0:User");
			if (extensions != null && !extensions.containsKey("manager")) {
				Map<String, Object> manager = new HashMap<>();
				manager.put("value", "");
				extensions.put("manager", manager);
			}
			createMeta(new Date(), id, existingEntity, mainSchema.getName(), location);
			storageImplementationFactory.getStorageImplementation(mainSchema).update(id, existingEntity, CurrentConsumer.getCurrent());


			logger.info("resource of type {} with id {} patched in {} ms", mainSchema.getName(), id,(System.currentTimeMillis() - start));
			return new ResponseEntity<>( filterAttributes(mainSchema, existingEntity, getListFromString(attributes), excludedAttributes), HttpStatus.OK);

		} 
		catch (SchemaException e) {
			logger.error("invalid schema in {} ms : {}", (System.currentTimeMillis() - start), e.getMessage());
			return showError(400, SCHEMA_VALIDATION + e.getMessage(), ScimErrorType.invalidSyntax);
		}
		catch (ConstraintViolationException e) {
			logger.error("constraint violation", e);
			return showError(409, SCHEMA_VALIDATION + e.getMessage(), ScimErrorType.uniqueness);
		}
		catch (DataException | ConfigurationException e) {
			return showError(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
		}
	}

	
	
	
	
	//GET
	protected ResponseEntity<Map<String, Object>> get(String id, HttpServletRequest request, HttpServletResponse response, Schema schema, String attributes, String excludedAttributes) {
		long start = System.currentTimeMillis();
		try {
			Map<String, Object> object = storageImplementationFactory.getStorageImplementation(schema).get(id, CurrentConsumer.getCurrent());

			ResponseEntity<Map<String, Object>> result = null;
			if (object != null) {
				List<String> includeList = getListFromString(attributes);
				object = filterAttributes(schema, object, includeList, excludedAttributes);
				//include groups
				if ( haveToIncludeGroups(schema,includeList,excludedAttributes, Constants.HTTP_METHOD_GET)) {
					logger.debug("have to include groups");
					includeGroups(id, object);
				}
				result = new ResponseEntity<>(object, HttpStatus.OK);
				response.addHeader(Constants.HEADER_LOCATION, UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request)).build().toUriString());
			}
			else {
				return showError(HttpStatus.NOT_FOUND.value(), "the resource with id " + id + " is not found");
			}
			logger.info("resource of type {} with id {} fetched in {} ms", schema.getName(), id, (System.currentTimeMillis() - start));
			return result;
		} 
		catch (DataException | ConfigurationException e) {
			return showError(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
		}
	}



	private void includeGroups(String id, Map<String, Object> object ) {
		Schema groupsSchema = schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_GROUP);
		List<Map> groupSearch = storageImplementationFactory.getStorageImplementation(groupsSchema).search(new SearchCriteria(new SearchCriterium("members.value", id, SearchOperation.ENDS_WITH)), 1, returnGroupsMax, null, null, CurrentConsumer.getCurrent());
		if ( returnGroupsIncludedFieldsArray == null ) {
			returnGroupsIncludedFieldsArray = Arrays.asList(returnGroupsIncludedFields.split(StringUtils.COMMA));
		}
		List<Map<String, Object>> filteredGroups = new ArrayList<Map<String, Object>>();
		for( Map m : groupSearch ) {
			filteredGroups.add(filterAttributes(groupsSchema, m, returnGroupsIncludedFieldsArray, null));
		}
		object.put(Constants.KEY_GROUPS, filteredGroups);
	}

	
	
	
	private boolean haveToIncludeGroups(Schema schema, List<String> includeList, String excludedAttributes, String operation ) {
		if ( schema.getName().equalsIgnoreCase(Constants.RESOURCE_TYPE_USER)){
			if ( excludedAttributes== null || !excludedAttributes.contains(Constants.KEY_GROUPS)) {
				if ( operation.equalsIgnoreCase(Constants.HTTP_METHOD_GET) && returnGroupsOnUserGet ) {
					logger.debug("have to return groups for get");
					return true;
				}
				else if ( operation.equalsIgnoreCase(Constants.SEARCH) && returnGroupsOnUserSearch) {
					logger.debug("have to return groups for search");
					return true;
				}
			}
		}
		return false;
	}


	protected ResponseEntity<Map<String, Object>> search(Integer startIndex, Integer count, Schema schema, String filter) {
		return search(startIndex, count, schema, filter, null, null, null, null);
	}

	
	/**
	 * Search entities
	 * @param startIndex the start index
	 * @param count the count
	 * @param schema the schema
	 * @param filter the filter
	 * @param sortBy sortby 
	 * @param sortOrder the order
	 * @param attributes the attributes
	 * @param excludedAttributes the excluded attributes
	 * @return a response entity
	 */
	protected ResponseEntity<Map<String, Object>> search(Integer startIndex, Integer count, Schema schema, String filter, String sortBy, String sortOrder, String attributes, String excludedAttributes) {

		long start = System.currentTimeMillis();

		try {
			SearchCriteria searchCriteria = searchCriteriaUtil.composeSearchCriteriaFromSCIMFilter(filter);
			for (SearchCriterium criterium : searchCriteria.getCriteria()) {
				criterium.setValue(criterium.getValue().toString().toLowerCase());
			}


			Storage storage = storageImplementationFactory.getStorageImplementation(schema);

			List<String> includeList = getListFromString(attributes);
			
			if ( StringUtils.isEmpty(sortBy)) {
				sortBy = getSearchDefault(schema, SORT_BY);
			}
			
			if ( StringUtils.isEmpty(sortOrder)) {
				sortOrder = getSearchDefault(schema, SORT_ORDER);
			}

			List<Map> dataFetched = storage.search(searchCriteria, startIndex, count, sortBy, sortOrder, includeList, CurrentConsumer.getCurrent());
			System.out.println("dataFetched size: " + dataFetched.size());
			System.out.println("dataFetched: " + dataFetched.toString());
			List<Map<String, Object>> data = new ArrayList<>();
			
			if (dataFetched != null) {
				for (Map<String, Object> entity : dataFetched) {
					if ( haveToIncludeGroups(schema, includeList, excludedAttributes, Constants.SEARCH)) {
						includeGroups( (String)entity.get("id"), entity );
					}
					System.out.println("--entity: " + entity.toString());
					System.out.println("--filterAttributes: " + filterAttributes(schema, entity, includeList, excludedAttributes).toString());
					data.add(filterAttributes(schema, entity, includeList, excludedAttributes));
				}

				ResponseEntity<Map<String, Object>> result = null;

				Map<String, Object> responseObject = new HashMap<>();
				responseObject.put(Constants.KEY_SCHEMAS, new String[] { Constants.SCHEMA_LISTRESPONSE });
				responseObject.put(Constants.KEY_STARTINDEX, startIndex);
				responseObject.put(Constants.KEY_ITEMSPERPAGE, count);
				responseObject.put(Constants.KEY_TOTALRESULTS, storage.count(searchCriteria, CurrentConsumer.getCurrent()));
				responseObject.put(Constants.KEY_RESOURCES, data);

				result = new ResponseEntity<>(responseObject, HttpStatus.OK);

				logger.info("resources of type {} start [{}] count[{}] filter [{}] fetched in {} ms", schema.getName(), startIndex, count, filter, (System.currentTimeMillis() - start));

				return result;
			} 
			else {
				throw new DataException("null value returned from storage layer implemetation, should be empty list");
			}
		} 
		catch (InvalidFilterException ife) {
			return showError(HttpStatus.BAD_REQUEST.value(), "the filter [" + filter + "] is not correct : " + ife.getMessage(), ScimErrorType.invalidFilter);
		} 
		catch (DataException | ConfigurationException e) {
			return showError(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
		}
	}
	
	

	
	
	

	
	


	protected ResponseEntity<?> delete(String id, Schema schema) {
		long start = System.currentTimeMillis();

		Map<String, Object> m = storageImplementationFactory.getStorageImplementation(schema).get(id, CurrentConsumer.getCurrent());

		ResponseEntity<?> result = null;
		if (m != null) {
			boolean deleted = storageImplementationFactory.getStorageImplementation(schema).delete(id, CurrentConsumer.getCurrent());
			if (deleted) {
				result = new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
				//if user is deleted, also remove from members
				if ( schema.getName().equalsIgnoreCase(Constants.RESOURCE_TYPE_USER)){
					deleteUserFromMembers(id);
				}
			}
			else {
				return showError(400, "could not delete resource of type " + schema.getName() + " with id " + id);
			}
		} 
		else {
			return showError(HttpStatus.NOT_FOUND.value(), "the resource with id " + id + " is not found");
		}
		logger.info("resource of type {} with id {} deleted in {} ms", schema.getName(), id, (System.currentTimeMillis() - start));

		return result;
	}


	
	
	private void deleteUserFromMembers(String id) {
		Storage groupStorage = storageImplementationFactory.getStorageImplementation(schemaReader.getSchemaByName(Constants.RESOURCE_TYPE_GROUP));
		//find all groups where the user is in
		SearchCriteria groupSearchCriteria = new SearchCriteria(new SearchCriterium("members.value", id));
		List<Map> groups = groupStorage.search(groupSearchCriteria, 1, Integer.MAX_VALUE, null, null, CurrentConsumer.getCurrent());
		if ( groups != null && groups.size() > 0 ) {
			logger.info("found {} groups containing the deleted user", groups.size());
			for ( Map group : groups ) {
				String groupId = (String)group.get(Constants.ID);
				List newMembers = new ArrayList<>();
				List currentMembers = (List)group.get(Constants.KEY_MEMBERS);
				for ( Object member : currentMembers ) {
					Map memberMap = (Map)member;
					if ( !memberMap.get(Constants.KEY_VALUE).equals(id)) {
						newMembers.add(memberMap);
					}
				}
				group.put(Constants.KEY_MEMBERS, newMembers);
				try {
					groupStorage.update(groupId, group, CurrentConsumer.getCurrent());
					logger.info("updated group {} containing the deleted user {}", groupId, id);
				}
				catch ( Exception e ) {
					logger.error("can not remove user from group", e);
				}
			}
		}
	}

	
	
	protected void createMeta(Date date, String id, Map<String, Object> user, String resourceType, String location) {

		logger.debug("create meta {} {} {} {} {}",  date, id, user, resourceType, location);
		Map<String, String> map = new HashMap<String, String>();
		String formattedDate = new SimpleDateFormat(Constants.DATEFORMAT_STRING, Locale.US).format(date);
		if (user.containsKey(Constants.KEY_META)) {
			map = (Map<String, String>) user.get(Constants.KEY_META);
			if ( map == null ) {
				map = new HashMap<String, String>();
			}
		}
		else {
			map.put(Constants.KEY_CREATED, formattedDate);
		}
		map.put(Constants.KEY_RESOURCE_TYPE, resourceType);
		map.put(Constants.KEY_LAST_MODIFIED, formattedDate);
		map.put(Constants.KEY_VERSION, createVersion(date));
		map.put(Constants.KEY_LOCATION, location);

		user.put(Constants.KEY_META, map);
	}
	
	
	
	private String getSearchDefault(Schema schema, String type) {
		String kk = schema.getName() + StringUtils.DOT + type;
		if ( defaultSort.containsKey(kk)) {
			return defaultSort.get(kk);
		}
		else {
			String key = "scim.search." + schema.getName() + ".default" + type;
			String value = propertyFactory.getProperty(key);
			if ( StringUtils.isEmpty(value)) {
				value = StringUtils.EMPTY_STRING;
			}
			logger.info("putting default sort for type {} schema {} to value {}", type, schema.getName(), value);
			defaultSort.put(kk, value);
			return value;
		}
	}
	
	

	protected String createVersion(Date date) {
		return StringUtils.EMPTY_STRING + date.getTime();
	}

	protected ResponseEntity<Map<String, Object>> showError(int status, String detail) {
		return showError(status, detail, null);
	}

	protected ResponseEntity<Map<String, Object>> showError(int status, String detail, ScimErrorType scimType) {
		Map<String, Object> error = composeError(status, detail, scimType);
		return new ResponseEntity<Map<String, Object>>(error, HttpStatus.valueOf(status));
	}



	protected Map<String, Object> composeError(int status, String detail, ScimErrorType scimType) {
		Map<String, Object> error = new HashMap<>();
		error.put(Constants.KEY_SCHEMAS, Constants.SCHEMA_ERROR);
		if (scimType != null) {
			error.put(Constants.KEY_SCIMTYPE, scimType);
		}
		error.put(Constants.KEY_DETAIL, detail);
		error.put(Constants.KEY_STATUS, StringUtils.EMPTY_STRING + status);
		return error;
	}

	
	
	protected Map<String, Object> filterAttributes(Schema schema, Map<String, Object> entity, List<String> includeList, String excludedAttributes) {
		Map<String, Object> copy = new HashMap<>();
		copy.putAll(entity);
		List<String> excludeList = getListFromString(excludedAttributes);
		for (SchemaAttribute attribute : schema.getAttributes()) {
			if (attribute.getReturned().equalsIgnoreCase(Constants.RETURNED_NEVER)) {
				copy.remove(attribute.getName());
			}
			if (includeList != null && includeList.size() > 0) {
				if (!attribute.getReturned().equalsIgnoreCase(Constants.RETURNED_ALWAYS)
						&& !includeList.contains(attribute.getName())) {
					copy.remove(attribute.getName());
				}
			}
			else if (excludeList != null && excludeList.size() > 0) {
				if (!attribute.getReturned().equalsIgnoreCase(Constants.RETURNED_ALWAYS)
						&& excludeList.contains(attribute.getName())) {
					copy.remove(attribute.getName());
				}
			}
		}

		// meta is not in the schema?
		if (includeList != null && includeList.size() > 0) {
			if (!includeList.contains(Constants.KEY_META)) {
				copy.remove(Constants.KEY_META);
			}
		}
		else if (excludeList != null && excludeList.size() > 0) {
			if (excludeList.contains(Constants.KEY_META)) {
				copy.remove(Constants.KEY_META);
			}
		}

		return copy;
	}

	protected List<String> getListFromString(String attributes) {
		if (!StringUtils.isEmpty(attributes)) {
			return Arrays.asList(attributes.split(StringUtils.COMMA));
		}
		return null;
	}
	


	protected String createId(Map<String, Object> object) {
		Object id = object.get(Constants.ID);
		if (allowIdOnCreate) {
			return id != null ? id.toString() : UUID.randomUUID().toString();
		}
		else {
			return UUID.randomUUID().toString();
		}
	}

	

	protected ResponseEntity<Map<String, Object>> invalidSchemaForEndpoint(List<String> schemas, String resourceType) {
		return showError(400, "for type " + resourceType + " content is not compliant with schemas " + schemas.toString(), ScimErrorType.invalidSyntax);
	}
	
	protected ResponseEntity<Map<String, Object>> invalidSchemaForResource(String resourceType, String requiredSchema) {
		return showError(400, "for patching " + resourceType + ", given schemas is not containing " + requiredSchema, ScimErrorType.invalidSyntax);
	}
	
	protected ResponseEntity<Map<String, Object>> missingRequiredSchemaForResource(SchemaResourceType resourceType, String requiredSchema) {
		return showError(400, "the required schema " + requiredSchema + " for resourcetype " + resourceType.getName() + " is not present in the body ", ScimErrorType.invalidSyntax);
	}
	
	
	
	
	
	private boolean canPerformAction(Schema schema, PatchOperation operation, String path) {
		logger.debug("operation {} path {}", operation , path );
		if ( path != null ){
			SchemaAttribute attribute = null;
			//custom schema
			if ( path.startsWith(Constants.URN)) {
				String schemaString = path.substring(0, path.lastIndexOf(StringUtils.COLON));
				String attributeName = path.substring(path.lastIndexOf(StringUtils.COLON) +1, path.length());
				logger.debug("custom schema string {}", schemaString);
				Schema customSchema = schemaReader.getSchema(schemaString);
				logger.debug("custom schema {}", customSchema);
				attribute = customSchema.getAttribute(attributeName);
				if ( !canPreformActionOnAttribute(operation, attribute)) {
					return false;
				}
			}
			//simple attribute
			else if (path.indexOf(StringUtils.DOT) == -1 ) {
				attribute = schema.getAttribute(path);
				if ( !canPreformActionOnAttribute(operation, attribute)) {
					return false;
				}
			}
			else {
				String[] splitted = path.split("\\.");
				if ( splitted.length == 2){
					attribute = schema.getAttribute(removeExpression(splitted[0]));
					if ( attribute != null && !canPreformActionOnAttribute(operation, attribute) ) {
						return false;
					}
					else {
						attribute = attribute.getSubAttribute(removeExpression(splitted[1]));
						if ( attribute != null && !canPreformActionOnAttribute(operation, attribute) ) {
							return false;
						}
					}
				}
				else {
					logger.info("authorization check skipped for operation {}, path {} containing more then two levels of depth", operation, path);
				}
			}
		}
		return true;
	}
	
	
	
	private String removeExpression(String p ) {
		int startExpression = p.indexOf("[");
		int endExpression = p.indexOf("]");
		logger.debug("start {} end {}", startExpression, endExpression );
		if ( startExpression != -1 && endExpression != -1) {
			p = p.substring(0, startExpression) + p.substring(endExpression+1, p.length());
		}
		logger.debug("removed expression {}", p);
		return p;
	}



	private boolean canPreformActionOnAttribute(PatchOperation operation, SchemaAttribute attribute) {
		if ( attribute != null && !StringUtils.isEmpty(attribute.getMutability())){
			ScimMutability mutability = ScimMutability.valueOf(attribute.getMutability());
			if ( operation == PatchOperation.add) {
				if ( mutability == ScimMutability.immutable || mutability == ScimMutability.readOnly){
					return false;
				}
			}
			else if ( operation == PatchOperation.remove) {
				if ( mutability == ScimMutability.immutable || mutability == ScimMutability.readOnly){
					return false;
				}
			}
			else if ( operation == PatchOperation.replace) {
				if ( mutability == ScimMutability.immutable || mutability == ScimMutability.readOnly){
					return false;
				}
			}
			
		}
		//required attributes can not be removed according to spec
		if ( attribute.isRequired() && operation == PatchOperation.remove ){
			return false;
		}
		
		return true;
	}

	
	
}
