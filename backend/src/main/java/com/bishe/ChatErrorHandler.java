package com.bishe;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
@RestControllerAdvice(assignableTypes=ChatController.class)
class ChatErrorHandler {
 @ExceptionHandler(ResponseStatusException.class)
 ResponseEntity<Map<String,String>> unavailable(ResponseStatusException e) {
   return ResponseEntity.status(e.getStatusCode()).body(Map.of("status","unavailable","message",e.getReason()==null?"问答服务暂时不可用。":e.getReason().strip()));
 }
}
