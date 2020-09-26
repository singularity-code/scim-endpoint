package be.personify.iam.scim.storage.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Value;

import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.ODatabasePool;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.db.ODatabaseType;
import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.db.OrientDBConfig;
import com.orientechnologies.orient.core.db.OrientDBConfigBuilder;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;

import be.personify.iam.scim.storage.ConfigurationException;
import be.personify.iam.scim.storage.ConstraintViolationException;
import be.personify.iam.scim.storage.DataException;
import be.personify.iam.scim.storage.Storage;
import be.personify.iam.scim.util.Constants;
import be.personify.util.SearchCriteria;
import be.personify.util.SearchCriterium;
import be.personify.util.SearchOperation;
import be.personify.util.StringUtils;


/**
 * 
 * @author wouter vdb
 *
 */
public class OrientDBStorage implements Storage, DisposableBean {
	
	private static final String COUNT = "count";
	private static final String ORIENT_OPERATOR_PRESENT = " is not null ";
	private static final String ORIENT_OPERATOR_NOT_EQUALS = " <> ";
	private static final String ORIENT_OPERATOR_EQUALS = " = ";

	private static final Logger logger = LogManager.getLogger(OrientDBStorage.class);
	
	private String type;
	
	@Value("${scim.storage.orientdb.url}")
	private String url;
	
	@Value("${scim.storage.orientdb.database}")
	private String database;
	
	@Value("${scim.storage.orientdb.user}")
	private String user;
	
	@Value("${scim.storage.orientdb.password}")
	private String password;
	
	@Value("${scim.storage.orientdb.poolMin}")
	private int poolMin;
	
	@Value("${scim.storage.orientdb.poolMin}")
	private int poolMax;
	
	@Value("${scim.storage.orientdb.uniqueIndexes}")
	private String uniqueIndexes;
	
	@Value("${scim.storage.orientdb.indexes}")
	private String indexes;
	
   
	private static OrientDB orientDB;
    private static ODatabasePool pool;
    
    private String queryAll = null;
    private String queryDelete = null;
	private String queryFindById = null;
	private String querySelectCount = null;

    
    @Override
    public Map<String, Object> get(String id) {
    	try (ODatabaseSession db = pool.acquire()) {
    		OResultSet result = db.query(queryFindById, id);
    		if ( result.hasNext()) {
    			return resultToMap(result.next());
    		}
    	}
    	return null;
    }


   
    @Override
    public boolean delete(String id) {
    	try (ODatabaseSession db = pool.acquire()) {
    		db.command(queryDelete, id);
    		return true;
    	}
    	
    }
    
    

    @Override
    public boolean deleteAll() {
    	try (ODatabaseSession db = pool.acquire()) {
    		db.command("delete from " + type);
    		return true;
    	}
    }

    
 
    @Override
    public void create(String id, Map<String, Object> object) throws ConstraintViolationException {
    	try (ODatabaseSession db = pool.acquire()) {
    		ODocument doc = new ODocument(type);
    		for ( String key : object.keySet()) {
    			doc.field(key, object.get(key)); 
    		}
    		doc.save();
    	}
    	catch( ORecordDuplicatedException dup ) {
    		throw new ConstraintViolationException(dup.getMessage());
    	}
    }
    
    
    

    @Override
    public void update(String id, Map<String, Object> object) {
    	try (ODatabaseSession db = pool.acquire()) {
    		
    		OSQLSynchQuery<ODocument> query = new OSQLSynchQuery<ODocument>("select * from " + type + " where id = :id");
    		Map<String,Object> params = new HashMap<String,Object>();
    		params.put(Constants.ID, id);
    		List<ODocument> result = db.command(query).execute(params);
    		
    		for ( ODocument doc : result) {
    			for ( String key : object.keySet()) {
        			doc.field(key, object.get(key)); 
        		}
        		doc.save();
    		}
    	}
    }

    
   
    
    
    @Override
    public List<Map<String, Object>> search(SearchCriteria searchCriteria, int start, int count, String sortBy, String sortOrder) {
    	try (ODatabaseSession db = pool.acquire()) {
    		StringBuilder builder = new StringBuilder(queryAll);
    		String query = constructQuery(searchCriteria, builder);
    		List<Object> objects = new ArrayList<>();
    		for ( SearchCriterium c : searchCriteria.getCriteria() ) {
    			if ( c.getSearchOperation().getParts() == 3) {
    				objects.add(c.getValue());
    			}
    		}
    		logger.debug("query {}", query);
    		OResultSet result = db.query(query, objects);
    		List<Map<String,Object>> resultList  = new ArrayList<>();
    		while( result.hasNext()) {
    			resultList.add(resultToMap(result.next()));
    		}
    		return resultList;
    	}
    }
    
    
    private String constructQuery(SearchCriteria searchCriteria, StringBuilder sb) {
		if ( searchCriteria != null && searchCriteria.size() > 0) {
			sb.append(Constants.WHERE);
			SearchCriterium criterium = null;
			for ( int i = 0; i < searchCriteria.size(); i++) {
				criterium = searchCriteria.getCriteria().get(i);
				sb.append(criterium.getKey());
				sb.append(searchOperationToString(criterium.getSearchOperation()));
				if ( criterium.getSearchOperation().getParts() == 3) {
					sb.append(Constants.QUESTION_MARK__WITH_SPACES);
				}
				if ( i < (searchCriteria.size() -1)) {
					sb.append(Constants.AND_WITH_SPACES);
				}
			}
		}
		return sb.toString();
	}
    
    
    
    
    private Object searchOperationToString(SearchOperation searchOperation) {
    	if ( searchOperation.equals(SearchOperation.EQUALS)) {
    		return ORIENT_OPERATOR_EQUALS;
    	}
    	else if ( searchOperation.equals(SearchOperation.NOT_EQUALS)) {
    		return ORIENT_OPERATOR_NOT_EQUALS;
    	}
    	else if ( searchOperation.equals(SearchOperation.PRESENT)) {
    		return ORIENT_OPERATOR_PRESENT;
    	}
    	return null;
	}



	@Override
	public long count(SearchCriteria searchCriteria) {
		try (ODatabaseSession db = pool.acquire()) {
    		StringBuilder builder = new StringBuilder(querySelectCount);
    		String query = constructQuery(searchCriteria, builder);
    		List<Object> objects = new ArrayList<>();
    		for ( SearchCriterium c : searchCriteria.getCriteria() ) {
    			if ( c.getSearchOperation().getParts() == 3) {
    				objects.add(c.getValue());
    			}
    		}
    		OResultSet result = db.query(query, objects);
    		if( result.hasNext()) {
    			return (Long)result.next().getProperty(COUNT);
    		}
    	}
		return 0;
	}

    
    
    

    @Override
    public void flush() {
    }

    
    @Override
    public void initialize(String type) {
    	this.type = type;
    	 try {
    		 if ( orientDB == null ) {
	    		 orientDB = new OrientDB(url, user, password ,OrientDBConfig.defaultConfig());
	    		 
	    		 orientDB.createIfNotExists(database, ODatabaseType.PLOCAL, OrientDBConfig.defaultConfig());
	    		 
	    		 OrientDBConfigBuilder poolCfg = OrientDBConfig.builder();
	    		 poolCfg.addConfig(OGlobalConfiguration.DB_POOL_MIN, poolMin);
	    		 poolCfg.addConfig(OGlobalConfiguration.DB_POOL_MAX, poolMax);
	
	    		 pool = new ODatabasePool(orientDB,database, user , password, poolCfg.build());
	    		 
	    		 try (ODatabaseSession db = pool.acquire()) {
	    			 OSchema schema = db.getMetadata().getSchema();
	    			 if ( !schema.existsClass(type) ) {
	    				 OClass typeClass = schema.createClass(type);
	    				 String[] uniqueIndexesArray = uniqueIndexes.split(StringUtils.COMMA);
	    				 for ( String ui : uniqueIndexesArray) {
	    					 if ( ui.startsWith(type.toLowerCase() + StringUtils.COLON)) {
	    						 OProperty idProp = typeClass.createProperty(ui, OType.STRING);
	    						 idProp.createIndex(OClass.INDEX_TYPE.UNIQUE);
	    					 }
	    				 }
	    				 String[] indexesArray = indexes.split(StringUtils.COMMA);
	    				 for ( String ui : indexesArray) {
	    					 if ( ui.startsWith(type.toLowerCase() + StringUtils.COLON)) {
	    						 OProperty idProp = typeClass.createProperty(ui, OType.STRING);
	    						 idProp.createIndex(OClass.INDEX_TYPE.NOTUNIQUE);
	    					 }
	    				 }
	    			 }
	    		 }
    		 }
	    		 
	    	queryAll = "select * from " + type;
	    	queryFindById = queryAll + " where id = ?";
	    	queryDelete = "delete from " + type + " where id = ?";
	    	querySelectCount = "select count(id) as count from " + type;
    		 
    		 
    		 
    	 }
    	 catch( Exception e) {
    		 e.printStackTrace();
    		 throw new ConfigurationException(e.getMessage());
    	 }
    }
    
    
    
    @Override
    public void destroy() {
    	logger.info("closing orientdb pool and db");
        pool.close();
        orientDB.close();
    }
    
    
    
    private Map<String, Object> resultToMap(OResult r) {
		Map<String,Object> m = new HashMap<>();
		for ( String key : r.getPropertyNames()) {
			m.put(key, r.getProperty(key));
		}
		return m;
	}

    
    
    
    @Override
    public Map<String, Object> get(String id, String version) {
    	throw new DataException("versioning not implemented");
    }

    @Override
    public List<String> getVersions(String id) {
    	throw new DataException("versioning not implemented");
    }



	

}
