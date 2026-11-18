package br.ucsal.medroutine.web;

import br.ucsal.medroutine.domain.DoseJaConfirmadaException;
import br.ucsal.medroutine.domain.ValidacaoException;
import br.ucsal.medroutine.service.ApiException;
import br.ucsal.medroutine.web.dto.ErroDTO;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErroDTO> api(ApiException e) {
        return ResponseEntity.status(e.getStatus()).body(new ErroDTO(e.getMessage(), null));
    }

    @ExceptionHandler(ValidacaoException.class)
    public ResponseEntity<ErroDTO> validacao(ValidacaoException e) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ErroDTO(e.getMessage(), e.getCampo()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroDTO> beanValidation(MethodArgumentNotValidException e) {
        var erro = e.getBindingResult().getFieldErrors().stream().findFirst();
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(new ErroDTO(
                erro.map(f -> f.getDefaultMessage()).orElse("Dados inválidos."),
                erro.map(f -> f.getField()).orElse(null)));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErroDTO> formato(Exception e) {
        return ResponseEntity.badRequest().body(new ErroDTO("Dados em formato inválido.", null));
    }

    @ExceptionHandler(DoseJaConfirmadaException.class)
    public ResponseEntity<ErroDTO> jaConfirmada(DoseJaConfirmadaException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErroDTO(e.getMessage(), "dose"));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroDTO> integridade(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErroDTO("A operação conflita com um registro existente.", null));
    }
}
