package be.personify.iam.scim.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import be.personify.iam.scim.schema.Schema;
import be.personify.iam.scim.schema.SchemaAttribute;
import be.personify.iam.scim.schema.SchemaAttributeType;
import be.personify.iam.scim.schema.SchemaReader;
import be.personify.iam.scim.util.Constants;
import be.personify.util.StringUtils;
import be.personify.util.scim.PatchOperation;

/**
 * Class that bundles patch operations
 * 
 * @author wouter
 *
 */
public class PatchUtils {
	
	
	
	private static final String EQ = " eq ";
	private static final String FALSE = "false";
	private static final String TRUE = "true";
	private static final String OBJECT = "object";
	private static final String TYPE = "type";
	private static final String ATTRIBUTENAME = "attributename";

	@Autowired
	private SchemaReader schemaReader;
	
	private static final Logger logger = LogManager.getLogger(PatchUtils.class);
	

	public void patchEntity(Map<String, Object> existingEntity, PatchOperation opType, String path, Object value, Schema schema) {
		
		Object entry = null;
		
		//ADD
		if ( opType == PatchOperation.add) {
			logger.info("adding {} to {}", value, path);
			Map pathResult = (Map)getPath(path, existingEntity, true, schema);
			entry = pathResult.get(OBJECT);
			logger.info("entry {} {}", entry);
			if ( entry == null ) {
				logger.info("entry is null");
				existingEntity.put(removeUrnFromString(path), value);
			}
			else if (entry instanceof List) {
				List eList = (List)entry;
				logger.info("its a list {}", eList);
				if ( value instanceof List ) {
					List ll = (List)value;
					for ( Object o : ll ) {
						if ( !eList.contains(o)) {
							eList.add(o);
						}
					}
				}
				else if ( value instanceof Map ) {
					eList.add(value);
				}
				else if ( value instanceof String) {
					logger.info("pathResult {}", pathResult);
					String type = (String)pathResult.get(TYPE); 
					if ( type != null && type.equals(SchemaAttributeType.COMPLEX.name())) {
						Map m = new HashMap<>();
						m.put((String)pathResult.get(ATTRIBUTENAME), value);
						String conditions =  (String)pathResult.get("conditions");
						if (!StringUtils.isEmpty(conditions)) {
							putConditionsInMap( m, conditions);
						}
						eList.add(m);
						logger.info("eList {}", eList);
					}
					logger.info("value {}", value);
				}
			} 
			else if (entry instanceof Map) {
				logger.info("its a map");
				Map<String, Object> eMap = (Map) entry;
				if ( value instanceof Map ) {
					logger.info("value is a map");
					Map<String, Object> aMap = (Map) value;
					for (String key : aMap.keySet()) {
						logger.info("key {}", key);
						if (eMap.containsKey(key)) {
							Object e1 = eMap.get(key);
							if (e1 instanceof List) {
								Collection c = (Collection) aMap.get(key);
								//((List) e1).addAll(c);
								logger.info("c {}", c);
								List currentList = ((List) e1);
								logger.info("currentList {}", currentList);
								for ( Object co : c ) {
									logger.info("co {}", co);
									if ( !currentList.contains(co) ){
										((List) e1).add(co);
									}
								}
							} 
							else {
								eMap.put(key, aMap.get(key));
							}
						} 
						else {
							eMap.put(key, aMap.get(key));
						}					}
				}
				else if ( value instanceof List ) {
					logger.info("entry {} path {} list {}", entry , path, value );
					eMap.put(removeUrnFromString(path), value);
				}
			} 
			else {
				logger.error("Cannot perform add patch: path {} value {} on {}", path, value );
			}
			
		}
		//REMOVE
		else if ( opType == PatchOperation.remove) {
			logger.info("removing {} from {}", value, path);
			List<String> segs = getPathSegments(path, schema);
			logger.info("segs {} {}", segs, segs.size());
			if (segs.size() == 1) {
				if ( value instanceof List ) {
					List entriesToRemove = (List)value;
					logger.info("entriesToRemove {}", entriesToRemove);
					Object existingAttribute = existingEntity.get(path);
					if ( existingAttribute instanceof List ) {
						logger.info("existing attribute is a list", existingAttribute);
						List existingAttributeList = (List)existingAttribute;
						List newList = new ArrayList();
						for (Object oo : existingAttributeList ) {
							if ( oo instanceof Map ) {
								logger.info("oo is a map");
								Map ooo = (Map)oo;
								for ( Object ee : entriesToRemove ) {
									if ( ee instanceof Map) {
										if ( !areEqual(ooo, (Map)ee)) {
											logger.info("ooo {} not equals ee {}", ooo, (Map)ee );
											newList.add(oo);
										}
									}
								}
							}
							
							
							if( !entriesToRemove.contains(oo)) {
								//??
							}
						}
						existingEntity.put(path, newList);
					}
				}
				else {
					existingEntity.remove(path);
				}
			}
			else {
				logger.error("Cannot perform remove patch: path {} value {} ", path, value);
			}
		}
		//REPLACE
		else if ( opType == PatchOperation.replace) {
			logger.info("replace {} with {}", path, value);
			Map pathResult = (Map)getPath(path, existingEntity, false, schema);
			logger.info("pathResult {}", pathResult);
			if ( pathResult == null) {
				String error = "can not replace non existent attribute " +  path + " " + value;
				logger.error(error);
				throw new RuntimeException(error);
			}
			entry = pathResult.get(OBJECT);
			logger.info("entry {}", entry);
			if ( entry == null ) {
				logger.info("path {}", path);
				int dotFound = path.indexOf(StringUtils.DOT);
				if ( pathResult.get(TYPE).equals(SchemaAttributeType.COMPLEX.name()) && dotFound != -1) {
					String[] splitted = path.split("\\.");
					logger.info("splitted {} {}", splitted, splitted.length);
					((Map)existingEntity.get(splitted[0])).put(splitted[1], value);
				}
			}
			else if (entry instanceof Map) {
				((Map) entry).putAll((Map) value);
			}
			else if (entry instanceof List) {
				List list = (List) entry;
				if ( pathResult.get(TYPE).equals(SchemaAttributeType.COMPLEX.name())) {
					for ( Object m : list ) {
						Map map = (Map)m;
						map.put(pathResult.get(ATTRIBUTENAME), value);
					}
				}
				else {
					list.clear();
					if (value instanceof List ) {
						list.addAll((List) value);
					}
					else if (value instanceof String ) {
						list.add(value);
					}
				}
			}
			else if ( entry instanceof String || entry instanceof Boolean ) {
				//entry = value;
				int dotFOund = path.indexOf(StringUtils.DOT);
				if ( dotFOund == -1 || path.startsWith(Constants.URN)) {
					existingEntity.put(removeUrnFromString(path), value);
				}
				else {
					String[] splitted = path.split("\\.");
					
					if ( splitted.length == 2 ) {
						((Map)existingEntity.get(splitted[0])).put(splitted[1], value);
					}
					else {
						throw new RuntimeException("path with depth more then two not supported");
					}
				}
			}
			else {
				logger.error("Cannot perform replace patch: path {} value {}", path, value);
			}
		}
		
		
	}
	
	
	
	private void putConditionsInMap(Map m, String conditions) {
		conditions = conditions.substring(1,conditions.length() -1);
		logger.info("conditions {}", conditions);
		if ( conditions.contains(EQ)) {
			String[] keyValue = conditions.split(EQ);
			if ( keyValue.length == 2 ) {
				String replaced = keyValue[1].replaceAll("\"", StringUtils.EMPTY_STRING);
				if ( replaced.equalsIgnoreCase(TRUE) || replaced.equalsIgnoreCase(FALSE) ) {
					m.put(keyValue[0], Boolean.valueOf(replaced));
				}
				else {
					m.put(keyValue[0], replaced);
				}
			}
		}
	}



	/**
	 * This function needs to handle paths with selectors... doesn't yet. See
	 * example user_patch4 .. (Microsoft AD example).
	 *
	 * @param path
	 * @param entity
	 * @return
	 */
	private Object getPath(String path, Map<String, Object> entity, boolean createTree, Schema schema ) {
		Map m = new HashMap<>();
		Object current = entity;
		if (StringUtils.isEmpty(path)) {
			current = entity;
	}
		else {
			List<String> segs = getPathSegments(path, schema);
			while (!segs.isEmpty()) {
				String seg = segs.remove(0);
				//slogger.info(current.getClass().getName() + " {} ", seg);
				if (current instanceof Map) {
					//parse conditions
					String conditions = StringUtils.EMPTY_STRING;
					logger.info("seg {}", seg);
					
					if ( seg.endsWith("]")) {
						int startCondition = seg.indexOf("[");
						conditions = seg.substring(startCondition, seg.length());
						seg = seg.substring(0, startCondition );
						logger.info("conditions {} seg {}", conditions, seg);
						m.put("conditions", conditions);
					}
					
					SchemaAttribute attribute = getAttribute(path, schema, seg);
					if ( SchemaAttributeType.fromString(attribute.getType()) == SchemaAttributeType.COMPLEX) {
						logger.info("it's complex");
						m.put(TYPE, SchemaAttributeType.COMPLEX.name());
					}
					
					current = ((Map) current).get(seg);
					logger.info("current {}", current);
					
					boolean empty = false; 
					if ( current instanceof List && ((List)current).size() == 0 ) {
						logger.info("empty list");
						empty = true;
					}
	
					if ( (current == null || empty ) && createTree ) {
						logger.info("seg {}", seg);
						
						
						
						if ( attribute.isMultiValued()) {
							logger.info("multivalued {}", attribute.isMultiValued());
							
							logger.info("attribute {} type {}", attribute , attribute.getType());
							if ( SchemaAttributeType.fromString(attribute.getType()) == SchemaAttributeType.COMPLEX) {
								logger.info("it's complex");
								
								if ( current instanceof Map ) {
									List<Object> list = new ArrayList<>();
									Map<String,Object> newObject = new HashMap<>();
									list.add(newObject);
									((Map)current).put(seg, list);
									current = newObject;
								}
								else if ( current == null || current instanceof List ) {
									List l = new ArrayList();
									entity.put(seg, l);
									current = l;
									//((List)current).add(list);
								}
								
							}
						}
						logger.info("seg {} attribute {} current {}", seg, attribute, current);
					}
					else {
						if ( current instanceof List && ((List)current).size() == 0 ) {
							
						}
					}
				}
				else if ( current instanceof List ) {
					logger.info("list seg {}", seg);
					if ( segs.isEmpty()) {
						m.put(ATTRIBUTENAME, seg);
					}
				}
				else {
					return null;
				}
			}
		}
		
		m.put(OBJECT, current);
		
		return m;
	}



	private SchemaAttribute getAttribute(String path, Schema schema, String seg) {
		logger.info("path {} seg {}", path, seg);
		SchemaAttribute attribute = schema.getAttribute(seg);
		int dotFound = path.indexOf(StringUtils.DOT);
		if ( dotFound != -1 && path.endsWith(seg)) {
			String firstPath = path.substring(0,dotFound);
			logger.info("firstPath {}", firstPath);
			attribute = schema.getAttribute(firstPath);
			if ( attribute != null && attribute.getType().equals(SchemaAttributeType.COMPLEX.name())) {
				attribute = attribute.getSubAttribute(path.substring(dotFound, seg.length()));
			}
		}
		
		if ( path.startsWith(Constants.URN)) {
			String schemaString = path.substring(0, path.lastIndexOf(StringUtils.COLON));
			logger.info("custom schema string {}", schemaString);
			Schema customSchema = schemaReader.getSchema(schemaString);
			logger.info("custom schema {}", customSchema);
			attribute = customSchema.getAttribute(seg);
		}
		return attribute;
	}
	
	
	public List<String> getPathSegments(String path, Schema schema ) {
		
		path = removeUrnFromString(path);
		
		logger.info("path {} schema {}", path, schema.getId());
		
		List<String> rest = new ArrayList<>();
		if (!StringUtils.isEmpty(path) && !path.contains(StringUtils.DOT)) {
			rest.add(path);
			return rest;
		}
		else if (!StringUtils.isEmpty(path)) {
			rest.addAll(Arrays.asList(path.split("\\.")));
			return rest;
		}
		return rest;
	}



	private String removeUrnFromString(String path) {
		if ( path.startsWith(Constants.URN)) {
			path = path.substring(path.lastIndexOf(StringUtils.COLON) +1, path.length());
		}
		return path;
	}
	
	
	
	
	private boolean areEqual(Map first, Map second) {
	    if (first.size() != second.size()) {
	        return false;
	    }
	    
	    for (Object key :  first.keySet()) {
	    	if (second.containsKey(key)) {
	    		if (!first.get(key).equals(second.get(key))){
	    			return false;
	    		}
	    	}
	    	else {
	    		return false;
	    	}
	    }
	    
	    return true;

	}

}
