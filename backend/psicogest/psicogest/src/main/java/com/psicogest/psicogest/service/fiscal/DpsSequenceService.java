package com.psicogest.psicogest.service.fiscal;

import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.psicogest.psicogest.exception.FiscalConfigurationException;

@Service
public class DpsSequenceService {

    private static final String SERIES = "1";
    private final JdbcTemplate jdbcTemplate;

    public DpsSequenceService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public long next(UUID fiscalIssuerId) {
        try {
            jdbcTemplate.update("""
                    insert into dps_sequences (fiscal_issuer_id, series, next_number, updated_at)
                    values (?, ?, 2, current_timestamp)
                    on conflict (fiscal_issuer_id, series) do nothing
                    """, fiscalIssuerId, SERIES);
            Long number = jdbcTemplate.queryForObject(
                    "select next_number from dps_sequences where fiscal_issuer_id = ? and series = ? for update",
                    Long.class, fiscalIssuerId, SERIES);
            if (number == null || number < 1) {
                throw new FiscalConfigurationException("Sequência DPS inválida");
            }
            jdbcTemplate.update("update dps_sequences set next_number = ?, updated_at = current_timestamp where fiscal_issuer_id = ? and series = ?",
                    number + 1, fiscalIssuerId, SERIES);
            return number;
        } catch (DataAccessException exception) {
            throw new FiscalConfigurationException("Sequência DPS não configurada para o emissor fiscal");
        }
    }
}
