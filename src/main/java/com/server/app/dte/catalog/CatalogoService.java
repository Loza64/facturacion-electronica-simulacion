package com.server.app.dte.catalog;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

/**
 * Catálogos oficiales "Catálogos - Facturación Electrónica v1.1" (CAT-001 a CAT-033), cargados desde
 * {@code resources/dte/catalogos.json}, generado a partir del Excel del Ministerio de Hacienda.
 */
@Service
public class CatalogoService {

  /** seccion: sólo CAT-015 (1, 2 ó 3). departamento: sólo CAT-013 (los códigos de municipio se repiten). */
  public record Entrada(String codigo, String valor, Integer seccion, String departamento) {
  }

  private final Map<String, List<Entrada>> catalogos;
  private final Map<String, Set<String>> codigos = new HashMap<>();

  @Autowired
  public CatalogoService(ObjectMapper mapper) {
    this(cargar(mapper));
  }

  public CatalogoService(Map<String, List<Entrada>> catalogos) {
    this.catalogos = catalogos;
    catalogos.forEach((k, v) -> {
      Set<String> s = new HashSet<>();
      v.forEach(e -> s.add(e.codigo()));
      codigos.put(k, s);
    });
  }

  private static Map<String, List<Entrada>> cargar(ObjectMapper mapper) {
    try (InputStream in = new ClassPathResource("dte/catalogos.json").getInputStream()) {
      return mapper.readValue(in, new TypeReference<Map<String, List<Entrada>>>() {
      });
    } catch (IOException e) {
      throw new UncheckedIOException("No se pudieron cargar los catálogos DTE", e);
    }
  }

  /** ¿Existe el código en el catálogo? (p. ej. {@code existe("CAT-019", "47111")}). */
  public boolean existe(String catalogo, String codigo) {
    return codigo != null && codigos.getOrDefault(catalogo, Set.of()).contains(codigo);
  }

  public Optional<String> valor(String catalogo, String codigo) {
    return catalogos.getOrDefault(catalogo, List.of()).stream()
        .filter(e -> e.codigo().equals(codigo)).map(Entrada::valor).findFirst();
  }

  /** CAT-013: el municipio sólo es válido dentro de su departamento ("00" = extranjero). */
  public boolean municipioDeDepartamento(String departamento, String municipio) {
    if ("00".equals(departamento)) {
      return "00".equals(municipio);
    }
    return catalogos.getOrDefault("CAT-013", List.of()).stream()
        .anyMatch(e -> e.codigo().equals(municipio) && departamento != null
            && departamento.equals(e.departamento()));
  }

  /** CAT-015: ¿el tributo está listado en la sección indicada (1, 2 ó 3)? */
  public boolean tributoEnSeccion(String codigo, int seccion) {
    return catalogos.getOrDefault("CAT-015", List.of()).stream()
        .anyMatch(e -> e.codigo().equals(codigo) && e.seccion() != null && e.seccion() == seccion);
  }
}
