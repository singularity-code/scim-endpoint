package be.personify.iam.scim.storage;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple searchcriteria
 * @author vanderw
 *
 */
public class SearchCriteria implements Serializable {
	
	private static final long serialVersionUID = 6020373022517713585L;

	private List<SearchCriterium> criteria = new ArrayList<>();

	/**
	 * constructor with array of criteriums
	 * @param criteriums
	 */
	public SearchCriteria( SearchCriterium...criteriums ) {
		for ( SearchCriterium sc : criteriums ) {
			criteria.add(sc);
		}
	}
	
	/**
	 * Gets the criteria
	 * @return
	 */
	public List<SearchCriterium> getCriteria() {
		return criteria;
	}

	/**
	 * Sets the criteria
	 * @param criteria
	 */
	public void setCriteria(List<SearchCriterium> criteria) {
		this.criteria = criteria;
	}

	@Override
	public String toString() {
		return "SearchCriteria [criteria=" + criteria + "]";
	}
	
	public int size() {
		return criteria.size();
	}

	
	
	
	
	
}
