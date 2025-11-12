package com.gestorcorreo.model;

/**
 * Configuración de conexión para servidores de correo
 */
public class EmailConfig {
    private String accountName;
    private String email;
    private String displayName;
    private String smtpHost;
    private int smtpPort;
    private String imapHost;
    private int imapPort;
    private String pop3Host;
    private int pop3Port;
    private String username;
    private String password;
    private boolean useSsl;
    private boolean useTls;
    private boolean useAuth;
    private boolean defaultAccount;
    private String accountType; // Gmail, Outlook, Yahoo, Custom
    private String signature; // Firma del correo
    private boolean useSignature; // Si se debe usar la firma automáticamente

    public EmailConfig() {
        // Valores por defecto
        this.smtpPort = 587;
        this.imapPort = 993;
        this.pop3Port = 995;
        this.useSsl = true;
        this.useTls = true;
        this.useAuth = true;
        this.signature = "";
        this.useSignature = false;
    }

    // Constructor para Gmail
    public static EmailConfig forGmail(String username, String password) {
        EmailConfig config = new EmailConfig();
        config.setSmtpHost("smtp.gmail.com");
        config.setSmtpPort(587);
        config.setImapHost("imap.gmail.com");
        config.setImapPort(993);
        config.setUsername(username);
        config.setPassword(password);
        config.setUseTls(true);
        config.setUseAuth(true);
        return config;
    }

    // Constructor para Outlook/Hotmail
    public static EmailConfig forOutlook(String username, String password) {
        EmailConfig config = new EmailConfig();
        config.setSmtpHost("smtp-mail.outlook.com");
        config.setSmtpPort(587);
        config.setImapHost("outlook.office365.com");
        config.setImapPort(993);
        config.setUsername(username);
        config.setPassword(password);
        config.setUseTls(true);
        config.setUseAuth(true);
        return config;
    }

    // Getters y Setters
    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }

    public int getSmtpPort() {
        return smtpPort;
    }

    public void setSmtpPort(int smtpPort) {
        this.smtpPort = smtpPort;
    }

    public String getImapHost() {
        return imapHost;
    }

    public void setImapHost(String imapHost) {
        this.imapHost = imapHost;
    }

    public int getImapPort() {
        return imapPort;
    }

    public void setImapPort(int imapPort) {
        this.imapPort = imapPort;
    }

    public String getPop3Host() {
        return pop3Host;
    }

    public void setPop3Host(String pop3Host) {
        this.pop3Host = pop3Host;
    }

    public int getPop3Port() {
        return pop3Port;
    }

    public void setPop3Port(int pop3Port) {
        this.pop3Port = pop3Port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isUseSsl() {
        return useSsl;
    }

    public void setUseSsl(boolean useSsl) {
        this.useSsl = useSsl;
    }

    public boolean isUseTls() {
        return useTls;
    }

    public void setUseTls(boolean useTls) {
        this.useTls = useTls;
    }

    public boolean isUseAuth() {
        return useAuth;
    }

    public void setUseAuth(boolean useAuth) {
        this.useAuth = useAuth;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isDefaultAccount() {
        return defaultAccount;
    }

    public void setDefaultAccount(boolean defaultAccount) {
        this.defaultAccount = defaultAccount;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public boolean isUseSignature() {
        return useSignature;
    }

    public void setUseSignature(boolean useSignature) {
        this.useSignature = useSignature;
    }

    @Override
    public String toString() {
        return "EmailConfig{" +
                "accountName='" + accountName + '\'' +
                ", email='" + email + '\'' +
                ", smtpHost='" + smtpHost + '\'' +
                ", smtpPort=" + smtpPort +
                ", username='" + username + '\'' +
                ", useAuth=" + useAuth +
                '}';
    }
}
