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

import be.personify.iam.provisioning.connectors.scim.schema.SchemaAttributeType;
import be.personify.iam.scim.schema.Schema;
import be.personify.iam.scim.schema.SchemaAttribute;
import be.personify.iam.scim.schema.SchemaReader;
import be.personify.util.StringUtils;
import be.personify.util.scim.PatchOperation;

/**
 * Class that bundles patch operations
 * 
 * @author wouter
 *
 */
public class PatchUtils {
	
	
	private static final String URN = "urn:";

	@Autowired
	private SchemaReader schemaReader;
	
	private static final Logger logger = LogManager.getLogger(PatchUtils.class);
	

	public void patchEntity(Map<String, Object> existingEntity, PatchOperation opType, String path, Object value, Schema schema) {
		
		Object entry = null;
		
		//ADD
		if ( opType == PatchOperation.add) {
			logger.info("adding {} to {}", value, path);
			entry = getPath(path, existingEntity, true, schema);
			if ( entry == null ) {
				existingEntity.put(removeUrnFromString(path), value);
			}
			else if (entry instanceof List) {
				List eList = (List)entry;
				logger.debug("its a list {}", eList);
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
			} 
			else if (entry instanceof Map) {
				logger.debug("its a map");
				Map<String, Object> eMap = (Map) entry;
				if ( value instanceof Map ) {
					logger.debug("value is a map");
					Map<String, Object> aMap = (Map) value;
					for (String key : aMap.keySet()) {
						logger.debug("key {}", key);
						if (eMap.containsKey(key)) {
							Object e1 = eMap.get(key);
							if (e1 instanceof List) {
								Collection c = (Collection) aMap.get(key);
								//((List) e1).addAll(c);
								logger.debug("c {}", c);
								List currentList = ((List) e1);
								logger.debug("currentList {}", currentList);
								for ( Object co : c ) {
									logger.debug("co {}", co);
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
					logger.debug("entry {} path {} list {}", entry , path, value );
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
			logger.debug("segs {} {}", segs, segs.size());
			if (segs.size() == 1) {
				if ( value instanceof List ) {
					List entriesToRemove = (List)value;
					logger.debug("entriesToRemove {}", entriesToRemove);
					Object existingAttribute = existingEntity.get(path);
					if ( existingAttribute instanceof List ) {
						logger.debug("existing attribute is a list", existingAttribute);
						List existingAttributeList = (List)existingAttribute;
						List newList = new ArrayList();
						for (Object oo : existingAttributeList ) {
							if ( oo instanceof Map ) {
								logger.debug("oo is a map");
								Map ooo = (Map)oo;
								for ( Object ee : entriesToRemove ) {
									if ( ee instanceof Map) {
										if ( !areEqual(ooo, (Map)ee)) {
											logger.debug("ooo {} not equals ee {}", ooo, (Map)ee );
											newList.add(oo);
										}
									}
								}
							}
							
							
							if( !entriesToRemove.contains(oo)) {
								
								
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
			entry = getPath(path, existingEntity, false, schema);
			logger.debug("entry {}", entry);
			if (entry instanceof Map) {
				((Map) entry).putAll((Map) value);
			}
			else if (entry instanceof List) {
				List list = (List) entry;
				list.clear();
				list.addAll((List) value);
			}
			else if ( entry instanceof String ) {
				existingEntity.put(removeUrnFromString(path), value);
			}
			else {
				logger.error("Cannot perform replace patch: path {} value {}", path, value);
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
		if (StringUtils.isEmpty(path)) {
			return entity;
		}
		List<String> segs = getPathSegments(path, schema);
		Object current = entity;
		while (!segs.isEmpty()) {
			String seg = segs.remove(0);
			logger.info(current.getClass().getName() + " {} ", seg);
			if (current instanceof Map) {
				//parse conditions
				String conditions = StringUtils.EMPTY_STRING;
				logger.info("seg {}", seg);
				if ( seg.endsWith("]")) {
					int startCondition = seg.indexOf("[");
					conditions = seg.substring(startCondition, seg.length());
					seg = seg.substring(0, startCondition );
					logger.info("conditions {} seg {}", conditions, seg);
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
					SchemaAttribute attribute = schema.getAttribute(seg);
					if ( path.startsWith(URN)) {
						String schemaString = path.substring(0, path.lastIndexOf(StringUtils.COLON));
						logger.info("custom schema string {}", schemaString);
						Schema customSchema = schemaReader.getSchema(schemaString);
						logger.info("custom schema {}", customSchema);
						attribute = customSchema.getAttribute(seg);
					}
					
					
					
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
								entity.put(path, l);
								current = l;
								//((List)current).add(list);
							}
							
						}
					}
					logger.info("seg {} attribute {}", seg, attribute);
				}
				else {
					if ( current instanceof List && ((List)current).size() == 0 ) {
						
					}
				}
			}
			else if ( current instanceof List ) {
				logger.info("list seg {}", seg);
			}
			else {
				return null;
			}
		}
		return current;
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
		if ( path.startsWith(URN)) {
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
