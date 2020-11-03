package io.quarkus.ts.openshift.messaging.artemisjta;

import io.quarkus.scheduler.Scheduled;
import io.quarkus.scheduler.Scheduled.ConcurrentExecution;

import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.Session;

import java.util.function.Consumer;

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

    @Scheduled(every = "1s", concurrentExecution = ConcurrentExecution.SKIP)
    public void receiveMessagesInCustomPriceOne() throws JMSException {
        receiveMessagesInQueue("custom-prices-1", this::setPrice1);
    }

    @Scheduled(every = "1s", concurrentExecution = ConcurrentExecution.SKIP)
    public void receiveMessagesInCustomPriceTwo() throws JMSException {
        receiveMessagesInQueue("custom-prices-2", this::setPrice2);
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

    private void receiveMessagesInQueue(String queueName, Consumer<String> setter) throws JMSException {
        try (JMSContext context = connectionFactory.createContext(Session.AUTO_ACKNOWLEDGE)) {
            JMSConsumer consumer = context.createConsumer(context.createQueue(queueName));
            Message message = consumer.receive(500);
            if (message != null) {
                String price = message.getBody(String.class);
                setter.accept(price);
                LOG.info("Price set to " + price + " from " + queueName);
            } else {
                LOG.info("Nothing to see in queue " + queueName);
            }
        }
    }
}
