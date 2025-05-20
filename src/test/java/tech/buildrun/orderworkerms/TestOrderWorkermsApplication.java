package tech.buildrun.orderworkerms;

import org.springframework.boot.SpringApplication;

import java.util.List;

import static tech.buildrun.orderworkerms.ContainerConfig.*;

public class TestOrderWorkermsApplication {

    public static void main(String[] args) {

        //inicio - Configuracao para rodar local
        localStackContainer.setPortBindings(List.of("4566:4566"));
        localStackContainer.start();
        setupSqs();
        getProperties().forEach(System::setProperty);
        //Fim - Configuracao para rodar local

        SpringApplication.from(OrderworkermsApplication::main)
                .with(ServiceConnectionConfig.class)
                .run(args);
    }
}