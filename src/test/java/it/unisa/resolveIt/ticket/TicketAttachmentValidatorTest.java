package it.unisa.resolveIt.ticket;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TicketAttachmentValidatorTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"documento.pdf", "virus.exe", "immagine.jpg.exe", "senza_estensione"})
    void hasAllowedExtension_InvalidoONull_RestituisceFalse(String filename) {
        assertFalse(TicketAttachmentValidator.hasAllowedExtension(filename));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "allegato.txt", "allegato.jpg", "allegato.jpeg", "allegato.zip",
            "allegato.TXT", "allegato.JPG", "allegato.JPEG", "allegato.ZIP"
    })
    void hasAllowedExtension_Valido_RestituisceTrue(String filename) {
        assertTrue(TicketAttachmentValidator.hasAllowedExtension(filename));
    }
}