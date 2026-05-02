package org.openmrs.module.doctor.ui.api.globalexp;

import org.openmrs.module.doctor.ui.api.api.impl.exp.BeanValidationException;
import org.openmrs.module.doctor.ui.api.api.impl.exp.DuplicateEntryException;
import org.openmrs.module.doctor.ui.api.api.impl.exp.InvalidInputParamException;
import org.openmrs.module.doctor.ui.api.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {
	
	@ExceptionHandler({
            BeanValidationException.class})
    public ResponseEntity<?> badRequest(BeanValidationException ex, HttpServletRequest request) {
        ex.printStackTrace();
        ApiResponse errRes = new ApiResponse();
        errRes.setStatus(HttpStatus.BAD_REQUEST.value());
        errRes.setMessage(ex.getMessage());
        return new ResponseEntity<>(errRes, HttpStatus.BAD_REQUEST);
    }
	
	@ExceptionHandler({
            DuplicateEntryException.class})
    public ResponseEntity<?> badRequest(DuplicateEntryException ex, HttpServletRequest request) {
        ex.printStackTrace();
        ApiResponse errRes = new ApiResponse();
        errRes.setStatus(HttpStatus.BAD_REQUEST.value());
        errRes.setMessage(ex.getMessage());
        return new ResponseEntity<>(errRes, HttpStatus.BAD_REQUEST);
    }
	
	@ExceptionHandler({
            InvalidInputParamException.class})
    public ResponseEntity<?> badRequest(InvalidInputParamException ex, HttpServletRequest request) {
        ex.printStackTrace();
        ApiResponse errRes = new ApiResponse();
        errRes.setStatus(HttpStatus.BAD_REQUEST.value());
        errRes.setMessage(ex.getMessage());
        return new ResponseEntity<>(errRes, HttpStatus.BAD_REQUEST);
    }
	//	@ExceptionHandler({
	//            Exception.class})
	//    public ResponseEntity<?> internalError(Exception ex, HttpServletRequest request) {
	//        ex.printStackTrace();
	//        ErrorResponse errRes = new ErrorResponse();
	//        errRes.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
	//        errRes.setMessage(ex.getMessage());
	//        return new ResponseEntity<>(errRes, HttpStatus.INTERNAL_SERVER_ERROR);
	//    }
}
