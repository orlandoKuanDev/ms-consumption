package com.example.msconsumption.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Document(collection = "credit-consumer")
@Data
public class Consumption {
    @Id
    private String id;

    @Field(name = "amount")
    private Double amount;

    @Field(name = "purchase")
    private Acquisition purchase;

    @Field(name = "description")
    private String description;

    @Field(name = "consumptionDate")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime consumptionDate = LocalDateTime.now();
}
