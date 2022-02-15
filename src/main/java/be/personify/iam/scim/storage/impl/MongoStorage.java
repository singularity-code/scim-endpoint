package be.personify.iam.scim.storage.impl;

import be.personify.iam.scim.storage.ConstraintViolationException;
import be.personify.iam.scim.storage.DataException;
import be.personify.iam.scim.storage.Storage;
import be.personify.iam.scim.util.Constants;
import be.personify.util.SearchCriteria;
import be.personify.util.SearchCriterium;
import be.personify.util.SearchOperation;
import com.mongodb.QueryOperators;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.BsonBinary;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author jingzhou wang
 * @author wouter vdb
 */
public class MongoStorage implements Storage {

	private static final Logger logger = LogManager.getLogger(MongoStorage.class);

	@Value("${scim.storage.mongo.constr}")
	private String constr;

	@Value("${scim.storage.mongo.users.database}")
	private String userDatabase = "users";

	@Value("${scim.storage.mongo.users.collection}")
	private String userCollection = "users";
	
	
	@Value("${scim.storage.mongo.groups.database}")
	private String groupDatabase = "groups";

	@Value("${scim.storage.mongo.groups.collection}")
	private String groupCollection = "groups";
	
	private String type = null;
	
	

	private static final String oid = "_id";
	private static final String userName = "userName";
	private static final String extid = "externalId";
	private static final String $set = "$set";
	private static final String $regex = "$regex";
	private static final String start = "^";
	private static final String end = "$";
	private static final String minus = "-1";
	private static final String descending = "descending";
	private static final String used = "either id, externalId, or userName had been used";

	private MongoCollection<Document> col;

	@Override
	public Map<String, Object> get(String id) {
		Document query = new Document(oid, new BsonBinary(UUID.fromString(id)));
		Document doc = col.find(query).first();
		if (doc != null) {
			doc.remove(oid);
			doc.put(Constants.ID, id);
		}
		return doc;
	}

	@Override
	public Map<String, Object> get(String id, String version) {
		Document query = new Document(oid, new BsonBinary(UUID.fromString(id))).append(Constants.KEY_META,
				new Document(Constants.KEY_VERSION, version));
		Document doc = col.find(query).first();
		if (doc != null) {
			doc.remove(oid);
			doc.put(Constants.ID, id);
		}
		return doc;
	}

	@Override
	public List<String> getVersions(String id) {
		List<String> vs = new ArrayList<>();
		Document query = new Document(oid, new BsonBinary(UUID.fromString(id)));
		Document doc = col.find(query).first();
		if (doc != null) {
			vs.add(((Document) doc.get(Constants.KEY_META)).getString(Constants.KEY_VERSION));
		}
		return vs;
	}

	@Override
	public boolean delete(String id) {
		Document query = new Document(oid, new BsonBinary(UUID.fromString(id)));
		return col.findOneAndDelete(query) != null;
	}

	@Override
	public boolean deleteAll() {
		col.drop();
		return true;
	}

	@Override
	public void create(String id, Map<String, Object> object) throws ConstraintViolationException {
		logger.info("creating new object {} {}", id, object);
		Document doc = new Document(object);
		List<Document> vlist = new ArrayList<>();
		BsonBinary objId = new BsonBinary(UUID.fromString(id));
		if ( type.equals( Constants.RESOURCE_TYPE_USER ) ) {
			vlist.add(new Document(oid, objId));
			vlist.add(new Document(extid, doc.getString(extid)));
			vlist.add(new Document(userName, doc.getString(userName)));
			Document filter = new Document(QueryOperators.OR, vlist);
			if (col.find(filter).first() != null) {
				throw new ConstraintViolationException(used);
			}
		}
		else if ( type.equals( Constants.RESOURCE_TYPE_GROUP ) ) {
			vlist.add(new Document(oid, objId));
			Document filter = new Document(QueryOperators.OR, vlist);
			if (col.find(filter).first() != null) {
				throw new ConstraintViolationException(used);
			}
		}
		doc.remove(Constants.ID);
		doc.put(oid, objId);
		col.insertOne(doc);
	}

	@Override
	public void update(String id, Map<String, Object> object) {
		Document doc = new Document(object);
		Document query = new Document(oid, new BsonBinary(UUID.fromString(id)));
		doc.remove(Constants.ID);
		col.findOneAndUpdate(query, new Document($set, doc));
	}

	@Override
	public List<Map> search(SearchCriteria searchCriteria, int start, int count, String sortBy, String sortOrder) {
		return search(searchCriteria, start, count, sortBy, sortOrder, null);
	}

	@Override
	public List<Map> search(SearchCriteria searchCriteria, int start, int count, String sortBy, String sortOrder,
			List<String> includeAttributes) {

		Document query = new Document();
		if (searchCriteria != null && searchCriteria.getCriteria() != null && searchCriteria.getCriteria().size() > 0) {
			searchCriteria.getCriteria().forEach(sc -> genQuery(query, sc));
		}

		FindIterable<Document> finds = find(start, count, includeAttributes, query);
		sort(sortBy, sortOrder, finds);

		List<Map> all = new ArrayList<>();
		String id = null;
		for (Document doc : finds) {
			id = ((UUID) doc.remove(oid)).toString();
			doc.put(Constants.ID, id);
			all.add(doc);
		}
		return all;
	}

	private FindIterable<Document> find(int start, int count, List<String> includeAttributes, Document query) {
		FindIterable<Document> finds;
		int skip = (start - 1) * count;
		if (includeAttributes != null) {
			Document projection = new Document();
			for (String includeAttribute : includeAttributes) {
				projection.append(includeAttribute, 1);
			}
			finds = col.find(query).projection(projection).skip(skip).limit(count);
		} else {
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

	@Override
	public long count(SearchCriteria searchCriteria) {
		Document query = new Document();
		if (searchCriteria != null && searchCriteria.getCriteria() != null && searchCriteria.getCriteria().size() > 0) {
			searchCriteria.getCriteria().forEach(sc -> genQuery(query, sc));
		}
		return col.countDocuments(query);
	}

	private void genQuery(Document query, SearchCriterium sc) {
		if (sc != null) {
			SearchOperation op = sc.getSearchOperation();
			if (SearchOperation.EQUALS.equals(op)) {
				query.append(sc.getKey(), sc.getValue());
			} else if (SearchOperation.NOT_EQUALS.equals(op)) {
				query.append(sc.getKey(), new Document(QueryOperators.NE, sc.getValue()));
			} else if (SearchOperation.CONTAINS.equals(op)) {
				query.append(sc.getKey(), new Document($regex, sc.getValue()));
			} else if (SearchOperation.STARTS_WITH.equals(op)) {
				query.append(sc.getKey(), new Document($regex, startRgx((String) sc.getValue())));
			} else if (SearchOperation.ENDS_WITH.equals(op)) {
				query.append(sc.getKey(), new Document($regex, endRgx((String) sc.getValue())));
			} else if (SearchOperation.PRESENT.equals(op)) {
				query.append(sc.getKey(), new Document(QueryOperators.EXISTS, true));
			} else if (SearchOperation.GREATER_THEN.equals(op)) {
				query.append(sc.getKey(), new Document(QueryOperators.GT, sc.getValue()));
			} else if (SearchOperation.GREATER_THEN_OR_EQUAL.equals(op)) {
				query.append(sc.getKey(), new Document(QueryOperators.GTE, sc.getValue()));
			} else if (SearchOperation.LESS_THEN.equals(op)) {
				query.append(sc.getKey(), new Document(QueryOperators.LT, sc.getValue()));
			} else if (SearchOperation.LESS_THEN_EQUAL.equals(op)) {
				query.append(sc.getKey(), new Document(QueryOperators.LTE, sc.getValue()));
			} else {
				throw new DataException("the operator " + op.name() + " is not implemented");
			}
		}
	}

	private String startRgx(String value) {
		return start + value;
	}

	private String endRgx(String value) {
		return value + end;
	}

	@Override
	public void flush() {
	}

	@Override
	public void initialize(String type) {
		this.type = type;
		logger.info("initializing store of type {}", type);
		MongoClient client = MongoClients.create(constr);
		if ( type.equals(Constants.RESOURCE_TYPE_USER)) {
			col = client.getDatabase(userDatabase).getCollection(userCollection);
			try {
				col.createIndex(Indexes.descending(userName), new IndexOptions().background(true).unique(true));
			} catch (Exception e) {
				throw new DataException(e.getMessage());
			}
		}
		else if ( type.equalsIgnoreCase(Constants.RESOURCE_TYPE_GROUP)) {
			col = client.getDatabase(groupDatabase).getCollection(groupCollection);
			try {
				//col.createIndex(Indexes.descending(userName), new IndexOptions().background(true).unique(true));
			} catch (Exception e) {
				throw new DataException(e.getMessage());
			}
		}
		
	}
}
