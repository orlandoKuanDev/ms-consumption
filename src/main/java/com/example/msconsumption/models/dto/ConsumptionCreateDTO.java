package com.example.msconsumption.models.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConsumptionCreateDTO {
    private Double amount;
    private String description;
    private String productName;
    private String accountNumber;
    private Double totalDebt;
}
