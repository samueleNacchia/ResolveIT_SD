package it.unisa.resolveIt.ticket.service;

import it.unisa.resolveIt.model.entity.Cliente;
import it.unisa.resolveIt.model.entity.Operatore;
import it.unisa.resolveIt.model.entity.Ticket;
import it.unisa.resolveIt.model.enums.Stato;
import it.unisa.resolveIt.ticket.dto.TicketDTO;

import java.io.IOException;
import java.util.List;

public interface TicketService {

    String titolo_regex = "^[a-zA-Z0-9\\s.,!?'-]{3,100}$";

    /*@ public behavior
      @   requires dto != null && autore != null;
      @   signals_only RuntimeException, IOException;
      @*/
    void addTicket(/*@ nullable @*/ TicketDTO dto, /*@ nullable @*/ Cliente autore) throws IOException;

    /*@ public behavior
      @   requires ticketId != null && ticketId > 0;
      @   signals_only RuntimeException;
      @*/
    void deleteTicket(/*@ nullable @*/ Long ticketId);

    /*@ public behavior
      @   requires ticketId != null && ticketId > 0 && operatore != null;
      @   signals_only RuntimeException;
      @*/
    void assignTicket(/*@ nullable @*/ Long ticketId, /*@ nullable @*/ Operatore operatore);

    /*@ public behavior
      @   requires ticketId != null && ticketId > 0;
      @   signals_only RuntimeException;
      @*/
    void resolveTicket(/*@ nullable @*/ Long ticketId);

    /*@ public behavior
      @   requires ticketId != null && ticketId > 0;
      @   signals_only RuntimeException;
      @*/
    void releaseTicket(/*@ nullable @*/ Long ticketId);

    /*@ public behavior
      @   requires id != null && id > 0;
      @   ensures \result != null;
      @   signals_only RuntimeException;
      @*/
    Ticket getTicketById(/*@ nullable @*/ Long id);

    /*@ public behavior
      @   requires cliente != null;
      @   ensures \result != null;
      @   signals_only RuntimeException;
      @*/
    List<TicketDTO> getTicketUtente(/*@ nullable @*/ Cliente cliente);

    /*@ public behavior
      @   ensures \result != null;
      @   signals_only RuntimeException;
      @*/
    List<TicketDTO> getTicketDisponibili();

    /*@ public behavior
      @   requires operatore != null;
      @   ensures \result != null;
      @   signals_only RuntimeException;
      @*/
    List<TicketDTO> getTicketInCarico(/*@ nullable @*/ Operatore operatore);

    /*@ public behavior
      @   requires cliente != null;
      @   ensures \result != null;
      @   signals_only RuntimeException;
      @*/
    List<TicketDTO> getTicketUtenteFiltrati(/*@ nullable @*/ Cliente cliente, /*@ nullable @*/ Stato stato, /*@ nullable @*/ String ordine);
}