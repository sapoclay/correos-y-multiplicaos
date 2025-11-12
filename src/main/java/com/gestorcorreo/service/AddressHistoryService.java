package com.gestorcorreo.service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Servicio simple en memoria para historial de direcciones usadas recientemente.
 * Futuro: persistir en archivo cifrado.
 */
public class AddressHistoryService {
    private static final AddressHistoryService INSTANCE = new AddressHistoryService();
    private final Map<String, Integer> frequency = new ConcurrentHashMap<>();
    private final int MAX_SIZE = 500;
    private final String CONFIG_DIR = System.getProperty("user.home") + "/.correosymultiplicaos";
    private final String STORE_FILE = CONFIG_DIR + "/address_history.enc";

    private AddressHistoryService() {}

    public static AddressHistoryService getInstance() { return INSTANCE; }

    public synchronized void register(String rawList) {
        if (rawList == null) return;
        for (String part : rawList.split("[;,]")) {
            String email = part.trim().toLowerCase();
            if (email.isEmpty() || !email.contains("@")) continue;
            frequency.merge(email, 1, Integer::sum);
        }
        trimIfNecessary();
        persist();
    }

    public List<String> suggest(String prefix, int max) {
        if (prefix == null || prefix.isBlank()) return Collections.emptyList();
        String p = prefix.toLowerCase();
        return frequency.entrySet().stream()
                .filter(e -> e.getKey().startsWith(p))
                .sorted((a,b) -> Integer.compare(b.getValue(), a.getValue()))
                .limit(max)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private void trimIfNecessary() {
        if (frequency.size() <= MAX_SIZE) return;
        // Eliminar los menos usados
        List<Map.Entry<String,Integer>> sorted = new ArrayList<>(frequency.entrySet());
        sorted.sort(Comparator.comparingInt(Map.Entry::getValue));
        int remove = frequency.size() - MAX_SIZE;
        for (int i=0; i<remove; i++) {
            frequency.remove(sorted.get(i).getKey());
        }
    }

    // Persistencia cifrada
    {
        // Bloque de inicialización: cargar en arranque
        try { load(); } catch (Exception ignored) {}
    }

    private void load() throws Exception {
        java.nio.file.Path path = java.nio.file.Paths.get(STORE_FILE);
        if (!java.nio.file.Files.exists(path)) return;
        String enc = java.nio.file.Files.readString(path);
        String json = com.gestorcorreo.security.EncryptionService.getInstance().decrypt(enc);
        if (json == null || json.isBlank()) return;
        // Formato simple: "email1:count\nemail2:count\n"
        String[] lines = json.split("\n");
        for (String line : lines) {
            int idx = line.lastIndexOf(':');
            if (idx > 0) {
                String email = line.substring(0, idx).trim();
                String cntStr = line.substring(idx+1).trim();
                try {
                    int c = Integer.parseInt(cntStr);
                    if (!email.isEmpty()) frequency.put(email, c);
                } catch (NumberFormatException ignore) {}
            }
        }
    }

    private void persist() {
        try {
            java.nio.file.Path dir = java.nio.file.Paths.get(CONFIG_DIR);
            if (!java.nio.file.Files.exists(dir)) java.nio.file.Files.createDirectories(dir);
            StringBuilder sb = new StringBuilder();
            frequency.entrySet().stream()
                    .sorted((a,b)->Integer.compare(b.getValue(), a.getValue()))
                    .forEach(e -> sb.append(e.getKey()).append(':').append(e.getValue()).append('\n'));
            String enc = com.gestorcorreo.security.EncryptionService.getInstance().encrypt(sb.toString());
            java.nio.file.Files.writeString(java.nio.file.Paths.get(STORE_FILE), enc);
        } catch (Exception e) {
            // Registrar pero no interrumpir flujo
            System.err.println("No se pudo persistir address history: " + e.getMessage());
        }
    }
}
