package br.ucsal.medroutine.ui.cuidador;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import br.ucsal.medroutine.R;
import br.ucsal.medroutine.data.model.Codigo;
import br.ucsal.medroutine.ui.comum.Formatos;

final class Dialogos {
    private Dialogos() { }

    static void mostrarCodigo(Context c, int titulo, String explicacao, Codigo codigo) {
        View v = LayoutInflater.from(c).inflate(R.layout.dialog_codigo, null);
        ((TextView) v.findViewById(R.id.texto_explicacao)).setText(explicacao);
        ((TextView) v.findViewById(R.id.texto_codigo)).setText(codigo.codigo);
        ((TextView) v.findViewById(R.id.texto_validade)).setText(c.getString(R.string.codigo_valido_ate,
                Formatos.dataHora(codigo.expiraEm)));
        new MaterialAlertDialogBuilder(c)
                .setTitle(titulo)
                .setView(v)
                .setPositiveButton(R.string.ok, null)
                .setNeutralButton(R.string.compartilhar, (d, w) -> {
                    Intent enviar = new Intent(Intent.ACTION_SEND)
                            .setType("text/plain")
                            .putExtra(Intent.EXTRA_TEXT, explicacao + "\n\n" + codigo.codigo);
                    c.startActivity(Intent.createChooser(enviar, c.getString(R.string.compartilhar)));
                })
                .show();
    }
}
