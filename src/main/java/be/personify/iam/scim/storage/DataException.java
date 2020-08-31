package be.personify.iam.scim.storage;

public class DataException extends RuntimeException {
	
	private static final long serialVersionUID = 1960948131227749320L;

	public DataException() {
		super();
	}
	
	public DataException(String m) {
		super(m);
	}

}
