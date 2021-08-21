package com.example.msconsumption.services;


import com.example.msconsumption.models.Payment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Collections;

import static org.springframework.http.MediaType.APPLICATION_JSON;

@Service
public class PaymentService {
    private final WebClient.Builder webClientBuilder;

    Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Autowired
    public PaymentService(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public Mono<Payment> findByPaymentIban(String iban) {
        return webClientBuilder
                .baseUrl("http://SERVICE-PAYMENT/payment")
                .build()
                .get()
                .uri("/iban/{iban}", Collections.singletonMap("iban", iban))
                .accept(APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    logTraceResponse(logger, response);
                    return Mono.error(new RuntimeException(
                            String.format("THE IBAN DONT EXIST IN MICRO SERVICE CONSUMPTION-> %s", iban)
                    ));
                })
                .bodyToMono(Payment.class);
    }

    public Mono<Payment> updatePayment(Payment payment){
        return webClientBuilder
                .baseUrl("http://SERVICE-PAYMENT/payment")
                .build()
                .post()
                .uri("/update")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(payment), Payment.class)
                .retrieve()
                .onStatus(HttpStatus::isError, response -> {
                    logTraceResponse(logger, response);
                    return Mono.error(new RuntimeException("THE PAYMENT UPDATE FAILED"));
                })
                .bodyToMono(Payment.class);
    }

    public static void logTraceResponse(Logger log, ClientResponse response) {
        if (log.isTraceEnabled()) {
            log.trace("Response status: {}", response.statusCode());
            log.trace("Response headers: {}", response.headers().asHttpHeaders());
            response.bodyToMono(String.class)
                    .publishOn(Schedulers.boundedElastic())
                    .subscribe(body -> log.trace("Response body: {}", body));
        }
    }
}
