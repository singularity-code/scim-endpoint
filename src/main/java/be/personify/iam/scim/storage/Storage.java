package be.personify.iam.scim.storage;

import java.util.List;
import java.util.Map;

public interface Storage {

	/**
	 * Bootstrap your storage here
	 * @param type the type
	 */
	public void initialize(String type);
	
	
	/**
	 * Gets a entity by id
	 * @param id the id of the entity
	 * @return the entity
	 */
	public Map<String,Object> get(String id);
	
	/**
	 * Gets a antity by id and version
	 * @param id the id of the entity
	 * @param version the version of the entity
	 * @return the entity
	 */
	public Map<String,Object> get(String id, String version);
	

	/**
	 * Gets the version of a specific entity
	 * @param id the id of the entity
	 * @return a list containing the versions
	 */
	public List<String> getVersions(String id);
	
	
	/**
	 * Deletes a entity by id
	 * @param id the id of the entity
	 * @return boolean indicating success
	 */
	public boolean delete(String id);
	
	
	/**
	 * Deletes all entities
	 * @return boolean indicating success
	 */
	public boolean deleteAll();
	
	
	/**
	 * Creates a entity
	 * @param id the id of the entity
	 * @param object the entity itself
	 * @throws ConstraintViolationException indicating if constraints are violated
	 */
	public void create( String id, final Map<String,Object> object) throws ConstraintViolationException;
	
	
	/**
	 * Updates a entity
	 * @param id the id of the entity
	 * @param object the entity itself
	 * @throws ConstraintViolationException indicating if constraints are violated
	 */
	public void update( String id, final Map<String,Object> object) throws ConstraintViolationException;
	
	
	/**
	 * Searches entities
	 * @param searchCriteria the searchcriteria
	 * @param start the start position
	 * @param count the number of results to returm
	 * @param sortBy the sortby attributes separated by a comma
	 * @param sortOrder the sortorder ( ascending or descending )
	 * @return
	 */
	public List<Map<String,Object>> search(SearchCriteria searchCriteria, int start, int count, String sortBy, String sortOrder);
	
	
	/**
	 * Counts the results giving a searchcrtieria
	 * @param searchCriteria the searchcriteria
	 * @return a long indicating the number of results
	 */
	public long count(SearchCriteria searchCriteria );
	
	
	/**
	 * Optional to implement : persist 
	 */
	public void flush();
	
	
	
	
}