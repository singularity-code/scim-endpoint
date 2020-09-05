package be.personify.iam.scim.storage;

import java.util.List;
import java.util.Map;

public interface Storage {

	/**
	 * Bootstrap your storage here
	 * @param type the type
	 */
	public void initialize(String type);
	
	
	public Map<String,Object> get(String id);
	
	public Map<String,Object> get(String id, String version);
	

	public List<String> getVersions(String id);
	
	public boolean delete(String id);
	
	public boolean deleteAll();
	
	public void create( String id, final Map<String,Object> object) throws ConstraintViolationException;
	
	public void update( String id, final Map<String,Object> object) throws ConstraintViolationException;
	
	
	/**
	 * Searches by criteria and sort order
	 * @param searchCriteria the searchcriteria
	 * @param sortBy the comma separated list of attributes to sort by
	 * @param sortOrder the sortorder
	 * @return list containing the result
	 */
	public List<Map<String,Object>> search(SearchCriteria searchCriteria, String sortBy, String sortOrder);
	
	public List<Map<String,Object>> getAll(String sortBy, String sortOrder);
	
	/**
	 * Optional to implement : persist 
	 */
	public void flush();
	
	
	
	
}