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
package be.personify.iam.scim.storage.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.couchbase.client.core.error.DocumentExistsException;
import com.couchbase.client.core.error.DocumentNotFoundException;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.manager.bucket.BucketSettings;
import com.couchbase.client.java.manager.bucket.BucketType;
import com.couchbase.client.java.manager.query.CreatePrimaryQueryIndexOptions;
import com.couchbase.client.java.query.QueryOptions;
import com.couchbase.client.java.query.QueryResult;

import be.personify.iam.scim.authentication.Consumer;
import be.personify.iam.scim.schema.Schema;
import be.personify.iam.scim.schema.SchemaAttribute;
import be.personify.iam.scim.schema.SchemaReader;
import be.personify.iam.scim.storage.ConfigurationException;
import be.personify.iam.scim.storage.ConstraintViolationException;
import be.personify.iam.scim.storage.DataException;
import be.personify.iam.scim.storage.Storage;
import be.personify.iam.scim.storage.util.CouchBaseUtil;
import be.personify.iam.scim.util.Constants;
import be.personify.util.SearchCriteria;
import be.personify.util.SearchCriterium;
import be.personify.util.SearchOperation;
import be.personify.util.SortOrder;
import be.personify.util.StringUtils;

/**
 * 
 * @author wouter vdb
 * 
 * Couchbase storage implementation
 * 
 *
 **/
public class CouchBaseStorage implements Storage {

	private static final String IX = "ix_";
	private static final String OFFSET_WITH_SPACES = " offset ";
	private static final String LIMIT_WITH_SPACES = " limit ";


	private static final Logger logger = LogManager.getLogger(CouchBaseStorage.class);
	
	@Autowired
	private SchemaReader schemaReader;

	@Value("${scim.storage.couchbase.host}")
	private String host;

	@Value("${scim.storage.couchbase.user}")
	private String user;

	@Value("${scim.storage.couchbase.password}")
	private String password;
	
	@Value("${scim.storage.couchbase.indexes}")
	private String indexes;

	private String type;

	private Cluster cluster;
	private Bucket bucket;

	private String queryAll = null;
	private String querySelectCount = null;
	

	/**
	 * Creates the entry
	 */
	@Override
	public void create(String id, Map<String, Object> object, Consumer consumer) throws ConstraintViolationException {
		try {
			//add tenant attribute
			if ( !StringUtils.isEmpty(consumer.getTenant())){
				object.put(Constants.TENANT_ATTRIBUTE, consumer.getTenant());
			}
			//bucket.defaultCollection().insert(id, object);
			bucket.defaultCollection().upsert(id, object);
		} 
		catch (DocumentExistsException dup) {
			throw new ConstraintViolationException(dup.getMessage());
		}
	}

	/**
	 * Gets the entry by id
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> get(String id, Consumer consumer) {
		try {
			if ( StringUtils.isEmpty(consumer.getTenant())) {
				return bucket.defaultCollection().get(id).contentAs(Map.class);
			}
			else {
				Map<String,Object> found = bucket.defaultCollection().get(id).contentAs(Map.class);
				String tenant = (String)found.get(Constants.TENANT_ATTRIBUTE);
				if ( !StringUtils.isEmpty(tenant) && tenant.equals(consumer.getTenant()) ) {
					return found;
				}
				else {
					throw new DataException("consumer from tenant" + consumer.getTenant() + " can not read object from tenant " + tenant);
				}
			}
			
		}
		catch( DocumentNotFoundException dnfe ) {
			logger.debug("document with id " + id + "not found", dnfe);
			return null;
		}
	}
	
	

	/**
	 * Delete the entry by id
	 */
	@Override
	public boolean delete(String id, Consumer consumer) {
		if ( !StringUtils.isEmpty(consumer.getTenant())) {
			Map<String,Object> storedObject = bucket.defaultCollection().get(id).contentAs(Map.class);
			if ( !StringUtils.isEmpty(storedObject)) {
				String tenant = (String)storedObject.get(Constants.TENANT_ATTRIBUTE);
				if ( StringUtils.isEmpty(tenant) || !tenant.equals(consumer.getTenant()) ) {
					throw new DataException("the consumer " + consumer.getClientid() + " can not delete a user of tenant " + tenant);
				}
			}
			else {
				throw new DataException("resource with id " + id + " not found for consumer of tenant " + consumer.getTenant());
			}
		}
		bucket.defaultCollection().remove(id);
		return true;
	}
	
	
	

	/**
	 * Updates the thing
	 */
	@Override
	public void update(String id, Map<String, Object> object, Consumer consumer) {
		try {
			if ( !StringUtils.isEmpty(consumer.getTenant())) {
				Map<String,Object> storedObject = bucket.defaultCollection().get(id).contentAs(Map.class);
				String tenant = (String)storedObject.get(Constants.TENANT_ATTRIBUTE);
				if ( StringUtils.isEmpty(tenant) || !tenant.equals(consumer.getTenant()) ) {
					throw new DataException("the consumer " + consumer.getClientid() + " can not update a user of tenant " + tenant);
				}
			}
			bucket.defaultCollection().upsert(id, object);
		} 
		catch (Exception dup) {
			throw new DataException(dup.getMessage());
		}
	}
	
	
	

	@SuppressWarnings("rawtypes")
	@Override
	public List<Map> search(SearchCriteria searchCriteria, int start, int count, String sortBy, String sortOrder, Consumer consumer) {
		return search(searchCriteria, start, count, sortBy, sortOrder, null, consumer);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public List<Map> search(SearchCriteria searchCriteria, int start, int count, String sortBy, String sortOrder, List<String> includeAttributes, Consumer consumer) {
		
		if ( searchCriteria == null ) {
			searchCriteria = new SearchCriteria();
		}
		
		//add tenant
		if ( !StringUtils.isEmpty(consumer.getTenant())) {
			searchCriteria.getCriteria().add(new SearchCriterium(Constants.TENANT_ATTRIBUTE, consumer.getTenant()));
		}
		
		String query = queryAll;
		if (includeAttributes != null) {
			logger.info("includeAttributes present");
			StringBuffer b = new StringBuffer("select ");
			for (int i = 0; i < includeAttributes.size(); i++) {
				b.append(includeAttributes.get(i));
				if (i != includeAttributes.size() - 1) {
					b.append(StringUtils.COMMA);
				}
			}
			b.append(" from `" + type + "` ");
			query = b.toString();
		}
		
		
		query = query + constructUnnestString(searchCriteria);
		
		if (searchCriteria != null && searchCriteria.size() > 0) {
			query = query + Constants.WHERE;
		}
		StringBuilder builder = CouchBaseUtil.constructQuery(searchCriteria, new StringBuilder(query),0);
		JsonObject namedParameters = composeNamedParameters(searchCriteria,0,null);
		
		builder.append(CouchBaseUtil.getSort(sortBy, sortOrder, SortOrder.ascending));
		
		builder.append(LIMIT_WITH_SPACES + count + OFFSET_WITH_SPACES + (start - 1));
		logger.info("query {}", builder);
		try {
			QueryResult result = cluster.query(builder.toString(), QueryOptions.queryOptions().parameters(namedParameters));
			List<Map> list = new ArrayList<>();
			List<Map> maps = result.rowsAs(Map.class);
			Iterator<Map> iter = maps.iterator();
			while (iter.hasNext()) {
				Map m = iter.next();
				if (m.containsKey(type)) {
					list.add((Map) m.get(type));
				} 
				else {
					list.add(m);
				}
			}
			return list;
		}
		catch (Exception e) {
			throw new DataException(e.getMessage());
		}
	}
	

	

	@Override
	public long count(SearchCriteria searchCriteria, Consumer consumer) {
		
		if ( searchCriteria == null ) {
			searchCriteria = new SearchCriteria();
		}
		
		//add tenant
		if ( !StringUtils.isEmpty(consumer.getTenant())) {
			searchCriteria.getCriteria().add(new SearchCriterium(Constants.TENANT_ATTRIBUTE, consumer.getTenant()));
		}
		
		StringBuilder builder = new StringBuilder(querySelectCount);
		if (searchCriteria != null && searchCriteria.size() > 0) {
			builder.append(Constants.WHERE);
		}
		builder = CouchBaseUtil.constructQuery(searchCriteria, builder,0);
		JsonObject namedParameters = composeNamedParameters(searchCriteria,0, null);
		QueryResult result = cluster.query(builder.toString(), QueryOptions.queryOptions().parameters(namedParameters));
		return (Integer) result.rowsAs(Map.class).get(0).get(Constants.COUNT);
	}
	

	
	
	private JsonObject composeNamedParameters(SearchCriteria searchCriteria, int count, JsonObject namedParameters) {
		if ( namedParameters == null ) {
			namedParameters = JsonObject.create();
		}
		for (SearchCriterium c : searchCriteria.getCriteria()) {
			if (c.getSearchOperation().getParts() == 3) {
			
				String key = CouchBaseUtil.safeSubAttribute( c.getKey() + StringUtils.UNDERSCORE + count);
				
				if ( c.getSearchOperation() == SearchOperation.CONTAINS) {
					namedParameters.put(key, "%" + c.getValue() + "%");
				}
				else if ( c.getSearchOperation() == SearchOperation.ENDS_WITH) {
					namedParameters.put(key, "%" + c.getValue());
				}
				else if ( c.getSearchOperation() == SearchOperation.STARTS_WITH) {
					namedParameters.put(key, c.getValue() + "%");
				}
				else {
					namedParameters.put(key, c.getValue());
				}

				count++;
			}
		}
		for (SearchCriteria sc : searchCriteria.getGroupedCriteria()) {
			namedParameters = composeNamedParameters( sc, count, namedParameters);
			count = count + 100;
		}
		return namedParameters;
	}

	
	
	
	@Override
	public void flush() {}

	
	
	/**
	 * Initializes the thing
	 */
	@Override
	public void initialize(String type) {
		try {
			this.type = type;
			cluster = Cluster.connect(host, user, password);
			
			//check if the bucket exists
			Map<String, BucketSettings> allBuckets = cluster.buckets().getAllBuckets();
			if ( !allBuckets.containsKey(type)) {
				
				//create bucket
				cluster.buckets().createBucket(BucketSettings.create(type).bucketType(BucketType.COUCHBASE).ramQuotaMB(120).numReplicas(1).replicaIndexes(true).flushEnabled(true));
				cluster.queryIndexes().createPrimaryIndex(type, CreatePrimaryQueryIndexOptions.createPrimaryQueryIndexOptions().indexName(IX + type));
				
				//create indexes
				String[] splitted = indexes.split(StringUtils.COMMA);
				for ( String s : splitted ) {
					String[] pp = s.split(StringUtils.COLON);
					if ( pp[0].toLowerCase().equals(type.toLowerCase())) {
						cluster.queryIndexes().createIndex(type, IX + pp[1], Arrays.asList(new String[] {pp[1]}));
					}
				}
				
			}
			
			bucket = cluster.bucket(type);
			
			queryAll = "select t.* from `" + type + "` as t";
			querySelectCount = "select count(id) as count from `" + type + "` as t";
			
			
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new ConfigurationException(e.getMessage());
		}
	}

	private String constructUnnestString(SearchCriteria criteria ) {
		Schema schema = schemaReader.getSchemaByName(type);
		StringBuffer u = new StringBuffer(StringUtils.SPACE);
		if ( criteria != null ) {
			for ( SchemaAttribute a : schema.getAttributes()) {
				if ( a.getType().equals("complex") && a.isMultiValued()) {
					if ( criteria.containsKey(a.getName())) {
						logger.debug("criteria contains [{}], adding unnest", a.getName());
						u.append("unnest t.`").append(a.getName()).append("` as `").append(a.getName()).append("` ");
					}
				}
			}
		}
		return u.toString();
	}

	@Override
	public Map<String, Object> get(String id, String version, Consumer consumer) {
		throw new DataException("versioning not implemented");
	}

	@Override
	public List<String> getVersions(String id, Consumer consumer) {
		throw new DataException("versioning not implemented");
	}

	@Override
	public boolean deleteAll(Consumer consumer) {
		throw new DataException("delete all not implemented");
	}

	@Override
	public boolean tenantCompatible() {
		return true;
	}
}
