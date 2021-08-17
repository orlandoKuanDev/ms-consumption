package com.example.msconsumption.handler;

import com.example.msconsumption.models.Consumption;
import com.example.msconsumption.repositories.IConsumptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Component
@Slf4j(topic = "COMSUMPTION_HANDLER")
public class ConsumptionHandler {
    private final IConsumptionService consumptionService;

    @Autowired
    public ConsumptionHandler(IConsumptionService consumptionService) {
        this.consumptionService = consumptionService;
    }


    public Mono<ServerResponse> findAll(ServerRequest request){
        return ServerResponse.ok().contentType(MediaType.APPLICATION_JSON)
                .body(consumptionService.findAll(), Consumption.class);
    }

    public Mono<ServerResponse> findById(ServerRequest request){
        String id = request.pathVariable("id");
        return   consumptionService.findById(id).flatMap(p -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(p))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> save(ServerRequest request){
        Mono<Consumption> bill = request.bodyToMono(Consumption.class);
        return bill.flatMap(consumptionService::create)
                .flatMap(consumption -> ServerResponse.created(URI.create("/consumption/".concat(consumption.getId())))
                        .contentType(APPLICATION_JSON)
                        .bodyValue(consumption));
    }
}
