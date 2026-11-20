package it.unisa.resolveIt.model.repository;

import it.unisa.resolveIt.model.entity.Categoria;
import it.unisa.resolveIt.model.entity.Cliente;
import it.unisa.resolveIt.model.entity.Ticket;
import it.unisa.resolveIt.model.enums.Stato;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Transactional
class TicketRepositoryIntegrationTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Test
    void filtersByStateAndSortsByCreationDate() {
        Cliente cliente = createCliente();
        Categoria categoria = categoriaRepository.saveAndFlush(new Categoria("Hardware", true));

        Ticket older = createTicket(cliente, categoria, "Ticket vecchio", Stato.APERTO,
                LocalDateTime.of(2025, 1, 1, 10, 0));
        Ticket newer = createTicket(cliente, categoria, "Ticket recente", Stato.IN_CORSO,
                LocalDateTime.of(2025, 1, 2, 10, 0));
        ticketRepository.saveAllAndFlush(List.of(older, newer));

        List<Ticket> ascending = ticketRepository.findByClienteAndOptionalStato(cliente, null, "asc");
        List<Ticket> descending = ticketRepository.findByClienteAndOptionalStato(cliente, null, "desc");
        List<Ticket> open = ticketRepository.findByClienteAndOptionalStato(cliente, Stato.APERTO, "desc");

        assertEquals(List.of("Ticket vecchio", "Ticket recente"),
                ascending.stream().map(Ticket::getTitolo).toList());
        assertEquals(List.of("Ticket recente", "Ticket vecchio"),
                descending.stream().map(Ticket::getTitolo).toList());
        assertEquals(List.of("Ticket vecchio"), open.stream().map(Ticket::getTitolo).toList());
    }

    @Test
    void returnsOnlyTicketsOwnedByRequestedClient() {
        Cliente owner = createCliente();
        Cliente other = createCliente();
        Categoria categoria = categoriaRepository.saveAndFlush(new Categoria("Software", true));

        ticketRepository.saveAndFlush(createTicket(owner, categoria, "Ticket proprietario",
                Stato.APERTO, LocalDateTime.of(2025, 1, 1, 10, 0)));
        ticketRepository.saveAndFlush(createTicket(other, categoria, "Ticket altro",
                Stato.APERTO, LocalDateTime.of(2025, 1, 2, 10, 0)));

        List<Ticket> result = ticketRepository.findByClienteAndOptionalStato(owner, null, "desc");

        assertEquals(List.of("Ticket proprietario"), result.stream().map(Ticket::getTitolo).toList());
    }

    private Cliente createCliente() {
        String email = UUID.randomUUID() + "@test.example";
        return clienteRepository.saveAndFlush(new Cliente("Test", "Cliente", email, "hashed-password"));
    }

    private Ticket createTicket(Cliente cliente, Categoria categoria, String titolo,
                               Stato stato, LocalDateTime dataCreazione) {
        Ticket ticket = new Ticket();
        ticket.setTitolo(titolo);
        ticket.setDescrizione("Descrizione ticket");
        ticket.setCliente(cliente);
        ticket.setCategoria(categoria);
        ticket.setStato(stato);
        ticket.setDataCreazione(dataCreazione);
        return ticket;
    }
}
