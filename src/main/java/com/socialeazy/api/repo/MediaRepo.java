package com.socialeazy.api.repo;

import com.socialeazy.api.entities.MediaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import javax.persistence.criteria.CriteriaBuilder;

public interface MediaRepo extends JpaRepository<MediaEntity , Integer> {

}
