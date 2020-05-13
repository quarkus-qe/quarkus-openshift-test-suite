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

@ApplicationScoped
public class ConsumerService {

    private static final Logger LOG = Logger.getLogger(ConsumerService.class.getName());

    @Inject
    ConnectionFactory connectionFactory;

    private volatile String price1 = "dead";
    private volatile String price2 = "dead";

    /*
    Why empty getter and setter? CDI proxy...
    */
    public String getPrice() {
        return price1 + ":" + price2;
    }

    public void setPrice1(String price) {
        this.price1 = price;
    }

    public void setPrice2(String price) {
        this.price2 = price;
    }

    @Scheduled(every = "1s")
    public void receiveMessages() throws JMSException {
        try (JMSContext context = connectionFactory.createContext(Session.AUTO_ACKNOWLEDGE)) {
            JMSConsumer consumer = context.createConsumer(context.createQueue("custom-prices-1"));
            Message message = consumer.receive(500);
            if (message != null) {
                String price = message.getBody(String.class);
                setPrice1(price);
                LOG.info("Price 1 set to " + price);
            } else {
                LOG.info("Nothing to see in queue custom-prices-1.");
            }
            consumer = context.createConsumer(context.createQueue("custom-prices-2"));
            message = consumer.receive(500);
            if (message != null) {
                String price = message.getBody(String.class);
                setPrice2(price);
                LOG.info("Price 2 set to " + price);
            } else {
                LOG.info("Nothing to see in queue custom-prices-2.");
            }
        }
    }

    public String receiveAndAck(boolean ackIt) throws JMSException {
        try (JMSContext context = connectionFactory.createContext(Session.CLIENT_ACKNOWLEDGE)) {
            JMSConsumer consumer = context.createConsumer(context.createQueue("custom-prices-cack"));
            Message message = consumer.receiveNoWait();
            String messageBody = (message != null) ? message.getBody(String.class) : "";
            if (ackIt) {
                context.acknowledge();
            }
            return messageBody;
        }
    }
}
