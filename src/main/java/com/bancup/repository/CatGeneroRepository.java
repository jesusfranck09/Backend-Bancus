package com.bancup.repository;

import com.bancup.entity.CatGenero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CatGeneroRepository extends JpaRepository<CatGenero, Long> {
}
