package com.bfsi.aml.repository;

import com.bfsi.aml.model.SanctionsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SanctionsEntityRepository extends JpaRepository<SanctionsEntity, Long> {
    @Query("SELECT s FROM SanctionsEntity s WHERE s.active = true AND " +
           "(UPPER(s.fullName) LIKE UPPER(CONCAT('%', :name, '%')) OR " +
           " UPPER(s.aliases) LIKE UPPER(CONCAT('%', :name, '%')))")
    List<SanctionsEntity> searchByName(@Param("name") String name);
}
