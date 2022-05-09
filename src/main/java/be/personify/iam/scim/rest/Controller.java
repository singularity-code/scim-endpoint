package be.personify.iam.scim.rest;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.util.UriComponentsBuilder;

import be.personify.iam.scim.schema.Schema;
import be.personify.iam.scim.schema.SchemaAttribute;
import be.personify.iam.scim.schema.SchemaException;
import be.personify.iam.scim.schema.SchemaReader;
import be.personify.iam.scim.storage.ConfigurationException;
import be.personify.iam.scim.storage.ConstraintViolationException;
import be.personify.iam.scim.storage.DataException;
import be.personify.iam.scim.storage.Storage;
import be.personify.iam.scim.storage.StorageImplementationFactory;
import be.personify.iam.scim.util.Constants;
import be.personify.iam.scim.util.PropertyFactory;
import be.personify.iam.scim.util.ScimErrorType;
import be.personify.iam.scim.util.SearchCriteriaUtil;
import be.personify.util.SearchCriteria;
import be.personify.util.SearchCriterium;
import be.personify.util.SearchOperation;
import be.personify.util.StringUtils;

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
	
	@Value("${scim.returnGroupsOnUser:true}")
	private boolean returnGroupsOnUser;
	
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
	private SchemaReader schemaReader;

	
	//POST
	protected ResponseEntity<Map<String, Object>> post(Map<String, Object> entity, HttpServletRequest request, HttpServletResponse response, Schema schema, String attributes, String excludedAttributes) {
		long start = System.currentTimeMillis();
		try {
			// validate
			schemaReader.validate(schema, entity, true, request.getMethod());
			
			// prepare
			String id = createId(entity);
			entity.put(Constants.ID, id);
			String location = UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request)).build().toUriString() + StringUtils.SLASH + id;
			createMeta(new Date(), id, entity, schema.getName(), location);
			response.addHeader(Constants.HEADER_LOCATION, location);

			// store and return
			storageImplementationFactory.getStorageImplementation(schema).create(id, entity);
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
		catch (DataException | ConfigurationException e) {
			return showError(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
		}
	}

	
	
	//PUT
	protected ResponseEntity<Map<String, Object>> put(String id, Map<String, Object> entity, HttpServletRequest request, HttpServletResponse response, Schema schema, String attributes, String excludedAttributes) {
		long start = System.currentTimeMillis();
		try {
			// validate
			logger.info("schema {} ", schema);
			schemaReader.validate(schema, entity, true, request.getMethod());
			if (!entity.get(Constants.ID).equals(id)) {
				return showError(400, "id [" + entity.get(Constants.ID)	+ "] given in the data does not match the one in the url [" + id + "]");
			}
			;
			Map<String, Object> existingUser = storageImplementationFactory.getStorageImplementation(schema).get(id);
			if (existingUser != null) {
				entity.put(Constants.KEY_META, existingUser.get(Constants.KEY_META));
			} 
			else {
				return showError(404, "resource of type " + schema.getName() + " with id " + id + " can not be updated");
			}

			// prepare
			String location = UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request)).build().toUriString();
			createMeta(new Date(), id, entity, schema.getName(), location);
			response.addHeader(Constants.HEADER_LOCATION, location);

			// store
			storageImplementationFactory.getStorageImplementation(schema).update(id, entity);

			logger.info("resource of type {} with id {} updated in {} ms", schema.getName(), id, (System.currentTimeMillis() - start));
			return new ResponseEntity<>(filterAttributes(schema, entity, getListFromString(attributes), excludedAttributes), HttpStatus.OK);

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
	protected ResponseEntity<Map<String, Object>> patch(String id, Map<String, Object> entity, HttpServletRequest request, HttpServletResponse response, Schema schema, String attributes, String excludedAttributes) {

		long start = System.currentTimeMillis();
		try {
			// validate
			schemaReader.validate(schema, entity, false, request.getMethod());
			if (!entity.containsKey(Constants.KEY_OPERATIONS)) {
				return showError(400, "no operations present");
			}

			String location = UriComponentsBuilder.fromHttpRequest(new ServletServerHttpRequest(request)).build().toUriString();

			Map<String, Object> existingEntity = storageImplementationFactory.getStorageImplementation(schema).get(id);
			if (existingEntity != null) {
				entity.put(Constants.KEY_META, existingEntity.get(Constants.KEY_META));
			} 
			else {
				return showError(404, "resource of type " + schema.getName() + " with id " + id + " can not be updated");
			}

			response.addHeader(Constants.HEADER_LOCATION, location);

			List<Map<String, Object>> operations = (List<Map<String, Object>>) entity.get(Constants.KEY_OPERATIONS);
			
			String opType = null;
			String path = null;
			Object value = null;

			for (Map<String, Object> operation : operations) {
				if (!operation.containsKey(Constants.KEY_OP)) {
					return showError(404, "Invalid Operation : " + operation);
				}

				opType = ((String) operation.get(Constants.KEY_OP)).toLowerCase();
				path = (String) operation.get(Constants.KEY_PATH);
				value = operation.get(Constants.KEY_VALUE);

				switch (opType) {
				case "add":
					logger.debug("adding {} to {} in {}", value, path, entity);
					Object entry = getPath(path, existingEntity);
					logger.info(entry.getClass().getName() + " {} ", entry);
					if (entry instanceof List) {
						((List) entry).addAll((List) value);
					} 
					else if (entry instanceof Map) {
						Map<String, Object> eMap = (Map) entry;
						Map<String, Object> aMap = (Map) value;
						for (String key : aMap.keySet()) {
							if (eMap.containsKey(key)) {
								Object e1 = eMap.get(key);
								if (e1 instanceof List) {
									((List) e1).addAll((Collection) aMap.get(key));
								} else {
									eMap.put(key, aMap.get(key));
								}
							} else {
								eMap.put(key, aMap.get(key));
							}
						}
					} 
					else {
						logger.error("Cannot perform add patch: path {} value {} on {}", path, value, entity);
					}
					break;
				case "remove":
					logger.debug("removing {} from {} in {}", value, path, entity);
					List<String> segs = getPathSegments(path);
					if (segs.size() == 1) {
						existingEntity.remove(path);
					} else {
						logger.error("Cannot perform remove patch: path {} value {} on {}", path, value, entity);
					}
					break;
				case "replace":
					logger.debug("replace {} with {} in {}", path, value, entity);
					entry = getPath(path, existingEntity);
					if (entry instanceof Map) {
						((Map) entry).putAll((Map) value);
					} else if (entry instanceof List) {
						List list = (List) entry;
						list.clear();
						list.addAll((List) value);
					} else {
						logger.error("Cannot perform replace patch: path {} value {} on {}", path, value, entity);
					}
					break;

				default:
					return showError(404, "Invalid Operation");
				}
			}

			createMeta(new Date(), id, existingEntity, schema.getName(), location);
			storageImplementationFactory.getStorageImplementation(schema).update(id, existingEntity);

			logger.info("resource of type {} with id {} patched in {} ms", schema.getName(), id,(System.currentTimeMillis() - start));
			return new ResponseEntity<>( filterAttributes(schema, existingEntity, getListFromString(attributes), excludedAttributes), HttpStatus.OK);

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
			Map<String, Object> object = storageImplementationFactory.getStorageImplementation(schema).get(id);

			ResponseEntity<Map<String, Object>> result = null;
			if (object != null) {
				List<String> includeList = getListFromString(attributes);
				object = filterAttributes(schema, object, includeList, excludedAttributes);
				//include groups
				if ( haveToIncludeGroups(schema,includeList,excludedAttributes)) {
					Schema groupsSchema = schemaReader.getSchemaByResourceType(Constants.RESOURCE_TYPE_GROUP);
					List<Map> groupSearch = storageImplementationFactory.getStorageImplementation(groupsSchema).search(new SearchCriteria(new SearchCriterium("members.$ref", id, SearchOperation.ENDS_WITH)), 1, returnGroupsMax, null, null);
					if ( returnGroupsIncludedFieldsArray == null ) {
						returnGroupsIncludedFieldsArray = Arrays.asList(returnGroupsIncludedFields.split(StringUtils.COMMA));
					}
					List<Map<String, Object>> filteredGroups = new ArrayList<Map<String, Object>>();
					for( Map m : groupSearch ) {
						filteredGroups.add(filterAttributes(groupsSchema, m, returnGroupsIncludedFieldsArray, null));
					}
					object.put("groups", filteredGroups);
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

	
	private boolean haveToIncludeGroups(Schema schema, List<String> includeList, String excludedAttributes) {
		if ( schema.getName().equalsIgnoreCase(Constants.RESOURCE_TYPE_USER) && returnGroupsOnUser ){
			if ( excludedAttributes== null || !excludedAttributes.contains("groups")) {
				logger.debug("have to return groups");
				return true;
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
			SearchCriteria searchCriteria = searchCriteriaUtil.composeSearchCriteria(filter);
			Storage storage = storageImplementationFactory.getStorageImplementation(schema);

			List<String> includeList = getListFromString(attributes);
			
			if ( StringUtils.isEmpty(sortBy)) {
				sortBy = getSearchDefault(schema, SORT_BY);
			}
			
			if ( StringUtils.isEmpty(sortOrder)) {
				sortOrder = getSearchDefault(schema, SORT_ORDER);
			}

			List<Map> dataFetched = storage.search(searchCriteria, startIndex, count, sortBy, sortOrder, includeList);
			List<Map<String, Object>> data = new ArrayList<>();
			
			if (dataFetched != null) {
				for (Map<String, Object> entity : dataFetched) {
					data.add(filterAttributes(schema, entity, includeList, excludedAttributes));
				}

				ResponseEntity<Map<String, Object>> result = null;

				Map<String, Object> responseObject = new HashMap<>();
				responseObject.put(Constants.KEY_SCHEMAS, new String[] { Constants.SCHEMA_LISTRESPONSE });
				responseObject.put(Constants.KEY_STARTINDEX, startIndex);
				responseObject.put(Constants.KEY_ITEMSPERPAGE, count);
				responseObject.put(Constants.KEY_TOTALRESULTS, storage.count(searchCriteria));
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

		Map<String, Object> m = storageImplementationFactory.getStorageImplementation(schema).get(id);

		ResponseEntity<?> result = null;
		if (m != null) {
			boolean deleted = storageImplementationFactory.getStorageImplementation(schema).delete(id);
			if (deleted) {
				result = new ResponseEntity<Object>(HttpStatus.NO_CONTENT);
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
		Map<String, Object> error = new HashMap<>();
		error.put(Constants.KEY_SCHEMAS, Constants.SCHEMA_ERROR);
		if (scimType != null) {
			error.put(Constants.KEY_SCIMTYPE, scimType);
		}
		error.put(Constants.KEY_DETAIL, detail);
		error.put(Constants.KEY_STATUS, StringUtils.EMPTY_STRING + status);
		return new ResponseEntity<Map<String, Object>>(error, HttpStatus.valueOf(status));
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
	

	protected List<String> getPathSegments(String path) {
		List<String> rest = new ArrayList<>();
		if (!path.contains(StringUtils.DOT)) {
			rest.add(path);
			return rest;
		}
		if (!StringUtils.isEmpty(path)) {
			rest.addAll(Arrays.asList(path.split("\\.")));
			return rest;
		}
		return rest;
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

	protected List<String> extractSchemas(Map<String, Object> user) {
		return (List<String>) user.get(Constants.KEY_SCHEMAS);
	}

	protected ResponseEntity<Map<String, Object>> invalidSchemaForResource(List<String> schemas, String resourceType) {
		return showError(400, "for type " + resourceType + " content is not compliant with schemas " + schemas.toString(), ScimErrorType.invalidSyntax);
	}
	
	protected ResponseEntity<Map<String, Object>> invalidSchemaForResource(String resourceType, String requiredSchema) {
		return showError(400, "for patching " + resourceType + ", given schemas is not containing " + requiredSchema, ScimErrorType.invalidSyntax);
	}

	/**
	 * This function needs to handle paths with selectors... doesn't yet. See
	 * example user_patch4 .. (Microsoft AD example).
	 *
	 * @param path
	 * @param entity
	 * @return
	 */
	Object getPath(String path, Map<String, Object> entity) {
		if (StringUtils.isEmpty(path)) {
			return entity;
		}
		List<String> segs = getPathSegments(path);
		Object current = entity;
		while (!segs.isEmpty()) {
			String seg = segs.remove(0);
			if (current instanceof Map) {
				current = ((Map) current).get(seg);
			} else {
				return null;
			}
		}
		return current;
	}
}
