package be.personify.iam.scim.storage;

import java.util.List;
import java.util.Map;

public interface Storage {

	
	public Map<String,Object> get(String id);
	
	public Map<String,Object> get(String id, String version);
	
	public List<String> getVersions(String id);
	
	public boolean delete(String id);
	
	public boolean deleteAll();
	
	public void put( String id, Map<String,Object> object) throws ConstraintViolationException;
	
	public List<Map<String,Object>> getAll();
	
	public List<Map<String,Object>> search(SearchCriteria searchCriteria);
	
	public void flush();
	
	public void initialize(String type);
	
	
}