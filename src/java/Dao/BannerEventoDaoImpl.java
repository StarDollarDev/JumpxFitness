package Dao;

import Interface.IBannerEvento;
import Model.BannerEvento;
import Util.ConexionSqlSingleton;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BannerEventoDaoImpl implements IBannerEvento {

    @Override
    public BannerEvento buscarActivo() {
        try (Connection cn = ConexionSqlSingleton.getConnection();
             PreparedStatement st = cn.prepareStatement(
                     "SELECT * FROM banner_evento_jx WHERE activo = 1 ORDER BY fecha_creacion DESC FETCH FIRST 1 ROWS ONLY");
             ResultSet rs = st.executeQuery()) {
            if (rs.next()) return mapear(rs);
        } catch (SQLException e) {
            System.err.println(">>> ERROR SQL al buscar banner activo <<<");
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public List<BannerEvento> listaTodos() {
        List<BannerEvento> lista = new ArrayList<>();
        try (Connection cn = ConexionSqlSingleton.getConnection();
             PreparedStatement st = cn.prepareStatement("SELECT * FROM banner_evento_jx ORDER BY fecha_creacion DESC");
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            System.err.println(">>> ERROR SQL al listar banners <<<");
            e.printStackTrace();
        }
        return lista;
    }

    /**
     * Inserta o actualiza. Si queda activo=true, desactiva cualquier otro banner
     * (todo en una transacción) para que nunca haya dos activos al mismo tiempo.
     */
    @Override
    public boolean guardar(BannerEvento banner) {
        Connection cn = ConexionSqlSingleton.getConnection();
        synchronized (cn) {
            boolean autoCommitOriginal = true;
            try {
                autoCommitOriginal = cn.getAutoCommit();
                cn.setAutoCommit(false);

                if (banner.getIdBanner() > 0) {
                    String sql = "UPDATE banner_evento_jx SET titulo=?, descripcion=?, hora_texto=?, "
                            + "imagen_fondo=?, activo=? WHERE id_banner=?";
                    try (PreparedStatement st = cn.prepareStatement(sql)) {
                        st.setString(1, banner.getTitulo());
                        st.setString(2, banner.getDescripcion());
                        st.setString(3, banner.getHoraTexto());
                        st.setString(4, banner.getImagenFondo());
                        st.setInt(5, banner.isActivo() ? 1 : 0);
                        st.setInt(6, banner.getIdBanner());
                        if (st.executeUpdate() == 0) {
                            cn.rollback();
                            return false;
                        }
                    }
                } else {
                    String sql = "INSERT INTO banner_evento_jx (titulo, descripcion, hora_texto, imagen_fondo, activo) "
                            + "VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement st = cn.prepareStatement(sql, new String[]{"id_banner"})) {
                        st.setString(1, banner.getTitulo());
                        st.setString(2, banner.getDescripcion());
                        st.setString(3, banner.getHoraTexto());
                        st.setString(4, banner.getImagenFondo());
                        st.setInt(5, banner.isActivo() ? 1 : 0);
                        st.executeUpdate();
                        try (ResultSet rs = st.getGeneratedKeys()) {
                            if (rs.next()) banner.setIdBanner(rs.getInt(1));
                        }
                    }
                }

                if (banner.isActivo()) {
                    try (PreparedStatement st = cn.prepareStatement(
                            "UPDATE banner_evento_jx SET activo = 0 WHERE id_banner <> ?")) {
                        st.setInt(1, banner.getIdBanner());
                        st.executeUpdate();
                    }
                }

                cn.commit();
                return true;

            } catch (SQLException e) {
                try { cn.rollback(); } catch (SQLException ignored) {}
                System.err.println(">>> ERROR SQL al guardar banner <<<");
                e.printStackTrace();
                return false;
            } finally {
                try { cn.setAutoCommit(autoCommitOriginal); } catch (SQLException ignored) {}
            }
        }
    }

    @Override
    public boolean eliminar(int idBanner) {
        try (Connection cn = ConexionSqlSingleton.getConnection();
             PreparedStatement st = cn.prepareStatement("DELETE FROM banner_evento_jx WHERE id_banner = ?")) {
            st.setInt(1, idBanner);
            return st.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println(">>> ERROR SQL al eliminar banner <<<");
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public BannerEvento SearchById(int idBanner) {
        try (Connection cn = ConexionSqlSingleton.getConnection();
             PreparedStatement st = cn.prepareStatement("SELECT * FROM banner_evento_jx WHERE id_banner = ?")) {
            st.setInt(1, idBanner);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (SQLException e) {
            System.err.println(">>> ERROR SQL al buscar banner <<<");
            e.printStackTrace();
        }
        return null;
    }

    private BannerEvento mapear(ResultSet rs) throws SQLException {
        BannerEvento b = new BannerEvento();
        b.setIdBanner(rs.getInt("id_banner"));
        b.setTitulo(rs.getString("titulo"));
        b.setDescripcion(rs.getString("descripcion"));
        b.setHoraTexto(rs.getString("hora_texto"));
        b.setImagenFondo(rs.getString("imagen_fondo"));
        b.setActivo(rs.getInt("activo") == 1);
        b.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
        return b;
    }
}
