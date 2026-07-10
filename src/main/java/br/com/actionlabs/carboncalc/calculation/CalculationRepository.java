package br.com.actionlabs.carboncalc.calculation;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CalculationRepository extends MongoRepository<Calculation, String> {}
