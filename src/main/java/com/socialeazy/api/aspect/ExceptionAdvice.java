package com.socialeazy.api.aspect;



import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

@Slf4j
@RestControllerAdvice
public class ExceptionAdvice {
    @ExceptionHandler(BadRequestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Problem onBadRequestException(BadRequestException e) {
        log.error("onBadRequestException ::{} ::{}", e.getMessage(), e);
        return Problem.builder()
                .withTitle("Invalid Request")
                .withDetail(e.getMessage())
                .withStatus(Status.BAD_REQUEST)
                .build();
    }

    @ExceptionHandler(RecordNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Problem onRecordNotFoundException(RecordNotFoundException e) {
        log.error("onRecordNotFoundException ::{} ::{}", e.getMessage(), e);
        return Problem.builder()
                .withTitle("Invalid Request")
                .withDetail(e.getMessage())
                .withStatus(Status.NOT_FOUND)
                .build();
    }

    @ExceptionHandler(FileParsingException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public Problem onFileParsingException(FileParsingException e) {
        log.error("onFileParsingException ::{} ::{}", e.getMessage(), e);
        return Problem.builder()
                .withTitle("Unknown Error, try again")
                .withDetail(e.getMessage())
                .withStatus(Status.UNPROCESSABLE_ENTITY)
                .build();
    }

    @ExceptionHandler(ForbiddenException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public Problem onForbiddenException(ForbiddenException e) {
        log.error("onForbiddenException ::{} ::{}", e.getMessage(), e);
        return Problem.builder()
                .withTitle("Access Restricted")
                .withDetail(e.getMessage())
                .withStatus(Status.FORBIDDEN)
                .build();
    }

}
