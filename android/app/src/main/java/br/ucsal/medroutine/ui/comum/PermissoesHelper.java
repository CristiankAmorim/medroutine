package br.ucsal.medroutine.ui.comum;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

import br.ucsal.medroutine.R;
import br.ucsal.medroutine.alarm.AgendadorLembretes;
import br.ucsal.medroutine.session.SessionManager;

public final class PermissoesHelper {
    private PermissoesHelper() { }

    public static boolean notificacoesPermitidas(Context c) {
        return NotificationManagerCompat.from(c).areNotificationsEnabled();
    }

    public static boolean telaCheiaPermitida(Context c) {
        if (Build.VERSION.SDK_INT < 34) {
            return true;
        }
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        return nm == null || nm.canUseFullScreenIntent();
    }

    public static void pedirNotificacoesSeNecessario(AppCompatActivity activity, ActivityResultLauncher<String> launcher) {
        SessionManager sessao = SessionManager.get(activity);
        if (Build.VERSION.SDK_INT >= 33 && !notificacoesPermitidas(activity) && !sessao.jaPediuPermissaoNotificacao()) {
            sessao.marcarPedidoPermissaoNotificacao();
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    public static void atualizarAviso(AppCompatActivity activity, View aviso, boolean perfilIdoso) {
        TextView texto = aviso.findViewById(R.id.texto_aviso_permissao);
        Button botao = aviso.findViewById(R.id.botao_aviso_permissao);
        Intent destino = null;
        String mensagem = null;

        if (!notificacoesPermitidas(activity)) {
            mensagem = activity.getString(perfilIdoso ? R.string.aviso_notificacoes_idoso : R.string.aviso_notificacoes_cuidador);
            destino = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, activity.getPackageName());
        } else if (perfilIdoso && !AgendadorLembretes.podeAgendarExato(activity)) {
            mensagem = activity.getString(R.string.aviso_alarmes_exatos);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                destino = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                        Uri.parse("package:" + activity.getPackageName()));
            }
        } else if (perfilIdoso && !telaCheiaPermitida(activity)) {
            mensagem = activity.getString(R.string.aviso_tela_cheia);
            if (Build.VERSION.SDK_INT >= 34) {
                destino = new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                        Uri.parse("package:" + activity.getPackageName()));
            }
        }

        if (mensagem == null) {
            aviso.setVisibility(View.GONE);
            return;
        }
        aviso.setVisibility(View.VISIBLE);
        texto.setText(mensagem);
        final Intent abrir = destino;
        botao.setVisibility(abrir == null ? View.GONE : View.VISIBLE);
        botao.setOnClickListener(v -> {
            try {
                activity.startActivity(abrir);
            } catch (RuntimeException e) {
                activity.startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.parse("package:" + activity.getPackageName())));
            }
        });
    }

    public static void atualizarFaixaDemo(AppCompatActivity activity, View faixa) {
        if (faixa != null) {
            faixa.setVisibility(SessionManager.get(activity).isDemo() ? View.VISIBLE : View.GONE);
        }
    }
}
