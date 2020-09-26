package be.personify.iam.scim.storage.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bson.BsonBinary;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Value;

import com.mongodb.QueryOperators;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;

import be.personify.iam.scim.storage.ConstraintViolationException;
import be.personify.iam.scim.storage.Storage;
import be.personify.iam.scim.util.Constants;
import be.personify.util.SearchCriteria;
import be.personify.util.SearchCriterium;
import be.personify.util.SearchOperation;


/**
 * 
 * @author jingzhou wang
 * @author wouter vdb
 *
 */
public class MongoStorage implements Storage {
	
	
	@Value("${scim.storage.mongo.constr}")
	private String constr;
	@Value("${scim.storage.mongo.database}")
	private String database="users";
	@Value("${scim.storage.mongo.collection}")
	private String collection="users";

	
    private final static String oid = "_id";
    private final static String userName = "userName";
    private final static String extid = "externalId";
    private final static String $set = "$set";
    private final static String $regex = "$regex";
    private final static String start = "^";
    private final static String end = "$";
    private final static String minus = "-1";
    private final static String descending = "descending";
    private final static String used = "either id, externalId, or userName had been used";

    
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
        Document query = new Document(oid, new BsonBinary(UUID.fromString(id))).append(Constants.KEY_META, new Document(Constants.KEY_VERSION, version));
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
        Document doc = new Document(object);
        List<Document> vlist = new ArrayList<>();
        BsonBinary objId = new BsonBinary(UUID.fromString(id));
        vlist.add(new Document(oid, objId));
        vlist.add(new Document(extid, doc.getString(extid)));
        vlist.add(new Document(userName, doc.getString(userName)));
        Document filter = new Document(QueryOperators.OR, vlist);
        if (col.find(filter).first() != null) {
            throw new ConstraintViolationException(used);
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
        doc.remove(userName);
        doc.remove(extid);
        col.findOneAndUpdate(query, new Document($set, doc));
    }

    
   
    @Override
    public List<Map> search(SearchCriteria searchCriteria, int start, int count, String sortBy, String sortOrder) {
    	return search(searchCriteria, start, count, sortBy, sortOrder, null);
    }
    
    @Override
    public List<Map> search(SearchCriteria searchCriteria, int start, int count, String sortBy, String sortOrder, List<String> includeAttributes ) {

        Document query = new Document();
        if (searchCriteria != null && searchCriteria.getCriteria() != null && searchCriteria.getCriteria().size() > 0) {
        	searchCriteria.getCriteria().forEach(sc -> genQuery(query, sc));
        }
        
        FindIterable<Document> finds = col.find(query).skip(start -1 * count).limit(count);
        if (sortBy != null) {
            int order = 1;
            if (sortOrder != null) {
                if (descending.equals(sortOrder) || minus.equals(sortOrder)) {
                    order = -1;
                }
            }
            Document sort = new Document(sortBy, order);
            finds.sort(sort);
        }
        List<Map> all = new ArrayList<>();
        String id = null;
        for (Document doc : finds) {
            id = ((UUID) doc.remove(oid)).toString();
            doc.put(Constants.ID, id);
            all.add(doc);
        }
        return all;
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
                return;
            }

            if (SearchOperation.NOT_EQUALS.equals(op)) {
                query.append(sc.getKey(), new Document(QueryOperators.NE, sc.getValue()));
                return;
            }

            if (SearchOperation.CONTAINS.equals(op)) {
                query.append(sc.getKey(), new Document($regex, sc.getValue()));
                return;
            }

            if (SearchOperation.STARTS_WITH.equals(op)) {
                query.append(sc.getKey(), new Document($regex, startRgx((String) sc.getValue())));
                return;
            }

            if (SearchOperation.ENDS_WITH.equals(op)) {
                query.append(sc.getKey(), new Document($regex, endRgx((String) sc.getValue())));
                return;
            }

            if (SearchOperation.PRESENT.equals(op)) {
                query.append(sc.getKey(), new Document(QueryOperators.EXISTS, true));
                return;
            }

            if (SearchOperation.GREATER_THEN.equals(op)) {
                query.append(sc.getKey(), new Document(QueryOperators.GT, sc.getValue()));
                return;
            }

            if (SearchOperation.GREATER_THEN_OR_EQUAL.equals(op)) {
                query.append(sc.getKey(), new Document(QueryOperators.GTE, sc.getValue()));
                return;
            }

            if (SearchOperation.LESS_THEN.equals(op)) {
                query.append(sc.getKey(), new Document(QueryOperators.LT, sc.getValue()));
                return;
            }

            if (SearchOperation.LESS_THEN_EQUAL.equals(op)) {
                query.append(sc.getKey(), new Document(QueryOperators.LTE, sc.getValue()));
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
        MongoClient client = MongoClients.create(constr);
        col = client.getDatabase(database).getCollection(collection);
        col.createIndex(Indexes.descending(userName), new IndexOptions().background(true).unique(true));
    }



	

}
