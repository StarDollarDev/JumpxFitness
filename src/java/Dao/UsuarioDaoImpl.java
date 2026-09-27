package Dao;

import Interface.IUsuario;
import Model.Persona;
import Model.Rol;
import Model.Usuario;
import Util.ConexionSqlSingleton;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDaoImpl implements IUsuario {

    private Connection cn;
    private PersonaDaoImpl personaDAO;

    // Un hash BCrypt real siempre tiene esta forma: $2a$10$<53 caracteres>
    private static final java.util.regex.Pattern PATRON_BCRYPT
            = java.util.regex.Pattern.compile("^\\$2[aby]\\$\\d{2}\\$.{53}$");

    /**
     * Red de seguridad: pase lo que pase desde el Controller, esta capa
     * NUNCA deja pasar una contraseña en texto plano hacia la base de
     * datos. Si lo que llega no tiene forma de hash BCrypt, se hashea
     * aquí mismo antes de guardar.
     */
    private String asegurarHash(String contraseña) {
        if (contraseña != null && PATRON_BCRYPT.matcher(contraseña).matches()) {
            return contraseña; // ya viene hasheada, no se toca (evita doble hash)
        }
        System.out.println("Aviso: se recibió una contraseña sin hashear; se hashea antes de guardar.");
        return org.mindrot.jbcrypt.BCrypt.hashpw(contraseña, org.mindrot.jbcrypt.BCrypt.gensalt(12));
    }

    public UsuarioDaoImpl() {
        this.personaDAO = new PersonaDaoImpl();
    }

    @Override
    public List<Usuario> lista() {
        List<Usuario> lista = new ArrayList<>();
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            cn = ConexionSqlSingleton.getConnection();
            String query = "SELECT * FROM usuario ORDER BY id_usuario";
            st = cn.prepareStatement(query);
            rs = st.executeQuery();
            while (rs.next()) {
                Usuario u = new Usuario();
                u.setId_usuario(rs.getInt("id_usuario"));
                u.setUsuario(rs.getString("usuario"));
                u.setContraseña(rs.getString("contrasena"));
                u.setRol(Rol.valueOf(rs.getString("rol"))); // Convertir String a enum
                Persona p = personaDAO.SearchById(rs.getInt("id_persona"));
                u.setPersona(p);
                lista.add(u);
            }
        } catch (Exception e) {
            System.out.println("Error al listar usuarios: " + e.getMessage());
        } finally {
            cerrarRecursos(rs, st);
        }
        return lista;
    }

    /**
     * Inserta persona + usuario en UNA sola transacción: si el insert de
     * usuario falla por cualquier motivo (ej. un CHECK de rol, un usuario
     * duplicado, lo que sea), se revierte también el insert de persona.
     * Antes cada insert se confirmaba solo (autocommit) y una falla a
     * mitad de camino dejaba una "persona" huérfana sin usuario asociado,
     * que además bloqueaba reintentar el registro con el mismo documento
     * (ORA-00001 en la restricción única de persona.numero_doc).
     */
    @Override
    public boolean insertar(Usuario usuario) {
        Connection conexion = ConexionSqlSingleton.getConnection();

        // La app entera comparte una sola Connection (sin pool), así que
        // sincronizamos para que dos registros al mismo tiempo no mezclen
        // sus transacciones entre sí.
        synchronized (conexion) {
            boolean autoCommitOriginal = true;
            PreparedStatement st = null;
            ResultSet rs = null;
            try {
                autoCommitOriginal = conexion.getAutoCommit();
                conexion.setAutoCommit(false);

                boolean personaInsert = personaDAO.insertSoloPersona(usuario.getPersona(), null);
                if (!personaInsert) {
                    conexion.rollback();
                    return false;
                }

                String query = "INSERT INTO usuario (usuario, contrasena, rol, id_persona) "
                        + "VALUES (?, ?, ?, ?)";
                st = conexion.prepareStatement(query, new String[]{"id_usuario"});
                st.setString(1, usuario.getUsuario());
                st.setString(2, asegurarHash(usuario.getContraseña()));
                st.setString(3, usuario.getRol().name());
                st.setInt(4, usuario.getPersona().getId_persona());

                int r = st.executeUpdate();
                if (r == 0) {
                    conexion.rollback();
                    return false;
                }

                rs = st.getGeneratedKeys();
                if (rs.next()) {
                    usuario.setId_usuario(rs.getInt(1));
                }

                conexion.commit();
                System.out.println("Usuario registrado correctamente con ID: " + usuario.getId_usuario());
                return true;

            } catch (Exception e) {
                System.out.println("Error al insertar usuario: " + e.getMessage());
                try {
                    conexion.rollback();
                } catch (SQLException ex) {
                    System.out.println("Error al revertir la transacción: " + ex.getMessage());
                }
                return false;
            } finally {
                cerrarRecursos(rs, st);
                try {
                    conexion.setAutoCommit(autoCommitOriginal);
                } catch (SQLException ex) {
                    System.out.println("Error al restaurar autoCommit: " + ex.getMessage());
                }
            }
        }
    }

    @Override
    public boolean update(Usuario usuario) {
        PreparedStatement st = null;
        boolean resultado = false;
        try {
            cn = ConexionSqlSingleton.getConnection();

            // Actualizar persona
            boolean personaUpdate = personaDAO.update(usuario.getPersona());
            if (!personaUpdate) {
                return false;
            }

            String query = "UPDATE usuario SET usuario=?, contrasena=?, rol=?, id_persona=? WHERE id_usuario=?";
            st = cn.prepareStatement(query);
            st.setString(1, usuario.getUsuario());
            st.setString(2, asegurarHash(usuario.getContraseña()));
            st.setString(3, usuario.getRol().name());
            st.setInt(4, usuario.getPersona().getId_persona());
            st.setInt(5, usuario.getId_usuario());

            int r = st.executeUpdate();
            resultado = r > 0;
        } catch (Exception e) {
            System.out.println("Error al actualizar usuario: " + e.getMessage());
        } finally {
            cerrarRecursos(null, st);
        }
        return resultado;
    }

    @Override
    public Usuario SearchById(int id) {
        Usuario usuario = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            cn = ConexionSqlSingleton.getConnection();
            String query = "SELECT * FROM usuario WHERE id_usuario = ?";
            st = cn.prepareStatement(query);
            st.setInt(1, id);
            rs = st.executeQuery();
            if (rs.next()) {
                usuario = new Usuario();
                usuario.setId_usuario(rs.getInt("id_usuario"));
                usuario.setUsuario(rs.getString("usuario"));
                usuario.setContraseña(rs.getString("contrasena"));
                usuario.setRol(Rol.valueOf(rs.getString("rol")));
                Persona p = personaDAO.SearchById(rs.getInt("id_persona"));
                usuario.setPersona(p);
            }
        } catch (Exception e) {
            System.out.println("Error al buscar usuario: " + e.getMessage());
        } finally {
            cerrarRecursos(rs, st);
        }
        return usuario;
    }

    @Override
    public boolean delete(int id) {
        PreparedStatement st = null;
        boolean resultado = false;
        try {
            cn = ConexionSqlSingleton.getConnection();

            // Obtener usuario para eliminar persona
            Usuario usuario = SearchById(id);
            if (usuario == null) {
                return false;
            }

            String query = "DELETE FROM usuario WHERE id_usuario = ?";
            st = cn.prepareStatement(query);
            st.setInt(1, id);
            int r = st.executeUpdate();
            resultado = r > 0;

            if (resultado) {
                personaDAO.delete(usuario.getPersona().getId_persona());
            }
        } catch (Exception e) {
            System.out.println("Error al eliminar usuario: " + e.getMessage());
        } finally {
            cerrarRecursos(null, st);
        }
        return resultado;
    }

    @Override
    public Usuario SearchByUsername(String username) {
        Usuario usuario = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            cn = ConexionSqlSingleton.getConnection();
            String query = "SELECT * FROM usuario WHERE usuario = ?";
            st = cn.prepareStatement(query);
            st.setString(1, username);
            rs = st.executeQuery();
            if (rs.next()) {
                usuario = new Usuario();
                usuario.setId_usuario(rs.getInt("id_usuario"));
                usuario.setUsuario(rs.getString("usuario"));
                usuario.setContraseña(rs.getString("contrasena"));
                usuario.setRol(Rol.valueOf(rs.getString("rol")));
                Persona p = personaDAO.SearchById(rs.getInt("id_persona"));
                usuario.setPersona(p);
            }
        } catch (Exception e) {
            System.out.println("Error al buscar usuario por nombre: " + e.getMessage());
        } finally {
            cerrarRecursos(rs, st);
        }
        return usuario;
    }

    @Override
    public List<Usuario> SearchByRol(String rol) {
        List<Usuario> lista = new ArrayList<>();
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            cn = ConexionSqlSingleton.getConnection();
            String query = "SELECT * FROM usuario WHERE rol = ? ORDER BY usuario";
            st = cn.prepareStatement(query);
            st.setString(1, rol);
            rs = st.executeQuery();
            while (rs.next()) {
                Usuario usuario = new Usuario();
                usuario.setId_usuario(rs.getInt("id_usuario"));
                usuario.setUsuario(rs.getString("usuario"));
                usuario.setContraseña(rs.getString("contrasena"));
                usuario.setRol(Rol.valueOf(rs.getString("rol")));
                Persona p = personaDAO.SearchById(rs.getInt("id_persona"));
                usuario.setPersona(p);
                lista.add(usuario);
            }
        } catch (Exception e) {
            System.out.println("Error al buscar usuarios por rol: " + e.getMessage());
        } finally {
            cerrarRecursos(rs, st);
        }
        return lista;
    }

    private void cerrarRecursos(ResultSet rs, PreparedStatement st) {
        try {
            if (rs != null) {
                rs.close();
            }
            if (st != null) {
                st.close();
            }
        } catch (Exception ex) {
            System.out.println("Error cerrando recursos: " + ex.getMessage());
        }
    }

    @Override
    public Usuario SearchByPersonaId(int idPersona) {
        Usuario usuario = null;
        PreparedStatement st = null;
        ResultSet rs = null;
        try {
            cn = ConexionSqlSingleton.getConnection();
            String query = "SELECT * FROM usuario WHERE id_persona = ?";
            st = cn.prepareStatement(query);
            st.setInt(1, idPersona);
            rs = st.executeQuery();
            if (rs.next()) {
                usuario = new Usuario();
                usuario.setId_usuario(rs.getInt("id_usuario"));
                usuario.setUsuario(rs.getString("usuario"));
                usuario.setContraseña(rs.getString("contrasena"));
                usuario.setRol(Rol.valueOf(rs.getString("rol")));
                Persona p = personaDAO.SearchById(rs.getInt("id_persona"));
                usuario.setPersona(p);
            }
        } catch (Exception e) {
            System.out.println("Error al buscar usuario por id_persona: " + e.getMessage());
        } finally {
            cerrarRecursos(rs, st);
        }
        return usuario;
    }

    @Override
    public Usuario validate(String user, String passPlano) {

        // 1) Buscamos SOLO por nombre de usuario (ya no por contraseña en el SQL)
        Usuario usuario = SearchByUsername(user);

        if (usuario == null) {
            return null; // el usuario no existe
        }

        // 2) Comparamos la contraseña ingresada contra el hash guardado, en Java
        boolean coincide = Usuario.checkPassword(passPlano, usuario.getContraseña());

        return coincide ? usuario : null;
    }

}
