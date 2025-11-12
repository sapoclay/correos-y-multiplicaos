package com.gestorcorreo.security;

import javax.net.ssl.*;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Properties;

/**
 * Servicio para verificación de certificados SSL/TLS
 * Permite validación estricta o confianza bajo petición del usuario
 */
public class SSLCertificateValidator {
    private static SSLCertificateValidator instance;
    private boolean strictMode = true; // Por defecto, verificación estricta
    
    private SSLCertificateValidator() {}
    
    public static SSLCertificateValidator getInstance() {
        if (instance == null) {
            instance = new SSLCertificateValidator();
        }
        return instance;
    }
    
    /**
     * Configura las propiedades SSL/TLS para conexiones seguras
     */
    public void configureSSLProperties(Properties props, String host, int port, String protocol) {
        if (strictMode) {
            // Modo estricto: validación completa de certificados
            props.remove("mail." + protocol + ".ssl.trust");
            props.put("mail." + protocol + ".ssl.checkserveridentity", "true");
            props.put("mail." + protocol + ".ssl.enable", "true");
            props.put("mail." + protocol + ".ssl.protocols", "TLSv1.2 TLSv1.3");
            props.put("mail." + protocol + ".ssl.ciphersuites", getSecureCipherSuites());
            
            SecureLogger.info("SSL configurado en modo estricto para " + host);
        } else {
            // Modo permisivo: confiar en todos (NO RECOMENDADO para producción)
            props.put("mail." + protocol + ".ssl.trust", "*");
            props.put("mail." + protocol + ".ssl.checkserveridentity", "false");
            props.put("mail." + protocol + ".ssl.enable", "true");
            
            SecureLogger.warn("SSL configurado en modo permisivo para " + host);
        }
    }
    
    /**
     * Obtiene la lista de cipher suites seguros
     */
    private String getSecureCipherSuites() {
        return String.join(" ", new String[]{
            "TLS_AES_256_GCM_SHA384",
            "TLS_AES_128_GCM_SHA256",
            "TLS_CHACHA20_POLY1305_SHA256",
            "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
            "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"
        });
    }
    
    /**
     * Valida un certificado SSL
     */
    public CertificateValidationResult validateCertificate(String host, int port) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustManager[] trustManagers = new TrustManager[]{
                new ValidationTrustManager()
            };
            sslContext.init(null, trustManagers, new java.security.SecureRandom());
            
            SSLSocketFactory factory = sslContext.getSocketFactory();
            SSLSocket socket = (SSLSocket) factory.createSocket(host, port);
            
            socket.startHandshake();
            SSLSession session = socket.getSession();
            
            X509Certificate[] certificates = (X509Certificate[]) session.getPeerCertificates();
            
            if (certificates.length > 0) {
                X509Certificate cert = certificates[0];
                
                CertificateValidationResult result = new CertificateValidationResult();
                result.valid = true;
                result.subject = cert.getSubjectX500Principal().getName();
                result.issuer = cert.getIssuerX500Principal().getName();
                result.notBefore = cert.getNotBefore().toString();
                result.notAfter = cert.getNotAfter().toString();
                result.serialNumber = cert.getSerialNumber().toString();
                
                socket.close();
                
                SecureLogger.info("Certificado válido para " + host);
                return result;
            }
            
            socket.close();
            
        } catch (Exception e) {
            SecureLogger.error("Error al validar certificado para " + host, e);
            
            CertificateValidationResult result = new CertificateValidationResult();
            result.valid = false;
            result.error = e.getMessage();
            return result;
        }
        
        CertificateValidationResult result = new CertificateValidationResult();
        result.valid = false;
        result.error = "No se pudo obtener certificado";
        return result;
    }
    
    /**
     * Establece el modo de verificación
     */
    public void setStrictMode(boolean strict) {
        this.strictMode = strict;
        SecureLogger.security("Modo SSL cambiado a: " + (strict ? "ESTRICTO" : "PERMISIVO"));
    }
    
    /**
     * Obtiene el modo actual
     */
    public boolean isStrictMode() {
        return strictMode;
    }
    
    /**
     * TrustManager personalizado para validación
     */
    private static class ValidationTrustManager implements X509TrustManager {
        private final X509TrustManager defaultTrustManager;
        
        ValidationTrustManager() throws Exception {
            TrustManagerFactory factory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm()
            );
            factory.init((KeyStore) null);
            
            for (TrustManager tm : factory.getTrustManagers()) {
                if (tm instanceof X509TrustManager) {
                    defaultTrustManager = (X509TrustManager) tm;
                    return;
                }
            }
            throw new Exception("No se encontró X509TrustManager");
        }
        
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) 
                throws CertificateException {
            defaultTrustManager.checkClientTrusted(chain, authType);
        }
        
        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) 
                throws CertificateException {
            try {
                defaultTrustManager.checkServerTrusted(chain, authType);
            } catch (CertificateException e) {
                SecureLogger.error("Certificado del servidor no confiable", e);
                throw e;
            }
        }
        
        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return defaultTrustManager.getAcceptedIssuers();
        }
    }
    
    /**
     * Resultado de validación de certificado
     */
    public static class CertificateValidationResult {
        public boolean valid;
        public String subject;
        public String issuer;
        public String notBefore;
        public String notAfter;
        public String serialNumber;
        public String error;
        
        @Override
        public String toString() {
            if (!valid) {
                return "Certificado inválido: " + error;
            }
            
            return String.format(
                "Certificado válido:\n" +
                "  Sujeto: %s\n" +
                "  Emisor: %s\n" +
                "  Válido desde: %s\n" +
                "  Válido hasta: %s\n" +
                "  Número de serie: %s",
                subject, issuer, notBefore, notAfter, serialNumber
            );
        }
    }
}
