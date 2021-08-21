package com.example.msconsumption.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Acquisition {
    @Field(name = "product")
    private Product product;

    @Field(name = "customerOwner")
    private List<Customer> customerHolder;

    @Field(name = "authorizedSigner")
    private List<Customer> customerAuthorizedSigner;

    @Field(name = "initial")
    private double initial;

    @Field(name = "iban")
    private String iban;

    @Field(name = "bill")
    private Bill bill;
}

