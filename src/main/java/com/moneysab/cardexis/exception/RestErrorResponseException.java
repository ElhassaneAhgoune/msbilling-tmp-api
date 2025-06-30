package com.moneysab.cardexis.exception;

import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import static org.springframework.http.HttpStatusCode.valueOf;

public class RestErrorResponseException extends ErrorResponseException {

    public RestErrorResponseException(final ProblemDetail problemDetail) {
        super(valueOf(problemDetail.getStatus()), problemDetail, null);
    }

}