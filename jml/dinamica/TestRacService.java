package jml.dinamica;

import it.unisa.resolveIt.account.dto.MyProfileDTO;
import it.unisa.resolveIt.account.service.AccountImpl;
import it.unisa.resolveIt.categoria.service.CategoriaImpl;
import it.unisa.resolveIt.model.entity.Categoria;
import it.unisa.resolveIt.model.entity.Cliente;
import it.unisa.resolveIt.model.entity.Operatore;
import it.unisa.resolveIt.model.entity.Ticket;
import it.unisa.resolveIt.model.enums.Stato;
import it.unisa.resolveIt.model.repository.CategoriaRepository;
import it.unisa.resolveIt.model.repository.ClienteRepository;
import it.unisa.resolveIt.model.repository.OperatoreRepository;
import it.unisa.resolveIt.model.repository.TicketRepository;
import it.unisa.resolveIt.ticket.dto.TicketDTO;
import it.unisa.resolveIt.ticket.service.TicketImpl;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TestRacService {

    private static void inject(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    public static void main(String[] args) throws Exception {
        System.out.println("=== AVVIO VERIFICA DINAMICA RAC SUI SERVICE ===");

        testCategoriaService();
        testAccountService();
        testTicketService();

        System.out.println("=== TUTTI I CONTROLLI RAC COMPLETATI CON SUCCESSO ===");
    }

    // =========================================================================
    // 1. CATEGORIA SERVICE / IMPL
    // =========================================================================
    private static void testCategoriaService() throws Exception {
        System.out.println("\n[1/3] Test RAC: CategoriaImpl...");

        Map<Long, Categoria> db = new HashMap<>();
        Categoria cat1 = new Categoria();
        cat1.setID_C(1);
        cat1.setNome("Hardware");
        cat1.enable();
        db.put(1L, cat1);

        CategoriaRepository repo = (CategoriaRepository) Proxy.newProxyInstance(
                CategoriaRepository.class.getClassLoader(),
                new Class<?>[]{CategoriaRepository.class},
                (proxy, method, methodArgs) -> {
                    String name = method.getName();
                    if ("findById".equals(name)) {
                        return Optional.ofNullable(db.get((Long) methodArgs[0]));
                    }
                    if ("existsById".equals(name)) {
                        return db.containsKey((Long) methodArgs[0]);
                    }
                    if ("findByNome".equals(name)) {
                        String n = (String) methodArgs[0];
                        for (Categoria c : db.values()) {
                            if (n != null && n.equals(c.getNome())) return c;
                        }
                        return null;
                    }
                    if ("save".equals(name)) {
                        Categoria c = (Categoria) methodArgs[0];
                        if (c.getID_C() == 0) c.setID_C(db.size() + 1);
                        db.put(c.getID_C(), c);
                        return c;
                    }
                    return null;
                }
        );

        CategoriaImpl service = new CategoriaImpl();
        inject(service, "categoriaRepository", repo);

        // Caso nominale: disableCategoria -> enableCategoria
        service.disableCategoria(1L);
        service.enableCategoria(1L);

        // Caso nominale: addCategoria -> updateCategoria
        Categoria nuova = new Categoria();
        nuova.setID_C(2);
        nuova.setNome("Rete");
        service.addCategoria(nuova);

        Categoria agg = new Categoria();
        agg.setID_C(2);
        agg.setNome("Rete e Connettività");
        service.updateCategoria(agg);

        // Caso eccezione (verifica signals_only RuntimeException)
        try {
            service.disableCategoria(999L);
            throw new AssertionError("Attesa RuntimeException per ID inesistente");
        } catch (RuntimeException expected) {
            // OK: contratto signals_only rispettato
        }

        System.out.println("  -> CategoriaImpl OK");
    }

    // =========================================================================
    // 2. ACCOUNT SERVICE / IMPL
    // =========================================================================
    private static void testAccountService() throws Exception {
        System.out.println("\n[2/3] Test RAC: AccountImpl...");

        Map<Long, Cliente> clienti = new HashMap<>();
        Cliente c1 = new Cliente("Mario", "Rossi", "mario@test.it", "hashedPwd");
        Field idCliente = Cliente.class.getDeclaredField("id");
        idCliente.setAccessible(true);
        idCliente.set(c1, 1L);
        clienti.put(1L, c1);

        Map<Long, Operatore> operatori = new HashMap<>();
        Operatore op1 = new Operatore("Luigi", "Verdi", "luigi@test.it", "hashedPwd");
        Field idOp = Operatore.class.getDeclaredField("id");
        idOp.setAccessible(true);
        idOp.set(op1, 1L);
        operatori.put(1L, op1);

        ClienteRepository clienteRepo = (ClienteRepository) Proxy.newProxyInstance(
                ClienteRepository.class.getClassLoader(),
                new Class<?>[]{ClienteRepository.class},
                (proxy, method, methodArgs) -> {
                    String name = method.getName();
                    if ("findById".equals(name)) return Optional.ofNullable(clienti.get((Long) methodArgs[0]));
                    if ("findByEmail".equals(name)) {
                        String mail = (String) methodArgs[0];
                        for (Cliente c : clienti.values()) {
                            if (mail.equals(c.getEmail())) return c;
                        }
                        return null;
                    }
                    if ("save".equals(name)) return methodArgs[0];
                    return null;
                }
        );

        OperatoreRepository operatoreRepo = (OperatoreRepository) Proxy.newProxyInstance(
                OperatoreRepository.class.getClassLoader(),
                new Class<?>[]{OperatoreRepository.class},
                (proxy, method, methodArgs) -> {
                    String name = method.getName();
                    if ("findById".equals(name)) return Optional.ofNullable(operatori.get((Long) methodArgs[0]));
                    if ("findByEmail".equals(name)) {
                        String mail = (String) methodArgs[0];
                        for (Operatore o : operatori.values()) {
                            if (mail.equals(o.getEmail())) return o;
                        }
                        return null;
                    }
                    if ("save".equals(name)) return methodArgs[0];
                    return null;
                }
        );

        PasswordEncoder encoder = new PasswordEncoder() {
            @Override
            public String encode(CharSequence rawPassword) {
                return "ENC_" + rawPassword;
            }
            @Override
            public boolean matches(CharSequence rawPassword, String encodedPassword) {
                return ("ENC_" + rawPassword).equals(encodedPassword);
            }
        };

        AccountImpl service = new AccountImpl();
        inject(service, "clienteRepository", clienteRepo);
        inject(service, "operatoreRepository", operatoreRepo);
        inject(service, "passwordEncoder", encoder);

        // Caso nominale: getUserByEmail (ensures \result != null)
        MyProfileDTO profilo = service.getUserByEmail("mario@test.it");
        if (profilo == null || !profilo.isClient()) {
            throw new AssertionError("Profilo cliente non valido");
        }

        // Caso nominale: modifyUser
        MyProfileDTO updateDto = new MyProfileDTO();
        updateDto.setEmail("mario@test.it");
        updateDto.setNome("Mario Aggiornato");
        updateDto.setCognome("Rossi");
        updateDto.setPassword("NuovaPass1!");
        updateDto.setConfermaPassword("NuovaPass1!");
        service.modifyUser(updateDto);

        // Caso nominale: disabilitazione account
        service.removeAccountCliente(1L);
        service.removeAccountOperatore(1L);

        // Caso eccezione (signals_only RuntimeException)
        try {
            service.getUserByEmail("inesistente@test.it");
            throw new AssertionError("Attesa RuntimeException per email non registrata");
        } catch (RuntimeException expected) {
            // OK
        }

        System.out.println("  -> AccountImpl OK");
    }

    // =========================================================================
    // 3. TICKET SERVICE / IMPL (include anche i metodi esclusi da ESC)
    // =========================================================================
    private static void testTicketService() throws Exception {
        System.out.println("\n[3/3] Test RAC: TicketImpl...");

        Map<Long, Ticket> tickets = new HashMap<>();
        Cliente autore = new Cliente("Mario", "Rossi", "mario@test.it", "pwd");
        Operatore op = new Operatore("Luigi", "Verdi", "luigi@test.it", "pwd");
        Categoria cat = new Categoria();
        cat.setID_C(1);
        cat.setNome("Software");
        cat.enable();

        Ticket t1 = new Ticket("Problema stampante", autore, LocalDateTime.now(), null, null, null, null, "Non stampa");
        t1.setID_T(1L);
        t1.setCategoria(cat);
        t1.setStato(Stato.APERTO);
        tickets.put(1L, t1);

        Ticket t2 = new Ticket("Errore rete", autore, LocalDateTime.now(), null, null, null, null, "WiFi assente");
        t2.setID_T(2L);
        t2.setCategoria(cat);
        t2.setStato(Stato.APERTO);
        tickets.put(2L, t2);

        TicketRepository ticketRepo = (TicketRepository) Proxy.newProxyInstance(
                TicketRepository.class.getClassLoader(),
                new Class<?>[]{TicketRepository.class},
                (proxy, method, methodArgs) -> {
                    String name = method.getName();
                    if ("findById".equals(name)) return Optional.ofNullable(tickets.get((Long) methodArgs[0]));
                    if ("save".equals(name)) {
                        Ticket t = (Ticket) methodArgs[0];
                        if (t.getID_T() == null) t.setID_T((long) (tickets.size() + 1));
                        tickets.put(t.getID_T(), t);
                        return t;
                    }
                    if ("findByClienteOrderByDataCreazioneDesc".equals(name)
                            || "findByStato".equals(name)
                            || "findByOperatore".equals(name)
                            || "findByClienteAndOptionalStato".equals(name)) {
                        return Collections.singletonList(t1);
                    }
                    return null;
                }
        );

        CategoriaRepository catRepo = (CategoriaRepository) Proxy.newProxyInstance(
                CategoriaRepository.class.getClassLoader(),
                new Class<?>[]{CategoriaRepository.class},
                (proxy, method, methodArgs) -> {
                    if ("findById".equals(method.getName())) return Optional.of(cat);
                    return null;
                }
        );

        OperatoreRepository opRepo = (OperatoreRepository) Proxy.newProxyInstance(
                OperatoreRepository.class.getClassLoader(),
                new Class<?>[]{OperatoreRepository.class},
                (proxy, method, methodArgs) -> Optional.of(op)
        );

        TicketImpl service = new TicketImpl();
        inject(service, "ticketRepository", ticketRepo);
        inject(service, "categoriaRepository", catRepo);
        inject(service, "operatoreRepository", opRepo);

        // 1. Verifica dinamica di addTicket (escluso da ESC)
        TicketDTO nuovoDto = new TicketDTO();
        nuovoDto.setTitolo("Nuovo ticket di prova");
        nuovoDto.setDescrizione("Descrizione valida del problema");
        nuovoDto.setIdCategoria(1L);
        service.addTicket(nuovoDto, autore);

        // 2. Ciclo di vita completo: assign -> release (escluso da ESC) -> assign -> resolve
        service.assignTicket(1L, op);
        service.releaseTicket(1L);
        service.assignTicket(1L, op);
        service.resolveTicket(1L);

        // 3. Annullamento ticket APERTO
        service.deleteTicket(2L);

        // 4. Lettura e query su stream (escluse da ESC)
        Ticket letto = service.getTicketById(1L);
        if (letto == null) throw new AssertionError("Ticket non restituito");
        List<TicketDTO> lista1 = service.getTicketUtente(autore);
        List<TicketDTO> lista2 = service.getTicketDisponibili();
        List<TicketDTO> lista3 = service.getTicketInCarico(op);
        List<TicketDTO> lista4 = service.getTicketUtenteFiltrati(autore, Stato.APERTO, "ASC");
        if (lista1.isEmpty() || lista2.isEmpty() || lista3.isEmpty() || lista4.isEmpty()) {
            throw new AssertionError("Liste DTO vuote");
        }

        // 5. Caso eccezione (transizione di stato non valida: resolve su ticket già RISOLTO)
        try {
            service.resolveTicket(1L);
            throw new AssertionError("Attesa RuntimeException per stato non valido");
        } catch (RuntimeException expected) {
            // OK
        }

        System.out.println("  -> TicketImpl OK");
    }
}