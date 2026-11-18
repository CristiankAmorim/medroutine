package br.ucsal.medroutine.service;

import java.util.Map;

public interface NotificationSender {
    void enviar(String tokenAparelho, Map<String, String> dados) throws EnvioNotificacaoException;

    class EnvioNotificacaoException extends Exception {
        private static final long serialVersionUID = 1L;

        private final boolean temporaria;

        public EnvioNotificacaoException(String mensagem, boolean temporaria, Throwable causa) {
            super(mensagem, causa);
            this.temporaria = temporaria;
        }

        public boolean isTemporaria() {
            return temporaria;
        }
    }
}
