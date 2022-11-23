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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoWriteException;
import com.mongodb.QueryOperators;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import be.personify.iam.scim.authentication.Consumer;
import be.personify.iam.scim.schema.Schema;
import be.personify.iam.scim.schema.SchemaAttribute;
import be.personify.iam.scim.schema.SchemaReader;
import be.personify.iam.scim.storage.ConstraintViolationException;
import be.personify.iam.scim.storage.DataException;
import be.personify.iam.scim.storage.Storage;
import be.personify.iam.scim.util.Constants;
import be.personify.util.LogicalOperator;
import be.personify.util.SearchCriteria;
import be.personify.util.SearchCriterium;
import be.personify.util.SearchOperation;
import be.personify.util.StringUtils;

/**
 * @author jingzhou wang
 * @author wouter vdb
 */
public class MongoStorage implements Storage {

	private static final Logger logger = LogManager.getLogger(MongoStorage.class);

	@Value("${scim.storage.mongo.constr}")
	private String constr;
	
	@Value("${scim.storage.mongo.connectionTimeout:5000}")
	private int connectionTimeout;
	
	@Value("${scim.storage.mongo.readTimeout:5000}")
	private int readTimeout;
	
	@Value("${scim.storage.mongo.maxConnectionPoolSize:4}")
	private int maxConnectionPoolSize;
	
	@Value("${scim.storage.mongo.serverSelectionTimeout:5000}")
	private int serverSelectionTimeout;
	
	@Value("${scim.storage.mongo.sslEnabled:false}")
	private boolean sslEnabled;

	
	@Value("${scim.storage.mongo.database}")
	private String database = "scim-database";

	@Value("${scim.storage.mongo.collection.users}")
	private String userCollection = "users";
	
	@Value("${scim.storage.mongo.collection.groups}")
	private String groupCollection = "groups";
	
	private String type = null;
	
	@Autowired
	private SchemaReader schemaReader;
	
	private MongoClient client;
	
	

	private static final String oid = "_id";
	private static final String $set = "$set";
	private static final String $regex = "$regex";
	private static final String start = "^";
	private static final String end = "$";
	private static final String minus = "-1";
	private static final String descending = "descending";

	private MongoCollection<Document> col;

	
	@Override
	public Map<String, Object> get(String id, Consumer consumer) {
		Document doc = col.find(new Document(oid, id)).first();
		logger.info("doc {}", doc);
		if (doc != null) {
			doc.remove(oid);
			doc.put(Constants.ID, id);
			if ( consumerCanRead(consumer, doc)) {
				return unsafetyfyAttributes(doc);
			}
		}
		return null;
	}

	
	
	@Override
	public Map<String, Object> get(String id, String version, Consumer consumer) {
		Document query = new Document(oid, id).append(Constants.KEY_META, new Document(Constants.KEY_VERSION, version));
		Document doc = col.find(query).first();
		if (doc != null) {
			doc.remove(oid);
			doc.put(Constants.ID, id);
			if ( consumerCanRead(consumer, doc)) {
				return unsafetyfyAttributes(doc);
			}
		}
		return null;
	}
	

	@Override
	public List<String> getVersions(String id, Consumer consumer) {
		List<String> vs = new ArrayList<>();
		Document query = new Document(oid, id);
		Document doc = col.find(query).first();
		if (doc != null) {
			vs.add((unsafetyfyAttributes((Document) doc.get(Constants.KEY_META)).getString(Constants.KEY_VERSION)));
		}
		return vs;
	}
	
	

	@Override
	public boolean delete(String id, Consumer consumer) {
		if ( !StringUtils.isEmpty(consumer.getTenant())) {
			Document doc = col.find(new Document(oid, id)).first();
			String tenant = (String)doc.get(Constants.TENANT_ATTRIBUTE);
			if ( StringUtils.isEmpty(tenant) || !tenant.equals(consumer.getTenant()) ) {
				throw new DataException("the consumer " + consumer.getClientid() + " can not delete a user of tenant " + tenant);
			}
		}
		return col.findOneAndDelete(new Document(oid, id)) != null;
	}

	
	@Override
	public boolean deleteAll(Consumer consumer) {
		//check tenant
		if ( !StringUtils.isEmpty(consumer.getTenant())) {
			throw new DataException("consumer of tenant " + consumer.getTenant() + " can not delete all data");
		}
		col.drop();
		return true;
	}

	
	/**
	 * Creates a new entry
	 */
	@Override
	public void create(String id, Map<String, Object> object, Consumer consumer) throws ConstraintViolationException {
		
		object = safetyfyAttributes(object);
		Document doc = new Document(object);
		
		//check if already present
		boolean found = entryExists(id);
		
		doc.remove(Constants.ID);
		doc.put(oid, id);
		
		//add tenant attribute
		if ( !StringUtils.isEmpty(consumer.getTenant())){
			doc.put(Constants.TENANT_ATTRIBUTE, consumer.getTenant());
		}
		
		
		try {
			if ( !found ) {
				col.insertOne(doc);
			}
			else {
				col.findOneAndUpdate(new Document(oid, id), new Document($set, doc));
			}
		}
		catch( MongoWriteException mwe ) {
			throw new DataException(mwe.getError().getMessage());
		}
	}


	private boolean entryExists(String id) {
		boolean found = false;
		List<Document> vlist = new ArrayList<>();
		vlist.add(new Document(oid, id));
		Document filter = new Document(QueryOperators.OR, vlist);
		if (col.find(filter).first() != null) {
			found = true;
		}
		return found;
	}

	
	
	
	@Override
	public void update(String id, Map<String, Object> object, Consumer consumer) {
		if ( !StringUtils.isEmpty(consumer.getTenant())) {
			Document doc = col.find(new Document(oid, id)).first();
			String tenant = (String)doc.get(Constants.TENANT_ATTRIBUTE);
			if ( StringUtils.isEmpty(tenant) || !tenant.equals(consumer.getTenant()) ) {
				throw new DataException("the consumer " + consumer.getClientid() + " can not update a user of tenant " + tenant);
			}
		}
		object = safetyfyAttributes(object);
		Document doc = new Document(object);
		Document query = new Document(oid, id);
		doc.remove(Constants.ID);
		col.findOneAndUpdate(query, new Document($set, doc));
	}
	
	

	@Override
	public List<Map> search(SearchCriteria searchCriteria, int start, int count, String sortBy, String sortOrder, Consumer consumer) {
		return search(searchCriteria, start, count, sortBy, sortOrder, null, consumer);
	}

	@Override
	public List<Map> search(SearchCriteria searchCriteria, int start, int count, String sortBy, String sortOrder, List<String> includeAttributes, Consumer consumer) {
		
		if ( searchCriteria == null ) {
			searchCriteria = new SearchCriteria();
		}
		
		//add tenant
		if ( !StringUtils.isEmpty(consumer.getTenant())) {
			searchCriteria.getCriteria().add(new SearchCriterium(Constants.TENANT_ATTRIBUTE, consumer.getTenant()));
		}

		logger.info("searchcriteria {}", searchCriteria);
		FindIterable<Document> finds = find(start, count, includeAttributes, getCriteria(searchCriteria));
		logger.info("finds {}", finds);
		sort(sortBy, sortOrder, finds);

		List<Map> all = new ArrayList<>();
		String id = null;
		for (Document doc : finds) {
			id = ((String) doc.remove(oid)).toString();
			doc.put(Constants.ID, id);
			all.add(unsafetyfyAttributes(doc));
		}
		return all;
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
		
		
		return col.countDocuments(getCriteria(searchCriteria));
	}

	
	
	private FindIterable<Document> find(int start, int count, List<String> includeAttributes, Bson query) {
		FindIterable<Document> finds;
		int skip = start -1;
		if (includeAttributes != null) {
			Document projection = new Document();
			for (String includeAttribute : includeAttributes) {
				projection.append(includeAttribute, 1);
			}
			finds = col.find(query).projection(projection).skip(skip).limit(count);
		} 
		else {
			finds = col.find(query).skip(skip).limit(count);
		}
		return finds;
	}

	
	private void sort(String sortBy, String sortOrder, FindIterable<Document> finds) {
		if (sortBy != null) {
			int order = 1;
			if (sortOrder != null) {
				if (descending.equals(sortOrder) || minus.equals(sortOrder)) {
					order = -1;
				}
			}
			finds.sort(new Document(sortBy, order));
		}
	}

	
	
	
	
	private Bson getCriteria(SearchCriteria searchCriteria) {
		
		if ( searchCriteria != null ) {
			
			List<Bson> crit = new ArrayList<Bson>();
			for ( SearchCriterium sc : searchCriteria.getCriteria() ) {
				crit.add(getCriterium(sc));
			}
			for ( SearchCriteria sc : searchCriteria.getGroupedCriteria() ) {
				crit.add(getCriteria(sc));
			}
			
			if ( crit.size() > 0 ) {
				
				if ( searchCriteria.getOperator().equals(LogicalOperator.AND)) {
					return Filters.and(crit);
				}
				else if ( searchCriteria.getOperator().equals(LogicalOperator.OR)) {
					return Filters.or(crit);
				}

			}
		}
		
		return new Document();
	}
	
	
	
	
	private Bson getCriterium( SearchCriterium sc ) {
		SearchOperation op = sc.getSearchOperation();
		String key = safeName(sc.getKey());
		if (SearchOperation.EQUALS.equals(op)) {
			return Filters.eq(key, sc.getValue());
		}
		else if (SearchOperation.NOT_EQUALS.equals(op)) {
			return Filters.ne(key, sc.getValue());
		}
		else if (SearchOperation.CONTAINS.equals(op)) {
			return Filters.eq(key, new Document($regex, sc.getValue()));
		}
		else if (SearchOperation.STARTS_WITH.equals(op)) {
			return Filters.eq(key, new Document($regex, startRgx((String) sc.getValue())));
		}
		else if (SearchOperation.ENDS_WITH.equals(op)) {
			return Filters.eq(key, new Document($regex, endRgx((String) sc.getValue())));
		}
		else if (SearchOperation.PRESENT.equals(op)) {
			return Filters.exists(key);
		}
		else if (SearchOperation.GREATER_THEN.equals(op)) {
			return Filters.gt(key, sc.getValue());
		} 
		else if (SearchOperation.GREATER_THEN_OR_EQUAL.equals(op)) {
			return Filters.gte(key, sc.getValue());
		}
		else if (SearchOperation.LESS_THEN.equals(op)) {
			return Filters.lt(key, sc.getValue());
		} 
		else if (SearchOperation.LESS_THEN_EQUAL.equals(op)) {
			return Filters.lte(key, sc.getValue());
		}
		else {
			throw new DataException("the operator " + op.name() + " is not implemented");
		}
	}
	
	

	private String startRgx(String value) {
		return start + value;
	}

	private String endRgx(String value) {
		return value + end;
	}
	

	@Override
	public void flush() {}

	
	@Override
	public void initialize(String type) {
		this.type = type;
		logger.info("initializing store of type {}", type);
		client = MongoClients.create(getMongoClientSettings());
		if ( type.equals(Constants.RESOURCE_TYPE_USER)) {
			col = client.getDatabase(database).getCollection(userCollection);
		}
		else if ( type.equalsIgnoreCase(Constants.RESOURCE_TYPE_GROUP)) {
			col = client.getDatabase(database).getCollection(groupCollection);
		}
		else {
			throw new DataException("the type " + type + " is not a valid resource type");
		}
		
		createUniqueIndexes(type);
		
	}
	
	

	
	/**
	 * Returns the settings for the mongodb connection
	 * @return MongoClientSettings the settings
	 */
	private MongoClientSettings getMongoClientSettings() {
		
		return MongoClientSettings.builder()
		        .applyToSocketSettings(builder -> {
		        	builder.connectTimeout(connectionTimeout, TimeUnit.MILLISECONDS);
		        	builder.readTimeout(readTimeout, TimeUnit.MILLISECONDS);
		        })
		        .applyToClusterSettings( builder -> {
		        	builder.serverSelectionTimeout(serverSelectionTimeout, TimeUnit.MILLISECONDS);
		        })
		        .applyToConnectionPoolSettings(builder -> builder.maxSize(maxConnectionPoolSize))
		        .applyConnectionString(new ConnectionString(constr))
		        .applyToSslSettings(builder -> builder.enabled(sslEnabled))
		        .build();
	}


	
	private void createUniqueIndexes(String type) {
		try {
			Schema schema = schemaReader.getSchemaByName(type);
			for ( SchemaAttribute a : schema.getAttributes()) {
				if ( a.getUniqueness() != null && (a.getUniqueness().equalsIgnoreCase("server") || a.getUniqueness().equalsIgnoreCase("global"))) {
					logger.info("creating index for type {} and attribute [{}]", type, a.getName());
					col.createIndex(Indexes.descending(a.getName()), new IndexOptions().background(true).unique(true));
				}
			}
		} 
		catch (Exception e) {
			throw new DataException(e.getMessage());
		}
	}
	
	
	
	
	public Map<String,Object> safetyfyAttributes( Map<String,Object> map) {
		if ( type.equalsIgnoreCase(Constants.RESOURCE_TYPE_GROUP)) {
			Map<String,Object> newMap = new HashMap<String,Object>();
			for (String key :  map.keySet() ) {
				Object o = map.get(key);
				if ( o instanceof Map ) {
					o = safetyfyAttributes((Map)o);
				}
				else if ( o instanceof List ) {
					List newList = new ArrayList();
					for ( Object oo : (List)o ) {
						if ( oo instanceof Map ) {
							oo = safetyfyAttributes((Map)oo);
						}
						newList.add(oo);
					}
					o = newList;
					
				}
				newMap.put(safeName(key), o);
			}
			return newMap;
		}
		return map;
	}
	
	
	
	private String safeName( String name ) {
		if ( name.startsWith("$")) {
			name = StringUtils.UNDERSCORE + name;
		}
		return name;
	}
	
	
	
	
	public Document unsafetyfyAttributes( Document map) {
		if ( type.equalsIgnoreCase(Constants.RESOURCE_TYPE_GROUP) && map != null) {
			Document newMap = new Document();
			for (String key :  map.keySet() ) {
				Object o = map.get(key);
				if ( o instanceof Document ) {
					o = unsafetyfyAttributes((Document)o);
				}
				else if ( o instanceof List ) {
					List newList = new ArrayList();
					for ( Object oo : (List)o ) {
						if ( oo instanceof Document ) {
							oo = unsafetyfyAttributes((Document)oo);
						}
						newList.add(oo);
					}
					o = newList;
					
				}
				if ( key.startsWith("_$")) {
					key = key.substring(1,key.length());
				}
				newMap.put(key, o);
			}
			return newMap;
		}
		return map;
	}


	@Override
	public boolean tenantCompatible() {
		return true;
	}
	
	
	private boolean consumerCanRead(Consumer consumer, Document doc) {
		if ( StringUtils.isEmpty(consumer.getTenant())) {
			return true;
		}
		else {
			String tenant = (String)doc.get(Constants.TENANT_ATTRIBUTE);
			if ( !StringUtils.isEmpty(tenant) && tenant.equals(consumer.getTenant()) ) {
				return true;
			}
		}
		return false;
	}
	
	
}
