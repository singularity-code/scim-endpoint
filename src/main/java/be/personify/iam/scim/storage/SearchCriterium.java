package be.personify.iam.scim.storage;

import java.io.Serializable;

/**
 * A simple search criterium class
 * @author vanderw
 *
 */
public class SearchCriterium implements Serializable {

	private static final long serialVersionUID = -3184919196138992744L;

	private String key;

	private Object value;
	
	private SearchOperation searchOperation;
	
	
	/**
	 * Constructor
	 * @param key
	 * @param value
	 */
	public SearchCriterium(String key, Object value) {
		this.key = key;
		this.value = value;
		this.searchOperation = SearchOperation.EQUALS;
	}
	
	public SearchCriterium(String key, Object value, SearchOperation searchOperation) {
		this.key = key;
		this.value = value;
		this.searchOperation = searchOperation;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = value;
	}
	

	public SearchOperation getSearchOperation() {
		return searchOperation;
	}

	public void setSearchOperation(SearchOperation searchOperation) {
		this.searchOperation = searchOperation;
	}

	@Override
	public String toString() {
		return "SearchCriterium [key=" + key + ", value=" + value + ", searchOperation=" + searchOperation + "]";
	}

	
	
	
	
	
}
