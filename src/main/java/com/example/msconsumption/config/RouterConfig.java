package com.example.msconsumption.config;

import com.example.msconsumption.handler.ConsumptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@Configuration
public class RouterConfig {
    @Bean
    public RouterFunction<ServerResponse> rutas(ConsumptionHandler handler){
        return route(GET("/consumption"), handler::findAll)
                .andRoute(GET("/consumption/{id}"), handler::findById)
                .andRoute(GET("/consumption/account/{accountNumber}"), handler::findByBillAccountNumber)
                .andRoute(GET("/consumption/iban/{iban}"), handler::findByIban)
                .andRoute(POST("/consumption"), handler::save);
    }
}
