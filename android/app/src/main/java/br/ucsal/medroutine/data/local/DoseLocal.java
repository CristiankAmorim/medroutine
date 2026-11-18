package br.ucsal.medroutine.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.time.Instant;

import br.ucsal.medroutine.data.model.Dose;
import br.ucsal.medroutine.domain.EstadoDose;

@Entity(tableName = "dose_local")
public class DoseLocal {
    @PrimaryKey
    public long id;
    public long idosoId;
    @NonNull
    public String medicamentoNome = "";
    public String dosagem;
    public String orientacoes;
    public long previstaEm;
    public int toleranciaMinutos;
    @NonNull
    public String estado = EstadoDose.PENDENTE.name();
    public Long confirmadaEm;
    public boolean aguardandoEnvio;
    @ColumnInfo(defaultValue = "0")
    public boolean peloCuidador;
    public String confirmadaPor;

    public static DoseLocal de(Dose d) {
        DoseLocal l = new DoseLocal();
        l.id = d.id;
        l.idosoId = d.idosoId;
        l.medicamentoNome = d.medicamentoNome == null ? "" : d.medicamentoNome;
        l.dosagem = d.dosagem;
        l.orientacoes = d.orientacoes;
        l.previstaEm = d.dataHoraPrevista.toEpochMilli();
        l.toleranciaMinutos = d.toleranciaMinutos;
        l.estado = d.estado;
        l.confirmadaEm = d.confirmacao != null && d.confirmacao.confirmadoEm != null
                ? br.ucsal.medroutine.domain.RegrasDose.momentoDaConfirmacao(d.dataHoraPrevista,
                        d.confirmacao.confirmadoEm, d.confirmacao.horarioAparelho).toEpochMilli()
                : null;
        if (d.confirmacao != null) {
            l.peloCuidador = d.confirmacao.peloCuidador;
            l.confirmadaPor = d.confirmacao.confirmadoPor;
        }
        return l;
    }

    public Instant previsto() {
        return Instant.ofEpochMilli(previstaEm);
    }

    public EstadoDose estadoDose() {
        return EstadoDose.valueOf(estado);
    }

    public boolean isConfirmada() {
        return estadoDose().isConfirmada();
    }
}
