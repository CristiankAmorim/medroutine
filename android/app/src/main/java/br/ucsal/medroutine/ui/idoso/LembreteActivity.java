package br.ucsal.medroutine.ui.idoso;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

import br.ucsal.medroutine.R;
import br.ucsal.medroutine.alarm.AgendadorLembretes;
import br.ucsal.medroutine.data.local.AppDatabase;
import br.ucsal.medroutine.data.local.DoseLocal;
import br.ucsal.medroutine.data.sync.FilaConfirmacoes;
import br.ucsal.medroutine.notification.Notificador;
import br.ucsal.medroutine.session.SessionManager;
import br.ucsal.medroutine.ui.comum.Formatos;
import br.ucsal.medroutine.util.Executores;

public class LembreteActivity extends AppCompatActivity {
    private static final String EXTRA_DOSE = "doseId";
    private static final long DURACAO_SOM_MS = 60_000;

    private long doseId;
    private MediaPlayer som;
    private TextToSpeech voz;
    private final Handler handler = new Handler(Looper.getMainLooper());

    public static Intent intent(Context context, long doseId) {
        return new Intent(context, LembreteActivity.class)
                .putExtra(EXTRA_DOSE, doseId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mostrarSobreBloqueio();
        setContentView(R.layout.activity_lembrete);
        doseId = getIntent().getLongExtra(EXTRA_DOSE, -1);
        findViewById(R.id.faixa_demo).setVisibility(SessionManager.get(this).isDemo() ? View.VISIBLE : View.GONE);

        findViewById(R.id.botao_tomei).setOnClickListener(v -> confirmar());
        findViewById(R.id.botao_adiar).setOnClickListener(v -> adiar());
        carregarDose();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        doseId = intent.getLongExtra(EXTRA_DOSE, -1);
        carregarDose();
    }

    private void mostrarSobreBloqueio() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager km = getSystemService(KeyguardManager.class);
            if (km != null) {
                km.requestDismissKeyguard(this, null);
            }
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void carregarDose() {
        Executores.disco(() -> {
            DoseLocal d = AppDatabase.get(getApplicationContext()).doses().porId(doseId);
            Executores.principal(() -> exibir(d));
        });
    }

    private void exibir(DoseLocal d) {
        if (d == null || d.isConfirmada()) {
            Notificador.cancelarLembrete(this, doseId);
            Toast.makeText(this, R.string.dose_ja_confirmada, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        ((TextView) findViewById(R.id.texto_hora)).setText(Formatos.hora(d.previsto()));
        ((TextView) findViewById(R.id.texto_medicamento)).setText(d.medicamentoNome);
        ((TextView) findViewById(R.id.texto_dosagem)).setText(d.dosagem == null ? "" : d.dosagem);
        TextView orientacoes = findViewById(R.id.texto_orientacoes);
        orientacoes.setText(d.orientacoes == null ? "" : d.orientacoes);
        orientacoes.setVisibility(d.orientacoes == null ? View.GONE : View.VISIBLE);
        tocarSom();
        if (SessionManager.get(this).isAnuncioVoz()) {
            anunciar(getString(R.string.anuncio_voz, d.medicamentoNome, d.dosagem == null ? "" : d.dosagem));
        }
    }

    private void tocarSom() {
        pararSom();
        som = MediaPlayer.create(this, R.raw.lembrete, new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(), 0);
        if (som != null) {
            som.setLooping(true);
            som.start();
            handler.postDelayed(this::pararSom, DURACAO_SOM_MS);
        }
    }

    private void anunciar(String texto) {
        voz = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS && voz != null) {
                voz.setLanguage(new Locale("pt", "BR"));
                voz.setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build());
                handler.postDelayed(() -> {
                    pararSom();
                    if (voz != null) {
                        voz.speak(texto, TextToSpeech.QUEUE_FLUSH, null, "lembrete");
                    }
                }, 2500);
            }
        });
    }

    private void pararSom() {
        if (som != null) {
            try {
                som.stop();
            } catch (IllegalStateException ignorado) {
            }
            som.release();
            som = null;
        }
    }

    private void confirmar() {
        pararSom();
        FilaConfirmacoes.confirmar(this, doseId, ok -> {
            Toast.makeText(this, ok ? R.string.lembrete_confirmado : R.string.lembrete_cedo, Toast.LENGTH_LONG).show();
            finish();
        });
    }

    private void adiar() {
        pararSom();
        Notificador.cancelarLembrete(this, doseId);
        AgendadorLembretes.adiar(this, doseId);
        Toast.makeText(this, R.string.lembrete_adiado, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        pararSom();
        if (voz != null) {
            voz.stop();
            voz.shutdown();
            voz = null;
        }
        super.onDestroy();
    }
}
