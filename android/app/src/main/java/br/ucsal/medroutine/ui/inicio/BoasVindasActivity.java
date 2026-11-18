package br.ucsal.medroutine.ui.inicio;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import br.ucsal.medroutine.R;
import br.ucsal.medroutine.alarm.AgendadorLembretes;
import br.ucsal.medroutine.data.model.TokenResponse;
import br.ucsal.medroutine.demo.DemoRepository;
import br.ucsal.medroutine.session.SessionManager;
import br.ucsal.medroutine.ui.auth.LoginActivity;
import br.ucsal.medroutine.ui.cuidador.CuidadorMainActivity;
import br.ucsal.medroutine.ui.idoso.IdosoCodigoActivity;
import br.ucsal.medroutine.ui.idoso.IdosoHomeActivity;
import br.ucsal.medroutine.util.Executores;

public class BoasVindasActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_boas_vindas);

        findViewById(R.id.botao_cuidador).setOnClickListener(v ->
                startActivity(new Intent(this, LoginActivity.class)));
        findViewById(R.id.botao_idoso).setOnClickListener(v ->
                startActivity(new Intent(this, IdosoCodigoActivity.class)));
        findViewById(R.id.botao_demo).setOnClickListener(v -> escolherPerfilDemo());
        findViewById(R.id.botao_servidor).setOnClickListener(v -> configurarServidor());
    }

    private void escolherPerfilDemo() {
        String[] opcoes = {getString(R.string.demo_como_idoso), getString(R.string.demo_como_cuidador)};
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.demo_titulo)
                .setItems(opcoes, (d, qual) -> iniciarDemo(qual == 0))
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    public static void iniciarDemo(AppCompatActivity activity, boolean comoIdoso) {
        Executores.disco(() -> {
            TokenResponse resp = DemoRepository.get(activity).iniciar(comoIdoso);
            SessionManager.get(activity).iniciar(resp, true);
            AgendadorLembretes.reagendarTodos(activity.getApplicationContext());
            Executores.principal(() -> {
                Intent i = new Intent(activity, comoIdoso ? IdosoHomeActivity.class : CuidadorMainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                activity.startActivity(i);
                activity.finish();
            });
        });
    }

    private void iniciarDemo(boolean comoIdoso) {
        iniciarDemo(this, comoIdoso);
    }

    private void configurarServidor() {
        SessionManager sessao = SessionManager.get(this);
        EditText entrada = new EditText(this);
        entrada.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        entrada.setText(sessao.getUrlServidor());
        entrada.setMinHeight(getResources().getDimensionPixelSize(R.dimen.toque_minimo));
        FrameLayout moldura = new FrameLayout(this);
        int m = getResources().getDimensionPixelSize(R.dimen.margem_tela);
        moldura.setPadding(m, m / 2, m, 0);
        moldura.addView(entrada);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.configurar_servidor)
                .setMessage(R.string.servidor_ajuda)
                .setView(moldura)
                .setPositiveButton(R.string.salvar, (d, w) -> {
                    String url = entrada.getText().toString().trim();
                    if (url.startsWith("http://") || url.startsWith("https://")) {
                        sessao.setUrlServidor(url);
                    }
                })
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }
}
