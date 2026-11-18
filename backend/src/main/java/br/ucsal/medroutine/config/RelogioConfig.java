package br.ucsal.medroutine.config;

import br.ucsal.medroutine.domain.Relogio;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RelogioConfig {
    @Bean
    public Relogio relogio() {
        return Relogio.sistema();
    }
}
