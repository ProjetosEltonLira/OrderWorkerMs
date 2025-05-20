package tech.buildrun.orderworkerms.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Service;
import tech.buildrun.orderworkerms.dto.OrderDto;
import tech.buildrun.orderworkerms.entity.Order;
import tech.buildrun.orderworkerms.producer.ShippingProducer;
import tech.buildrun.orderworkerms.repository.OrderRepository;

import java.util.Optional;

@Service
public class OrderProcessingService {

    private final OrderRepository orderRepository;
    private final ShippingProducer shippingProducer;

    public OrderProcessingService(OrderRepository orderRepository, ShippingProducer shippingProducer) {
        this.orderRepository = orderRepository;
        this.shippingProducer = shippingProducer;
    }

    public void processOrder(String orderNumber) throws JsonProcessingException {
        Optional<Order> orderOpt = orderRepository.findByOrderNumber(orderNumber);

        if (orderOpt.isPresent()) {
            Order order = orderOpt.get();
            OrderDto dto = new OrderDto(order.getOrderNumber(), order.getCustomerEmail());

            shippingProducer.publishToShippingQueue(dto);

            order.setNotified(true);
            orderRepository.save(order);

        } else {
            throw new RuntimeException("Order not found: " + orderNumber);
        }
    }
}
