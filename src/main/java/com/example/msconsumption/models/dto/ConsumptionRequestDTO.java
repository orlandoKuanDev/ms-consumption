package com.example.msconsumption.models.dto;

import com.example.msconsumption.models.Acquisition;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConsumptionRequestDTO {
    private Double amount;
    private String creditCard;
    private String description;
}
