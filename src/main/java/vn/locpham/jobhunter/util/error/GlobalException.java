package vn.locpham.jobhunter.util.error;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import vn.locpham.jobhunter.domain.RestResponse;

@RestControllerAdvice
public class GlobalException {
    @ExceptionHandler(value = {
            UsernameNotFoundException.class,
            BadCredentialsException.class
    })
    public ResponseEntity<RestResponse<Object>> handleBIdException(Exception ex) {
        RestResponse<Object> res = new RestResponse<Object>();
        res.setStatus(HttpStatus.BAD_REQUEST.value());
        res.setError(ex.getMessage());
        res.setMessage("Exception occurs...");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RestResponse<Object>> validationError(MethodArgumentNotValidException ex) {
        BindingResult result = ex.getBindingResult();
        final List<FieldError> filedErrors = result.getFieldErrors();
        RestResponse<Object> res = new RestResponse<Object>();
        res.setStatus(HttpStatus.BAD_REQUEST.value());
        res.setError(ex.getBody().getDetail());

        List<String> errors = filedErrors.stream().map(f -> f.getDefaultMessage()).collect(Collectors.toList());
        res.setMessage(errors.size() > 1 ? errors : errors.get(0));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(res);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<RestResponse<Object>> handleAuthenticationException(AuthenticationException ex) {
        RestResponse<Object> res = new RestResponse<>();
        res.setStatus(HttpStatus.UNAUTHORIZED.value()); // 401
        res.setError(ex.getMessage());
        res.setMessage("Sai username hoặc password");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(res);
    }

}
