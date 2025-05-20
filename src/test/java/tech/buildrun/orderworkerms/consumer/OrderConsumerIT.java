package tech.buildrun.orderworkerms.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest;
import tech.buildrun.orderworkerms.ContainerConfig;
import tech.buildrun.orderworkerms.ServiceConnectionConfig;
import tech.buildrun.orderworkerms.dto.OrderDto;
import tech.buildrun.orderworkerms.dto.OrderEventDto;
import tech.buildrun.orderworkerms.entity.Order;
import tech.buildrun.orderworkerms.repository.OrderRepository;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static tech.buildrun.orderworkerms.consumer.OrderConsumer.ORDER_CONFIRMED_QUEUE;
import static tech.buildrun.orderworkerms.producer.ShippingProducer.SHIPPING_QUEUE;

@Import(ServiceConnectionConfig.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderConsumerIT extends ContainerConfig {

    @Autowired
    private SqsTemplate sqsTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    @BeforeAll
    static void beforeAll() {
        setupSqs();
    }

    @BeforeEach
    void beforeEach() {
        //Deleta tudo do orderRepository
        orderRepository.deleteAll();

        //Deleta tudo das filas SQS
        var sqsClient = getSqsClient();
        sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(ORDER_CONFIRMED_QUEUE).build());
        sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(SHIPPING_QUEUE).build());
    }


    @Test
    void quandoExistirPedidoDevePublicarNaFilaDeEnvio() throws JsonProcessingException {

        //Arrange
        //Cria a ordem no banco de dados;
        String orderNumber = "1234";
        var order = new Order(orderNumber,"teste@teste.com",false);
        orderRepository.save(order);

        //Cria o envio da mensagem
        var event = new OrderEventDto(orderNumber);
        var payload = objectMapper.writeValueAsString(event); //Transforma a classe em json

        //Act
        sqsTemplate.send(ORDER_CONFIRMED_QUEUE,payload);

        //Assert
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var message = sqsTemplate.receive(SHIPPING_QUEUE, String.class);
            assertTrue(message.isPresent());

            var dto = objectMapper.readValue(message.get().getPayload(), OrderDto.class);
            assertEquals(order.getOrderNumber(),dto.orderNumber());
            assertEquals(order.getCustomerEmail(),dto.customerEmail());
                });

    }

    @Test
    void quandoExistirPedidoDeveAtualizarOBancoDeDados() throws JsonProcessingException {

        //Arrange
        //Cria a ordem no banco de dados;
        String orderNumber = "1234";
        var order = new Order(orderNumber,"teste@teste.com",false);
        orderRepository.save(order);

        //Cria o envio da mensagem
        var event = new OrderEventDto(orderNumber);
        var payload = objectMapper.writeValueAsString(event); //Transforma a classe em json

        //Act
        sqsTemplate.send(ORDER_CONFIRMED_QUEUE,payload);

        //Assert
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            var orderDB = orderRepository.findByOrderNumber(orderNumber);
            assertTrue(orderDB.isPresent());
            assertTrue(orderDB.get().isNotified());
        });
    }

    @Test
    void quandoNaoExistirPedidoNaoDevePublicarNaFilaDeEnvio() throws JsonProcessingException {

        //Arrange
        String orderNumber = "1234";
        var event = new OrderEventDto(orderNumber);
        var payload = objectMapper.writeValueAsString(event); //Transforma a classe em json

        //Act
        sqsTemplate.send(ORDER_CONFIRMED_QUEUE,payload);

        //Assert
        await().atMost(12, TimeUnit.SECONDS).untilAsserted(() -> {
            var message = sqsTemplate.receive(SHIPPING_QUEUE, String.class);
            assertTrue(message.isEmpty());

        });
    }
}