package be.personify.iam.scim.authentication;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class CurrentConsumer {
	
	private static final ThreadLocal<Consumer> CONSUMER = new ThreadLocal<>();
	
	private static final Logger logger = LogManager.getLogger(CurrentConsumer.class);
	
	
	public static Consumer getCurrent() {
		logger.debug("returning consumer {}", CONSUMER.get());
        return CONSUMER.get();
    }

    public static void setCurrent(Consumer c) {
    	logger.debug("setting consumer {}", c);
        CONSUMER.set(c);
    }
    
     
}
