package com.socialeazy.api.repo;

import com.socialeazy.api.entities.ContentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentRepo extends JpaRepository<ContentEntity,Integer> {

}
