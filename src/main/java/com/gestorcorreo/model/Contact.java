package com.gestorcorreo.model;

/**
 * Representa un contacto en la libreta de direcciones
 */
public class Contact {
    private String name;
    private String email;
    private String phone;
    private String company;
    private String notes;
    private int frequency; // Frecuencia de uso para autocompletar
    
    public Contact() {
        this.frequency = 0;
    }
    
    public Contact(String name, String email) {
        this();
        this.name = name;
        this.email = email;
    }
    
    public Contact(String name, String email, String phone, String company, String notes) {
        this(name, email);
        this.phone = phone;
        this.company = company;
        this.notes = notes;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getCompany() {
        return company;
    }
    
    public void setCompany(String company) {
        this.company = company;
    }
    
    public String getNote() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public int getFrequency() {
        return frequency;
    }
    
    public void setFrequency(int frequency) {
        this.frequency = frequency;
    }
    
    public void incrementFrequency() {
        this.frequency++;
    }
    
    /**
     * Retorna una representación formateada para autocompletar
     * Formato: Nombre <email>
     */
    public String getFormattedAddress() {
        if (name != null && !name.isEmpty()) {
            return name + " <" + email + ">";
        }
        return email;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Contact contact = (Contact) obj;
        return email != null && email.equalsIgnoreCase(contact.email);
    }
    
    @Override
    public int hashCode() {
        return email != null ? email.toLowerCase().hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return getFormattedAddress();
    }
}
