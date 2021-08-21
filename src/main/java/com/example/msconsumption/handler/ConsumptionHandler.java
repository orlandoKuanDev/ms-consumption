package com.example.msconsumption.handler;

import com.example.msconsumption.models.Consumption;
import com.example.msconsumption.models.Payment;
import com.example.msconsumption.models.Transaction;
import com.example.msconsumption.models.dto.ConsumptionCreateDTO;
import com.example.msconsumption.services.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.AtomicDouble;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Slf4j(topic = "COMSUMPTION_HANDLER")
public class ConsumptionHandler {
    private final IConsumptionService consumptionService;
    private final AcquisitionService acquisitionService;
    private final TransactionService transactionService;
    private final PaymentService paymentService;

    @Autowired
    public ConsumptionHandler(IConsumptionService consumptionService, AcquisitionService acquisitionService, BillService billService, TransactionService transactionService, PaymentService paymentService) {
        this.consumptionService = consumptionService;
        this.acquisitionService = acquisitionService;
        this.transactionService = transactionService;
        this.paymentService = paymentService;
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

    public Mono<ServerResponse> findByBillAccountNumber(ServerRequest request){
        String accountNumber = request.pathVariable("accountNumber");
        return   acquisitionService.findByBillAccountNumber(accountNumber).flatMap(p -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(p))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> findByIban(ServerRequest request){
        String iban = request.pathVariable("iban");
        return   acquisitionService.findByIban(iban).flatMap(p -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(p))
                .switchIfEmpty(ServerResponse.notFound().build());
    }

    public Mono<ServerResponse> save(ServerRequest request) {
        Mono<Consumption> consumptionRequest = request.bodyToMono(Consumption.class);
        Consumption consumptionDTO = new Consumption();
        return consumptionRequest
                .flatMap(consumption -> {
                    consumptionDTO.setAmount(consumption.getAmount());
                    consumptionDTO.setDescription(consumption.getDescription());
                    return acquisitionService.findByIban(consumption.getAcquisition().getIban());
                })
                .doOnError(error -> log.error("consultation acquisition service web-client by account number failed !!"))
                .checkpoint("after consultation acquisition service web-client by account number", false)
                .zipWhen(acquisition -> paymentService.findByPaymentIban(acquisition.getIban()))
                .zipWhen(acquisition -> {
                    if (LocalDateTime.now().isAfter(acquisition.getT2().getExpirationDate())) {
                        return Mono.error(new RuntimeException(String.format("You cannot make consumptions because you have to debt of -> %s soles", acquisition.getT2().getAmount())));
                    }
                    if (!Objects.equals(acquisition.getT1().getProduct().getProductName(), "TARJETA DE CREDITO")) {
                        return Mono.error(new RuntimeException("You can only make consumptions to credit cards"));
                    }
                    if (consumptionDTO.getAmount() > acquisition.getT1().getBill().getBalance()) {
                        return Mono.error(new RuntimeException("The amount to consumption is higher than my credit line"));
                        // pasar a evaluar la siguiente cuenta
                    }
                    acquisition.getT1().getBill().setBalance(acquisition.getT1().getBill().getBalance() - consumptionDTO.getAmount());
                    Transaction transaction = new Transaction();
                    transaction.setTransactionType("CONSUMPTION");
                    transaction.setTransactionAmount(consumptionDTO.getAmount());
                    transaction.setBill(acquisition.getT1().getBill());
                    transaction.setDescription(consumptionDTO.getDescription());
                    return transactionService.createTransaction(transaction);
                })
                .doOnError(error -> log.error("transaction create failed !!"))
                .checkpoint("after transaction create", false)
                .zipWhen(transaction -> {
                    log.info("DEUDA -> {}", transaction.getT1().getT1().getInitial() - transaction.getT2().getBill().getBalance());
                    return paymentService.updatePayment(Payment.builder()
                            .acquisition(transaction.getT1().getT1())
                            .amount(transaction.getT1().getT1().getInitial() - transaction.getT2().getBill().getBalance())
                            .build());
                })
                .doOnError(error -> log.error("payment create failed !!"))
                .checkpoint("after payment update", false)
                .flatMap(transaction -> {
                    consumptionDTO.setAcquisition(transaction.getT1().getT1().getT1());
                    return consumptionService.create(consumptionDTO);
                })
                .map(consumption ->  {
                    return ConsumptionCreateDTO.builder()
                            .amount(consumption.getAmount())
                            .description(consumption.getDescription())
                            .accountNumber(consumption.getAcquisition().getBill().getAccountNumber())

                            .productName(consumption.getAcquisition().getProduct().getProductName())
                            .totalDebt(consumption.getAcquisition().getInitial() - consumption.getAcquisition().getBill().getBalance())

                            .build();
                })
                .doOnError(error -> log.error("consumption create failed !!"))
                .checkpoint("after consumption create", false)
                .flatMap(p -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(p))
                .switchIfEmpty(ServerResponse.notFound().build())
                .log()
                .onErrorResume(e -> Mono.error(new RuntimeException(e.getMessage())));
    }
}
