package com.bishe;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
class FeedbackServiceTest {
 @TempDir Path temp;
 @Test void persistsFeedbackAndOnlyCountsJudgedExposurePositions() {
  var file=temp.resolve("feedback.json");var service=new FeedbackService(file,new ObjectMapper());
  var e=service.exposure("user-a",List.of(1L,2L,3L));
  service.feedback("user-a",2,e.id(),5,"值得参观");
  service.feedback("user-a",2,e.id(),4,"更正");
  service.feedback("user-b",1,null,1,"");
  var read=new FeedbackService(file,new ObjectMapper()).summary();
  assertEquals(2,read.ratings());assertEquals(2,read.participants());assertEquals(1,read.exposures());
  assertEquals(1,read.judgedPositions());assertEquals(1,read.positivePositions());assertEquals(1.0,read.observedPositiveRate());assertEquals(2.5,read.averageRating());
 }
 @Test void rejectsFeedbackForAnUnseenOrOtherUsersExposure(){
  var service=new FeedbackService(temp.resolve("x.json"),new ObjectMapper());var e=service.exposure("a",List.of(1L,2L));
  assertThrows(IllegalArgumentException.class,()->service.feedback("b",1,e.id(),5,""));
  assertThrows(IllegalArgumentException.class,()->service.feedback("a",3,e.id(),5,""));
 }
 @Test void noFeedbackHasNullRatesAndDeletingRemovesOnlyOwnData(){
  var service=new FeedbackService(temp.resolve("x.json"),new ObjectMapper());service.exposure("a",List.of(1L));service.feedback("b",1,null,3,"");
  assertNull(service.summary().observedPositiveRate());service.delete("a");assertEquals(0,service.summary().exposures());assertEquals(1,service.summary().ratings());
 }
}
