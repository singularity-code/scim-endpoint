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

public class PatchUtils {
	
	
	@Autowired
	private SchemaReader schemaReader;
	
	private static final Logger logger = LogManager.getLogger(PatchUtils.class);
	

	public void patchEntity(Map<String, Object> existingEntity, PatchOperation opType, String path, Object value, Schema schema) {
		
		Object entry = null;
		
		if ( opType == PatchOperation.add) {
			logger.info("adding {} to {}", value, path);
			entry = getPath(path, existingEntity, true, schema);
			logger.info(entry.getClass().getName() + " {} ", entry);
			if (entry instanceof List) {
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
					eMap.put(path, value);
				}
			} 
			else {
				logger.error("Cannot perform add patch: path {} value {} on {}", path, value );
			}
			
		}
		else if ( opType == PatchOperation.remove) {
			logger.info("removing {} from {}", value, path);
			List<String> segs = getPathSegments(path);
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
							logger.info("oo {}", oo);
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
		else if ( opType == PatchOperation.replace) {
			logger.debug("replace {} with {}", path, value);
			entry = getPath(path, existingEntity, false, schema);
			if (entry instanceof Map) {
				((Map) entry).putAll((Map) value);
			}
			else if (entry instanceof List) {
				List list = (List) entry;
				list.clear();
				list.addAll((List) value);
			} else {
				logger.error("Cannot perform replace patch: path {} value {}", path, value);
			}
		}
		
//		switch (opType) {
//		case "add":
//			logger.debug("adding {} to {} in {}", value, path, patchRequest);
//			Object entry = getPath(path, existingEntity, true, schema);
//			logger.info(entry.getClass().getName() + " {} ", entry);
//			if (entry instanceof List) {
//				((List) entry).addAll((List) value);
//			} 
//			else if (entry instanceof Map) {
//				Map<String, Object> eMap = (Map) entry;
//				Map<String, Object> aMap = (Map) value;
//				for (String key : aMap.keySet()) {
//					if (eMap.containsKey(key)) {
//						Object e1 = eMap.get(key);
//						if (e1 instanceof List) {
//							((List) e1).addAll((Collection) aMap.get(key));
//						} else {
//							eMap.put(key, aMap.get(key));
//						}
//					} else {
//						eMap.put(key, aMap.get(key));
//					}
//				}
//			} 
//			else {
//				logger.error("Cannot perform add patch: path {} value {} on {}", path, value, patchRequest);
//			}
//			break;
//		case "remove":
//			logger.debug("removing {} from {} in {}", value, path, patchRequest);
//			List<String> segs = getPathSegments(path);
//			if (segs.size() == 1) {
//				existingEntity.remove(path);
//			}
//			else {
//				logger.error("Cannot perform remove patch: path {} value {} on {}", path, value, patchRequest);
//			}
//			break;
//		case "replace":
//			logger.debug("replace {} with {} in {}", path, value, patchRequest);
//			entry = getPath(path, existingEntity, false, schema);
//			if (entry instanceof Map) {
//				((Map) entry).putAll((Map) value);
//			}
//			else if (entry instanceof List) {
//				List list = (List) entry;
//				list.clear();
//				list.addAll((List) value);
//			} else {
//				logger.error("Cannot perform replace patch: path {} value {} on {}", path, value, patchRequest);
//			}
//			break;
//
//		default:
//			return showError(404, "Invalid Operation");
//		}
		
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
		List<String> segs = getPathSegments(path);
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
							else if ( current instanceof List ) {
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
	
	
	public List<String> getPathSegments(String path) {
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
