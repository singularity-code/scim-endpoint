package be.personify.iam.scim.schema;

import java.util.List;

/**
 * Basic SchemaAttribute class
 * @author wouter
 *
 */
public class SchemaAttribute {
	
	private String name;
	private String type;
	private boolean multiValued;
	private String description;
	private boolean required;
	private boolean caseExact;
	private String mutability;
	private String returned;
	private String uniqueness;
	private String[] referenceTypes;
	private String[] canonicalValues;
	
	private List<SchemaAttribute> subAttributes;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public boolean isMultiValued() {
		return multiValued;
	}

	public void setMultiValued(boolean multiValued) {
		this.multiValued = multiValued;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public boolean isCaseExact() {
		return caseExact;
	}

	public void setCaseExact(boolean caseExact) {
		this.caseExact = caseExact;
	}

	public String getMutability() {
		return mutability;
	}

	public void setMutability(String mutability) {
		this.mutability = mutability;
	}

	public String getReturned() {
		return returned;
	}

	public void setReturned(String returned) {
		this.returned = returned;
	}

	public List<SchemaAttribute> getSubAttributes() {
		return subAttributes;
	}

	public void setSubAttributes(List<SchemaAttribute> subAttributes) {
		this.subAttributes = subAttributes;
	}

	public String getUniqueness() {
		return uniqueness;
	}

	public void setUniqueness(String uniqueness) {
		this.uniqueness = uniqueness;
	}

	public String[] getReferenceTypes() {
		return referenceTypes;
	}

	public void setReferenceTypes(String[] referenceTypes) {
		this.referenceTypes = referenceTypes;
	}

	public String[] getCanonicalValues() {
		return canonicalValues;
	}

	public void setCanonicalValues(String[] canonicalValues) {
		this.canonicalValues = canonicalValues;
	}

	
	
	
	

}
