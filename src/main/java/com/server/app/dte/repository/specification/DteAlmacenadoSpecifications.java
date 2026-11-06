package com.server.app.dte.repository.specification;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import com.server.app.dte.model.DteAlmacenado;

import jakarta.persistence.criteria.Predicate;

public final class DteAlmacenadoSpecifications {

  private DteAlmacenadoSpecifications() {
  }

  public static Specification<DteAlmacenado> search(String search) {
    return (root, query, criteriaBuilder) -> {
      if (search == null || search.isBlank()) {
        return criteriaBuilder.conjunction();
      }

      String term = "%" + search.trim().toLowerCase() + "%";
      List<Predicate> matches = new ArrayList<>();
      matches.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("codigoGeneracion")), term));
      matches.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("nitEmisor")), term));
      matches.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("numeroControl")), term));
      return criteriaBuilder.or(matches.toArray(new Predicate[0]));
    };
  }
}
