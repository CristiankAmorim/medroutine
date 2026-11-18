UPDATE dose_programada d
   SET estado = 'CONFIRMADA'
  FROM confirmacao_dose c, horario_medicacao h
 WHERE c.dose_id = d.id
   AND h.id = d.horario_id
   AND d.estado = 'CONFIRMADA_COM_ATRASO'
   AND c.horario_aparelho IS NOT NULL
   AND c.horario_aparelho <= c.confirmado_em + INTERVAL '2 minutes'
   AND c.horario_aparelho >= d.data_hora_prevista - INTERVAL '60 minutes'
   AND LEAST(c.horario_aparelho, c.confirmado_em)
       <= d.data_hora_prevista + make_interval(mins => h.tolerancia_minutos);
