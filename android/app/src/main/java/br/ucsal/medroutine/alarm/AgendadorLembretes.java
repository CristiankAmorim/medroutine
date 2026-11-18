package br.ucsal.medroutine.alarm;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import br.ucsal.medroutine.data.local.AppDatabase;
import br.ucsal.medroutine.data.local.DoseLocal;
import br.ucsal.medroutine.domain.RegrasDose;
import br.ucsal.medroutine.session.SessionManager;

public final class AgendadorLembretes {
    public static final String ACAO_LEMBRETE = "br.ucsal.medroutine.LEMBRETE";
    public static final String EXTRA_DOSE_ID = "doseId";

    private static final String PREFS = "medroutine_alarmes";
    private static final String CHAVE_AGENDADOS = "agendados";

    private AgendadorLembretes() { }

    public static boolean podeAgendarExato(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true;
        }
        AlarmManager am = context.getSystemService(AlarmManager.class);
        return am != null && am.canScheduleExactAlarms();
    }

    public static void reagendarTodos(Context context) {
        SessionManager sessao = SessionManager.get(context);
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        Set<String> anteriores = new HashSet<>(prefs.getStringSet(CHAVE_AGENDADOS, new HashSet<>()));
        Set<String> agora = new HashSet<>();

        if (sessao.isLogado() && sessao.isIdoso()) {
            List<DoseLocal> futuras = AppDatabase.get(context).doses()
                    .naoConfirmadasApos(System.currentTimeMillis());
            for (DoseLocal d : futuras) {
                if (!d.isConfirmada()) {
                    agendar(context, d.id, d.previstaEm);
                    agora.add(String.valueOf(d.id));
                }
            }
        }
        for (String id : anteriores) {
            if (!agora.contains(id)) {
                cancelar(context, Long.parseLong(id));
            }
        }
        prefs.edit().putStringSet(CHAVE_AGENDADOS, agora).apply();
    }

    public static void adiar(Context context, long doseId) {
        agendar(context, doseId, System.currentTimeMillis() + RegrasDose.ADIAMENTO_LEMBRETE.toMillis());
    }

    public static void cancelar(Context context, long doseId) {
        AlarmManager am = context.getSystemService(AlarmManager.class);
        if (am != null) {
            am.cancel(pendingIntent(context, doseId));
        }
    }

    public static void cancelarTodos(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        for (String id : prefs.getStringSet(CHAVE_AGENDADOS, new HashSet<>())) {
            cancelar(context, Long.parseLong(id));
        }
        prefs.edit().remove(CHAVE_AGENDADOS).apply();
    }

    static void agendar(Context context, long doseId, long quando) {
        AlarmManager am = context.getSystemService(AlarmManager.class);
        if (am == null) {
            return;
        }
        PendingIntent pi = pendingIntent(context, doseId);
        try {
            if (podeAgendarExato(context)) {
                AlarmManager.AlarmClockInfo info = new AlarmManager.AlarmClockInfo(quando, telaInicial(context));
                am.setAlarmClock(info, pi);
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quando, pi);
            }
        } catch (SecurityException e) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quando, pi);
        }
    }

    private static PendingIntent telaInicial(Context context) {
        Intent abrir = new Intent(context, br.ucsal.medroutine.ui.inicio.SplashActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return PendingIntent.getActivity(context, 0, abrir,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent pendingIntent(Context context, long doseId) {
        Intent intent = new Intent(context, LembreteReceiver.class)
                .setAction(ACAO_LEMBRETE)
                .setData(Uri.parse("medroutine://lembrete/" + doseId))
                .putExtra(EXTRA_DOSE_ID, doseId);
        return PendingIntent.getBroadcast(context, (int) (doseId % Integer.MAX_VALUE), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
