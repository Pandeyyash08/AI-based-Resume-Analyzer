package com.yash.analyzer.repository;

import com.yash.analyzer.model.ResumeRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ResumeRepository extends MongoRepository<ResumeRecord, String> {
}