package com.bishe;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
@Service
public class FeedbackService {
 record Exposure(String id,String userId,List<Long> attractionIds,long timestamp) {}
 record Feedback(String userId,long attractionId,String exposureId,int rating,String comment,long timestamp) {}
 record Summary(int ratings,int participants,int exposures,int judgedPositions,int positivePositions,Double observedPositiveRate,Double averageRating) {}
 record Data(List<Exposure> exposures,List<Feedback> feedback) {}
 private final Path file; private final ObjectMapper mapper; private Data data;
 @Autowired FeedbackService(@Value("${EVALUATION_FILE:./data/evaluation.json}") String file,ObjectMapper mapper){this(Path.of(file),mapper);}
 FeedbackService(Path file,ObjectMapper mapper){this.file=file.toAbsolutePath();this.mapper=mapper;try{data=Files.exists(file)?mapper.readValue(file.toFile(),Data.class):new Data(List.of(),List.of());if(data.exposures()==null||data.feedback()==null)throw new IOException("Invalid store");}catch(IOException e){throw new IllegalStateException("Cannot read feedback store",e);}}
 private void persist(Data next){
  try {Path parent=file.getParent();Files.createDirectories(parent);Path temp=Files.createTempFile(parent,".feedback-",".tmp");
   try{mapper.writeValue(temp.toFile(),next);try{Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(temp);}
   data=next;
  }catch(IOException e){throw new IllegalStateException("Cannot save feedback store",e);}
 }
 synchronized Exposure exposure(String userId,List<Long> ids){
  if(data.exposures().size()>=100000)throw new IllegalStateException("Feedback store is full");
  var exposure=new Exposure(UUID.randomUUID().toString(),userId,List.copyOf(new LinkedHashSet<>(ids)),System.currentTimeMillis());
  var rows=new ArrayList<>(data.exposures());rows.add(exposure);persist(new Data(rows,data.feedback()));return exposure;
 }
 synchronized void feedback(String userId,long attractionId,String exposureId,int rating,String comment){
  if(exposureId!=null && data.exposures().stream().noneMatch(x->x.id().equals(exposureId)&&x.userId().equals(userId)&&x.attractionIds().contains(attractionId)))throw new IllegalArgumentException("评分不属于该次推荐展示");
  var rows=new ArrayList<>(data.feedback());rows.removeIf(x->x.userId().equals(userId)&&x.attractionId()==attractionId&&Objects.equals(x.exposureId(),exposureId));
  if(rows.size()>=100000)throw new IllegalStateException("Feedback store is full");
  rows.add(new Feedback(userId,attractionId,exposureId,rating,comment==null?"":comment.trim(),System.currentTimeMillis()));persist(new Data(data.exposures(),rows));
 }
 synchronized Summary summary(){
  Set<String> users=new HashSet<>();data.exposures().forEach(x->users.add(x.userId()));data.feedback().forEach(x->users.add(x.userId()));
  Map<String,Exposure> exposuresById=new HashMap<>();data.exposures().forEach(e->exposuresById.put(e.id(),e));
  List<Feedback> judged=data.feedback().stream().filter(x->{Exposure e=exposuresById.get(x.exposureId());return e!=null && e.userId().equals(x.userId()) && e.attractionIds().contains(x.attractionId());}).toList();
  int positive=(int)judged.stream().filter(x->x.rating()>=4).count();
  Double rate=judged.isEmpty()?null:(double)positive/judged.size();
  Double avg=data.feedback().isEmpty()?null:data.feedback().stream().mapToInt(Feedback::rating).average().orElseThrow();
  return new Summary(data.feedback().size(),users.size(),data.exposures().size(),judged.size(),positive,rate,avg);
 }
 synchronized void delete(String userId){persist(new Data(data.exposures().stream().filter(x->!x.userId().equals(userId)).toList(),data.feedback().stream().filter(x->!x.userId().equals(userId)).toList()));}
}
