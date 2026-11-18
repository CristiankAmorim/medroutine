package br.ucsal.medroutine.ui.comum;

import android.app.Activity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;

import br.ucsal.medroutine.data.model.ErroApi;

public final class Ui {
    private Ui() { }

    public static String texto(TextView campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }

    public static void mensagem(View raiz, String texto) {
        Snackbar.make(raiz, texto, Snackbar.LENGTH_LONG).show();
    }

    public static void erro(View raiz, ErroApi erro) {
        mensagem(raiz, erro.mensagem);
    }

    public static void erroNoCampo(View raiz, ErroApi erro, @Nullable TextInputLayout campo) {
        if (campo != null) {
            campo.setError(erro.mensagem);
            campo.requestFocus();
        } else {
            mensagem(raiz, erro.mensagem);
        }
    }

    public static void limparErros(TextInputLayout... campos) {
        for (TextInputLayout c : campos) {
            c.setError(null);
        }
    }

    public static void esconderTeclado(Activity activity) {
        View v = activity.getCurrentFocus();
        if (v != null) {
            InputMethodManager imm = activity.getSystemService(InputMethodManager.class);
            if (imm != null) {
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        }
    }

    public static void carregando(View botao, View progresso, boolean ativo) {
        if (botao != null) botao.setEnabled(!ativo);
        if (progresso != null) progresso.setVisibility(ativo ? View.VISIBLE : View.GONE);
    }
}
