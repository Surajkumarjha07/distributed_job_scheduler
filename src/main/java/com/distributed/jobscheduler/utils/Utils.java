package com.distributed.jobscheduler.utils;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

public class Utils {
    public static String extractSQLState(DataIntegrityViolationException ex) {
        Throwable cause = ex.getCause();

        if (cause instanceof ConstraintViolationException cve) {
            return cve.getSQLState();
        }

        if (ex.getMostSpecificCause() instanceof SQLException sqlEx) {
            return sqlEx.getSQLState();
        }

        return null;
    }
}
