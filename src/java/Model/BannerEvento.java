package Model;

import java.sql.Timestamp;

public class BannerEvento {

    private int idBanner;
    private String titulo;
    private String descripcion;
    private String horaTexto;
    private String imagenFondo;
    private boolean activo;
    private Timestamp fechaCreacion;

    public BannerEvento() {
    }

    public int getIdBanner() { return idBanner; }
    public void setIdBanner(int idBanner) { this.idBanner = idBanner; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getHoraTexto() { return horaTexto; }
    public void setHoraTexto(String horaTexto) { this.horaTexto = horaTexto; }
    public String getImagenFondo() { return imagenFondo; }
    public void setImagenFondo(String imagenFondo) { this.imagenFondo = imagenFondo; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public Timestamp getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Timestamp fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
