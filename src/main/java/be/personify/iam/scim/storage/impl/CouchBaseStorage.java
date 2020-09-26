package be.personify.iam.scim.storage.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import com.couchbase.client.core.error.DocumentExistsException;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.query.QueryOptions;
import com.couchbase.client.java.query.QueryResult;

import be.personify.iam.scim.storage.ConfigurationException;
import be.personify.iam.scim.storage.ConstraintViolationException;
import be.personify.iam.scim.storage.DataException;
import be.personify.iam.scim.storage.Storage;
import be.personify.iam.scim.util.Constants;
import be.personify.util.SearchCriteria;
import be.personify.util.SearchCriterium;
import be.personify.util.SearchOperation;


/**
 * 
 * @author wouter vdb
 *
 */
public class CouchBaseStorage implements Storage {
	
	private static final String COUNT = "count";
	private static final String ORIENT_OPERATOR_PRESENT = " is not null ";
	private static final String ORIENT_OPERATOR_NOT_EQUALS = " <> ";
	private static final String ORIENT_OPERATOR_EQUALS = " = ";

	private static final Logger logger = LogManager.getLogger(CouchBaseStorage.class);
	
	private String type;
	
	@Value("${scim.storage.couchbase.host}")
	private String host;
	
	@Value("${scim.storage.couchbase.user}")
	private String user;
	
	@Value("${scim.storage.couchbase.password}")
	private String password;

	
   
    
    private static Cluster cluster;
    private static Bucket bucket;
    
    private String queryAll = null;
    private String querySelectCount = null;

    
	
    @Override
    public Map<String, Object> get(String id) {
    	return bucket.defaultCollection().get(id).contentAs(Map.class);
    }


   
    @Override
    public boolean delete(String id) {
    	bucket.defaultCollection().remove(id);
    	return true;
    }
    
    

    @Override
    public boolean deleteAll() {
    	throw new DataException("delete all not implemented");
    }

    
 
    @Override
    public void create(String id, Map<String, Object> object) throws ConstraintViolationException {
    	try {
    		bucket.defaultCollection().insert(id, object);
    	}
    	catch( DocumentExistsException dup ) {
    		throw new ConstraintViolationException(dup.getMessage());
    	}
    }
    
    
    

    @Override
    public void update(String id, Map<String, Object> object) {
    	try {
    		bucket.defaultCollection().upsert(id, object);
    	}
    	catch( Exception dup ) {
    		throw new DataException(dup.getMessage());
    	}
    }

    
   
    
    
    @Override
    public List<Map<String, Object>> search(SearchCriteria searchCriteria, int start, int count, String sortBy, String sortOrder) {
    	
    	StringBuilder builder = new StringBuilder(queryAll);
   		String query = constructQuery(searchCriteria, builder);
   		JsonObject namedParameters = JsonObject.create();
   		for ( SearchCriterium c : searchCriteria.getCriteria() ) {
   			if ( c.getSearchOperation().getParts() == 3) {
   				namedParameters.put(c.getKey(), c.getValue());
   			}
   		}
  		logger.debug("query {}", query);
  		QueryResult result = cluster.query(query, QueryOptions.queryOptions().parameters(namedParameters));
   		List<Map<String,Object>> resultList  = new ArrayList<>();
   		for( Map m : result.rowsAs(Map.class)) {
   			resultList.add(m);
   		}
   		return resultList;
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
					sb.append("$" + criterium.getKey());
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
		StringBuilder builder = new StringBuilder(querySelectCount);
   		String query = constructQuery(searchCriteria, builder);
   		JsonObject namedParameters = JsonObject.create();
   		for ( SearchCriterium c : searchCriteria.getCriteria() ) {
   			if ( c.getSearchOperation().getParts() == 3) {
   				namedParameters.put(c.getKey(), c.getValue());
   			}
   		}
  		QueryResult result = cluster.query(query, QueryOptions.queryOptions().parameters(namedParameters));
   		return (Integer)result.rowsAs(Map.class).get(0).get("count");
		
	}

    
    
    

    @Override
    public void flush() {
    }

    
    @Override
    public void initialize(String type) {
    	this.type = type;
    	 try {
    		 if ( cluster == null ) {
    			 cluster = Cluster.connect(host, user, password);
    			 bucket = cluster.bucket(type);
    			 
    		 }
    		 queryAll = "select * from `" + type + "`";
    		 querySelectCount = "select count(id) as count from `" + type + "`";
    		 
    	 }
    	 catch( Exception e) {
    		 e.printStackTrace();
    		 throw new ConfigurationException(e.getMessage());
    	 }
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
