package com.socialeazy.api.repo;

import com.socialeazy.api.entities.OrganisationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganisationRepo extends JpaRepository<OrganisationEntity, Integer> {
}
