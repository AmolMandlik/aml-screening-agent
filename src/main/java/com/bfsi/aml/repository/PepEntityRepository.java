package com.bfsi.aml.repository;

import com.bfsi.aml.model.PepEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PepEntityRepository extends JpaRepository<PepEntity, Long> {
    @Query("SELECT p FROM PepEntity p WHERE UPPER(p.fullName) LIKE UPPER(CONCAT('%', :name, '%'))")
    List<PepEntity> searchByName(@Param("name") String name);
}
