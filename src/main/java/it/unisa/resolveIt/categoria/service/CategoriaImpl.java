package it.unisa.resolveIt.categoria.service;

import it.unisa.resolveIt.model.repository.CategoriaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import it.unisa.resolveIt.model.entity.Categoria;

import java.util.Optional;

@Service
public class CategoriaImpl implements CategoriaService {

    //@ spec_public nullable
    @Autowired
    private CategoriaRepository categoriaRepository;

    @Transactional
    @Override
    public void disableCategoria(long id) {
        //@ assume categoriaRepository != null;
        Optional<Categoria> esistente = categoriaRepository.findById(id);
        //@ assume esistente != null;
        if (esistente.isPresent()) {
            //@ assume esistente.get() != null;
            if (esistente.get().disable()) {
                categoriaRepository.<Categoria>save(esistente.get());
            } else {
                throw new RuntimeException("Categoria già disabilitata");
            }
        } else {
            throw new RuntimeException("Categoria non trovata per la disattivazione");
        }
    }

    @Transactional
    @Override
    public void enableCategoria(long id) {
        //@ assume categoriaRepository != null;
        Optional<Categoria> esistente = categoriaRepository.findById(id);
        //@ assume esistente != null;
        if (esistente.isPresent()) {
            //@ assume esistente.get() != null;
            if (esistente.get().enable()) {
                categoriaRepository.<Categoria>save(esistente.get());
            } else {
                throw new RuntimeException("Categoria già abilitata");
            }
        } else {
            throw new RuntimeException("Categoria non trovata per l'aggiornamento");
        }
    }

    @Transactional
    @Override
    public void addCategoria(Categoria categoria) {
        //@ assume categoriaRepository != null;
        if (categoria == null) {
            throw new IllegalArgumentException("Dati categoria non validi");
        }
        if (categoriaRepository.existsById(categoria.getID_C()) || (categoriaRepository.findByNome(categoria.getNome()) != null))
            throw new RuntimeException("Categoria già presente nel database");
        else
            categoriaRepository.<Categoria>save(categoria);
    }

    @Transactional
    @Override
    public void updateCategoria(Categoria categoria) {
        //@ assume categoriaRepository != null;
        if (categoria == null) {
            throw new IllegalArgumentException("Dati categoria non validi");
        }
        if (categoriaRepository.findByNome(categoria.getNome()) != null) {
            throw new IllegalArgumentException("Categoria già esistente");
        }
        Optional<Categoria> esistente = categoriaRepository.findById(categoria.getID_C());
        //@ assume esistente != null;
        if (esistente.isPresent()) {
            //@ assume esistente.get() != null;
            esistente.get().setNome(categoria.getNome());
            categoriaRepository.<Categoria>save(esistente.get());
        } else
            throw new RuntimeException("Categoria non trovata per l'aggiornamento");
    }
}