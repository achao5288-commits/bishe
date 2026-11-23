package com.bishe;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController @RequestMapping("/api/evaluation")
class FeedbackController {
 private static final String UUID_RE="^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
 record ExposureRequest(@NotBlank @Pattern(regexp=UUID_RE) String userId,@NotEmpty @Size(max=20) List<@NotNull @Min(1) Long> attractionIds){}
 record ExposureResponse(String id){}
 record FeedbackRequest(@NotBlank @Pattern(regexp=UUID_RE) String userId,@NotNull @Min(1) Long attractionId,@Pattern(regexp=UUID_RE) String exposureId,@NotNull @Min(1) @Max(5) Integer rating,@Size(max=500) String comment){}
 record DeleteRequest(@NotBlank @Pattern(regexp=UUID_RE) String userId){}
 private final FeedbackStore service;private final GraphService graph;
 FeedbackController(FeedbackStore service,GraphService graph){this.service=service;this.graph=graph;}
 private Set<Long> validIds(){return graph.attractions(null).stream().map(GraphService.Attraction::id).collect(Collectors.toSet());}
 @PostMapping("/exposures") ExposureResponse exposure(@Valid @RequestBody ExposureRequest request,java.security.Principal principal){if(!validIds().containsAll(request.attractionIds()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"包含未知景点");return new ExposureResponse(service.exposure(principal.getName(),request.attractionIds()).id());}
 @PostMapping("/feedback") @ResponseStatus(HttpStatus.NO_CONTENT) void feedback(@Valid @RequestBody FeedbackRequest request,java.security.Principal principal){if(!validIds().contains(request.attractionId()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"景点不存在");try{service.feedback(principal.getName(),request.attractionId(),request.exposureId(),request.rating(),request.comment());}catch(IllegalArgumentException e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,e.getMessage());}}
 @GetMapping("/summary") FeedbackService.Summary summary(){return service.summary();}
 @PostMapping("/delete-own") @ResponseStatus(HttpStatus.NO_CONTENT) void delete(@Valid @RequestBody DeleteRequest request,java.security.Principal principal){service.delete(principal.getName());}
}
