package it.unisa.resolveIt.categoria.service;

import it.unisa.resolveIt.model.entity.Categoria;

public interface CategoriaService {

    /*@ public behavior
      @   requires id > 0;
      @   signals_only RuntimeException;
      @*/
    void disableCategoria(long id);

    /*@ public behavior
      @   requires id > 0;
      @   signals_only RuntimeException;
      @*/
    void enableCategoria(long id);

    /*@ public behavior
      @   requires categoria != null;
      @   signals_only RuntimeException, IllegalArgumentException;
      @*/
    void addCategoria(/*@ nullable @*/ Categoria categoria);

    /*@ public behavior
      @   requires categoria != null;
      @   signals_only RuntimeException, IllegalArgumentException;
      @*/
    void updateCategoria(/*@ nullable @*/ Categoria categoria);
}