package com.gestorcorreo.model;

/**
 * Representa una etiqueta de color para clasificar correos
 */
public class Tag {
    private String name;
    private String color; // Color en formato hexadecimal (#RRGGBB)
    private String description;
    
    public Tag() {
    }
    
    public Tag(String name, String color) {
        this.name = name;
        this.color = color;
        this.description = "";
    }
    
    public Tag(String name, String color, String description) {
        this.name = name;
        this.color = color;
        this.description = description;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Tag tag = (Tag) obj;
        return name != null && name.equals(tag.name);
    }
    
    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return name;
    }
    
    /**
     * Etiquetas predefinidas
     */
    public static Tag[] getDefaultTags() {
        return new Tag[]{
            new Tag("Importante", "#FF0000", "Correos importantes que requieren atención"),
            new Tag("Personal", "#0000FF", "Correos personales"),
            new Tag("Trabajo", "#FFA500", "Correos relacionados con el trabajo"),
            new Tag("Pendiente", "#FFFF00", "Correos pendientes de respuesta"),
            new Tag("Completado", "#00FF00", "Correos completados o resueltos"),
            new Tag("Seguimiento", "#FF00FF", "Correos que requieren seguimiento")
        };
    }
}
