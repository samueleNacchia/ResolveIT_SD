package it.unisa.resolveIt.account.service;

import it.unisa.resolveIt.account.dto.MyProfileDTO;
import it.unisa.resolveIt.model.entity.Cliente;
import it.unisa.resolveIt.model.entity.Operatore;
import it.unisa.resolveIt.model.repository.ClienteRepository;
import it.unisa.resolveIt.model.repository.OperatoreRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AccountImpl implements AccountService {

    //@ spec_public nullable
    @Autowired
    private ClienteRepository clienteRepository;

    //@ spec_public nullable
    @Autowired
    private OperatoreRepository operatoreRepository;

    //@ spec_public nullable
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    @Override
    public void removeAccountCliente(long id) {
        //@ assume clienteRepository != null && operatoreRepository != null && passwordEncoder != null;
        Optional<Cliente> cliente = clienteRepository.findById(id);
        //@ assume cliente != null;

        if (cliente.isPresent()) {
            //@ assume cliente.get() != null;
            if (cliente.get().isEnabled()) {
                cliente.get().disable();
                clienteRepository.<Cliente>save(cliente.get());
            } else {
                throw new RuntimeException("Account cliente già disabilitato");
            }
        } else {
            throw new RuntimeException("Account cliente non trovato");
        }
    }

    @Transactional
    @Override
    public void removeAccountOperatore(long id) {
        //@ assume clienteRepository != null && operatoreRepository != null && passwordEncoder != null;
        Optional<Operatore> operatore = operatoreRepository.findById(id);
        //@ assume operatore != null;

        if (operatore.isPresent()) {
            //@ assume operatore.get() != null;
            if (operatore.get().isEnabled()) {
                operatore.get().disable();
                operatoreRepository.<Operatore>save(operatore.get());
            } else {
                throw new RuntimeException("Account operatore già disabilitato");
            }
        } else {
            throw new RuntimeException("Account operatore non trovato");
        }
    }

    @Override
    public MyProfileDTO getUserByEmail(String email) {
        //@ assume clienteRepository != null && operatoreRepository != null && passwordEncoder != null;
        if (email == null || email.isEmpty()) {
            throw new RuntimeException("Email non valida");
        }
        Operatore operatore = operatoreRepository.findByEmail(email);

        if (operatore == null) {
            Cliente cliente = clienteRepository.findByEmail(email);
            if (cliente == null) {
                throw new RuntimeException("Email non registrata!");
            }

            MyProfileDTO dto = new MyProfileDTO();
            dto.setNome(cliente.getNome());
            dto.setCognome(cliente.getCognome());
            dto.setClient(true);

            return dto;
        }

        MyProfileDTO dto = new MyProfileDTO();
        dto.setNome(operatore.getNome());
        dto.setCognome(operatore.getCognome());
        dto.setClient(false);

        return dto;
    }

    @Transactional
    @Override
    public boolean modifyUser(MyProfileDTO userDto) {
        //@ assume clienteRepository != null && operatoreRepository != null && passwordEncoder != null;
        if (userDto == null) {
            throw new RuntimeException("Dati profilo mancanti");
        }
        /*@ nullable @*/ String email = userDto.getEmail();
        /*@ nullable @*/ String nome = userDto.getNome();
        /*@ nullable @*/ String cognome = userDto.getCognome();
        /*@ nullable @*/ String nuovaPassword = userDto.getPassword();
        /*@ nullable @*/ String confermaNuovaPassword = userDto.getConfermaPassword();
        //@ assume email != null;
        boolean passwordChanged = false;

        Operatore operatore = operatoreRepository.findByEmail(email);

        if (operatore != null) {
            if (nuovaPassword != null && !nuovaPassword.isEmpty()) {
                if (!nuovaPassword.equals(confermaNuovaPassword)) {
                    throw new RuntimeException("Le password non coincidono!");
                }
                String encoded = passwordEncoder.encode(nuovaPassword);
                //@ assume encoded != null;
                operatore.setPassword(encoded);
                passwordChanged = true;
            }
            operatore.setNome(nome);
            operatore.setCognome(cognome);
            return passwordChanged;
        }

        Cliente cliente = clienteRepository.findByEmail(email);

        if (cliente != null) {
            if (nuovaPassword != null && !nuovaPassword.isEmpty()) {
                if (!nuovaPassword.equals(confermaNuovaPassword)) {
                    throw new RuntimeException("Le password non coincidono!");
                }
                String encoded = passwordEncoder.encode(nuovaPassword);
                //@ assume encoded != null;
                cliente.setPassword(encoded);
                passwordChanged = true;
            }
            cliente.setNome(nome);
            cliente.setCognome(cognome);
            return passwordChanged;
        }

        throw new RuntimeException("L'utente non è autorizzato alla modifica o non esiste.");
    }
}