package it.unisa.resolveIt.account.service;

import it.unisa.resolveIt.account.dto.MyProfileDTO;

public interface AccountService {

    /*@ public behavior
      @   requires id > 0;
      @   signals_only RuntimeException;
      @*/
    void removeAccountCliente(long id);

    /*@ public behavior
      @   requires id > 0;
      @   signals_only RuntimeException;
      @*/
    void removeAccountOperatore(long id);

    /*@ public behavior
      @   requires email != null && email.length() > 0;
      @   ensures \result != null;
      @   signals_only RuntimeException;
      @*/
    MyProfileDTO getUserByEmail(/*@ nullable @*/ String email);

    /*@ public behavior
      @   requires userDto != null;
      @   signals_only RuntimeException;
      @*/
    boolean modifyUser(/*@ nullable @*/ MyProfileDTO userDto);
}