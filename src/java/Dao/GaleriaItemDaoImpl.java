package Dao;

import Interface.IGaleriaItem;
import Model.GaleriaItem;
import Util.ConexionSqlSingleton;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GaleriaItemDaoImpl implements IGaleriaItem {

    @Override
    public List<GaleriaItem> listaActivos() {
        return listar("SELECT * FROM galeria_cancha_jx WHERE activo = 1 ORDER BY orden ASC, fecha_subida DESC");
    }

    @Override
    public List<GaleriaItem> listaTodos() {
        return listar("SELECT * FROM galeria_cancha_jx ORDER BY orden ASC, fecha_subida DESC");
    }

    private List<GaleriaItem> listar(String sql) {
        List<GaleriaItem> lista = new ArrayList<>();
        try (Connection cn = ConexionSqlSingleton.getConnection();
             PreparedStatement st = cn.prepareStatement(sql);
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            System.err.println(">>> ERROR SQL al listar galería <<<");
            e.printStackTrace();
        }
        return lista;
    }

    @Override
    public boolean insertar(GaleriaItem item) {
        String sql = "INSERT INTO galeria_cancha_jx (tipo, titulo, url, orden, activo) VALUES (?, ?, ?, ?, ?)";
        try (Connection cn = ConexionSqlSingleton.getConnection();
             PreparedStatement st = cn.prepareStatement(sql, new String[]{"id_item"})) {
            st.setString(1, item.getTipo());
            st.setString(2, item.getTitulo());
            st.setString(3, item.getUrl());
            st.setInt(4, item.getOrden());
            st.setInt(5, item.isActivo() ? 1 : 0);
            int r = st.executeUpdate();
            if (r == 0) return false;
            try (ResultSet rs = st.getGeneratedKeys()) {
                if (rs.next()) item.setIdItem(rs.getInt(1));
            }
            return true;
        } catch (SQLException e) {
            System.err.println(">>> ERROR SQL al insertar item de galería <<<");
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean eliminar(int idItem) {
        try (Connection cn = ConexionSqlSingleton.getConnection();
             PreparedStatement st = cn.prepareStatement("DELETE FROM galeria_cancha_jx WHERE id_item = ?")) {
            st.setInt(1, idItem);
            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println(">>> ERROR SQL al eliminar item de galería <<<");
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public GaleriaItem SearchById(int idItem) {
        try (Connection cn = ConexionSqlSingleton.getConnection();
             PreparedStatement st = cn.prepareStatement("SELECT * FROM galeria_cancha_jx WHERE id_item = ?")) {
            st.setInt(1, idItem);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (SQLException e) {
            System.err.println(">>> ERROR SQL al buscar item de galería <<<");
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean actualizarOrden(int idItem, int nuevoOrden) {
        try (Connection cn = ConexionSqlSingleton.getConnection();
             PreparedStatement st = cn.prepareStatement("UPDATE galeria_cancha_jx SET orden = ? WHERE id_item = ?")) {
            st.setInt(1, nuevoOrden);
            st.setInt(2, idItem);
            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println(">>> ERROR SQL al actualizar orden <<<");
            e.printStackTrace();
            return false;
        }
    }

    private GaleriaItem mapear(ResultSet rs) throws SQLException {
        GaleriaItem g = new GaleriaItem();
        g.setIdItem(rs.getInt("id_item"));
        g.setTipo(rs.getString("tipo"));
        g.setTitulo(rs.getString("titulo"));
        g.setUrl(rs.getString("url"));
        g.setOrden(rs.getInt("orden"));
        g.setActivo(rs.getInt("activo") == 1);
        g.setFechaSubida(rs.getTimestamp("fecha_subida"));
        return g;
    }
}
