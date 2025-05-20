package tech.buildrun.orderworkerms.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import tech.buildrun.orderworkerms.dto.OrderDto;

@Component
public class ShippingProducer {

    private static final Logger logger = LoggerFactory.getLogger(ShippingProducer.class);
    public static final String SHIPPING_QUEUE = "shipping-queue";

    private final SqsTemplate sqsTemplate;
    private final ObjectMapper objectMapper;

    public ShippingProducer(SqsTemplate sqsTemplate, ObjectMapper objectMapper) {
        this.sqsTemplate = sqsTemplate;
        this.objectMapper = objectMapper;
    }

    public void publishToShippingQueue(OrderDto orderDTO) throws JsonProcessingException {
        var payload = objectMapper.writeValueAsString(orderDTO);

        logger.info("Sending {}", payload);

        sqsTemplate.send(SHIPPING_QUEUE, payload);

        logger.info("Order sent to Shipping Queue {}", payload);
    }
}
