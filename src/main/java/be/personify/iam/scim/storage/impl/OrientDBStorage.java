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
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;

import be.personify.iam.scim.storage.ConfigurationException;
import be.personify.iam.scim.storage.ConstraintViolationException;
import be.personify.iam.scim.storage.Storage;
import be.personify.iam.scim.util.Constants;
import be.personify.util.SearchCriteria;


/**
 * 
 * @author wouter vdb
 *
 */
public class OrientDBStorage implements Storage, DisposableBean {
	
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
	
   
	private OrientDB orientDB;
    private ODatabasePool pool;

    
    @Override
    public Map<String, Object> get(String id) {
    	try (ODatabaseSession db = pool.acquire()) {
    		OResultSet result = db.query("select * from " + type + " where id = ?", id);
    		OResult r = result.next();
    		Map<String, Object> m = resultToMap(r);
    		return m;
    	}
    }



	private Map<String, Object> resultToMap(OResult r) {
		Map<String,Object> m = new HashMap<>();
		for ( String key : r.getPropertyNames()) {
			logger.info("key {} value {}", key, r.getProperty(key));
			m.put(key, r.getProperty(key));
		}
		return m;
	}

    
    
    @Override
    public Map<String, Object> get(String id, String version) {
    	return null;
    }

    
    
    @Override
    public List<String> getVersions(String id) {
        List<String> vs = new ArrayList<>();
        return null;
    }
    

    
   
    @Override
    public boolean delete(String id) {
    	try (ODatabaseSession db = pool.acquire()) {
    		OResultSet result = db.query("select * from " + type + " where id = ?", id);
    		if ( result.hasNext() ) {
    			OResult r = result.next();
    			logger.info("object to delete found {}", r);
        		r.getRecord().get().delete();
        		return true;
    		}
    	}
    	return false;
    	
    }
    
    

    @Override
    public boolean deleteAll() {
        return true;
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
      
    }

    
   
    
    
    @Override
    public List<Map<String, Object>> search(SearchCriteria searchCriteria, int start, int count, String sortBy, String sortOrder) {
    	try (ODatabaseSession db = pool.acquire()) {
    		OResultSet result = db.query("select * from " + type);
    		List<Map<String,Object>> resultList  = new ArrayList<>();
    		while( result.hasNext()) {
    			resultList.add(resultToMap(result.next()));
    		}
    		return resultList;
    	}
    }
    
    
    
    
    @Override
	public long count(SearchCriteria searchCriteria) {
    	return 0l;	
	}

    
    
    

    @Override
    public void flush() {
    }

    
    @Override
    public void initialize(String type) {
    	this.type = type;
    	 try {
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
    				 OProperty idProp = typeClass.createProperty(Constants.ID, OType.STRING);
    				 idProp.createIndex(OClass.INDEX_TYPE.UNIQUE);
    			 }
    		 }
    		 
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



	

}
