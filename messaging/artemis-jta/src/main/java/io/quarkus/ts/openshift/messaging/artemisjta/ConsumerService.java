package io.quarkus.ts.openshift.messaging.artemisjta;

import io.quarkus.scheduler.Scheduled;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

@ApplicationScoped
public class ConsumerService {

    private static final Logger LOG = Logger.getLogger(ConsumerService.class.getName());

    @Inject
    ConnectionFactory connectionFactory;

    private volatile String price = "dead";

    /*
    Why empty getter and setter? CDI proxy...
    */
    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    @Scheduled(every = "1s")
    public void receiveMessages() throws JMSException {
        try (JMSContext context = connectionFactory.createContext(Session.AUTO_ACKNOWLEDGE)) {
            JMSConsumer consumer = context.createConsumer(context.createQueue("custom-prices-1"));
            Message message = consumer.receiveNoWait();
            String message1body = (message != null) ? message.getBody(String.class) : "";
            consumer = context.createConsumer(context.createQueue("custom-prices-2"));
            message = consumer.receiveNoWait();
            String message2body = (message != null) ? message.getBody(String.class) : "";
            setPrice(message1body + ":" + message2body);
        }
    }

    public static String exToS(Throwable throwable) {
        Writer w = new StringWriter();
        throwable.printStackTrace(new PrintWriter(w, true));
        return w.toString();
    }
}
