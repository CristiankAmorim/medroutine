package br.ucsal.medroutine.config;

import br.ucsal.medroutine.service.NotificationSender;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;
import java.util.Set;

@Configuration
public class FirebaseConfig {
    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    private static final Set<MessagingErrorCode> ERROS_TEMPORARIOS =
            EnumSet.of(MessagingErrorCode.UNAVAILABLE, MessagingErrorCode.INTERNAL, MessagingErrorCode.QUOTA_EXCEEDED);

    @Bean
    public NotificationSender notificationSender(@Value("${medroutine.firebase.credenciais:}") String credenciais)
            throws IOException {
        if (credenciais == null || credenciais.isBlank()) {
            log.warn("FIREBASE_CREDENTIALS não configurado: as notificações push serão apenas registradas no log.");
            return (token, dados) -> log.info("[push simulado] token={} dados={}", token, dados);
        }
        FirebaseApp app;
        try (InputStream in = new FileInputStream(credenciais)) {
            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(in))
                    .build();
            app = FirebaseApp.getApps().isEmpty() ? FirebaseApp.initializeApp(options) : FirebaseApp.getInstance();
        }
        FirebaseMessaging messaging = FirebaseMessaging.getInstance(app);
        return (token, dados) -> {
            Message message = Message.builder()
                    .setToken(token)
                    .putAllData(dados)
                    .setAndroidConfig(AndroidConfig.builder().setPriority(AndroidConfig.Priority.HIGH).build())
                    .build();
            try {
                messaging.send(message);
            } catch (FirebaseMessagingException e) {
                MessagingErrorCode codigo = e.getMessagingErrorCode();
                boolean temporaria = codigo == null || ERROS_TEMPORARIOS.contains(codigo);
                throw new NotificationSender.EnvioNotificacaoException(
                        (codigo == null ? "ERRO" : codigo.name()) + ": " + e.getMessage(), temporaria, e);
            }
        };
    }
}
