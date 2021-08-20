package com.example.msconsumption.handler;

import com.example.msconsumption.models.Consumption;
import com.example.msconsumption.models.Transaction;
import com.example.msconsumption.services.AcquisitionService;
import com.example.msconsumption.services.BillService;
import com.example.msconsumption.services.IConsumptionService;
import com.example.msconsumption.services.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
@Slf4j(topic = "COMSUMPTION_HANDLER")
public class ConsumptionHandler {
    private final IConsumptionService consumptionService;
    private final AcquisitionService acquisitionService;
    private final BillService billService;
    private final TransactionService transactionService;
    @Autowired
    public ConsumptionHandler(IConsumptionService consumptionService, AcquisitionService acquisitionService, BillService billService, TransactionService transactionService) {
        this.consumptionService = consumptionService;
        this.acquisitionService = acquisitionService;
        this.billService = billService;
        this.transactionService = transactionService;
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

    public Mono<ServerResponse> save(ServerRequest request){
        Mono<Consumption> consumptionRequest = request.bodyToMono(Consumption.class);
        Consumption consumptionDTO = new Consumption();
        return consumptionRequest
                .flatMap(consumption -> {
                    consumptionDTO.setAmount(consumption.getAmount());
                    return acquisitionService.findByBillAccountNumber(consumption.getBill().getAccountNumber());
                })
                .checkpoint("after consultation acquisition service web-client by account number")
                .flatMap(bill -> {
                    if (!Objects.equals(bill.getProduct().getProductName(), "TARJETA DE CREDITO")) {
                        return Mono.error(new RuntimeException("You can only make consumptions to credit cards"));
                    }
                    if (consumptionDTO.getAmount() > bill.getBill().getBalance()){
                        return Mono.error(new RuntimeException("The amount to consumption is higher than my credit line"));
                        // pasar a evaluar la siguiente cuenta
                    }
                    bill.getBill().setBalance(bill.getBill().getBalance() - consumptionDTO.getAmount());
                    Transaction transaction = new Transaction();
                    transaction.setTransactionType("CONSUMPTION");
                    transaction.setTransactionAmount(consumptionDTO.getAmount());
                    transaction.setBill(bill.getBill());
                    transaction.setDescription(consumptionDTO.getDescription());
                    return transactionService.createTransaction(transaction);
                })
                .checkpoint("after transaction create")
                .flatMap(transaction -> {
            consumptionDTO.setBill(transaction.getBill());
            return consumptionService.create(consumptionDTO);
        }).flatMap(p -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(p))
                .switchIfEmpty(ServerResponse.notFound().build())
                .onErrorResume(e -> Mono.error(new RuntimeException(e.getMessage())));
    }
}
