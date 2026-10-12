package com.spark.campusday.meal.order.repository;

import com.spark.campusday.meal.order.model.OrderCutoffConfiguration;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrderCutoffConfigurationRepository extends MongoRepository<OrderCutoffConfiguration, String> {}
