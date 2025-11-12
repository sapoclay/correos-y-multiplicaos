package com.gestorcorreo.service;

import com.gestorcorreo.model.EmailMessage;

import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio para destacar temporalmente correos nuevos y mostrar badges por cuenta.
 */
public class NewEmailHighlightService {
    private static NewEmailHighlightService instance;

    // Claves de correos nuevos por cuenta (INBOX)
    private final Map<String, Set<String>> newKeysByAccount = new ConcurrentHashMap<>();
    // Contador de badge por cuenta
    private final Map<String, Integer> badgeByAccount = new ConcurrentHashMap<>();

    public static synchronized NewEmailHighlightService getInstance() {
        if (instance == null) instance = new NewEmailHighlightService();
        return instance;
    }

    private NewEmailHighlightService() {}

    /** Añade correos nuevos a resaltar y aumenta el badge. */
    public void addNewEmails(String accountEmail, Collection<EmailMessage> newEmails) {
        if (accountEmail == null || newEmails == null || newEmails.isEmpty()) return;
        newKeysByAccount.computeIfAbsent(accountEmail, k -> ConcurrentHashMap.newKeySet());
        Set<String> set = newKeysByAccount.get(accountEmail);
        int added = 0;
        for (EmailMessage m : newEmails) {
            if (m == null) continue;
            String key = keyOf(m);
            if (key != null && set.add(key)) {
                added++;
            }
        }
        if (added > 0) {
            badgeByAccount.merge(accountEmail, added, Integer::sum);
        }
    }

    /** Obtiene las claves a resaltar (no limpia). */
    public Set<String> getNewKeys(String accountEmail) {
        return newKeysByAccount.getOrDefault(accountEmail, Collections.emptySet());
    }

    /** Limpia las claves de resaltado para la cuenta. */
    public void clearNewKeys(String accountEmail) {
        Set<String> set = newKeysByAccount.get(accountEmail);
        if (set != null) set.clear();
    }

    /** Obtiene el contador de badge para la cuenta. */
    public int getBadge(String accountEmail) {
        return badgeByAccount.getOrDefault(accountEmail, 0);
    }

    /** Resetea el contador de badge para la cuenta. */
    public void resetBadge(String accountEmail) {
        badgeByAccount.put(accountEmail, 0);
    }

    /** Crea una clave única equivalente a la usada para deduplicación. */
    public static String keyOf(EmailMessage m) {
        if (m == null) return null;
        String from = m.getFrom() != null ? m.getFrom() : "";
        String subj = m.getSubject() != null ? m.getSubject() : "";
        Long sent = null;
        if (m.getSentDate() != null) {
            sent = m.getSentDate().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        }
        return from + "|" + subj + "|" + (sent != null ? sent.toString() : "null");
    }
}
