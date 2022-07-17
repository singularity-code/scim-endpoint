package be.personify.iam.scim.storage.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
	public void create(String id, Map<String, Object> object) throws ConstraintViolationException {
		try {
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
	public Map<String, Object> get(String id) {
		try {
			return bucket.defaultCollection().get(id).contentAs(Map.class);
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
	public boolean delete(String id) {
		bucket.defaultCollection().remove(id);
		return true;
	}

	/**
	 * Updates the thing
	 */
	@Override
	public void update(String id, Map<String, Object> object) {
		try {
			bucket.defaultCollection().upsert(id, object);
		} 
		catch (Exception dup) {
			throw new DataException(dup.getMessage());
		}
	}

	@SuppressWarnings("rawtypes")
	@Override
	public List<Map> search(SearchCriteria searchCriteria, int start, int count, String sortBy, String sortOrder) {
		return search(searchCriteria, start, count, sortBy, sortOrder, null);
	}

	@SuppressWarnings("rawtypes")
	@Override
	public List<Map> search(SearchCriteria searchCriteria, int start, int count, String sortBy, String sortOrder, List<String> includeAttributes) {
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
	public long count(SearchCriteria searchCriteria) {
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
	public void flush() {
	}

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
				cluster.queryIndexes().createPrimaryIndex(type, CreatePrimaryQueryIndexOptions.createPrimaryQueryIndexOptions().indexName("ix_" + type));
				
				//create indexes
				String[] splitted = indexes.split(StringUtils.COMMA);
				for ( String s : splitted ) {
					String[] pp = s.split(StringUtils.COLON);
					if ( pp[0].toLowerCase().equals(type.toLowerCase())) {
						cluster.queryIndexes().createIndex(type, "ix_" + pp[1], Arrays.asList(new String[] {pp[1]}));
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
		Schema schema = schemaReader.getSchemaByResourceType(type);
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
	public Map<String, Object> get(String id, String version) {
		throw new DataException("versioning not implemented");
	}

	@Override
	public List<String> getVersions(String id) {
		throw new DataException("versioning not implemented");
	}

	@Override
	public boolean deleteAll() {
		throw new DataException("delete all not implemented");
	}
}
