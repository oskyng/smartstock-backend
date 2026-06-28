package com.osanzana.smartstock.inventory.core.repositories;

import com.osanzana.smartstock.inventory.core.entities.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
}
