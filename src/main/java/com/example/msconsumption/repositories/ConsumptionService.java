package com.example.msconsumption.repositories;

import com.example.msconsumption.models.Consumption;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ConsumptionService extends BaseService<Consumption, String> implements IConsumptionService{
    private final IConsumptionRepository consumptionRepository;

    @Autowired
    public ConsumptionService(IConsumptionRepository consumptionRepository) {
        this.consumptionRepository = consumptionRepository;
    }

    @Override
    protected IRepository<Consumption, String> getRepository() {
        return consumptionRepository;
    }
}
