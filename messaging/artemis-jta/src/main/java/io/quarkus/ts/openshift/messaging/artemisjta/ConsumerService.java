package io.quarkus.ts.openshift.messaging.artemisjta;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.Session;

import org.jboss.logging.Logger;

@ApplicationScoped
public class ConsumerService {

    private static final Logger LOG = Logger.getLogger(ConsumerService.class.getName());

    @Inject
    ConnectionFactory connectionFactory;

    public String readPriceOne() {
        return receiveMessagesInQueue("custom-prices-1");
    }

    public String readPriceTwo() {
        return receiveMessagesInQueue("custom-prices-2");
    }

    public String receiveAndAck(boolean ackIt) throws JMSException {
        try (JMSContext context = connectionFactory.createContext(Session.CLIENT_ACKNOWLEDGE);
                JMSConsumer consumer = context.createConsumer(context.createQueue("custom-prices-cack"))) {
            Message message = consumer.receiveNoWait();
            String messageBody = (message != null) ? message.getBody(String.class) : "";
            if (ackIt) {
                context.acknowledge();
            }
            return messageBody;
        }
    }

    private String receiveMessagesInQueue(String queueName) {
        String price = "";
        try (JMSContext context = connectionFactory.createContext(Session.AUTO_ACKNOWLEDGE);
                JMSConsumer consumer = context.createConsumer(context.createQueue(queueName))) {
            Message message = consumer.receive(500);
            if (message != null) {
                price = message.getBody(String.class);
                LOG.info("Price set to " + price + " from " + queueName);
            } else {
                LOG.info("Nothing to see in queue " + queueName);
            }
        } catch (JMSException ex) {
            LOG.error("Error reading queue. ", ex);
        }

        return price;
    }
}
