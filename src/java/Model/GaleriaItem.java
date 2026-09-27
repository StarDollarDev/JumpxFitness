package Model;

import java.sql.Timestamp;

public class GaleriaItem {

    private int idItem;
    private String tipo;   // FOTO | VIDEO
    private String titulo;
    private String url;    // ruta relativa (foto) o link embebido (video)
    private int orden;
    private boolean activo;
    private Timestamp fechaSubida;

    public GaleriaItem() {
    }

    public int getIdItem() { return idItem; }
    public void setIdItem(int idItem) { this.idItem = idItem; }
    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getTitulo() { return titulo; }
    public void setTitulo(String titulo) { this.titulo = titulo; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public Timestamp getFechaSubida() { return fechaSubida; }
    public void setFechaSubida(Timestamp fechaSubida) { this.fechaSubida = fechaSubida; }
}
