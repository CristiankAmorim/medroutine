package br.ucsal.medroutine.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.net.Uri;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import java.time.Instant;

import br.ucsal.medroutine.R;
import br.ucsal.medroutine.alarm.AcaoLembreteReceiver;
import br.ucsal.medroutine.alarm.AgendadorLembretes;
import br.ucsal.medroutine.data.local.DoseLocal;
import br.ucsal.medroutine.data.model.Notificacao;
import br.ucsal.medroutine.session.SessionManager;
import br.ucsal.medroutine.ui.comum.Formatos;
import br.ucsal.medroutine.ui.cuidador.DoseDetalheActivity;
import br.ucsal.medroutine.ui.idoso.LembreteActivity;

public final class Notificador {
    public static final String CANAL_LEMBRETES = "lembretes_v1";
    public static final String CANAL_CONFIRMACOES = "confirmacoes_v1";
    public static final String CANAL_ALERTAS = "alertas_v1";

    private static final String TAG_LEMBRETE = "lembrete";
    private static final String TAG_CUIDADOR = "cuidador";

    private Notificador() { }

    public static void criarCanais(Context context) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);
        if (nm == null) {
            return;
        }
        Uri som = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + context.getPackageName()
                + "/" + R.raw.lembrete);
        NotificationChannel lembretes = new NotificationChannel(CANAL_LEMBRETES,
                context.getString(R.string.canal_lembretes), NotificationManager.IMPORTANCE_HIGH);
        lembretes.setDescription(context.getString(R.string.canal_lembretes_desc));
        lembretes.setSound(som, new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build());
        lembretes.enableVibration(true);
        lembretes.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        NotificationChannel confirmacoes = new NotificationChannel(CANAL_CONFIRMACOES,
                context.getString(R.string.canal_confirmacoes), NotificationManager.IMPORTANCE_DEFAULT);
        confirmacoes.setDescription(context.getString(R.string.canal_confirmacoes_desc));

        NotificationChannel alertas = new NotificationChannel(CANAL_ALERTAS,
                context.getString(R.string.canal_alertas), NotificationManager.IMPORTANCE_HIGH);
        alertas.setDescription(context.getString(R.string.canal_alertas_desc));
        alertas.enableVibration(true);

        nm.createNotificationChannel(lembretes);
        nm.createNotificationChannel(confirmacoes);
        nm.createNotificationChannel(alertas);
    }

    private static boolean podeNotificar(Context context) {
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    public static void mostrarLembrete(Context context, DoseLocal dose) {
        if (!podeNotificar(context)) {
            return;
        }
        int id = (int) (dose.id % Integer.MAX_VALUE);
        Intent tela = LembreteActivity.intent(context, dose.id);
        PendingIntent piTela = PendingIntent.getActivity(context, id, tela,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String texto = dose.medicamentoNome + " " + (dose.dosagem == null ? "" : dose.dosagem)
                + " · " + Formatos.hora(dose.previsto());

        Notification n = new NotificationCompat.Builder(context, CANAL_LEMBRETES)
                .setSmallIcon(R.drawable.ic_notificacao)
                .setColor(ContextCompat.getColor(context, R.color.primaria))
                .setContentTitle(context.getString(R.string.lembrete_titulo))
                .setContentText(texto)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                        dose.orientacoes == null ? texto : texto + "\n" + dose.orientacoes))
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setContentIntent(piTela)
                .setFullScreenIntent(piTela, true)
                .setOngoing(true)
                .setAutoCancel(false)
                .addAction(R.drawable.ic_check, context.getString(R.string.ja_tomei),
                        acao(context, AcaoLembreteReceiver.ACAO_CONFIRMAR, dose.id))
                .addAction(R.drawable.ic_relogio, context.getString(R.string.lembrar_10_min),
                        acao(context, AcaoLembreteReceiver.ACAO_ADIAR, dose.id))
                .build();
        try {
            NotificationManagerCompat.from(context).notify(TAG_LEMBRETE, id, n);
        } catch (SecurityException semPermissao) {
        }
    }

    public static void cancelarLembrete(Context context, long doseId) {
        NotificationManagerCompat.from(context).cancel(TAG_LEMBRETE, (int) (doseId % Integer.MAX_VALUE));
    }

    private static PendingIntent acao(Context context, String acao, long doseId) {
        Intent i = new Intent(context, AcaoLembreteReceiver.class)
                .setAction(acao)
                .setData(Uri.parse("medroutine://acao/" + acao + "/" + doseId))
                .putExtra(AgendadorLembretes.EXTRA_DOSE_ID, doseId);
        return PendingIntent.getBroadcast(context, (int) (doseId % Integer.MAX_VALUE), i,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void mostrarParaCuidador(Context context, String tipo, long doseId, String idosoNome,
                                           String medicamento, Instant previsto, int toleranciaMinutos,
                                           Instant confirmadoEm, boolean atraso) {
        if (!podeNotificar(context)) {
            return;
        }
        boolean alerta = Notificacao.ALERTA.equals(tipo);
        String titulo;
        String texto;
        if (alerta) {
            titulo = context.getString(R.string.push_alerta_titulo);
            texto = context.getString(R.string.push_alerta_texto, idosoNome, medicamento, Formatos.hora(previsto),
                    toleranciaMinutos, Formatos.hora(previsto.plusSeconds(toleranciaMinutos * 60L)));
        } else if (atraso) {
            titulo = context.getString(R.string.push_atraso_titulo);
            texto = context.getString(R.string.push_atraso_texto, idosoNome, medicamento, Formatos.hora(previsto),
                    Formatos.hora(confirmadoEm));
        } else {
            titulo = context.getString(R.string.push_confirmada_titulo);
            texto = context.getString(R.string.push_confirmada_texto, idosoNome, medicamento,
                    Formatos.hora(confirmadoEm));
        }
        if (SessionManager.get(context).isDemo()) {
            titulo = context.getString(R.string.prefixo_demo) + " " + titulo;
        }
        int id = (int) ((doseId * 2 + (alerta ? 1 : 0)) % Integer.MAX_VALUE);
        PendingIntent abrir = PendingIntent.getActivity(context, id,
                DoseDetalheActivity.intent(context, doseId, false),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder b = new NotificationCompat.Builder(context, alerta ? CANAL_ALERTAS : CANAL_CONFIRMACOES)
                .setSmallIcon(R.drawable.ic_notificacao)
                .setColor(ContextCompat.getColor(context, alerta ? R.color.alerta : R.color.confirmacao))
                .setContentTitle(titulo)
                .setContentText(texto)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(texto))
                .setPriority(alerta ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(alerta ? NotificationCompat.CATEGORY_REMINDER : NotificationCompat.CATEGORY_STATUS)
                .setContentIntent(abrir)
                .setAutoCancel(true);
        if (alerta) {
            b.addAction(R.drawable.ic_alerta, context.getString(R.string.ver_situacao_atual), abrir);
            PendingIntent registrar = PendingIntent.getActivity(context, id + 1,
                    DoseDetalheActivity.intent(context, doseId, true),
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            b.addAction(R.drawable.ic_check, context.getString(R.string.registrar_tomada), registrar);
        }
        try {
            NotificationManagerCompat.from(context).notify(TAG_CUIDADOR, id, b.build());
        } catch (SecurityException semPermissao) {
        }
    }
}
