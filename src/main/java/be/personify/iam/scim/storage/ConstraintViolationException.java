package be.personify.iam.scim.storage;

public class ConstraintViolationException extends Exception {

  private static final long serialVersionUID = 1960948131227749320L;

  public ConstraintViolationException() {
    super();
  }

  public ConstraintViolationException(String m) {
    super(m);
  }
}
