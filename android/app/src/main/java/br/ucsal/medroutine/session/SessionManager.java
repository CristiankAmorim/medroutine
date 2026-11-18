package br.ucsal.medroutine.session;

import android.content.Context;
import android.content.SharedPreferences;

import br.ucsal.medroutine.BuildConfig;
import br.ucsal.medroutine.data.model.TokenResponse;
import br.ucsal.medroutine.data.model.Usuario;

public final class SessionManager {
    private static final String ARQUIVO = "medroutine_sessao";
    private static SessionManager instancia;

    private final SharedPreferences prefs;

    private SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE);
    }

    public static synchronized SessionManager get(Context context) {
        if (instancia == null) {
            instancia = new SessionManager(context);
        }
        return instancia;
    }

    public void iniciar(TokenResponse resposta, boolean demo) {
        Usuario u = resposta.usuario;
        prefs.edit()
                .putString("token", resposta.token)
                .putLong("usuarioId", u.id)
                .putString("tipo", u.tipo)
                .putString("nome", u.nome)
                .putString("email", u.email)
                .putBoolean("anuncioVoz", u.anuncioVoz)
                .putBoolean("demo", demo)
                .remove("idosoSelecionado")
                .apply();
    }

    public void encerrar() {
        String url = prefs.getString("urlServidor", null);
        prefs.edit().clear().apply();
        if (url != null) {
            prefs.edit().putString("urlServidor", url).apply();
        }
    }

    public boolean isLogado() { return prefs.getString("token", null) != null; }
    public boolean isDemo() { return prefs.getBoolean("demo", false); }
    public boolean isIdoso() { return Usuario.IDOSO.equals(prefs.getString("tipo", null)); }
    public boolean isCuidador() { return Usuario.CUIDADOR.equals(prefs.getString("tipo", null)); }
    public String getToken() { return prefs.getString("token", null); }
    public long getUsuarioId() { return prefs.getLong("usuarioId", 0); }
    public String getNome() { return prefs.getString("nome", ""); }
    public String getEmail() { return prefs.getString("email", ""); }

    public boolean isAnuncioVoz() { return prefs.getBoolean("anuncioVoz", false); }
    public void setAnuncioVoz(boolean valor) { prefs.edit().putBoolean("anuncioVoz", valor).apply(); }
    public void setNome(String nome) { prefs.edit().putString("nome", nome).apply(); }

    public long getIdosoSelecionado() { return prefs.getLong("idosoSelecionado", 0); }
    public void setIdosoSelecionado(long id) { prefs.edit().putLong("idosoSelecionado", id).apply(); }

    public String getUrlServidor() {
        String url = prefs.getString("urlServidor", BuildConfig.API_URL);
        return url.endsWith("/") ? url : url + "/";
    }

    public void setUrlServidor(String url) { prefs.edit().putString("urlServidor", url).apply(); }

    public boolean jaPediuPermissaoNotificacao() { return prefs.getBoolean("pediuNotificacao", false); }
    public void marcarPedidoPermissaoNotificacao() { prefs.edit().putBoolean("pediuNotificacao", true).apply(); }
}
