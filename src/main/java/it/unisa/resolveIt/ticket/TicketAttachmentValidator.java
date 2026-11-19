package it.unisa.resolveIt.ticket;

import java.util.Locale;
import java.util.Set;

public final class TicketAttachmentValidator {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".txt", ".jpg", ".jpeg", ".zip");

    private TicketAttachmentValidator() {
    }

    public static boolean hasAllowedExtension(String filename) {
        if (filename == null) {
            return false;
        }

        String normalizedFilename = filename.toLowerCase(Locale.ROOT);
        return ALLOWED_EXTENSIONS.stream().anyMatch(normalizedFilename::endsWith);
    }
}
