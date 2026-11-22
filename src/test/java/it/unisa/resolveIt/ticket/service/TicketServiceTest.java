package it.unisa.resolveIt.ticket.service;

import it.unisa.resolveIt.model.entity.Categoria;
import it.unisa.resolveIt.model.entity.Cliente;
import it.unisa.resolveIt.model.entity.Operatore;
import it.unisa.resolveIt.model.entity.Ticket;
import it.unisa.resolveIt.model.enums.Stato;
import it.unisa.resolveIt.model.repository.CategoriaRepository;
import it.unisa.resolveIt.model.repository.TicketRepository;
import it.unisa.resolveIt.ticket.dto.TicketDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private TicketImpl ticketService;

    @Test
    void addTicket_Successo() throws IOException {
        TicketDTO dto = new TicketDTO();
        dto.setTitolo("Problema Connessione");
        dto.setDescrizione("Internet non funziona");
        dto.setIdCategoria(1L);
        dto.setFileAllegato(new MockMultipartFile("file", "test.jpg", "image/jpeg", "content".getBytes()));

        Cliente autore = new Cliente();
        Categoria cat = new Categoria();
        cat.enable();

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(cat));

        ticketService.addTicket(dto, autore);

        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }


    @Test
    void addTicket_SenzaAllegato_Successo() throws IOException {
        TicketDTO dto = new TicketDTO();
        dto.setIdCategoria(1L);
        dto.setTitolo("Titolo Valido");
        dto.setDescrizione("Descrizione valida");
        dto.setFileAllegato(null); // Caso comune: l'utente non carica nulla

        Categoria cat = new Categoria();
        cat.enable();
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(cat));


        ticketService.addTicket(dto, new Cliente());

        verify(ticketRepository).save(any(Ticket.class));
    }



    @ParameterizedTest
    @ValueSource(strings = {"virus.exe", "documento.pdf", "immagine.jpg.exe"})
    void addTicket_FormatoAllegatoNonValido(String filename) throws IOException {
        TicketDTO dto = new TicketDTO();
        dto.setIdCategoria(1L);
        dto.setTitolo("Titolo valido");
        dto.setDescrizione("Descrizione valida");
        dto.setFileAllegato(new MockMultipartFile("file", filename, "application/octet-stream", "content".getBytes()));

        Categoria cat = new Categoria();
        cat.setStato(true);
        when(categoriaRepository.findById(any())).thenReturn(Optional.of(cat));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ticketService.addTicket(dto, new Cliente());
        });

        assertEquals("Formato allegato non valido", exception.getMessage());
        verify(ticketRepository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "allegato.txt", "allegato.jpg", "allegato.jpeg", "allegato.zip",
            "allegato.TXT", "allegato.JPG", "allegato.JPEG", "allegato.ZIP"
    })
    void addTicket_AllegatoConEstensioneValida(String filename) throws IOException {
        byte[] content = "contenuto di test".getBytes();
        TicketDTO dto = new TicketDTO();
        dto.setIdCategoria(1L);
        dto.setTitolo("Titolo valido");
        dto.setDescrizione("Descrizione valida");
        dto.setFileAllegato(new MockMultipartFile("file", filename, "application/octet-stream", content));

        Categoria categoria = new Categoria("Hardware", true);
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

        ticketService.addTicket(dto, new Cliente());

        ArgumentCaptor<Ticket> savedTicket = ArgumentCaptor.forClass(Ticket.class);
        verify(ticketRepository).save(savedTicket.capture());
        assertEquals(filename, savedTicket.getValue().getNomeFile());
        assertArrayEquals(content, savedTicket.getValue().getAllegato());
    }

    @Test
    void addTicket_FormatoTitoloNonValido() throws IOException {
        TicketDTO dto = new TicketDTO();
        dto.setIdCategoria(1L);
        dto.setTitolo("@_provea$");

        Categoria cat = new Categoria();
        cat.setStato(true);
        when(categoriaRepository.findById(any())).thenReturn(Optional.of(cat));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ticketService.addTicket(dto, new Cliente());
        });

        assertEquals("Formato titolo non valido", exception.getMessage());
    }

    @Test
    void addTicket_LunghezzaTestoNonValida() throws IOException {
        TicketDTO dto = new TicketDTO();
        dto.setIdCategoria(1L);
        dto.setTitolo("Problema x");
        dto.setDescrizione("a".repeat(2001));

        Categoria cat = new Categoria();
        cat.setStato(true);
        when(categoriaRepository.findById(any())).thenReturn(Optional.of(cat));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ticketService.addTicket(dto, new Cliente());
        });

        assertEquals("Lunghezza descrizione non valida", exception.getMessage());
    }

    @Test
    void addTicket_OriginalNameNull_LanciaEccezione() throws IOException {
        TicketDTO dto = new TicketDTO();
        dto.setIdCategoria(1L);
        dto.setTitolo("Titolo Valido");
        MockMultipartFile fileSenzaNome = new MockMultipartFile("file", null, "text/plain", "content".getBytes());
        dto.setFileAllegato(fileSenzaNome);

        Categoria cat = new Categoria();
        cat.setStato(true);
        when(categoriaRepository.findById(anyLong())).thenReturn(Optional.of(cat));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ticketService.addTicket(dto, new Cliente());
        });

        assertEquals("Formato allegato non valido", exception.getMessage());
    }



    @Test
    void addTicket_CategoriaNonValida() throws IOException {
        TicketDTO dto = new TicketDTO();
        dto.setIdCategoria(1L);
        dto.setTitolo("Titolo Valido");

        Categoria catInattiva = new Categoria();
        catInattiva.setStato(false); //

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(catInattiva));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ticketService.addTicket(dto, new Cliente());
        });

        assertEquals("Categoria non valida", exception.getMessage());
        verify(ticketRepository, never()).save(any());
    }


    @Test
    void addTicket_FormatoAllegatoValido_Successo() throws IOException {
        TicketDTO dto = new TicketDTO();
        dto.setIdCategoria(1L);
        dto.setTitolo("Titolo Valido");
        dto.setDescrizione("Descrizione valida");

        Categoria cat = new Categoria();
        cat.setStato(true);

        when(categoriaRepository.findById(anyLong())).thenReturn(Optional.of(cat));

        ticketService.addTicket(dto, new Cliente());

        verify(ticketRepository, times(1)).save(any());
    }

    @Test
    void addTicket_FormatoTitoloValido_Successo() throws IOException {
        TicketDTO dto = new TicketDTO();
        dto.setIdCategoria(1L);
        dto.setTitolo("Ticket Supporto 123!");
        dto.setDescrizione("blablabla");

        Categoria cat = new Categoria();
        cat.enable();

        when(categoriaRepository.findById(anyLong())).thenReturn(Optional.of(cat));

        ticketService.addTicket(dto, new Cliente());

        verify(ticketRepository, times(1)).save(any());
    }

    @Test
    void addTicket_LunghezzaTestoValida_Successo() throws IOException {
        TicketDTO dto = new TicketDTO();
        dto.setIdCategoria(1L);
        dto.setTitolo("Titolo Valido");

        dto.setDescrizione("a".repeat(2000));

        Categoria cat = new Categoria();
        cat.setStato(true);

        when(categoriaRepository.findById(anyLong())).thenReturn(Optional.of(cat));

        ticketService.addTicket(dto, new Cliente());

        verify(ticketRepository, times(1)).save(any());
    }

    @Test
    void addTicket_CategoriaAttiva_Successo() throws IOException {
        TicketDTO dto = new TicketDTO();
        dto.setIdCategoria(1L);
        dto.setTitolo("Titolo Valido");
        dto.setDescrizione("blablabla");

        Categoria catAttiva = new Categoria();
        catAttiva.setStato(true);

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(catAttiva));

        ticketService.addTicket(dto, new Cliente());

        verify(ticketRepository, times(1)).save(any());
    }

    @Test
    void addTicket_GrandezzaAllegatoValido_Successo() throws IOException {
        TicketDTO dto = new TicketDTO();
        dto.setIdCategoria(1L);
        dto.setTitolo("Titolo Valido");
        dto.setDescrizione("blablabla");

        MultipartFile validFile = mock(MultipartFile.class);
        when(validFile.isEmpty()).thenReturn(false);
        when(validFile.getSize()).thenReturn(16L * 1024 * 1024);
        when(validFile.getOriginalFilename()).thenReturn("test.zip");
        when(validFile.getBytes()).thenReturn(new byte[0]);
        dto.setFileAllegato(validFile);

        Categoria cat = new Categoria();
        cat.setStato(true);

        when(categoriaRepository.findById(anyLong())).thenReturn(Optional.of(cat));

        ticketService.addTicket(dto, new Cliente());

        verify(ticketRepository, times(1)).save(any());
    }


    @Test
    void addTicket_TitoloNull_Fallimento() throws IOException {
        TicketDTO dto = new TicketDTO();
        dto.setIdCategoria(1L);
        dto.setTitolo(null);
        dto.setDescrizione("Descrizione valida");

        Categoria cat = new Categoria();
        cat.enable();
        when(categoriaRepository.findById(anyLong())).thenReturn(Optional.of(cat));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ticketService.addTicket(dto, new Cliente());
        });

        assertEquals("Formato titolo non valido", exception.getMessage());
        verify(ticketRepository, never()).save(any());
    }



    @Test
    void addTicket_DescrizioneOltreLimite_Fallimento() throws IOException {
        TicketDTO dto = new TicketDTO();
        dto.setIdCategoria(1L);
        dto.setTitolo("Titolo Valido");

        dto.setDescrizione("a".repeat(2001));

        Categoria cat = new Categoria();
        cat.enable();
        when(categoriaRepository.findById(anyLong())).thenReturn(Optional.of(cat));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ticketService.addTicket(dto, new Cliente());
        });

        assertEquals("Lunghezza descrizione non valida", exception.getMessage());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void addTicket_GrandezzaAllegatoNonValido_LanciaEccezione() throws IOException {
        TicketDTO dto = new TicketDTO();
        dto.setIdCategoria(1L);
        dto.setTitolo("Titolo Valido");
        MultipartFile largeFile = mock(MultipartFile.class);
        when(largeFile.isEmpty()).thenReturn(false);
        when(largeFile.getSize()).thenReturn(16L * 1024 * 1024 + 1);
        dto.setFileAllegato(largeFile);

        Categoria cat = new Categoria();
        cat.enable();
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(cat));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ticketService.addTicket(dto, new Cliente());
        });

        assertEquals("Allegato troppo grande", exception.getMessage());
        verify(ticketRepository, never()).save(any(Ticket.class));
    }


    @Test
    void assignTicket_Successo() {
        Long ticketId = 1L;
        Ticket ticket = new Ticket();
        ticket.setStato(Stato.APERTO);
        Operatore op = new Operatore();

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        ticketService.assignTicket(ticketId, op);

        assertEquals(Stato.IN_CORSO, ticket.getStato());
        assertSame(op, ticket.getOperatore());
        assertNotNull(ticket.getDataInCarico());
        verify(ticketRepository).save(ticket);
    }



    @Test
    void assignTicket_Fallimento_StatoNonAperto() {
        Long ticketId = 1L;
        Ticket ticket = new Ticket();
        ticket.setStato(Stato.RISOLTO);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ticketService.assignTicket(ticketId, new Operatore());
        });

        assertEquals("Ticket in stato non valido", exception.getMessage());

        verify(ticketRepository, never()).save(any());
    }

    @Test
    void assignTicket_TicketInesistente_Fallimento() {
        Long ticketId = 999L;
        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ticketService.assignTicket(ticketId, new Operatore());
        });

        assertEquals("Ticket non trovato", exception.getMessage());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void assignTicket_OperatoreNull_Fallimento() {
        Long ticketId = 1L;
        Ticket ticket = new Ticket();
        ticket.setStato(Stato.APERTO);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ticketService.assignTicket(ticketId, null);
        });

        assertEquals("Operatore non valido", exception.getMessage());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void resolveTicket_Successo() {
        Long ticketId = 1L;
        Ticket ticket = new Ticket();
        ticket.setStato(Stato.IN_CORSO);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        ticketService.resolveTicket(ticketId);

        assertEquals(Stato.RISOLTO, ticket.getStato());
        assertNotNull(ticket.getDataResolved());
        verify(ticketRepository).save(ticket);
    }

    @Test
    void releaseTicket_TicketInesistente_Fallimento() {
        Long ticketId = 1L;

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ticketService.releaseTicket(ticketId);
        });

        assertEquals("Ticket non trovato", exception.getMessage());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void resolveTicket_Fallimento_StatoNonAssegnato() {
        Long ticketId = 1L;
        Ticket ticket = new Ticket();
        ticket.setStato(Stato.ANNULLATO);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ticketService.resolveTicket(ticketId);
        });

        assertEquals("Ticket in stato non valido", exception.getMessage());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void resolveTicket_TicketInesistente_Fallimento() {
        Long ticketId = 1L;
        when(ticketRepository.findById(anyLong())).thenReturn(Optional.empty());
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ticketService.resolveTicket(ticketId);
        });

        assertEquals("Ticket non trovato", exception.getMessage());
    }

    @Test
    void deleteTicket_TicketInesistente_Fallimento() {
        Long ticketId = 1L;
        when(ticketRepository.findById(anyLong())).thenReturn(Optional.empty());
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ticketService.deleteTicket(ticketId);
        });

        assertEquals("Ticket non trovato", exception.getMessage());
    }

    @Test
    void deleteTicket_Successo() {
        Long ticketId = 1L;
        Ticket ticket = new Ticket();
        ticket.setStato(Stato.APERTO);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        ticketService.deleteTicket(ticketId);

        assertEquals(Stato.ANNULLATO, ticket.getStato());
        assertNotNull(ticket.getDataAnnullamento());
        verify(ticketRepository).save(ticket);
    }

    @Test
    void deleteTicket_Fallimento_StatoNonAssegnato() {
        Long ticketId = 1L;
        Ticket ticket = new Ticket();
        ticket.setStato(Stato.RISOLTO);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ticketService.deleteTicket(ticketId);
        });

        assertEquals("Ticket in stato non valido", exception.getMessage());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void releaseTicket_Successo() {
        Long ticketId = 1L;
        Ticket ticket = new Ticket();
        ticket.setStato(Stato.IN_CORSO);
        ticket.setOperatore(new Operatore());

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        ticketService.releaseTicket(ticketId);

        assertEquals(Stato.APERTO, ticket.getStato());
        assertNull(ticket.getOperatore());
        verify(ticketRepository).save(ticket);
    }

    @Test
    void releaseTicket_Fallimento_StatoInCorso() {
        Long ticketId = 1L;
        Ticket ticket = new Ticket();
        ticket.setStato(Stato.RISOLTO);

        when(ticketRepository.findById(ticketId)).thenReturn(Optional.of(ticket));

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            ticketService.releaseTicket(ticketId);
        });

        assertEquals("Ticket in stato non valido", exception.getMessage());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void getTicketDisponibili_Successo() {
        ticketService.getTicketDisponibili();
        verify(ticketRepository).findByStato(Stato.APERTO);
    }

    @Test
    void getTicketUtente_Successo() {
        Cliente c = new Cliente();
        Ticket ticket = new Ticket();
        ticket.setID_T(1L);
        ticket.setTitolo("Test DTO");
        ticket.setStato(Stato.APERTO);
        when(ticketRepository.findByClienteOrderByDataCreazioneDesc(c)).thenReturn(java.util.List.of(ticket));

        List<TicketDTO> risultato = ticketService.getTicketUtente(c);

        assertEquals(1, risultato.size());
    }


    @Test
    void getTicketInCarico_Successo() {
        Operatore op = new Operatore();
        Ticket ticket = new Ticket();
        ticket.setID_T(5L);
        ticket.setTitolo("Ticket Operatore");

        when(ticketRepository.findByOperatore(op)).thenReturn(List.of(ticket));

        List<TicketDTO> risultato = ticketService.getTicketInCarico(op);

        assertEquals(1, risultato.size());
        assertEquals(5L, risultato.get(0).getId());
        assertEquals("Ticket Operatore", risultato.get(0).getTitolo());
        verify(ticketRepository).findByOperatore(op);
    }

    @Test
    public void testGetTicketById_Successo() {
        Long idTicket = 1L;
        Ticket mockTicket = new Ticket();
        mockTicket.setID_T(idTicket);
        mockTicket.setTitolo("Problema Test");

        when(ticketRepository.findById(idTicket)).thenReturn(Optional.of(mockTicket));

        Ticket result = ticketService.getTicketById(idTicket);

        assertNotNull(result);
        assertEquals(idTicket, result.getID_T());
        assertEquals("Problema Test", result.getTitolo());
        verify(ticketRepository, times(1)).findById(idTicket);
    }


    @Test
    public void testGetTicketUtenteFiltrati_Successo() {
        Cliente mockCliente = new Cliente();
        mockCliente.setEmail("cliente@test.it");
        Stato stato = Stato.APERTO;
        String ordine = "dataCreazione";

        Ticket t1 = new Ticket();
        t1.setID_T(1L);
        t1.setTitolo("Ticket 1");

        Ticket t2 = new Ticket();
        t2.setID_T(2L);
        t2.setTitolo("Ticket 2");

        List<Ticket> listaTicket = List.of(t1, t2);

        when(ticketRepository.findByClienteAndOptionalStato(mockCliente, stato, ordine))
                .thenReturn(listaTicket);

        List<TicketDTO> result = ticketService.getTicketUtenteFiltrati(mockCliente, stato, ordine);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Ticket 1", result.get(0).getTitolo());
        assertEquals("Ticket 2", result.get(1).getTitolo());

        verify(ticketRepository).findByClienteAndOptionalStato(mockCliente, stato, ordine);
    }
}