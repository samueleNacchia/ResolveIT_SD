package it.unisa.resolveIt.ticket.service;

import it.unisa.resolveIt.model.entity.Categoria;
import it.unisa.resolveIt.model.entity.Cliente;
import it.unisa.resolveIt.model.entity.Operatore;
import it.unisa.resolveIt.model.entity.Ticket;
import it.unisa.resolveIt.model.enums.Stato;
import it.unisa.resolveIt.model.repository.CategoriaRepository;
import it.unisa.resolveIt.model.repository.OperatoreRepository;
import it.unisa.resolveIt.model.repository.TicketRepository;
import it.unisa.resolveIt.ticket.TicketAttachmentValidator;
import it.unisa.resolveIt.ticket.dto.TicketDTO;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TicketImpl implements TicketService {

    //@ spec_public nullable
    @Autowired
    private TicketRepository ticketRepository;

    //@ spec_public nullable
    @Autowired
    private CategoriaRepository categoriaRepository;

    //@ spec_public nullable
    @Autowired
    private OperatoreRepository operatoreRepository;

    private TicketDTO convertToDTO(Ticket t) {
        TicketDTO dto = new TicketDTO();
        dto.setId(t.getID_T());
        dto.setTitolo(t.getTitolo());
        dto.setDescrizione(t.getDescrizione());
        dto.setStato(t.getStato());
        dto.setDataCreazione(t.getDataCreazione());
        dto.setDataRisoluzione(t.getDataResolved());
        dto.setDataAnnullamento(t.getDataAnnullamento());
        dto.setNomeFile(t.getNomeFile());

        if (t.getCategoria() != null) {
            dto.setNomeCategoria(t.getCategoria().getNome());
        }
        return dto;
    }

    @Transactional
    @Override
    //@ skipesc
    public void addTicket(TicketDTO dto, Cliente autore) throws IOException {
        if (dto == null || autore == null) {
            throw new RuntimeException("Dati ticket o autore non validi");
        }
        Ticket ticket = new Ticket();
        ticket.setTitolo(dto.getTitolo());
        ticket.setDescrizione(dto.getDescrizione());

        Categoria cat = categoriaRepository.findById(dto.getIdCategoria()).orElse(null);
        if (cat == null || !cat.getStato()) {
            throw new RuntimeException("Categoria non valida");
        }
        ticket.setCategoria(cat);

        if (dto.getFileAllegato() != null && !dto.getFileAllegato().isEmpty()) {

            if (dto.getFileAllegato().getSize() > 16 * 1024 * 1024) {
                throw new RuntimeException("Allegato troppo grande");
            }

            String originalName = dto.getFileAllegato().getOriginalFilename();
            if (!TicketAttachmentValidator.hasAllowedExtension(originalName)) {
                throw new RuntimeException("Formato allegato non valido");
            }

            ticket.setAllegato(dto.getFileAllegato().getBytes());
            ticket.setNomeFile(originalName);
        }

        if (dto.getTitolo() == null || !dto.getTitolo().matches(titolo_regex)) {
            throw new RuntimeException("Formato titolo non valido");
        }

        if (dto.getDescrizione().length() > 2000) {
            throw new RuntimeException("Lunghezza descrizione non valida");
        }

        ticket.setCliente(autore);
        ticket.setStato(Stato.APERTO);
        ticket.setDataCreazione(LocalDateTime.now());

        ticket.setOperatore(null);
        ticket.setDataInCarico(null);
        ticket.setDataAnnullamento(null);
        ticket.setDataResolved(null);

        try {
            ticketRepository.<Ticket>save(ticket);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void deleteTicket(Long ticketId) {
        //@ assume ticketRepository != null;
        if (ticketId == null) throw new RuntimeException("ID ticket non valido");
        Optional<Ticket> opt = ticketRepository.findById(ticketId);
        //@ assume opt != null;
        if (!opt.isPresent()) throw new RuntimeException("Ticket non trovato");
        Ticket ticket = opt.get();
        //@ assume ticket != null;

        Stato stato = ticket.getStato();
        //@ assume stato != null;
        if (stato != Stato.APERTO) {
            throw new RuntimeException("Ticket in stato non valido");
        }

        ticket.setDataAnnullamento(LocalDateTime.now());
        ticket.setStato(Stato.ANNULLATO);
        ticketRepository.<Ticket>save(ticket);
    }

    @Override
    public void assignTicket(Long ticketId, Operatore operatore) {
        //@ assume ticketRepository != null;
        if (ticketId == null) throw new RuntimeException("ID ticket non valido");
        Optional<Ticket> opt = ticketRepository.findById(ticketId);
        //@ assume opt != null;
        if (!opt.isPresent()) throw new RuntimeException("Ticket non trovato");
        Ticket ticket = opt.get();
        //@ assume ticket != null;

        Stato stato = ticket.getStato();
        //@ assume stato != null;
        if (stato != Stato.APERTO) {
            throw new RuntimeException("Ticket in stato non valido");
        }

        if (operatore == null) {
            throw new RuntimeException("Operatore non valido");
        }

        ticket.setOperatore(operatore);
        ticket.setDataInCarico(LocalDateTime.now());
        ticket.setStato(Stato.IN_CORSO);
        ticketRepository.<Ticket>save(ticket);
    }

    @Override
    public void resolveTicket(Long ticketId) {
        //@ assume ticketRepository != null;
        if (ticketId == null) throw new RuntimeException("ID ticket non valido");
        Optional<Ticket> opt = ticketRepository.findById(ticketId);
        //@ assume opt != null;
        if (!opt.isPresent()) throw new RuntimeException("Ticket non trovato");
        Ticket ticket = opt.get();
        //@ assume ticket != null;

        Stato stato = ticket.getStato();
        //@ assume stato != null;
        if (stato != Stato.IN_CORSO) {
            throw new RuntimeException("Ticket in stato non valido");
        }

        ticket.setDataResolved(LocalDateTime.now());
        ticket.setStato(Stato.RISOLTO);
        ticketRepository.<Ticket>save(ticket);
    }

    @Override
    //@ skipesc
    public void releaseTicket(Long ticketId) {
        //@ assume ticketRepository != null;
        if (ticketId == null) throw new RuntimeException("ID ticket non valido");
        Optional<Ticket> opt = ticketRepository.findById(ticketId);
        //@ assume opt != null;
        if (!opt.isPresent()) throw new RuntimeException("Ticket non trovato");
        Ticket ticket = opt.get();
        //@ assume ticket != null;

        Stato stato = ticket.getStato();
        //@ assume stato != null;
        if (stato != Stato.IN_CORSO) {
            throw new RuntimeException("Ticket in stato non valido");
        }

        ticket.setOperatore(null);
        ticket.setStato(Stato.APERTO);

        ticketRepository.<Ticket>save(ticket);
    }

    @Override
    public Ticket getTicketById(Long id) {
        //@ assume ticketRepository != null;
        if (id == null) throw new RuntimeException("ID non valido");
        Optional<Ticket> opt = ticketRepository.findById(id);
        //@ assume opt != null;
        if (!opt.isPresent()) throw new RuntimeException("Ticket non trovato");
        Ticket t = opt.get();
        //@ assume t != null;
        return t;
    }

    @Override
    //@ skipesc
    public List<TicketDTO> getTicketUtente(Cliente cliente) {
        if (cliente == null) throw new RuntimeException("Cliente non valido");
        List<TicketDTO> result = new ArrayList<>();
        for (Ticket t : ticketRepository.findByClienteOrderByDataCreazioneDesc(cliente)) {
            result.add(convertToDTO(t));
        }
        return result;
    }

    @Override
    //@ skipesc
    public List<TicketDTO> getTicketDisponibili() {
        List<TicketDTO> result = new ArrayList<>();
        for (Ticket t : ticketRepository.findByStato(Stato.APERTO)) {
            result.add(convertToDTO(t));
        }
        return result;
    }

    @Override
    //@ skipesc
    public List<TicketDTO> getTicketInCarico(Operatore operatore) {
        if (operatore == null) throw new RuntimeException("Operatore non valido");
        List<TicketDTO> result = new ArrayList<>();
        for (Ticket t : ticketRepository.findByOperatore(operatore)) {
            result.add(convertToDTO(t));
        }
        return result;
    }

    @Override
    //@ skipesc
    public List<TicketDTO> getTicketUtenteFiltrati(Cliente cliente, Stato stato, String ordine) {
        if (cliente == null) throw new RuntimeException("Cliente non valido");
        List<TicketDTO> result = new ArrayList<>();
        for (Ticket t : ticketRepository.findByClienteAndOptionalStato(cliente, stato, ordine)) {
            result.add(convertToDTO(t));
        }
        return result;
    }
}