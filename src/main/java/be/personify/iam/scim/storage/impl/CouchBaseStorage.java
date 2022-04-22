package be.personify.iam.scim.storage.impl;

import be.personify.iam.scim.storage.ConfigurationException;
import be.personify.iam.scim.storage.ConstraintViolationException;
import be.personify.iam.scim.storage.DataException;
import be.personify.iam.scim.storage.Storage;
import be.personify.iam.scim.util.Constants;
import be.personify.util.SearchCriteria;
import be.personify.util.SearchCriterium;
import be.personify.util.SearchOperation;
import be.personify.util.StringUtils;
import com.couchbase.client.core.error.DocumentExistsException;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.Cluster;
import com.couchbase.client.java.json.JsonObject;
import com.couchbase.client.java.query.QueryOptions;
import com.couchbase.client.java.query.QueryResult;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;

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

	private static final String COUCHBASE_OPERATOR_PRESENT = " is not null ";
	private static final String COUCHBASE_OPERATOR_NOT_EQUALS = " <> ";
	private static final String COUCHBASE_OPERATOR_EQUALS = " = ";
	private static final String COUCHBASE_OPERATOR_GT = " > ";
	private static final String COUCHBASE_OPERATOR_GTE = " >= ";
	private static final String COUCHBASE_OPERATOR_LT = " < ";
	private static final String COUCHBASE_OPERATOR_LTE = " <= ";

	private static final Logger logger = LogManager.getLogger(CouchBaseStorage.class);

	@Value("${scim.storage.couchbase.host}")
	private String host;

	@Value("${scim.storage.couchbase.user}")
	private String user;

	@Value("${scim.storage.couchbase.password}")
	private String password;

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
			bucket.defaultCollection().insert(id, object);
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
		return bucket.defaultCollection().get(id).contentAs(Map.class);
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
		StringBuilder builder = constructQuery(searchCriteria, new StringBuilder(query));
		JsonObject namedParameters = composeNamedParameters(searchCriteria);

		builder.append(LIMIT_WITH_SPACES + count + OFFSET_WITH_SPACES + (start - 1));
		logger.info("query {}", builder);
		try {
			QueryResult result = cluster.query(builder.toString(),QueryOptions.queryOptions().parameters(namedParameters));
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
		} catch (Exception e) {
			throw new DataException(e.getMessage());
		}
	}

	@Override
	public long count(SearchCriteria searchCriteria) {
		StringBuilder builder = new StringBuilder(querySelectCount);
		builder = constructQuery(searchCriteria, builder);
		JsonObject namedParameters = composeNamedParameters(searchCriteria);
		QueryResult result = cluster.query(builder.toString(), QueryOptions.queryOptions().parameters(namedParameters));
		return (Integer) result.rowsAs(Map.class).get(0).get(Constants.COUNT);
	}

	private JsonObject composeNamedParameters(SearchCriteria searchCriteria) {
		JsonObject namedParameters = JsonObject.create();
		for (SearchCriterium c : searchCriteria.getCriteria()) {
			if (c.getSearchOperation().getParts() == 3) {
				namedParameters.put(safeSubAttribute(c.getKey()), c.getValue());
			}
		}
		return namedParameters;
	}

	private StringBuilder constructQuery(SearchCriteria searchCriteria, StringBuilder sb) {
		if (searchCriteria != null && searchCriteria.size() > 0) {
			sb.append(Constants.WHERE);
			SearchCriterium criterium = null;
			for (int i = 0; i < searchCriteria.size(); i++) {
				criterium = searchCriteria.getCriteria().get(i);
				sb.append(criterium.getKey());
				sb.append(searchOperationToString(criterium.getSearchOperation()));
				if (criterium.getSearchOperation().getParts() == 3) {
					sb.append("$" + safeSubAttribute(criterium.getKey()));
				}
				if (i < (searchCriteria.size() - 1)) {
					sb.append(Constants.AND_WITH_SPACES);
				}
			}
		}
		return sb;
	}

	private String safeSubAttribute(String s) {
		return s.replace(StringUtils.DOT, StringUtils.EMPTY_STRING);
	}

	private Object searchOperationToString(SearchOperation searchOperation) {
		if (searchOperation.equals(SearchOperation.EQUALS)) {
			return COUCHBASE_OPERATOR_EQUALS;
		} else if (searchOperation.equals(SearchOperation.NOT_EQUALS)) {
			return COUCHBASE_OPERATOR_NOT_EQUALS;
		} else if (searchOperation.equals(SearchOperation.PRESENT)) {
			return COUCHBASE_OPERATOR_PRESENT;
		} else if (searchOperation.equals(SearchOperation.GREATER_THEN)) {
			return COUCHBASE_OPERATOR_GT;
		} else if (searchOperation.equals(SearchOperation.GREATER_THEN_OR_EQUAL)) {
			return COUCHBASE_OPERATOR_GTE;
		} else if (searchOperation.equals(SearchOperation.LESS_THEN)) {
			return COUCHBASE_OPERATOR_LT;
		} else if (searchOperation.equals(SearchOperation.LESS_THEN_EQUAL)) {
			return COUCHBASE_OPERATOR_LTE;
		}
		throw new DataException("search operation " + searchOperation.name() + " not implemented");
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
			bucket = cluster.bucket(type);
			queryAll = "select * from `" + type + "`";
			querySelectCount = "select count(id) as count from `" + type + "`";
		}
		catch (Exception e) {
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

	@Override
	public boolean deleteAll() {
		throw new DataException("delete all not implemented");
	}
}
