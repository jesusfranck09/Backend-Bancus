package com.bancup.repository;

import com.bancup.entity.CatEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CatEstadoRepository extends JpaRepository<CatEstado, Long> {
}
