package be.personify.iam.scim.storage;

import java.util.List;
import java.util.Map;

public interface Storage {

	/**
	 * Bootstrap your storage here
	 * @param type
	 */
	public void initialize(String type);
	
	
	public Map<String,Object> get(String id);
	
	public Map<String,Object> get(String id, String version);
	
	public List<String> getVersions(String id);
	
	public boolean delete(String id);
	
	public boolean deleteAll();
	
	public void create( String id, final Map<String,Object> object) throws ConstraintViolationException;
	
	public void update( String id, final Map<String,Object> object) throws ConstraintViolationException;
	
	public List<Map<String,Object>> getAll();
	
	/**
	 * Searches by criteria and sort order
	 * @param searchCriteria
	 * @param sortBy
	 * @param sortOrder
	 * @return
	 */
	public List<Map<String,Object>> search(SearchCriteria searchCriteria, String sortBy, String sortOrder);
	
	
	/**
	 * Optional to implement : persist 
	 */
	public void flush();
	
	
	
	
}