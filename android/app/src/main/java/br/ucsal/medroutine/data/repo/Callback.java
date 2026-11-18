package br.ucsal.medroutine.data.repo;

import br.ucsal.medroutine.data.model.ErroApi;

public interface Callback<T> {
    void sucesso(T resultado);

    void erro(ErroApi erro);
}
